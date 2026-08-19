# HH DEADLINE RUNBOOK — 19–20/08

> Mở file này vào chiều 19/08. Đừng đọc lại toàn bộ chat. Chạy từ trên xuống.

## 0. Mission duy nhất

Tạo một **Sprint 1 Review Candidate có thể chứng minh** và push lên GitHub:

`Browse/Search → Resource Detail → Availability`

Không cứu thêm feature. Không làm hệ thống “đẹp”. Không làm Borrow/Return/Auth/Admin/Digital Viewer/AI/Reservation.

**Source of truth cho API:**

`docs/lld/api-contracts/sprint-1-api.md`

**P0 thật sự:** clean clone chạy local được, frontend gọi API thật, có test evidence và source đã push.

---

## 1. Trong 10 phút đầu: chụp trạng thái thật

- [ ] Pull/fetch repo, xem branch và thay đổi hiện có.
- [ ] Xác nhận PL đã push frontend ở đâu.
- [ ] Xác nhận KL đã tạo checklist/evidence ở đâu.
- [ ] Tìm xem có backend local chưa; nếu có thì chạy trước khi sửa.
- [ ] Đọc contract Sprint 1 và ghi các điểm chưa rõ thành assumption.
- [ ] Kiểm tra `.gitignore`; tuyệt đối không commit secret, `.env` thật, `node_modules`, `target`, `.idea`.

Ghi nhanh:

```text
Backend hiện tại:
Frontend branch:
API contract mismatch đang thấy:
Blocker P0:
```

Nếu repo vẫn zero-code, **đừng thiết kế lại kiến trúc**. Scaffold tối thiểu rồi đi thẳng đến endpoint.

---

## 2. Thứ tự chạy deadline của HH

Chỉ chuyển bước khi output trước đã chạy được.

### Gate A — Backend start được

- [ ] Spring Boot project start thành công.
- [ ] PostgreSQL/config local hoạt động.
- [ ] Có config mẫu hoặc hướng dẫn biến môi trường; không commit credential thật.
- [ ] Có một health/startup proof trong terminal.

**Nếu kẹt DB quá 30–45 phút:** ưu tiên cách cấu hình local đơn giản nhất phù hợp docs. Không Docker hóa phức tạp, không cloud DB.

### Gate B — Data đủ để demo

- [ ] Schema/entity tối thiểu cho `Resource`, `PhysicalItem`, `DigitalItem`.
- [ ] Seed ổn định bốn case:
  - physical còn bản;
  - physical hết bản;
  - digital-only;
  - physical + digital.
- [ ] Seed có ID/title cố định để demo và test lặp lại được.

### Gate C — API đúng contract

- [ ] `GET /resources`
- [ ] `GET /resources?q={keyword}`
- [ ] Empty/whitespace query hoạt động như browse.
- [ ] Search match title/author theo assumption đã khóa.
- [ ] `GET /resources/{id}`
- [ ] ID không tồn tại trả 404 đúng nghĩa.
- [ ] DTO trả đúng `accessTypes`, `physical.totalCopies`, `physical.availableCopies`, `digital.available`.
- [ ] Không expose JPA entity trực tiếp.
- [ ] Không tự thêm `/api/v1` hoặc đổi JSON shape.

Tự test bằng `curl` trước khi gọi PL integrate.

### Gate D — Handoff cho PL

Gửi đúng một block text:

```text
API ready:
Branch/commit:
Base URL:
Run command:
Endpoints tested:
Known issue/assumption:
```

- [ ] CORS cho frontend local.
- [ ] PL gọi được API thật.
- [ ] Nếu mismatch: sửa implementation theo contract; không sửa contract cho khớp code một cách âm thầm.

### Gate E — Integration Candidate

- [ ] Browse dùng real API.
- [ ] Search có result và no-result.
- [ ] Detail hợp lệ.
- [ ] Detail 404.
- [ ] Physical-only render đúng.
- [ ] Out-of-stock render đúng.
- [ ] Digital-only render đúng.
- [ ] Mixed render đúng.
- [ ] Mock không còn nằm trên demo path.

### Gate F — Pushable Review Candidate

- [ ] Backend build/test pass.
- [ ] Frontend build pass.
- [ ] Không có secret hoặc build artifact trong diff.
- [ ] README có cách chạy từ clean clone.
- [ ] Có seed/demo IDs và `curl` mẫu.
- [ ] KL nhận được commit để chạy test.
- [ ] D02 Actual/Deviation Log có thể cập nhật từ trạng thái thật.
- [ ] Push branch/commit rõ ràng; không gom một commit mơ hồ kiểu `update`.

---

## 3. Dùng Codex để speedrun — giao từng packet

Không prompt “làm toàn bộ Sprint 1”. Giao lần lượt:

