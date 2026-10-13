# Librio — Architecture

## 1. Mục đích

Tài liệu này mô tả architecture mục tiêu của Librio V2.

Nó trả lời:

- system gồm những component chính nào;
- responsibility của mỗi component;
- dependency đi theo hướng nào;
- transaction boundary nằm ở đâu;
- security được enforce ở đâu;
- physical circulation, digital access, metadata integration và paid access tách nhau thế nào;
- external systems được gắn vào đâu;
- phần nào của V1 cần được giữ, refactor hoặc tách responsibility.

Tài liệu này ở mức **High-Level Design**.

Chi tiết API contract hoặc flow phức tạp có thể được tách sang `api/`, `design/` hoặc ADR khi cần.

---

# 2. Architecture Context

Librio V2 tiếp tục là một full-stack web application:

```text id="kexgkf"
Browser
   |
   v
React SPA
   |
   | HTTP / JSON
   | Session Cookie + CSRF
   v
Spring Boot Backend
   |
   +------ PostgreSQL
   |
   +------ External Metadata Provider
   |
   +------ Payment Provider
   |
   +------ Digital Content Storage/Delivery
```

Core stack:

```text id="va9dhy"
Frontend
→ React + Vite

Backend
→ Spring Boot

Persistence
→ PostgreSQL

Authentication
→ Spring Security session

Deployment
→ containerized backend + deployable frontend
```

V2 không chuyển sang microservice architecture.

Một modular monolith vẫn phù hợp với product hiện tại.

---

# 3. Architecture Principles

V2 tuân theo các nguyên tắc:

1. Backend là source of truth cho business behavior.
2. Frontend không quyết định authorization, availability hoặc entitlement.
3. Transaction boundary nằm ở application/service layer.
4. Repository chỉ chịu trách nhiệm persistence/query.
5. External providers nằm sau boundary có thể thay thế.
6. Physical Circulation không phụ thuộc Paid Access.
7. Digital Access không phụ thuộc trực tiếp Payment Provider.
8. Không tạo service abstraction nếu chưa có responsibility rõ.
9. Không tách microservice chỉ để thể hiện architecture.
10. Mỗi module phải đại diện cho một business capability có thể giải thích được.

---

# 4. Top-level Components

```text id="saphnx"
┌─────────────────────────┐
│       React SPA         │
└────────────┬────────────┘
             │
             v
┌─────────────────────────┐
│   Spring Web / Security │
└────────────┬────────────┘
             │
             v
┌─────────────────────────────────────┐
│         Application Services        │
│                                     │
│ Catalog                             │
│ Inventory                           │
│ Circulation                         │
│ Digital Access                      │
│ Identity                            │
│ External Metadata                   │
│ Paid Access                         │
└────────────┬────────────────────────┘
             │
             v
┌─────────────────────────┐
│   Repository / JPA      │
└────────────┬────────────┘
             │
             v
┌─────────────────────────┐
│       PostgreSQL        │
└─────────────────────────┘
```

External adapters nằm cạnh application services:

```text id="s0oozg"
External Metadata Provider
Payment Provider
Digital Content Provider
```

---

# 5. Frontend Architecture

## 5.1 Responsibility

React SPA chịu trách nhiệm:

- routing;
- page composition;
- form state;
- user interaction;
- rendering server state;
- calling backend APIs;
- presenting error/status information.

Frontend không chứa authoritative business rules.

---

## 5.2 Routing

Current frontend có các nhóm route chính:

```text id="h0xgcg"
Public
→ /home
→ /resources
→ /resources/:id
→ /login

Reader
→ /my-library
→ paid-access UI if retained

Librarian
→ /librarian/requests
→ /librarian/cockpit
→ /librarian/resources/*
```

`ProtectedRoute` cải thiện UX nhưng không phải security boundary.

Backend vẫn phải enforce authorization.

---

## 5.3 API Client

Frontend API layer chịu trách nhiệm:

- API base URL;
- session credentials;
- CSRF token handling;
- common error parsing;
- endpoint calls.

V2 nên giảm việc gom quá nhiều capability vào một file API lớn nếu responsibility trở nên khó hiểu.

