# Requirements: Librio

**Defined:** 2026-08-19  
**Core Value:** Readers can confidently move from an information need to a usable library resource because its access type and current availability are clear and backed by the server.

## v1 Requirements

These requirements cover the full Librio MVP described in the D01 proposal. Sprint 1 is the current execution milestone, not the whole product.

### Sprint 1 Current Milestone

- [ ] **LIB-05**: The catalog represents a resource independently from zero or more physical items and an optional digital item so physical-only, digital-only, mixed, and zero-item resources can be discovered.
- [ ] **LIB-01**: Reader can browse a default list of library resources and open a selected resource's detail view.
- [ ] **LIB-02**: Reader can search resources by a trimmed keyword, retain the query in the URL, and receive an explicit empty result when nothing matches.
- [ ] **LIB-03**: Reader can view a resource's title, authors, description, and physical/digital access types, with a not-found result for an unknown resource.
- [ ] **LIB-04**: Reader can view server-derived availability: physical available and total copy counts, and digital availability when a digital item exists.
- [ ] **LIB-06**: The application exposes the Sprint 1 browse/search and resource-detail REST APIs using the canonical response and error shapes.
- [ ] **ENV-01**: From a clean clone, a developer can start the documented local Spring Boot, React/Vite, and PostgreSQL stack, load fixed demo seed data, and run the real-API Browse/Search -> Detail -> Availability evidence checks.

### Future Milestone Backlog

- [ ] **ACC-01**: A reader can be identified by an account to borrow a physical item and have the resulting loan associated with that reader.
- [ ] **LIB-07**: A reader can borrow an available physical item, receive a due date, and see the server-confirmed circulation and availability result without allowing a second active loan for the same item.
- [ ] **LIB-08**: A physical loan can be returned and the physical item becomes available again through a valid server-managed state transition.
- [ ] **LIB-09**: Readers and librarians can identify loans that have passed their due date as overdue without introducing fine or payment processing.
- [ ] **ACC-02**: A librarian can use an appropriately authorized account to access the minimum operational functions reserved for library staff.
- [ ] **LIB-10**: A reader can open or otherwise access a digital resource that is available to the library, with no expiry requirement in this MVP.
- [ ] **LIB-11**: A librarian can maintain the minimum resource, physical-item, digital-item, and circulation data needed to operate the MVP workflow.

## v2 Requirements

None defined. The explicitly excluded extensions below are not implicitly scheduled for v2.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Reservation/holds | Explicitly excluded from the selected MVP workflow. |
| Automatic location detection, IoT, or hardware integration | Availability is system-state based; physical location tracking is outside scope. |
| Payments, fines, billing, VIP, or subscriptions | The MVP records overdue state but does not handle money. |
| Complex DRM/licensing management or digital expiry | Digital access is intentionally non-expiring in this MVP. |
| Advanced AI recommendations | Discovery quality beyond basic browse/search is deferred. |
| Third-party integrations and advanced analytics/reporting | Do not expand the MVP beyond its core library workflow. |
| Public/cloud deployment for Sprint 1 | Sprint 1's required target is a stable, locally runnable full-stack demo. |

## Source Notes

- LIB-01 through LIB-06 use the frozen Sprint 1 SRS definitions in docs/srs/sprint-1-srs.md; Sprint 1 API behavior is governed by docs/lld/api-contracts/sprint-1-api.md.
- LIB-07 through LIB-11 remain future PBI boundaries in Mock/D02_Librio_Product_Backlog_WBS.xlsx and are tracked separately from the Sprint 1 milestone.
- ACC-01 and ACC-02 make the proposal's required user-account and librarian-operation scope checkable without choosing the deferred authentication mechanism.
- ENV-01 records the user-supplied Sprint 1 clean-clone success metric. It is a delivery requirement for the current milestone, not a claim that cloud deployment is required.
- The D02 backlog labels LIB-04 as Access Type and LIB-05 as Availability, while the frozen SRS uses LIB-04 Availability and LIB-05 Resource/Item Data Model. This roadmap follows the frozen SRS and canonical API contract; update D02 when the team reconciles the document naming drift.

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| LIB-05 | Phase 1 | In Review |
| LIB-01 | Phase 2 | Pending |
| LIB-02 | Phase 2 | Pending |
| LIB-03 | Phase 3 | Pending |
| LIB-04 | Phase 3 | Pending |
| LIB-06 | Phase 3 | Pending |
| ENV-01 | Phase 4 | Pending |

**Coverage:**

- Sprint 1 requirements: 7 total
- Mapped to phases: 7
- Future backlog: 7

---
*Requirements defined: 2026-08-19*  
*Last updated: 2026-08-19 after current milestone restructuring*