1. Audit repo + contract + docs; chỉ báo cấu trúc và blocker.
2. Scaffold/start backend tối thiểu.
3. PostgreSQL config + schema/migration + seed bốn case.
4. Browse/search endpoint + tests.
5. Detail/availability + 404 + tests.
6. Contract-compliance audit bằng response thực tế.
7. CORS + frontend integration diagnosis.
8. README clean-run + `.gitignore`/secret audit.
9. Final build/test/diff review trước push.

### Prompt khung cho mỗi packet

```text
Task: [một output cụ thể, nhỏ]

Source of truth:
- docs/lld/api-contracts/sprint-1-api.md
- các SRS/HLD/DB spec hiện có trong repo

Constraints:
- Không đổi API contract.
- Không implement ngoài Sprint 1.
- Không thêm auth, borrow/return, admin, cloud deploy hoặc abstraction không cần thiết.
- Không sửa frontend/docs ngoài phạm vi task nếu chưa cần.
- Không commit secret.
- Không push, merge hoặc xóa thay đổi của người khác nếu tôi chưa yêu cầu.
- Bảo toàn mọi thay đổi đang có trong worktree.

Acceptance:
- [command cụ thể phải pass]
- [endpoint/response cụ thể phải đúng]

Hãy inspect trước, implement, rồi tự verify. Khi xong báo:
- Files changed
- Commands run
- Tests/results
- Assumptions
- Remaining blockers
```

### Trước khi cho Codex sửa tiếp

- [ ] Đọc `git diff`.
- [ ] Chạy command Codex nói đã pass.
- [ ] So response với contract, không chỉ tin test xanh.
- [ ] Nếu Codex thêm framework/abstraction lạ: hỏi nó chứng minh cần thiết; không cần thì bỏ.
- [ ] Mỗi packet chạy được mới sang packet sau.

---

## 4. Mốc cắt lỗ

| Khi nào | Nếu chưa đạt | Làm ngay |
|---|---|---|
| Chiều 19 | Backend chưa start | Dừng refactor/architecture; scaffold tối thiểu để start |
| Cuối chiều 19 | API chưa đủ | Làm `/resources` và `/{id}` bằng seed trước; bỏ mọi việc phụ |
| Tối 19 | Chưa integrate | Giữ backend + frontend độc lập chạy được, ghi mismatch chính xác cho sáng 20 |
| Trưa 20 | Chưa clean-run | Feature freeze tuyệt đối; chỉ sửa startup, config, DB, CORS, contract |
| Chiều 20 | Test còn fail | Chỉ fix P0; P1 ghi FAIL/PARTIAL trung thực |
| Tối 20 | Cloud deploy chưa xong | Bỏ deploy; dùng local demo ổn định + evidence dự phòng |

**Không bao giờ hy sinh vertical slice để cứu deployment, UI polish hoặc architecture.**

---

## 5. Khi nào được push?

Push feature branch sớm sau mỗi checkpoint chạy được; đừng giữ code local đến cuối ngày.

Một checkpoint đáng push khi:

- build/start được;
- diff chỉ chứa đúng phạm vi;
- không có secret;
- có commit message nói rõ outcome;
- có thể gửi kèm cách verify.

Ví dụ commit:

```text
feat(backend): scaffold Sprint 1 resource API
feat(resources): add browse and search endpoint
feat(resources): add detail availability and 404
test(resources): cover Sprint 1 seed cases
docs: add local run and demo instructions
fix(integration): allow frontend origin and align API contract
```

Không force-push branch chung. Không merge đè thay đổi PL/KL. Trước merge/rebase, fetch và kiểm tra branch/diff thật.

---

## 6. Final verification trước khi báo DONE

Chạy từ trạng thái gần clean clone nhất có thể:

- [ ] Backend start theo README.
- [ ] Frontend start theo README.
- [ ] DB được tạo/migrate/seed đúng hướng dẫn.
- [ ] Browse/search/detail/404 qua API thật.
- [ ] Bốn seed case hiển thị đúng.
- [ ] Backend tests pass.
- [ ] Frontend build pass.
- [ ] `git status` không còn file quan trọng chưa push.
- [ ] GitHub có đúng commit/branch review.
- [ ] KL có link và cách test.

Evidence tối thiểu:

- commit/branch link;
- commands đã chạy;
- test output;
- `curl` response mẫu;
- screenshot hoặc screen recording ngắn của happy path;
- known issues/PARTIAL/BLOCKED còn lại.

---

## 7. Dòng cuối cùng để tự kéo về đúng scope

Nếu đang phân vân làm một việc, hỏi:

> Việc này có trực tiếp giúp clean clone chạy được `Browse/Search → Detail → Availability` bằng API thật trước review không?

- **Có:** làm.
- **Không:** ghi backlog rồi bỏ.

Mục tiêu không phải chứng minh HH làm nhiều. Mục tiêu là GitHub có một phiên bản **chạy được, test được, và giải thích được**.
