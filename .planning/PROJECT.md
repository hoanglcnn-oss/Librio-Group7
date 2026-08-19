# Librio

## What This Is

Librio is an academic-library resource discovery and access platform for readers and librarians. It guides a reader from a need for information through browse/search, resource detail, and real availability to either borrowing a physical copy or accessing a digital resource; librarians maintain the catalog, item records, and circulation state that make that journey trustworthy.

Sprint 1 is only the first reviewable vertical slice of that product: Browse/Search → Resource Detail → Availability. It is not the full product definition.

## Core Value

Readers can confidently move from an information need to a usable library resource because its access type and current availability are clear and backed by the server.

## Business Context

- **Users**: Students/readers are the primary users; librarians are the operational users.
- **Service model**: Institutional/internal service platform for an academic or university library.
- **Current success metric**: A clean clone runs a stable local end-to-end Sprint 1 demo of Browse/Search → Resource Detail → Availability against the real backend API, fixed seed data, documented commands, and test/evidence output.

## Requirements

### Validated

None yet — the existing source documents are baselines and backlog records, not evidence of a verified running release.

### Active

- [ ] Phase 1: Deliver the Sprint 1 reader discovery and availability slice as a reproducible local full-stack demo.
- [ ] Phase 2: Let an identified reader borrow an available physical item with an authoritative server-side circulation update.
- [ ] Phase 3: Complete the physical circulation lifecycle with return and overdue handling.
- [ ] Phase 4: Let readers access digital resources and librarians perform the minimum operations needed to sustain catalog and circulation data.

The checkable source-of-truth list and phase mappings are in .planning/REQUIREMENTS.md.

### Out of Scope

- Reservation/hold workflows — explicitly excluded from the MVP scope.
- Automatic physical-location detection, IoT, or hardware integration — physical availability is derived from system state, not real-world location tracking.
- Payments, fines, subscriptions, VIP tiers, or billing — not part of the academic-library discovery-to-access workflow.
- Complex DRM/licensing management or digital-access expiry — digital access has no expiry in this MVP.
- Advanced AI recommendations, third-party integrations, and advanced reporting — extensions beyond the selected MVP workflow.
- Public/cloud deployment for Sprint 1 — the required Sprint 1 runtime is a stable local development and demo environment.

## Context

- The full MVP definition comes from Mock/proposal.docx and Mock/proposal_compressed.docx, with the product backlog in Mock/D02_Librio_Product_Backlog_WBS.xlsx.
- The design baseline is in docs/srs/, docs/database/, docs/hld/, and docs/lld/. The current repository only contains detailed SRS/LLD/API specifications for Sprint 1; later phases intentionally remain requirement-level until their Sprint-specific designs are created.
- The current rescue/review context is HH_DEADLINE_RUNBOOK_19-20_AUG.md. It defines Phase 1 execution and evidence priorities, not the complete Librio scope.
- docs/lld/api-contracts/sprint-1-api.md is the canonical API source of truth for Phase 1.
- Documentation alignment concern: Mock/D02_Librio_Product_Backlog_WBS.xlsx uses older labels for LIB-04 and LIB-05. For Sprint 1, this plan follows the frozen SRS meanings: LIB-04 Availability, LIB-05 Resource/Item Data Model, and LIB-06 API Foundation.

## Constraints

- **Sprint 1 runtime**: A local full-stack web environment of Spring Boot backend, React/Vite frontend, and PostgreSQL — it must run from a clean clone using documented commands.
- **Phase 1 integration**: The frontend must consume the real backend API; mock data must not be on the demo path.
- **Sprint 1 contract**: Use GET /resources?q={keyword} and GET /resources/{id} exactly as specified in docs/lld/api-contracts/sprint-1-api.md; do not add a version prefix or silently change JSON shapes.
- **Architecture**: Use a Spring Boot modular monolith with React SPA frontend, PostgreSQL persistence, and HTTP/REST JSON communication.
- **Backend boundaries**: Each backend module follows Controller → Service → Repository → DB.
- **Availability**: Compute availability on the server from PhysicalItem state and DigitalItem existence; do not create an Availability table or have the client recompute it.
- **Physical circulation**: One physical item may have at most one active borrowing. Borrowing must update the borrowing record and item state atomically; concurrency strategy is deferred until the detailed Sprint 2 design.
- **Security hygiene**: Do not commit secrets, real .env files, build artifacts, or dependency directories.

## Key Decisions

No ADR records or explicitly ADR-locked decisions were found in the provided materials, so no decisions are recorded as locked. The confirmed/frozen design baselines that currently constrain implementation are captured under **Constraints** and must be revisited through an ADR or updated specification if changed.

## Evolution

At each phase transition, update this file if the active scope, constraints, decisions, or product description change. Move verified requirements to the validated list only after implementation, manual verification, and evidence are complete.

---
*Last updated: 2026-08-19 after new-project intake and roadmap initialization*