Target có thể chia theo capability:

```text id="b331pk"
api/
├── authApi
├── resourceApi
├── circulationApi
├── librarianApi
├── digitalApi
└── accessApi
```

Việc tách chỉ thực hiện khi code V2 refactor, không bắt buộc tạo abstraction trước nhu cầu.

---

## 5.4 Mock frontend adapters

V1 còn các DEV-only mock flags.

V2 target:

```text id="y1co3y"
backend available
→ backend là source of truth

temporary frontend mocks
→ DROP khi không còn use case
```

Mock fixtures vẫn có thể tồn tại trong unit/component tests, nhưng không nên trở thành runtime architecture song song với backend.

---

# 6. Backend Layering

V2 giữ layering đơn giản:

```text id="2h5sfe"
Controller
    |
    v
Application / Domain Service
    |
    v
Repository
    |
    v
Database
```

---

## 6.1 Controller

Controller chịu trách nhiệm:

- HTTP input/output;
- request validation cơ bản;
- authentication principal extraction;
- mapping status code;
- gọi application service.

Controller không nên:

- quyết định circulation transition;
- tự update entity;
- implement quota/entitlement logic;
- query repository trực tiếp cho business flow phức tạp.

---

## 6.2 Service

Service là application boundary cho business operation.

Ví dụ:

```text id="oybphh"
create borrow request
prepare request
fulfil request
return borrowing
update resource
resolve digital access
process payment result
```

Service chịu trách nhiệm:

- business validation;
- transaction boundary;
- orchestration;
- domain state transition;
- repository coordination.

---

## 6.3 Repository

Repository chịu trách nhiệm:

- persistence;
- lock/select;
- query optimization;
- existence/count query;
- retrieval theo use case.

Repository không nên sở hữu business decision.

Ví dụ:

```text id="x1o9ks"
find available item for update
```

là persistence concern.

Nhưng:

```text id="18hkvr"
reader có được borrow không?
```

không phải repository responsibility.

---

# 7. Identity & Security Architecture

## 7.1 Authentication

V2 giữ Spring Security session-based authentication.

```text id="sp4fpm"
login
→ authenticated Spring Security context
→ server session
→ JSESSIONID cookie
```

Password được hash bằng BCrypt.

---

## 7.2 CSRF

Vì V2 dùng session/cookie authentication:

```text id="3fzpm7"
state-changing request
→ CSRF protection
```

Frontend lấy CSRF token và gửi lại theo contract của backend.

---

## 7.3 Authorization

Authorization có hai lớp:

### Route/API role

Ví dụ:

```text id="6o8br9"
READER
→ reader operations

LIBRARIAN
→ librarian operations
```

### Resource ownership / business authorization

Ví dụ:

```text id="xwbkcb"
Reader A
→ không được cancel request của Reader B
```

SecurityConfig bảo vệ endpoint class lớn.

Application service vẫn phải enforce ownership và business-specific authorization.

---

## 7.4 Reader Type

`readerType` không thay thế Spring Security role.

```text id="27wh1m"
role
→ authorization capability

readerType
→ institutional/external business classification
```

Physical borrowing policy đọc `readerType`, không SecurityConfig.

---

# 8. Catalog Module

Catalog chịu trách nhiệm:

```text id="18d99j"
Resource
metadata
search/browse/detail
ISBN
metadata provenance
```

Các capability chính:

- resource browse;
- search;
- resource detail;
- librarian create/update;
- archive;
- ISBN normalization.

Catalog không chịu trách nhiệm circulation state của exact physical copy.

---

# 9. External Metadata Boundary

External metadata lookup nằm sau provider boundary.

Conceptual architecture:

```text id="e4mijp"
Resource Admin
      |
      v
Metadata Lookup Service
      |
      v
MetadataProvider
      |
      v
Google Books
```

Google Books là adapter hiện tại.

Domain/application code không nên phụ thuộc trực tiếp Google-specific response structures.

Expected flow:

```text id="j38e3u"
ISBN
→ provider lookup
→ verify exact identifier
→ normalized metadata result
→ editable frontend prefill
```

External metadata không tự persist resource.

---

# 10. Physical Inventory Module

