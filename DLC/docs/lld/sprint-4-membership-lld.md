# Librio — Sprint 4 Membership Low-Level Design

**Scope:** US-16 membership eligibility, plans, mock payment and subscription lifecycle.
**Source:** T-161, T-162 and mentor clarification that payment must succeed before subscription creation.
**Related:** `api-contracts/sprint-4-api.md`.

## 1. Design goals

US-16 establishes the membership foundation: `eligibility → plan selection → payment transaction → successful payment → membership subscription`.
Payment and subscription are separate lifecycles. US-17 borrowing quota and US-18 digital entitlement consume membership later and are not implemented by US-16.

## 2. Domain model

```text
Account (membershipEligible)
MembershipPlan (code, name, durationMonths, priceAmount, currency, monthlyBorrowQuota, active)
PaymentTransaction (account, plan, amount, currency, status [SUCCESS | FAILED], createdAt, completedAt)
MembershipSubscription (account, plan, paymentTransaction, startsAt, expiresAt)
```
Only eligibility belongs directly to `Account`. Payment and subscription history remain separate entities.

## 3. Core rules

### 3.1 Activation lifecycle
Replaces `create PENDING subscription → payment → ACTIVE / FAILED` with:
```text
payment attempt
   ├── FAILED  → persist PaymentTransaction only
   └── SUCCESS → create MembershipSubscription
```
A failed payment never creates a subscription. A subscription may be created only when: authenticated account is `READER`, `membershipEligible = true`, selected plan exists and is active, payment transaction is `SUCCESS`, and no effective active membership exists.

### 3.2 Effective membership state
Derived from subscription existence and server time:
- `NONE`: no subscription
- `ACTIVE`: subscription exists AND `startsAt <= now` AND `expiresAt > now`
- `EXPIRED`: subscription exists AND `expiresAt <= now`
`PENDING` and `FAILED` are not subscription states. Failed payment may be exposed independently as the latest payment outcome.

### 3.3 Expiry and borrowing
Membership expiry blocks future membership-gated borrowing operations. It does not cancel, shorten or mutate an already active borrowing, which continues under its own `dueAt` / return lifecycle.

## 4. Persistence and concurrency design

### 4.1 Table schemas
- `accounts`: `membership_eligible BOOLEAN NOT NULL DEFAULT false`.
- `membership_plan`: `id`, `code` (UNIQUE), `name`, `duration_months`, `price_amount`, `currency`, `monthly_borrow_quota`, `active`.
- `payment_transaction`: `id`, `account_id`, `plan_id`, `amount`, `currency`, `status` (`SUCCESS` | `FAILED`), `created_at`, `completed_at`.
- `membership_subscription`: `id`, `account_id`, `plan_id`, `payment_transaction_id` (UNIQUE), `starts_at`, `expires_at` (`expires_at > starts_at`).

`payment_transaction_id UNIQUE` ensures one successful payment cannot create multiple subscriptions.

### 4.2 Concurrency control
Membership activation must execute within a backend transaction boundary. The server locks the account or equivalent membership activation boundary, validates effective membership inside the transaction, then creates payment/subscription records. Frontend button state is not a concurrency control.

## 5. API impact

Endpoints:
- `GET /membership/plans` — Public active plans.
- `GET /me/membership` — Reader membership view.
- `POST /me/membership-payments` — Accepts `{ "planId": 1, "outcome": "SUCCESS" }`.

Behavior:
- `FAILED`: persist failed `PaymentTransaction`, no `MembershipSubscription`.
- `SUCCESS`: persist successful `PaymentTransaction`, create `MembershipSubscription`, return effective membership.

| Condition | HTTP | Code |
| --- | ---: | --- |
| Account not eligible | 403 | `MEMBERSHIP_NOT_ELIGIBLE` |
| Plan missing or inactive | 404 | `MEMBERSHIP_PLAN_NOT_FOUND` |
| Active membership exists | 409 | `ACTIVE_MEMBERSHIP_EXISTS` |
| Concurrent activation conflict | 409 | `MEMBERSHIP_ACTIVATION_CONFLICT` |
| Invalid mock outcome | 400 | `INVALID_PAYMENT_OUTCOME` |

A simulated failed payment is a normal business outcome rather than a server failure.

## 6. Design decisions

- **D162-01 — Payment precedes subscription**: Create subscription only after successful payment. Prevents unfulfilled subscription lifecycle records.
- **D162-02 — Derive membership expiry**: Derive `ACTIVE` / `EXPIRED` from timestamps to eliminate dual sources of truth and scheduler dependency.
- **D162-03 — Do not mutate active borrowing on expiry**: Expiry affects new membership-gated operations only. Existing borrowing maintains its own due-date lifecycle.
- **D162-04 — Backend concurrency boundary**: Enforce membership activation consistency transactionally on the server rather than relying on UI button state.

## 7. Implementation boundary and completion criteria

- **T-163 — Membership persistence foundation**
  - Adds membership eligibility to `Account`.
  - Defines membership plans, payment transactions and membership subscriptions.
  - Enforces payment/subscription constraints and seed data.

- **T-164 — Membership backend APIs**
  - Implements `GET /membership/plans`.
  - Implements `GET /me/membership`.
  - Implements `POST /me/membership-payments`.
  - Membership ownership is derived from the authenticated principal.
  - Membership activation is enforced inside a backend transaction using an account-level locking boundary.
  - Failed payment persists only `PaymentTransaction`; successful payment creates the subscription.

- **T-165 — Reader membership UI**
  - Adds the protected reader `/membership` page.
  - Displays membership plans and effective `NONE`, `ACTIVE`, or `EXPIRED` status.
  - Supports Sprint 4 mock `SUCCESS` / `FAILED` payment outcomes.
  - Uses stable backend membership error mappings.

- **T-166 — Membership lifecycle integration**
  - Refreshes membership state from the backend after payment attempts rather than deriving activation locally.
  - Preserves server authority over `NONE`, `ACTIVE`, and `EXPIRED`.
  - Adds stale-response and unmount protection for membership requests.
  - Prevents redundant activation attempts in the UI while membership is already `ACTIVE`; this is only a UX guard and not a concurrency boundary.
  - Re-entry and direct navigation reload authoritative server state.

- **T-167 — Membership verification coverage**
  - Covers failed-payment retry followed by successful activation.
  - Covers `EXPIRED` state derivation without mutating the historical subscription.
  - Covers the invariant that membership expiry does not alter an already active borrowing.
  - Adds concurrent activation regression coverage using two worker threads targeting the same reader and plan.
  - The concurrency verification asserts the persisted invariant that only one membership subscription exists after competing activation attempts.

Automated backend verification coverage has been added for the membership lifecycle and concurrency invariants above. Backend test execution is not currently proven in the local environment because neither a Maven wrapper nor a globally available Maven executable was available.

Deployed end-to-end verification remains separate Sprint 4 evidence.