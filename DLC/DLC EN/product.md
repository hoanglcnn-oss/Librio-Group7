# Librio

## 1. Product Vision

Librio is an academic library system designed for university environments, supporting the discovery and access of both physical and digital resources.

Librio originally started as a Mock Project with an intentionally limited scope. The first version focused on completing one coherent workflow:

**Discover → View Details → Check Availability → Borrow Physical / Access Digital**

After the Mock Project, Librio continues as a personal project.

The immediate goal is not to add every possible library feature. Instead, the current implementation should first be converted into a codebase that can be understood, owned, maintained, and extended with confidence.

---

## 2. Why Librio Exists

A library resource is more than a catalog record.

Readers need to:

- discover resources;
- understand what a resource is;
- know which access forms are available;
- understand whether it can currently be accessed;
- borrow a physical copy or use a digital version.

At the same time, the library must maintain the information and state behind those resources consistently.

Librio focuses on connecting **resource discovery** with **actual resource access**, instead of treating search, availability, circulation, and digital access as unrelated features.

---

## 3. Product Context

Librio currently focuses on:

**Academic / University Libraries**

Keeping a specific context gives clearer foundations for decisions involving:

- resource types;
- circulation;
- access rules;
- user roles;
- library administration.

Librio does not currently aim to be a universal solution for every library context.

---

## 4. Primary Users

### 4.1 Reader / Student

Readers are the primary users of Librio.

Their main needs include:

- searching and discovering resources;
- viewing resource details;
- checking availability and access type;
- borrowing and returning physical resources;
- accessing digital resources;
- viewing account-related activity.

### 4.2 Librarian

Librarians maintain library data and support library operations.

Within the current scope, administrative capabilities only need to be sufficient to support the core product workflow.

Additional roles should be introduced only when a concrete requirement appears.

---

## 5. Core Product Workflow

The main Librio workflow is:

**Discover → Understand → Check Access → Use Resource**

Conceptually:

```text
Information Need
       ↓
Search / Browse
       ↓
Resource Detail
       ↓
Availability / Access Type
       ↓
 ┌───────────────┬───────────────┐
 │ Physical      │ Digital       │
 │               │               │
 │ Borrow/Return │ Access        │
 └───────────────┴───────────────┘
```

Other capabilities should support or extend this workflow instead of becoming isolated features without a clear relationship to the product core.

---

## 6. Product Evolution

### 6.1 V1 — Mock Project

V1 was developed in the context of a Mock Project.

Its primary goal was to complete an MVP with a scope small enough for the team to implement end-to-end.

V1 focused on:

- resource search and browsing;
- resource detail;
- availability and access type;
- physical borrowing;
- due dates, overdue status, and returns;
- basic digital access;
- user functionality required by the workflow;
- minimal librarian administration.

V1 also contains artifacts, organization choices, and implementation decisions influenced by:

- academic requirements;
- Mock Project sprint structure;
- deadlines;
- team-based development;
- decisions made primarily to complete the project on time.

These constraints are not automatically preserved after the Mock Project.

---

### 6.2 V2 — Personal Baseline

V2 is not a major feature expansion.

Its purpose is:

> **To convert the Mock Project implementation into a personal baseline that preserves useful product behavior and domain knowledge while removing unnecessary academic constraints and technical debt.**

V2 is primarily a phase of:

- review;
- refactoring;
- selective rebuilding;
- documentation repair;
- clarification of existing decisions.

Useful V1 behavior should remain functional in V2 unless an explicit product decision determines that it is no longer appropriate.

---

## 7. Current Product Baseline

V2 uses the useful behavior of V1 as its baseline.

The current scope includes:

- resource discovery;
- resource detail;
- availability;
- physical borrowing;
- due dates and overdue handling;
- returns;
- basic digital access;
- user functionality required by the core workflow;
- minimal librarian administration.

This list describes **existing behavior that should be preserved during the repair**, not the complete list of capabilities that Librio may eventually support.

---

## 8. Handling the V1 Implementation

Existing code is not preserved by default.

Each part of V1 should be evaluated using three possible outcomes.

### 8.1 Reuse As-Is

Keep an implementation when:

- its responsibility is clear;
- its behavior remains appropriate;
- its design does not introduce unnecessary coupling;
- the current maintainer is willing to own the code.

### 8.2 Reuse the Decision, Rebuild the Implementation

Preserve the business behavior or domain decision while rebuilding the code when:

- the original idea remains valid;
- the implementation contains meaningful technical debt;
- the design is heavily influenced by Mock Project constraints;
- rebuilding creates clearer responsibilities and boundaries.

This is expected to be the most common migration path from V1 to V2.

### 8.3 Drop

Remove parts that exist only because of:

- academic submission requirements;
- process requirements;
- deadline-driven shortcuts;
- obsolete assumptions;
- complexity that no longer provides value.

General principle:

> **Preserve valuable decisions, not artifacts simply because they already exist.**

---

## 9. V2 Definition of Done

V2 is considered complete when:

- the useful V1 core workflow still works end-to-end;
- current product requirements are documented clearly;
- important domain concepts and business rules are explicit;
- database design reflects the model the project intends to maintain;
- architecture clearly explains major responsibilities and dependencies;
- important technical debt has either been repaired or deliberately accepted;
- important flows have appropriate tests;
- the application can be configured and deployed reproducibly;
- the current codebase is one the developer is willing to maintain;
- adding a new capability no longer requires continuing unnecessary Mock Project constraints.

V2 does not need to predict or prepare implementations for every future feature.

---

## 10. Long-Term Product Direction

The original Library Management topic remains a **product horizon**, not the mandatory backlog for V2.

Future versions of Librio may progressively expand toward:

- richer catalog management;
- author and category management;
- stronger digital resource management;
- PDF, ebook, audio, and video support;
- moderation;
- reading and borrowing statistics;
- activity and history;
- reservation or other circulation extensions;
- broader librarian administration.

The implementation order is intentionally undecided.

Each capability should be analyzed when it becomes a real candidate for a future iteration.

---

### 10.1 Third-Party Integrations

Potential future integrations include:

- Google or social authentication;
- ISBN / Google Books APIs;
- email or other reminder channels;
- Google Drive;
- AWS S3 or equivalent object storage;
- appropriate viewer or media services.

Preferred principle:

> **Reuse commodity capabilities first. Internalize or replace them only when greater control, scale, or product differentiation justifies doing so.**

---

### 10.2 AI

Potential AI experiments include:

- metadata extraction from book covers or PDFs;
- recommendations based on preferences and history;
- summarization;
- plagiarism detection.

AI is not required for Librio to be a complete product.

No AI capability is part of the default V2 scope.

AI functionality should only move into implementation when there is a sufficiently clear use case and an appropriate way to evaluate its output.

---

## 11. Product Development Principles

Librio follows several principles:

- The reader journey remains the product spine.
- New features should address a concrete product or engineering need.
- Technical complexity should not be introduced only to demonstrate technology.
- Abstractions should not be designed in advance without a real use case.
- Existing solutions may be reused when the capability is not a product differentiator.
- Documentation and architecture may evolve as implementation provides new evidence.
- Major decisions should preserve their rationale, not only their final outcome.

The product direction may evolve over time, but changes should come from what is learned through development rather than from adding features arbitrarily.