Physical Inventory chịu trách nhiệm:

```text id="i9922n"
PhysicalItem
barcode
location
inventoryStatus
circulationStatus
inventory maintenance
```

Nó cung cấp capability cho Circulation như:

```text id="dmi5bv"
find available item
lock item
update circulation status
```

Inventory không quyết định reader eligibility.

---

# 11. Physical Circulation Module

Physical Circulation là technical core quan trọng nhất của V2.

Nó quản lý:

```text id="cd8bq4"
BorrowRequest
exact-copy reservation
Borrowing
due date
overdue
return
expiration
```

---

## 11.1 Target Responsibility

V1 hiện có `BorrowService` khá lớn nhưng responsibility về mặt domain vẫn coherent:

> Điều phối physical circulation lifecycle.

V2 không cần chia service chỉ để giảm số dòng.

Chỉ tách nếu xuất hiện responsibility độc lập rõ ràng.

Ví dụ có thể giữ:

```text id="6u5x8i"
CirculationService
```

cho lifecycle.

Các policy nhỏ có thể tách:

```text id="aq7lkl"
BorrowEligibilityPolicy
CirculationPolicy
```

nếu việc này làm dependency rõ hơn.

---

## 11.2 Borrow eligibility

V1 hiện có coupling:

```text id="ay4i4x"
BorrowService
→ BorrowingQuotaPolicy
→ MembershipSubscription
```

V2 phải loại bỏ dependency này.

Target:

```text id="2nx2cm"
CirculationService
        |
        v
BorrowEligibilityPolicy
        |
        +---- Account / readerType
        +---- active commitments
        +---- circulation configuration
```

Không:

```text id="tq8w3r"
CirculationService
→ AccessSubscription
→ Payment
```

---

## 11.3 Transaction Boundary

Các operation sau phải có transaction boundary rõ:

### Create request

```text id="xq7mh6"
lock reader if necessary
validate eligibility
validate duplicate
select + lock available item
reserve item
create BorrowRequest
COMMIT
```

### Fulfil

```text id="4qkt5n"
lock request
validate state
lock/validate reader and item
request → FULFILLED
item → BORROWED
create Borrowing
COMMIT
```

### Return

```text id="szp1zu"
lock Borrowing
lock exact PhysicalItem
set returnedAt
item → AVAILABLE
COMMIT
```

### Terminal request

```text id="fnhc9g"
cancel / reject / expire
→ request terminal
→ release exact reservation
→ COMMIT
```

---

# 12. Concurrency Strategy

V2 giữ database-assisted concurrency control.

Core approach:

```text id="mssm04"
transaction
+
pessimistic lock where contention matters
+
unique database constraints
```

Application check một mình không đủ.

Ví dụ allocation:

```text id="9mkdiz"
Request A
Request B
      |
      v
lock/select candidate item
      |
only one transaction claims the copy
```

Partial unique indexes tiếp tục là fallback integrity protection.

---

# 13. Expiration

BorrowRequest expiration hiện được xử lý bằng scheduled scanning.

Architecture:

```text id="7prxy7"
Scheduler
   |
   v
Circulation Service
   |
   v
find expired active requests FOR UPDATE
   |
   v
expire + release reservation
```

Expiration logic phải reuse cùng domain transition rule với interactive operation.

Scheduler không được bypass service invariant.

---

# 14. Digital Content Module

Digital Content chịu trách nhiệm:

```text id="hfdcgc"
DigitalItem
content references
preview/full distinction
delivery
```

DigitalItem không tự quyết định user entitlement.

---

# 15. Digital Access Architecture

V1 hiện có access decision gắn trực tiếp với active membership.

V2 target:

```text id="5e0bz3"
DigitalContentController
          |
          v
DigitalAccessService
          |
          +---- DigitalItem
          |
          +---- Account / readerType
          |
          +---- AccessSubscription (future/optional)
          |
          v
DigitalAccessResult
```

---

## 15.1 Access resolution

Conceptual flow:

```text id="x41e6l"
DigitalItem exists?
        |
        v
fullAccessPolicy
        |
        +---- PUBLIC
        |
        +---- INSTITUTIONAL
        |
        +---- RESTRICTED
```

Then:

```text id="n4aau2"
PUBLIC
→ FULL

INSTITUTIONAL
+ institutional reader
→ FULL

otherwise
+ preview exists
→ PREVIEW

otherwise
→ NONE
```

Future V3+ can insert:

```text id="w1mwct"
personal paid entitlement
```

without changing physical circulation.

---

# 16. Digital Content Delivery

V1 demo-generated PDF không phải target architecture.

Target boundary:

```text id="hhd847"
DigitalAccessService
        |
        v
DigitalContentService
        |
        v
ContentProvider / Storage Adapter
```

Possible adapters later:

```text id="r1nncn"
local/object storage
S3
Drive
external content platform
```

V2 không cần nhiều adapters.

Chỉ cần code không hard-wire domain vào generated demo PDF.

---

# 17. Paid Access Module

Paid Access là capability riêng:

```text id="nd1xn5"
AccessPlan
AccessSubscription
PaymentTransaction
PaymentProvider
```

Boundary:

```text id="logwg9"
Frontend
   |
   v
Access / Payment Controller
   |
   v
PaidAccessService
   |
   +---- AccessPlanRepository
   +---- AccessSubscriptionRepository
   +---- PaymentTransactionRepository
   |
   v
PaymentProvider
```

---

## 17.1 Payment Provider

VNPay là adapter hiện tại.

Application flow:

```text id="2fhfrc"
create payment attempt
→ persist PENDING transaction
→ build provider redirect
→ provider callback/return
→ verify/process result
→ SUCCESS / FAILED
→ create subscription when appropriate
```

Payment provider details phải nằm sau provider/service boundary.

---

## 17.2 Paid Access không đi vào Circulation

Dependency được phép:

```text id="uhpf5o"
DigitalAccessService
→ AccessSubscription
```

future.

Dependency không được phép:

```text id="dj5y2h"
CirculationService
→ PaidAccessService
```

chỉ để quyết định physical borrowing.

---

# 18. Resource Administration

V1 `ResourceAdminService` đang điều phối:

```text id="236wfr"
resource metadata
physical copy reconciliation
digital item reconciliation
```

Điều này chấp nhận được khi operation:

> Update one Resource aggregate

cần atomicity.

Nhưng V2 phải tránh biến nó thành God Service.

Target responsibility:

```text id="zs388g"
ResourceAdminService
→ orchestrate resource-level administration transaction
```

Nó có thể delegate:

```text id="aonhd6"
Catalog logic
Inventory logic
Digital content logic
```

khi các phần đủ phức tạp.

Không tách service chỉ vì class dài.

---

# 19. Persistence Architecture

JPA repositories kết nối application services với PostgreSQL.

```text id="5cnwwr"
Service
  |
  v
Repository
  |
  v
JPA/Hibernate
  |
  v
PostgreSQL
```

Repository custom query được phép cho:

- locking;
- partial-state query;
- pagination;
- inventory cockpit;
- efficient aggregate lookup.

Business policy không được nhúng sâu vào SQL nếu có thể giữ rõ ở service/policy layer.

---

# 20. Database Migration

V2 chuyển từ:

```text id="qh784x"
schema.sql on application startup
```

sang:

```text id="vsymlu"
Flyway
→ ordered versioned migrations
```

Application startup:

```text id="idd4k7"
start
→ run pending migrations
→ start application
```

Demo data không thuộc production migration mặc định.

---

# 21. Error Architecture

Backend nên có consistent error contract:

```text id="3z9xui"
status
code
message
timestamp
```

Business/service code throw domain/application exception có stable error code.

Global error handler hoặc security handler map exception sang HTTP response.

Frontend map stable code sang reader-friendly message.

Không để frontend phụ thuộc vào exact exception text.

---

# 22. Configuration Architecture

Environment-specific values được externalize.

Các nhóm config:

```text id="q5blbv"
Database
Security / cookie
CORS
Frontend origin
Circulation policy
Metadata provider
Payment provider
Digital storage
```

Không hard-code production secret.

---

# 23. Testing Architecture

V2 ưu tiên test theo capability.

```text id="hpusot"
Domain/service tests
        |
        v
API/integration tests
        |
        v
Selective real-PostgreSQL tests
```

Browser E2E không bắt buộc.

---

## 23.1 Fast tests

Dùng cho:

- state transition;
- validation;
- mapping;
- policy logic;
- controller contract khi phù hợp.

---

## 23.2 PostgreSQL integration tests

Dùng khi behavior phụ thuộc:

- pessimistic lock;
- concurrent allocation;
- partial unique indexes;
- migration;
- PostgreSQL-specific query/constraint.

Testcontainers PostgreSQL là candidate phù hợp.

---

# 24. Deployment Architecture

Target deployment đơn giản:

```text id="uxsxuh"
Frontend static hosting
        |
        v
Spring Boot container
        |
        v
Managed PostgreSQL
```

External:

```text id="lbof23"
Google Books
VNPay
Digital storage provider
```

Deployment vendor không phải architecture dependency.

Render/Neon có thể được dùng nhưng application phải cấu hình qua environment variables để có thể chuyển provider.

---

# 25. V2 Module Dependency Rules

Allowed dependency:

```text id="0ggm9n"
Frontend
→ Backend API

Controller
→ Service

Service
→ Repository

Circulation
→ Inventory

Digital Access
→ Digital Content

Digital Access
→ Paid Access (future)

Catalog
→ Metadata Provider

Paid Access
→ Payment Provider
```

Không nên có:

```text id="77fqj5"
Repository
→ Service

Inventory
→ Frontend

Circulation
→ Payment Provider

Physical borrowing
→ AccessSubscription

Google Books DTO
→ Resource domain model everywhere

VNPay-specific model
→ general payment domain
```

---

# 26. V1 → V2 Architecture Repair

Các repair chính:

```text id="9qar4x"
BorrowService
→ remove Membership/Quota dependency

BorrowingQuotaPolicy
→ DROP / replace with institutional circulation policy

MembershipService
→ reframe as PaidAccessService

MembershipPlan
→ AccessPlan

MembershipSubscription
→ AccessSubscription

DigitalAccessService
→ resolve public/institutional access
   instead of active-membership-only

demo PDF generation
→ replace with content delivery boundary

frontend runtime mock flags
→ remove when no longer needed

schema.sql bootstrapping
→ Flyway migrations
```

---

# 27. Architecture Decisions Cố Ý Không Làm Trong V2

V2 không:

- split backend thành microservices;
- add event bus;
- add generic entitlement engine;
- build multi-tenant architecture;
- add Institution service;
- add Elasticsearch;
- add distributed cache;
- add CQRS;
- add message queue;
- create separate payment microservice;
- create separate digital-content microservice.

Những công nghệ này chỉ được thêm khi requirement thực tế chứng minh cần.

---

# 28. Architecture Overview

```text id="s86act"
                        ┌─────────────────┐
                        │    React SPA    │
                        └────────┬────────┘
                                 │
                                 v
                     ┌────────────────────┐
                     │ Spring Web/Security│
                     └─────────┬──────────┘
                               │
        ┌──────────────────────┼─────────────────────┐
        │                      │                     │
        v                      v                     v
     Catalog               Circulation          Digital Access
        │                      │                     │
        │                      v                     v
        │                  Inventory          Digital Content
        │                                            │
        v                                            │
 Metadata Provider                                  │
                                                     │
                              Paid Access ───────────+
                                  │
                                  v
                           Payment Provider

        All persistence boundaries
                  |
                  v
              PostgreSQL
```

---

# 29. V2 Architecture Principles

1. Modular monolith trước microservices.
2. Backend là business source of truth.
3. Service sở hữu transaction và orchestration.
4. Repository sở hữu persistence/query, không sở hữu policy.
5. Database bảo vệ invariant phù hợp.
6. Circulation độc lập payment.
7. Digital entitlement độc lập content storage.
8. External provider nằm sau adapter boundary.
9. Security được enforce ở backend.
10. Concurrency là một phần của design, không phải edge case.
11. Architecture chỉ phức tạp khi product requirement yêu cầu.
12. V2 phải dễ giải thích, maintain và mở rộng theo vertical slice.