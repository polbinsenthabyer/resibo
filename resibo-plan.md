# Resibo — Plan & Source of Truth

> **This document is the source of truth for Resibo.** The code, the database
> schema, and the deployment follow this file. If the code and this file
> disagree, one of them is a bug: either the code drifted, or this file went
> stale and must be corrected in the same change that revealed it.

**Status: planning, with a deployed API and one-page React client.** A Spring Boot application runs on the server in Docker behind the
host's nginx, fronted by the Cloudflare tunnel that already serves the sibling
projects, answering on `https://resibo-api.polbinsenthabyer.uk` (§7.3). It
connects to MariaDB as a dedicated least-privilege account, with Hibernate
forbidden from touching the schema (§5.3). Since 2026-11-24 it also has **JWT
authentication with two roles** (§5.4), three tables, and an admin-only product
catalog.

**Read that narrowly.** What exists is a way to log in and a list of products.
There is no checkout, no sale, no receipt, no payment, no shift, no stock
movement; the client currently shows API health only — so there is no POS. **Every `[BLOCKER]` item below is
still unanswered, and nothing that depends on one should be written until it
is.** The two roles are a working minimum, not the answer to §1.3. The
`product` table is a name, a price and a stock count, not the §4 domain model.
And signup is still public, so on a fresh environment whoever signs up first
becomes the administrator — an open exposure, recorded as §8.1 risk 6.

---

## How to use this document

**If you are the owner (human):**

- Answer the lines marked `**Answer:**`. For multiple-choice items, tick one
  box: `- [x]`.
- You do not have to fill this in one sitting. Do the `[BLOCKER]` items first —
  those are the ones where guessing wrong means rewriting code later.
- "I don't know yet" is a valid answer. Write it. It tells the next session not
  to assume.
- If you change your mind later, change it **here** and say so; don't just say
  it in chat.

**If you are an AI session:**

1. **Read this entire file before touching any code**, every session. This file
   outranks your assumptions about how a POS "normally" works.
2. **Any change to behaviour, scope, schema, stack, or deployment must update
   this file in the same turn as the code.** Not afterwards, not at session end.
3. **Append to §13 (Decisions) with the rationale** whenever a decision is made
   or an approach rejected, and to §14 (Document change log) whenever this file
   changes.
4. **Never write a secret value anywhere in this file** — see §8.
5. Unanswered `[BLOCKER]` items are a hard stop for the modules that depend on
   them. Ask; do not invent an answer.
6. `README.md` in this repository is the public-facing summary. When this plan
   changes something the README states, update the README too.

**Priority tags used below:**

| Tag | Meaning |
|---|---|
| `[BLOCKER]` | No meaningful code can start until this is answered. |
| `[SOON]` | Needed before the module that depends on it is built. |
| `[LATER]` | Safe to defer; record the deferral rather than guessing. |

---

## 0. Document control

| Field | Value |
|---|---|
| Document version | 0.13 — CORS restricted to the deployed client and localhost; scope still unfilled |
| Created | 2026-11-24 |
| Last updated | 2026-11-24 |
| Last updated by | Claude (restricted CORS to named origins and verified it live) — 0.12 was Codex, recording agent ownership; both landed the same day |
| Plan status | **AWAITING OWNER INPUT** — the API is deployed and authenticated (§5.4, §7.3), and the React client is now deployed too, at `https://resibo.polbinsenthabyer.uk` (§5.6, §7.4). There is still no checkout, no sale and no receipt, and every `[BLOCKER]` in §1, §2 and §6.1.1 is still open |
| Working copy | `%USERPROFILE%\sprintify_solutions\personal\resibo` on `dev/resibo`. A second clone at `%USERPROFILE%\sprintify_solutions\personal\app\resibo` is a duplicate; do not edit it until its fate is decided. |
| Remote | `https://github.com/polbinsenthabyer/resibo.git` |
| Production URL | `https://resibo-api.polbinsenthabyer.uk` — health, auth, and admin-only products; see §7.3 |
| This document lives on | branch `dev/resibo`, the single working branch — see §7.1. The two-branch split that made this document exist twice is retired. |
| Vault notes | `Obsidian Vault/Projects/personal/resibo/` |

---

### 0.1 Agent roles and context continuity

The owner assigned **Codex to frontend work** in `client/` and **Claude Code to backend work** in `server/`. This is an ownership assignment, not a claim that either agent is currently running. Both work on the single `dev/resibo` branch. Contract changes that touch both halves need coordination, and a reviewer accepts a handoff after checking evidence; the implementer does not self-declare it accepted.

For Codex, `AGENTS.md` is the automatic entry point and `CODEX.md` is a short frontend handoff snapshot. **This document remains the product source of truth.** The Obsidian vault's four standard project notes remain the durable record: the hub for standing facts, `Decisions.md` for reasons, `Changelog.md` for dated changes and actual verification, and `Tasks.md` for open work. `CODEX.md` points to them and records the current frontend state and next action; it does not replace or duplicate the specification.

For each material frontend or backend update, change this plan in the same change as behavior or scope, append a decision with its rationale to §13 when one is made, bump the document version and append §14, and update the relevant vault notes as work happens. Refresh the agent handoff before ending the task. Do not record secrets or full conversation transcripts in any of these files.

---

## 1. Product definition

### 1.1 Purpose

Given (owner, 2026-11-24): *Resibo is a POS (point-of-sale) application.*

Expand that in your own words — what should a person be able to do with it by
the end of a shift that they cannot do today?

**Answer:**

### 1.2 `[BLOCKER]` What kind of business does it serve?

This decides a large share of the screens and roughly half the database.

- [ ] Retail / convenience / sari-sari — barcodes, stock counts, fast scan-and-pay
- [ ] Restaurant / cafe / bar — open tabs, tables, kitchen orders, course timing
- [ ] Pharmacy — batch and expiry tracking, prescription records, regulated items
- [ ] Hardware / lumber / agri supply — units of measure, cut-to-size, quotations
- [ ] Services (salon, clinic, repair) — appointments, staff commissions, labour lines
- [ ] Grocery / supermarket — weighing scales, PLU codes, many terminals
- [ ] General purpose, must fit several of the above
- [ ] Other:

**Answer:**

### 1.3 `[BLOCKER]` Who uses it, and what must each be able to do?

Delete rows you don't need; add rows you do. The "must NOT" column matters as
much as the first — it is where the permission model comes from.

| Role | Must be able to | Must NOT be able to |
|---|---|---|
| Cashier | | |
| Shift supervisor | | |
| Manager / owner | | |
| Bookkeeper / accountant | | |
| Stock clerk | | |
| System admin | | |

### 1.4 Why now, and what does success look like?

What is used today (paper, spreadsheet, another POS)? What would make you say
after three months that this was worth building?

**Answer:**

### 1.5 Non-goals — things it deliberately will not do

**Answer:**

### 1.6 Anti-goals — what would make this a failure even if every feature works?

For a POS this is usually something like "the queue moves slower than it did
with a calculator and a receipt book", or "a cashier can quietly delete a sale".

**Answer:**

---

## 2. Decisions that change the architecture

These come before everything else. Each one, answered differently, produces a
materially different application.

### 2.1 `[BLOCKER]` Must it keep selling when the internet is down?

The single biggest fork in the design.

- [ ] **A — Online only.** Browser talks to the server; no connection, no sales.
      Simplest by a wide margin. Acceptable only if the connection is reliable
      and a short outage is tolerable.
- [ ] **B — Offline-tolerant.** Catalog and prices cached locally; sales queued
      and pushed when the link returns. Moderate extra work. Needs rules for
      what a terminal may do blind (can it still check stock?).
- [ ] **C — Offline-first.** Each terminal holds a full local database and syncs.
      Most robust, significantly more work, and it forces conflict-resolution
      decisions on stock, receipt numbering, and pricing.

**My recommendation:** B, unless the store's connection genuinely drops often —
in which case say so and we design for C from the start. Retrofitting C onto A
is close to a rewrite, which is why this is question one.

**Answer:**

### 2.2 `[BLOCKER]` How many stores and terminals?

| Question | Answer |
|---|---|
| Stores today | |
| Stores in two years (best guess) | |
| Terminals (tills) per store | |
| One shared product catalog across stores, or per-store? | |
| Same prices in every store? | |
| Stock tracked per store? | |
| Can a customer return in store B what they bought in store A? | |

### 2.3 `[BLOCKER]` Does it issue official, tax-authority receipts?

The item most likely to force a rewrite if answered late, so please read it even
if the answer seems obvious.

- [ ] **A — Internal receipts only.** The receipt is a courtesy document; no tax
      authority involved. Simplest.
- [ ] **B — Philippine BIR-registered Official Receipt / Sales Invoice.**
- [ ] **C — Another jurisdiction:**

If **B**, the design must account for things that cannot be added after the
fact. From what I know of Philippine POS requirements, this typically includes:

- The POS or computerised accounting system being **registered/accredited** with
  the BIR, with the permit and machine serial number printed on receipts.
- **Sequential invoice numbering that is never reset or reused**, including after
  a reinstall or a database restore.
- A **non-resettable accumulated grand total** and a **Z-reading** per day.
- An **audit trail with no hard deletes** — a mistake is corrected by a void or
  return record, never by editing or deleting the original sale.
- **Senior citizen and PWD handling**: the statutory discount together with VAT
  exemption, recorded with the ID details and reported separately.
- VAT-inclusive pricing, with VAT-exempt and zero-rated sales distinguished on
  the receipt and in reports.

> ⚠️ **I have not verified current BIR rules, and I am not your accountant.** If
> you pick B, confirm the specifics against current BIR issuances or with your
> bookkeeper **before we lock the schema**, and record what they say in §13. The
> cost of getting this wrong is a non-compliant receipt and a rebuilt sales table.

**Answer:**

### 2.4 `[BLOCKER]` Tax model

| Question | Answer |
|---|---|
| Tax type and rate (e.g. 12% VAT) | |
| Prices shown to the customer: tax-inclusive or tax-exclusive? | |
| Any tax-exempt or zero-rated products? Which? | |
| Does a discount reduce the taxable amount, or is tax computed first? | |
| Any other charge (service charge, local tax, environmental fee)? | |

### 2.5 `[SOON]` Hardware

A browser cannot reliably drive a USB or serial receipt printer, a cash drawer,
or a scale. If any of the first four rows are needed, we will likely need a small
companion program running on the till — a real piece of the project, not a
detail. Be specific about models you already own.

| Device | Needed? | Make / model | How it connects (USB / network / Bluetooth) |
|---|---|---|---|
| Receipt printer (thermal 58mm / 80mm) | | | |
| Cash drawer (usually opened via the printer) | | | |
| Barcode scanner | | | |
| Weighing scale | | | |
| Customer-facing display | | | |
| Card / e-wallet terminal | | | |
| Label / barcode printer | | | |

### 2.6 `[SOON]` Payment methods

| Method | Accept in v1? | Notes |
|---|---|---|
| Cash | | |
| Debit / credit card | | |
| GCash / Maya / QR Ph | | |
| Bank transfer | | |
| Store credit / gift card | | |
| Customer account (credit terms) | | |
| Split across several methods in one sale | | |

Do we **integrate** with a payment provider (authorise the charge from Resibo),
or only **record** which tender the customer used? Integration adds a provider,
credentials, reconciliation, and failure handling.

**Answer:**

### 2.7 `[SOON]` Volume

| Question | Answer |
|---|---|
| Sales per day, typical / peak | |
| Items per sale, typical | |
| Number of SKUs | |
| Busiest moment (e.g. 60 sales in 15 minutes?) | |
| Years of history that must stay queryable | |

---

## 3. Functional scope

Mark each module. Be ruthless with v1 — a POS that does checkout well beats one
that does nine things poorly.

| Module | v1 | Later | Never | Notes |
|---|---|---|---|---|
| Checkout / ring up a sale | | | | |
| Product catalog | | | | |
| Variants / modifiers (size, colour, add-ons) | | | | |
| Barcode lookup | | | | |
| Price levels (retail / wholesale) | | | | |
| Discounts and promotions | | | | |
| Statutory discounts (senior / PWD) | | | | |
| Stock on hand | | | | |
| Stock receiving, transfers, adjustments | | | | |
| Purchase orders and suppliers | | | | |
| Physical / cycle count | | | | |
| Batch and expiry tracking | | | | |
| Customers | | | | |
| Loyalty / points | | | | |
| Customer credit (accounts receivable) | | | | |
| Returns and refunds | | | | |
| Voids and corrections | | | | |
| Cash drawer, shift open/close, X and Z reading | | | | |
| Users, roles, permissions | | | | |
| Audit log | | | | |
| Reports and dashboard | | | | |
| Receipt printing | | | | |
| Emailed / texted receipt | | | | |
| Multi-store | | | | |
| Tables / floor plan | | | | |
| Kitchen display or order printing | | | | |
| Online orders / delivery apps | | | | |
| Export to accounting software | | | | |
| Backup and restore | | | | |

---

## 4. Domain model — my proposal, please correct it

A first sketch so you have something concrete to disagree with. Mark anything
wrong or missing; the names matter less than the shapes.

| Entity | What it is | Note |
|---|---|---|
| `Store` | A physical location | Only if §2.2 says multi-store |
| `Terminal` | One till; owns its receipt number sequence | |
| `User`, `Role`, `Permission` | Who may do what | |
| `Shift` | One cashier's session on one terminal | Opening float to closing count |
| `CashMovement` | Drop, payout, petty cash, over/short | |
| `Product` | A sellable thing | |
| `ProductVariant` | A specific sellable unit of a product | Only if §3 includes variants |
| `Barcode` | Many per variant (supplier codes differ) | |
| `PriceList` / `Price` | Price of a variant in a context | Dated, so history survives |
| `TaxRate` | Rate plus exemption rules | |
| `DiscountRule` | Promotions and statutory discounts | |
| `StockItem` | Quantity on hand per variant per store | Derived from movements |
| `StockMovement` | Every plus/minus with a reason and a reference | Append-only |
| `Supplier`, `PurchaseOrder` | Buying side | |
| `Customer` | Plus senior/PWD ID if §2.3 is B | |
| `Sale` | Receipt header: totals, tax, status, sequence number | |
| `SaleLine` | One line: variant, qty, unit price, discount, tax | Price **copied**, not referenced |
| `Payment` | One tender against one sale | Several per sale |
| `Return` / `Void` | A correction that points at the original | New record, never an edit |
| `ReceiptSequence` | Per-terminal counter that only moves forward | Never reset |
| `AuditEvent` | Who did what, when, from where | Append-only |

**Three of these rows now exist in the database, ahead of this section being
answered** (§5.4): `product` as a name, description, price and stock count, and
`User` plus `Role` as the `user` and `user_role` tables with two roles. They are
a working minimum built to get authentication and a catalog moving, not a
verdict on this table — no `ProductVariant`, no `Barcode`, no `PriceList`, no
`Permission`. Treat the table below as still open, including for those three.

**Two rules I propose, which are hard to add later:**

1. **Sales are append-only.** A completed sale is never updated or deleted. A
   mistake becomes a void or a return that references it. Required if §2.3 is B,
   and good practice regardless — it is what lets you trust a day's totals.
2. **Line items copy the price and tax at the moment of sale.** Never reconstruct
   a historical receipt from today's price list.

**Agree? What's wrong or missing?**

**Answer:**

---

## 5. Tech stack

**Decided by owner, 2026-11-24:** Spring Boot backend · React frontend ·
MariaDB database. Those three are settled. The **modular monolith** was decided
here too, and is the one item on that list that has since changed — the code is
layer-first; see §6.1. The rest below are the choices that hang off them.

The "Your decision" column now records what the scaffold **actually uses** where
a choice has been made in code, and says **open** where nothing has been chosen.
"In use" means it is in `pom.xml` or `application.yaml` today; see §5.1.

| Choice | My recommendation | Your decision |
|---|---|---|
| Java version | 21 (LTS) | **17 — in use.** Matches gainsoclock, and 17 is the only JDK on this machine (§5.1). |
| Spring Boot version | latest 3.x | **4.2.0 — in use.** The Boot 4 line, as gainsoclock uses, not 3.x. |
| Build tool | Maven — more examples, easier handover | **Maven — in use**, through the committed `mvnw` wrapper (Maven itself is not installed locally). |
| Packaging | `jar`, embedded server | **`jar` — in use.** See §5.1 on how the owner's "packaging: yaml" note was read. |
| Deployment artifact | A container image, so the server needs only Docker | **Docker image — in use.** Multi-stage build: `eclipse-temurin:17-jdk` compiles with the committed `mvnw`, `eclipse-temurin:17-jre` runs the jar as a non-root user. See §5.1 and §7.3. |
| Container runtime | Docker Compose, one service | **In use** — `docker-compose.yml` at the repository root, service `api`, container `resibo-api`, published on loopback only. §7.3. |
| Configuration format | `application.yaml` | **YAML — in use**, `src/main/resources/application.yaml`. |
| Boilerplate | Lombok, per §6.1.2 | **Lombok — in use**, `optional` scope. |
| Runtime probes | Spring Boot Actuator | **Actuator — in use**; `health` and `info` exposed, details only `when-authorized`. |
| Local restart | Spring Boot DevTools | **DevTools — in use**, `runtime` and `optional`. |
| API style | REST + JSON | REST + JSON **as built** — eight endpoints (§5.1), with a bearer token in the `Authorization` header. The product listing is a `POST` despite being a read, which is a deliberate choice explained in §5.1. |
| API docs | OpenAPI generated from the controllers | **Open** — no OpenAPI dependency in the pom. It costs more than it did: there are now eight endpoints, four of which need a token. |
| DB migrations | Flyway, versioned SQL committed to the repo | **Open, and the gap is no longer theoretical** — no Flyway in the pom and no migrations, while `ddl-auto: none` means Hibernate will not create a table either. **Three tables now exist** (`product`, `user_role`, `user`), applied by hand from `sql/tables/`, and nothing records which environment has which. §12 #15. |
| Persistence | Spring Data JPA, with plain SQL where reporting needs it | **Spring Data JPA — in use** (§5.3), with the MariaDB driver at `runtime` scope. Three entities and three repositories now exist, and on the owner's instruction **every repository query is native SQL** — see §5.5 for the two rules that follow from that. |
| Auth | Short-lived JWT access token + refresh; roles from §1.3 | **JWT — in use** (§5.4). Spring Security, HS256, an 8-hour token, two roles (ADMIN, EMPLOYEE), and default-deny on any endpoint without an explicit rule. **No refresh token, no logout and no revocation** (§12 #18). The two roles are a working minimum, not an answer to §1.3. |
| Password hashing | BCrypt | **BCrypt — in use** (§5.4), `BCryptPasswordEncoder` at its default strength. The `password` column holds a hash and never a plaintext password. |
| Frontend build | Vite | **Vite 8 — in use** (§5.6), at `src/main/client` on branch `dev/app/resibo`. |
| Frontend language | TypeScript | **TypeScript 7 — in use**, with `strict`, `noUncheckedIndexedAccess` and `exactOptionalPropertyTypes` (§5.6). |
| Server state | TanStack Query | **Open, and deliberately not adopted.** The client has a small `useAsync` hook instead — no cache, no deduplication, no revalidation. That is the right size for one health call and the wrong size for a catalog; adopt TanStack Query when a screen needs it rather than growing the hook. |
| UI components | shadcn/ui on Tailwind | **Diverged from the recommendation, by the owner’s choice: plain SCSS.** Design tokens as CSS custom properties, a small reset, one CSS Module per component (§5.6). No Tailwind and no component library. A kit could still be added, but it would sit on top of this rather than replace it. |
| Routing | React Router | **React Router 7 — in use** (§5.6), one route table at `src/app/router.tsx`. |
| Backend tests | JUnit 5 + Testcontainers against real MariaDB | **JUnit 5 in use** (`spring-boot-starter-webmvc-test`, `spring-boot-starter-actuator-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`), **22 tests** as of 2026-11-24. **Testcontainers still open — and it stopped being academic**: the context test now needs a datasource, CI supplies a throwaway one (§5.3), but a local `mvnw verify` loads `.env.dev` and therefore connects to the **shared production database**, which now has real rows in it. §12 #13. |
| Frontend tests | Vitest + React Testing Library; Playwright for checkout | **Open** — no test runner is installed in the client. |
| Local database | Docker Compose for MariaDB | **Decided by default, and not well.** `.env.dev` points local runs *and local tests* at the shared MariaDB on the server, because that is the database that exists. A local container or Testcontainers would isolate it; neither is in place. **That schema is no longer empty**, so this has stopped being harmless. §12 #13. |
| Repo layout | One repo: `backend/` and `frontend/` | **Adopted, with different names.** One repository, one branch `dev/resibo`, `client/` and `server/` at the root — the owner's call on 2026-11-24, replacing both the root-level backend and the two-branch split. Independent deploys come from CI path filters and disjoint deploy targets, not from the layout (§7.4). |

**Accept all recommendations, or state your changes:**

**Answer:**

> Note: checkout is a keyboard-driven, latency-sensitive screen. Whatever the UI
> library, the sale screen needs deliberate keyboard handling — a design task in
> its own right, not a component choice.

### 5.1 The scaffold as it actually stands (2026-11-24)

Generated from start.spring.io and committed as `2b6dcd6`. Recorded here so no
session has to read the pom to learn what was chosen, and so nobody repeats the
two traps below.

| Field | Value |
|---|---|
| groupId / artifactId | `com.api` / `resibo` |
| Name / description | `resibo` / "Resibo POS API" |
| Version | `0.0.1-SNAPSHOT` |
| Base package | `com.api.resibo` |
| Java | 17 |
| Spring Boot | 4.2.0 |
| Build tool / packaging | Maven / `jar` |

Dependencies, exactly as they stand in `pom.xml`. The three JPA and driver rows
were added on 2026-11-24 for the database connection (§5.3); the five security
and JWT rows later the same day for authentication (§5.4):

| Dependency | Scope | Why it is there |
|---|---|---|
| `spring-boot-starter-webmvc` | compile | What the owner's "spring-web" resolves to — **Boot 4 renamed that starter to `webmvc`**. Same as gainsoclock. |
| `spring-boot-starter-actuator` | compile | `/actuator/health` for infrastructure probes |
| `lombok` | compile, `optional` | The §6.1.2 boilerplate convention |
| `spring-boot-devtools` | `runtime`, `optional` | Local restart on recompile |
| `spring-boot-starter-webmvc-test` | `test` | MockMvc slice tests |
| `spring-boot-starter-actuator-test` | `test` | Actuator slice |
| `spring-boot-starter-data-jpa` | compile | Hibernate, Spring Data, the transaction manager and HikariCP — the persistence layer §5 chose (§5.3) |
| `mariadb-java-client` | `runtime` | The JDBC driver. **`runtime`, not compile: nothing in the source imports it**, so the compiler has no business seeing it. |
| `spring-boot-starter-data-jpa-test` | `test` | The JPA test slice, so repository tests do not need the full context |
| `spring-boot-starter-security` | compile | The filter chain, the authorisation rules and `BCryptPasswordEncoder` (§5.4) |
| `jjwt-api` | compile | The JWT API. Version pinned by a `jjwt.version` property |
| `jjwt-impl` | `runtime` | jjwt implementation. `runtime` because nothing compiles against it |
| `jjwt-jackson` | `runtime` | jjwt's JSON serialisation |
| `spring-boot-starter-security-test` | `test` | Security-aware slice tests |

**jjwt is pinned at 0.13.0, and that is deliberate — gainsoclock uses 0.11.5.**
0.11's builder and parser calls are deprecated, and there is no reason to start
new code on them. The consequence is practical: **JWT code cannot be copied
between the two projects.** 0.13 writes
`Jwts.builder().subject(...).expiration(...)` and
`Jwts.parser().verifyWith(key).build().parseSignedClaims(...)`; 0.11 does not.

**The artifact ids were taken from Spring Initializr for Boot 4.2.0, not from
memory.** Boot 4 renamed starters (`web` → `webmvc`, and the `*-test` starters
are new in this line), so guessing a coordinate from a Boot 3 tutorial produces
a pom that does not resolve — the same class of failure as the
`4.2.0.RELEASE` trap above.

**Two traps, written down because they will recur:**

1. **`4.2.0.RELEASE` does not exist in Maven Central.** Initializr's metadata
   lists the version id as `4.2.0.RELEASE` and the generated pom used that
   literally; Central answers **HTTP 404** for that coordinate, because Spring
   Boot dropped the `.RELEASE` suffix years ago. The pom was corrected to
   `4.2.0`, which is the current latest release. A session regenerating from
   Initializr will hit this again — fix the parent version before the first
   build, not after reading a confusing resolution error.
2. **"packaging: yaml" was read as a configuration-format instruction, not a
   Maven one.** Maven packaging is only `jar` or `war`, so the owner's note was
   taken to mean YAML configuration — `application.yaml`, matching gainsoclock —
   with `jar` packaging. That reading was stated to the owner and is **not yet
   explicitly confirmed. Treat it as an assumption**, and correct it here if it
   was meant differently.

Beyond the build scaffolding — `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`
and `.gitattributes` — these are the files that were created. The pre-existing
`.gitignore` was kept rather than Initializr's, because it is stricter: it
already blocks `.env*`, keystores and `application-local.*`.

```
src/main/java/com/api/resibo/ResiboApplication.java
src/main/java/com/api/resibo/controllers/HealthController.java
src/main/java/com/api/resibo/dtos/ResponseDto.java
src/main/java/com/api/resibo/types/ResponseType.java
src/main/java/com/api/resibo/utilities/MessageType.java
src/main/java/com/api/resibo/utilities/ResponseUtility.java
src/main/resources/application.yaml
src/test/java/com/api/resibo/ResiboApplicationTests.java
src/test/java/com/api/resibo/controllers/HealthControllerTest.java
```

Note the paths: the backend sits at the repository root, not under `backend/` as
§5 proposed. Nothing depends on that yet, but it reopens the question of where a
frontend would live.

That list is the scaffold as generated, not the tree as it stands — §6.1 has the
current one.

`application.yaml` holds seven things and no secrets: the application name
(`resibo`); `spring.config.import:
optional:file:.env.${APP_ENV:dev}[.properties]`, which loads a per-environment
env file from the working directory — `APP_ENV` picks it, `dev` by default, and
`optional:` means a missing file is not a startup failure; the **datasource and
JPA block** added on 2026-11-24, which has its own section because its defaults
and its `ddl-auto` setting are load-bearing (§5.3); `server.port:
${SERVER_PORT:8080}`, so the port is an environment override with a sane
default; the actuator exposure
(`management.endpoints.web.exposure.include: health,info` and
`management.endpoint.health.show-details: when-authorized`, so an anonymous
caller learns liveness and nothing else — it now also reports database
reachability, so hiding the details from anonymous callers matters more than it
did); the **`jwt` block** added on 2026-11-24, `secret: ${JWT_SECRET}` with no
default and `expiration: ${JWT_EXPIRATION:28800000}` (§5.4); and
`logging.level.com.api.resibo: INFO`.

The env files that go with it: **`.env.example` is committed and carries names
only**, as §8 promised; `.env.dev`, `.env.prelive` and `.env.prod` are
git-ignored. They are parsed as a `.properties` file, which is a real
constraint worth remembering — no `export`, no shell expansion, no quotes
around values. **This has stopped being a file of harmless values.** The seven
keys are `SERVER_PORT`, `DATASOURCE_URL`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD`, `DB_POOL_MAX`, `JWT_SECRET` and `JWT_EXPIRATION`; the
datasource keys are no longer commented out, and `DATABASE_PASSWORD` and
`JWT_SECRET` are both real credentials — so `.env.dev` is now exactly the kind
of file the §8 rules exist for.

✅ **Fixed in `0135659`** — `.env.example` now lists all seven keys, and the CI
job sets `JWT_SECRET`. The paragraph below records why it mattered.

~~**`.env.example` lists only the first five keys.**~~ `JWT_SECRET` has no
default, so a fresh checkout that copies the template cannot start the
application. The CI `build` job has the same hole — it sets the datasource keys
and no `JWT_SECRET`, while `ResiboApplicationTests` starts the full context.
Recorded as §12 #20; both files are outside the documentation change that found
it.

**The API surface, in full.** Eight endpoints. All return the standard envelope
except `/actuator/health`, which returns Actuator's own shape. The "who" column
is enforced by `SecurityConfig` (§5.4), not by annotations on the controllers.

| Endpoint | Who | Purpose |
|---|---|---|
| `GET /api/health/check` | anyone | The application's own health check |
| `GET /actuator/health` | anyone | Actuator, for infrastructure probes |
| `POST /api/auth/signup` | anyone | Register an account — see the bootstrap warning in §5.4 |
| `POST /api/auth/login` | anyone | Exchange username and password for a token |
| `POST /api/admin/products/set` | ADMIN | Create when the payload has no id, update when it does |
| `POST /api/admin/products/list` | ADMIN | Filtered, sorted, paginated listing |
| `GET /api/admin/products/get/{productId}` | ADMIN | One product |
| `DELETE /api/admin/products/delete/{productId}` | ADMIN | Delete one product |

The listing is a `POST` despite being a read: the filter and pagination payload
belongs in a request body, and a `GET` body is not reliably carried by proxies,
browsers and HTTP clients. gainsoclock pairs `GET` with `@RequestBody`, which
works until something in the path drops it.

gainsoclock also exposes a `/check/prelive` variant of the health endpoint; it
was deliberately left out, because no prelive environment has been decided for
Resibo (§7). It costs one method to add when one exists.

**Toolchain on this machine — the build will not run without this.** No JDK is
on `PATH` and `JAVA_HOME` is unset. Maven is not installed either, which is why
the wrapper is committed. A JDK 17 does exist — Microsoft Build of OpenJDK
17.0.20, under `%USERPROFILE%\.jdks\ms-17.0.20`. `JAVA_HOME` has to be set
**per shell, every shell**; that is the one real piece of friction in building
this repository today, and it is why `README.md` spells the command out instead
of saying "run Maven".

**Verified, not assumed (2026-11-24):**

| Check | Result |
|---|---|
| `./mvnw -B -U clean verify` | **BUILD SUCCESS** — `Tests run: 2, Failures: 0, Errors: 0` |
| `GET /api/health/check` against the running jar | `200`, envelope intact: `responseType: SUCCESS`, `code: S0001`, `message: "Resibo API is healthy!"`, `className`/`methodName` resolved to `HealthController.healthCheck` |
| `GET /actuator/health` | `200`, `{"groups":["liveness","readiness"],"status":"UP"}` |
| An unmapped path | `404` |

**One defect found, and deliberately left in the code.** `responseTime`
serialises in **UTC**, not local time: the verified response read `03:27:49`
while the machine's clock said `11:27:49` (UTC+08:00). `ResponseDto`'s
`@JsonFormat` carries no timezone, and Spring's Jackson default is UTC. For a
POS that prints timestamps on receipts this is a real problem; if §2.3 turns out
to be B, it is potentially a compliance problem. The fix is one line —
`spring.jackson.time-zone` — but timezone is an open owner question in §10, so
applying it would be exactly the kind of quiet guess this document exists to
prevent. Tracked as §12 #8 and recorded in §13.

### 5.2 The container runtime (2026-11-24)

The same source also builds and runs in a container, which is how it is
deployed (§7.3). Three files at the repository root do it, and these are the
facts worth knowing without reading them.

**`Dockerfile` — multi-stage.** The build stage is `eclipse-temurin:17-jdk` and
runs the **committed `mvnw`**, which is the whole point of the wrapper: the
server needs only Docker, with no JDK and no Maven installed on it. `.mvn/`,
`mvnw` and `pom.xml` are copied first and `dependency:go-offline` runs on its
own, so dependency resolution is a cached layer and a source change does not
re-download the world. The runtime stage is `eclipse-temurin:17-jre` — no
compiler, no Maven, no source. It adds a **non-root user `resibo`** and owns the
jar to it; the application never needs to write to the filesystem. `curl` is
installed for exactly one reason, the container `HEALTHCHECK` against
`/actuator/health`, and nothing else is added to the runtime image. `ENV
SERVER_PORT=8080` with `EXPOSE 8080`: **the container always listens on 8080
internally**, whatever host port it is mapped to, so one image is identical in
every environment.

**`docker-compose.yml`.** Image
`${RESIBO_IMAGE:-resibo-api}:${RESIBO_TAG:-current}`, container `resibo-api`,
`restart: unless-stopped`. The env file uses the long `env_file` form pointing
at `${RESIBO_ENV_FILE:-./.env.dev}` with `required: false`, so a local checkout
starts with no setup while the server points the same variable at a file
outside the checkout. `SERVER_PORT` is forced to `8080` inside the container,
reinforcing the rule above. Ports publish as `127.0.0.1:${HOST_PORT:-8083}:8080`
— **loopback only, never on the LAN**; the only thing that may reach the
container is nginx on the same host. `extra_hosts:
host.docker.internal:host-gateway` is present so the container can reach
services on the host — which, since 2026-11-24, is no longer hypothetical: the
container needs a route to the host's MariaDB (§5.3). Logging is capped —
`json-file`, 10 MB × 3 — because the server hosts several applications and none
of them should be able to fill the shared disk.

**`.dockerignore`.** Keeps the build context small and, more to the point, keeps
secrets and local state out of the image under any name: `.env*` with
`.env.example` excepted, `*.pem` / `*.key` / `*.p12` / `*.jks`, `secrets/`,
`target/`, `.git/`, and the Markdown docs — the last of those so that editing
this file cannot invalidate the dependency cache layer.

One pleasant side effect: the `JAVA_HOME`-in-every-shell friction described
above is a local-development problem only. It does not exist in the container
or on the server.

### 5.3 The database connection (2026-11-24)

Committed as `9b57c2b`, with a follow-up documentation note in `bfafb3a`. This
is the first thing in the project that can damage data belonging to other
people, which is why it gets its own section rather than a row in a table.

**What it is, in one line:** the application opens a HikariCP pool to the
`resibo` schema on the shared MariaDB instance (§7.2), as a dedicated
least-privilege account, with Hibernate forbidden from issuing a single DDL
statement. **What it was not, when this section was written:** persistence —
there were no entities, no repositories and no tables, and the pool connected to
an empty schema. That changed later the same day: §5.4 added three tables and
three entities. Everything below about the pool, the account and `ddl-auto`
still holds exactly as written; **it is Hibernate that is forbidden from
creating a table, not the project.**

#### The datasource block

Three dependencies carry it, listed in §5.1's table: the JPA starter, the
MariaDB driver at `runtime` scope, and the JPA test starter. The configuration
lives in `application.yaml`:

| Key | Value | Why |
|---|---|---|
| `spring.datasource.url` | `${DATASOURCE_URL}` | **No default, deliberately** — see below |
| `spring.datasource.username` | `${DATABASE_USERNAME}` | as above |
| `spring.datasource.password` | `${DATABASE_PASSWORD}` | as above |
| `spring.datasource.driver-class-name` | `org.mariadb.jdbc.Driver` | Explicit, so a missing driver fails at startup with a name rather than a guess |
| `spring.datasource.hikari.pool-name` | `resibo-pool` | A named pool is findable in a log and in `information_schema.processlist` on a shared instance |
| `spring.datasource.hikari.maximum-pool-size` | `${DB_POOL_MAX:10}` | Tunable per environment; 10 is the Hikari default and nothing yet justifies a different number |
| `spring.datasource.hikari.connection-timeout` | `10000` (ms) | **Fail fast rather than hang.** A till that cannot reach the database must learn that immediately; a checkout request that blocks indefinitely is worse than one that errors in ten seconds |
| `spring.jpa.hibernate.ddl-auto` | `none` | The rule below |
| `spring.jpa.open-in-view` | `false` | Keeps the persistence context out of the view layer. Left at Spring's default (`true`) a connection is held for the whole request and lazy loads happen during serialisation — the usual cause of mystery pool exhaustion, and on a shared instance the pool is not ours alone to exhaust |

**No dialect is configured.** Hibernate auto-detects `MariaDBDialect` from the
connection, and the startup log says so, which is a better check than a property
asserting it.

**The three datasource values have no defaults on purpose.** A missing
configuration therefore **fails startup, loudly**, instead of silently falling
back to an embedded database or to whatever happens to be on `localhost:3306`.
On an instance that also hosts unrelated project databases (§7.2), "connected
to something unintended" is the failure worth engineering against, and a
startup error is the cheapest possible way to find out. Do not add defaults to
these three keys to make a test or a container start more conveniently — fix the
environment instead.

#### `ddl-auto: none` — the rule that does not get relaxed

The owner's explicit instruction, and it stays. Two reasons, the second of which
is the one that matters:

1. **The schema is owned outside the application**, the same arrangement as
   gainsoclock. Whatever creates tables — Flyway, a hand-run script, a person
   with the superuser — is not Hibernate.
2. **If §2.3 comes back as B (BIR-registered receipts), an ORM silently
   altering the sales tables is precisely the class of change that destroys an
   audit trail.** An auditor cannot be told that a column changed because a
   field was renamed in Java and the application reshaped the table at startup.
   A POS that issues official receipts needs its schema changes to be
   deliberate, reviewable, and attributable to a human.

> ⚠️ **`spring.jpa.hibernate.ddl-auto` must never be set to `update`, `create`
> or `create-drop`, in any environment, including a developer's.** `update` is
> the dangerous one, because it looks helpful: it will quietly alter a live
> sales table to match a changed entity. If a schema change is needed, write it
> as SQL, review it, and run it deliberately (§12 #15).

Verified rather than trusted, on 2026-11-24 before any table existed: **zero
DDL statements appeared in the application log, and the `resibo` schema still
held zero tables after the application had run** — locally and on the server.
The three tables it holds now were each applied by hand from `sql/tables/`, not
by Hibernate.

#### Environment keys

Five, and the names are not arbitrary: **they match gainsoclock's `.env`
exactly**, so the two projects stay interchangeable in a session's head and in a
server operator's.

| Key | Purpose | Default if unset |
|---|---|---|
| `SERVER_PORT` | Port the API listens on | `8080` |
| `DATASOURCE_URL` | JDBC URL for the `resibo` schema | **none — startup fails** |
| `DATABASE_USERNAME` | The application's database account | **none — startup fails** |
| `DATABASE_PASSWORD` | Its password | **none — startup fails** |
| `DB_POOL_MAX` | Hikari maximum pool size | `10` |

Where the values live, per §8: `.env.example` is committed and carries the
**names only**; `.env.dev` is git-ignored and carries local values; the server
reads `/opt/resibo/resibo.env`, mode `0600`, outside the checkout. **No URL and
no password appears in this document**, because the repository is public (§7.2).
The account *name* does appear, below: it is a design fact, not a credential.

**`.env.dev` now sets `SERVER_PORT=8083`, not 8088.** The old value was eloms'
port on the server, which made every local-versus-deployed comparison
misleading for no benefit. Local and deployed now agree on 8083, and the earlier
warning about the 8088 trap in §7.3 is resolved rather than live.

#### The least-privilege database account

This resolves §8.1 risk 2. A dedicated account was created on the shared
instance, and the application no longer connects as the superuser:

```sql
CREATE USER 'resibo'@'%' IDENTIFIED BY <redacted>;
GRANT ALL PRIVILEGES ON `resibo`.* TO 'resibo'@'%';
```

The resulting grants are exactly `USAGE ON *.*` plus `ALL PRIVILEGES ON
resibo.*` — **no global rights, and deliberately no `WITH GRANT OPTION`**, so
the account cannot widen its own access even if something inside Resibo is
compromised. The superuser is now reserved for migrations and backups run by a
person, and is not used by the application in any environment.

Isolation was proven by direct probing, not inferred from the grant statement:

| Probe | Result |
|---|---|
| Databases the account can see | `information_schema` and `resibo`, and nothing else |
| `CREATE TABLE` / `INSERT` / `DROP` inside `resibo` | Succeed — it owns its own schema completely |
| Reads against `gainsoclock`, `eloms`, `mysql` | **Denied**, `ER_TABLEACCESS_DENIED_ERROR` |
| `CREATE DATABASE` | **Denied** |
| `CREATE USER` | **Denied** |

**What remains, and is not done:** the host part is `%`, so the account is
connectable from any host that can reach the port. Narrowing it — to the
container's gateway, or to localhost — is a further hardening step, recorded as
§12 #14 rather than claimed. It is a smaller risk than the one just closed:
reaching the port at all requires being on the LAN.

#### The test datasource, and CI

The Spring context test now needs a datasource to start, which turns "where do
tests get a database?" into a question with consequences.

**CI answers it with a throwaway container.** The `build` job in
`.github/workflows/ci.yml` gained a **MariaDB 11 service container** — database
`resibo_test`, health-gated with
`healthcheck.sh --connect --innodb_initialized` — plus job-level
`DATASOURCE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` and `SERVER_PORT`
pointing at it. The reason is structural: **the real database sits on a private
network that GitHub-hosted runners cannot reach**, so CI could not use it even
if that were a good idea. Same approach as gainsoclock's `maven.yml`.

**Those credentials are deliberately not GitHub Secrets.** They are root/test
on a container created for one job and destroyed minutes later, on a runner
nobody else shares. Nothing is gained by hiding them, and a repository full of
secrets that are not secret makes the ones that are harder to take seriously.
The §8 rule is about credential **values that grant access to something that
outlives the job** — this is not one.

**A local `mvnw verify` has no such isolation.** It loads `.env.dev`, so it
connects to the shared production database. When this was written nothing could
write, because there were no entities — a property of the code being empty
rather than of the setup being safe. **That protection is gone**: there are now
three entities and real rows in that schema. §5 already recommended
Testcontainers; recorded as an open question (§12 #13) rather than a footnote.

#### Verified, not assumed (2026-11-24)

| Check | Result |
|---|---|
| Local `./mvnw -B clean verify` | **BUILD SUCCESS**, 2/2 tests. Hikari connected; the log reports `Database dialect: MariaDBDialect` |
| `GET /actuator/health` with details | `db: {status: UP, database: MariaDB, validationQuery: isValid()}` |
| DDL issued by the application | **None.** Zero DDL statements in the log, and the `resibo` schema still held **0 tables** after a run — `ddl-auto: none` proven, not assumed. (Checked before §5.4 created any table; the rule is unchanged, the table count is not) |
| Isolation of the `resibo` account | Probed directly; results in the table above |
| On the server after redeploy | Container `Up (healthy)`, zero DDL, and `information_schema.processlist` showed **10 live connections from the container as user `resibo` on database `resibo`**, with the superuser holding none — the account switch is confirmed in production, not merely configured |
| Public endpoint through the tunnel | Still `200` |

### 5.4 Authentication and security (2026-11-24)

Committed as `54c5e21`, `963c737`, `d30000f` and `8b57196`. Until these, every
endpoint was open to anyone who found the hostname. Now every endpoint needs a
token unless it is on a short public list.

**What it is, in one line:** stateless JWT. Log in, get a token, send it as
`Authorization: Bearer <token>` on every later call. HS256, 8-hour expiry —
roughly one shift on a till. No session, no cookie, nothing held server-side.

#### Dependencies

Five, listed in §5.1's table: the Spring Security starter, `jjwt-api`,
`jjwt-impl` and `jjwt-jackson` at **0.13.0** behind a `jjwt.version` property,
and `spring-boot-starter-security-test`. §5.1 also records why 0.13 and not
gainsoclock's 0.11.5, and that JWT code therefore cannot be copied between the
two projects.

#### Two new tables

`sql/tables/user_role.sql` and `sql/tables/user.sql`. Both `InnoDB`, both
`utf8mb4` / `utf8mb4_general_ci`, like every other table (`sql/README.md`).

> **Apply `user_role.sql` first.** `user` has a foreign key to it, so the other
> order fails.

`user_role`:

- `role_name`, unique and case-insensitive under the collation, so `admin` and
  `ADMIN` cannot both be inserted.
- **Seeded with `ADMIN` and `EMPLOYEE` by `INSERT IGNORE`**, which keeps the
  file idempotent and disturbs nothing that already exists.
- Roles are rows rather than an enum column, so adding one needs no schema
  change. The application still has to know the name — `types/UserRoleType` —
  and ignores a row whose name it does not recognise.

`user`:

- `username` `VARCHAR(100)`, **unique and case-insensitive**. The name used to
  sign up is the name that signs in, whatever the capitalisation.
- `password` `VARCHAR(100)`, holding a **BCrypt hash, never a plaintext
  password**. BCrypt output is 60 characters; the extra room is for a future
  algorithm change without a migration.
- `user_role_id`, foreign key to `user_role`, **`ON DELETE RESTRICT`** —
  deleting a role must never silently delete the people who hold it.
- `is_active`, so an account can be disabled instead of deleted. That matters
  once a user is referenced by sales records.
- `last_login_at`, written on every successful login.
- The four audit columns every table carries.

#### Who can reach what

Authentication and authorisation are all in `configs/SecurityConfig.java`, in
one place on purpose. **CORS is deliberately not there** — it moved to
`configs/CorsConfig.java` on 2026-11-24, because "which origins may read a
response" is a different question from "who may call this", it changes for
different reasons, and separating them means editing the allow-list cannot
disturb the filter chain. `SecurityConfig` keeps the `.cors(...)` opt-in;
without that line the bean is ignored and no CORS headers are written at all.

| Path | Who |
|---|---|
| `OPTIONS /**` | anyone — a preflight carries no token, and challenging it breaks every cross-origin call |
| `/api/auth/**` | anyone — signup and login |
| `/api/health/**` | anyone |
| `/actuator/**` | anyone |
| `/error` | anyone — see the traps below |
| `/api/admin/**` | ADMIN |
| `/api/employee/**` | ADMIN or EMPLOYEE |
| **everything else** | **any authenticated user** |

**The last row is the one that matters.** The chain ends in
`anyRequest().authenticated()`, so a new controller is protected the moment it
exists. Nobody has to remember to add a rule, and forgetting one fails closed.

Exactly two responses use a real HTTP error status:

| Status | Code | When |
|---|---|---|
| `401` | `E0401` | No token, a malformed token, a bad signature, or an expired one |
| `403` | `E0403` | A valid token whose role is not allowed here — logging in again would not help |

Both carry the normal `ResponseDto` envelope, written by
`JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler`. **Every business
error is still `200` with an error code**, as in gainsoclock. These two are the
exception because the request never reached the application.

`JwtFilter` rejects nothing. It reads the bearer token and, if the token is
good, sets the security context; a missing or bad token simply leaves the
request anonymous and lets `SecurityConfig` decide what that means. That is
what keeps every access rule in one file. The filter adds the `ROLE_` prefix
when building the authority, because `hasRole("ADMIN")` looks for
`ROLE_ADMIN` — the prefix is not stored in the database.

#### The signing key

| Key | Default | Note |
|---|---|---|
| `JWT_SECRET` | **none — startup fails** | Same rule as the three datasource keys |
| `JWT_EXPIRATION` | `28800000` (8 hours, in milliseconds) | Tunable per environment |

`jwt.secret` has **no default on purpose**. A fallback would mean every
environment that forgot to set one shared a signing key readable in a public
repository. `JwtUtil` builds the key in `@PostConstruct` and **fails the
application at boot if the secret is shorter than 32 characters**, which is
what HS256 requires — better at startup than at the first login.

**Local and production use different signing keys, deliberately.** A token
minted locally is worthless against production. The values live where §8 says;
neither appears in this document.

#### The role model, and the bootstrap problem

Two roles, `ADMIN` and `EMPLOYEE`. That is a working minimum, not an answer to
§1.3 — that question is open and the list is expected to grow.

Signup is **public**, and the rules on it are:

- The **first** account created on an empty system becomes ADMIN. Without that,
  there is no way to get an admin except by editing the database by hand.
- After that, an **anonymous signup is always EMPLOYEE**, whatever role it asks
  for. Only an authenticated ADMIN can create another ADMIN.

> ⚠️ **While signup is public and the `user` table is empty, whoever signs up
> first becomes the administrator.** On a publicly reachable API that is a race,
> and this API is publicly reachable (§7.3). The window is closed for the
> current deployment — an ADMIN account exists — but the mechanism is still
> there and it reopens for every new environment. **Create the admin account
> immediately after applying the schema.** Recorded as §8.1 risk 6 and §12 #17;
> closing it properly means requiring a token for signup, or an invite.

Password minimum is 8 characters. Nothing else is enforced yet.

Login returns `username`, `role` and `token` (commit `4883414` removed
`expiresInSeconds`; the lifetime is server-side only, and a client that needs
it can read the `exp` claim from the token). The role is
there so a frontend can decide what to render without decoding the token; the
token stays the only thing the server trusts, so a client editing that field
changes nothing about what it may do.

**There is no refresh token, no logout and no revocation.** A token is good for
its full 8 hours and cannot be withdrawn — disabling an account stops the next
login, not the token already issued. §12 #18.

#### New error codes

| Code | Meaning |
|---|---|
| `E0101` | username required |
| `E0102` | username longer than 100 characters |
| `E0103` | password shorter than 8 characters |
| `E0104` | username already exists — case-insensitively |
| `E0105` | invalid role |
| `E0106` | **invalid credentials** |
| `E0401` | not authenticated (HTTP 401) |
| `E0403` | no permission (HTTP 403) |

**`E0106` is deliberately one code for three failures**: unknown username,
wrong password, and disabled account. The message is identical too. Telling
them apart tells an attacker which usernames are real.

Signup returns a success envelope with no data. It confirms; it does not
disclose the account it created, not even without the hash.

#### CORS

Open to all origins, all methods, all headers, `maxAge` one hour, with
`Authorization` exposed so a browser can read it back from a cross-origin
response.

`allowCredentials` is **false**, which is a decision and not an oversight:
browsers reject credentials from a `*` origin, and nothing here needs them
because the token travels in a header rather than a cookie.

It is open so frontend work is not blocked. **Replace `*` with the real
frontend origins before this API holds live data** — §12 #19.

#### Products moved behind ADMIN

The product endpoints moved from `/api/products/**` to
`/api/admin/products/**`, and the controller is now
`controllers/admin/AdminProductController`.

**Naming rule from here on: an admin-only controller is `Admin<Name>Controller`
in `controllers/admin/`.** The class name and the path then say the same thing,
and the `/api/admin/**` rule covers it without a per-controller annotation.
gainsoclock does the same with its `admin/`, `client/` and `employee/`
sub-packages.

`created_by` and `updated_by` now record the signed-in username instead of the
literal `manual`. `SecurityUtil.currentUsername()` reads it from the security
context and falls back to `manual` when nobody is authenticated — which, under
the rules above, means only the public signup path.

#### Three traps hit while building this

Written down because all three will recur.

1. **A native `SELECT EXISTS(...)` returns `Integer`, not `Boolean`.**
   Declaring the repository method `boolean` throws
   `ClassCastException: Integer cannot be cast to Boolean` at runtime. Nothing
   catches it at compile time, JPQL would never have produced it, and it
   surfaced as a confusing `401` rather than as an obvious error. The fix is to
   return `Integer` and convert in a `default` method, which `UserRepository`
   now does. This is a direct cost of the switch to native SQL (§5.5) and will
   bite again on the next `EXISTS` or `COUNT`.
2. **`/error` was missing from the permitAll list.** Spring forwards unhandled
   errors to `/error`, so with that path protected **every 500 came back as a
   401** and hid the real failure. It is permitted now and must stay permitted.
   A `401` where a `500` was expected is the signature of this bug.
3. **Boot 4 uses Jackson 3.** `ObjectMapper` is
   `tools.jackson.databind.ObjectMapper`, not
   `com.fasterxml.jackson.databind.ObjectMapper`. The annotations did **not**
   move — `@JsonFormat` and friends are still `com.fasterxml.jackson.annotation`.
   So a file can legitimately import from both packages, which is exactly why
   this is easy to get wrong.

#### Verified, not assumed (2026-11-24)

Checked against the live public API, not inferred and not only local.

| Check | Result |
|---|---|
| First signup on an empty `user` table | Became **ADMIN** |
| Second signup, requesting ADMIN | Became **EMPLOYEE** — the requested role was ignored |
| Duplicate username in different capitalisation | `E0104` — the unique constraint is case-insensitive |
| Password under 8 characters | `E0103` |
| Unknown role name | `E0105` |
| Login | Returned `username`, `role` and a token — verified again after `4883414` |
| Wrong password | `E0106` |
| Unknown username | `E0106` — identical code and message, as intended |
| `/api/admin/products/**` with no token | `401` |
| the same with a garbage token | `401` |
| the same with an EMPLOYEE token | `403` |
| the same with an ADMIN token | `200` |
| CORS preflight from an arbitrary origin | The expected headers came back |
| The stored `password` column | A BCrypt hash, not the submitted password |
| `./mvnw -B clean verify` | **BUILD SUCCESS** — 22 tests |

### 5.5 Repositories are native SQL (2026-11-24)

**On the owner's instruction, every repository query is native SQL** —
`@Query(..., nativeQuery = true)` — rather than JPQL or a derived method name.
That holds for `ProductRepository`, `UserRepository` and `UserRoleRepository`,
and for whatever comes next.

Three consequences. All three have already cost time, so they are rules now
rather than observations:

1. **A paginated native query needs its own `countQuery`.** Spring Data can
   derive a count from JPQL. It cannot derive one from SQL, so without an
   explicit `countQuery` the total is unobtainable.
   `ProductRepository.search` carries one that repeats the `WHERE` clause.
2. **A `Sort` inside a `Pageable` must carry column names, not entity field
   names.** Spring Data appends `ORDER BY` using whatever string the `Sort`
   holds, verbatim: `Sort.by("productName")` produces `ORDER BY productName`,
   and the column is `product_name`. So `ProductServiceImpl` keeps a
   `SORT_COLUMNS` map — `productName` → `product_name`, and so on — and
   translates before building the `PageRequest`. It doubles as the **sort
   allow-list**: an unrecognised value falls back to `id`, so a typo cannot
   become a 500 and nothing a caller sends reaches the SQL verbatim.
3. **`SELECT EXISTS(...)` returns `Integer`.** §5.4's first trap.

Two things the collation buys, and they are why the queries do not wrap columns
in `LOWER()`: under `utf8mb4_general_ci` a plain `=` is already
case-insensitive **and** still uses the unique index, where `LOWER(column)`
would lose the index for no behavioural gain. The same applies to `LIKE`.

Every value is a bound parameter. Nothing is concatenated into a query string,
so the filters cannot be used to inject SQL.

---

### 5.6 The frontend client (2026-11-24)

A React application scaffolded with Vite, now living at **`client/`** on the
single **`dev/resibo`** branch. It renders, builds, type-checks and lints, is
deployed as static files (§7.4), and calls the API's health endpoint. **It is
not a POS screen and not a login screen.**

The health page includes a locally bundled SpongeBob dancing GIF as a visible
client pipeline test (2026-11-24). It changes no API behavior. Remove the
test image when this page becomes a product screen.

| Field | Value |
|---|---|
| Location | `client/` at the repository root, beside `server/` |
| Branch | `dev/resibo` — the single working branch for both halves |
| Scaffolded with | `npm create vite@latest` — create-vite 9.3.4, template `react-ts` |
| Vite / React | Vite **8.3.8** · React **19.3.1** |
| Language | TypeScript **7.0.2** with `strict`, plus `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` and `noImplicitOverride` |
| Styling | plain **SCSS** (`sass` 1.108) — tokens as CSS custom properties, one CSS Module per component |
| Routing | **React Router 7.18**, one route table at `src/app/router.tsx` |
| Linting / formatting | **oxlint** 1.95 — create-vite's default now, not ESLint — and **Prettier** 3.10 |
| Dev server | `localhost:5173`. **Calls the deployed API by default**, so `npm run dev` needs no backend, no database and no `JAVA_HOME`. Set `VITE_API_BASE_URL=/api` to use a local backend instead, routed through the dev proxy to `localhost:8083`. |
| Added to the template | `react-router-dom`; dev-only `sass` and `prettier`. Nothing else. |

#### Where it lives, and why that was a decision

The first scaffold used `src/main/client` on `dev/app/resibo`. That history and
the reasons for replacing it are in §7.1 and §13. The current layout puts
`client/` beside `server/` on one branch. The Docker build context is
`./server`, which keeps client files out of the API image. An endpoint and its
caller can now change in one commit; the API contract still needs review by
both owners when it changes.

#### Structure

Feature-first, with shared layers underneath:

```
client/src/
├── main.tsx              entry: mounts App, loads the one global stylesheet
├── App.tsx               app-wide providers (router today)
├── app/                  routing — route table and path constants
├── api/                  the HTTP layer; the only place that calls fetch
├── features/<name>/      a feature owns its api, types, hooks, components, pages
├── components/           shared, presentational, feature-agnostic
├── layouts/              page chrome with an <Outlet />
├── hooks/  lib/          shared hooks and framework-free helpers
├── pages/                pages belonging to no feature (404)
└── styles/               global only: tokens, reset, element defaults
```

**The import rule that keeps it honest:** a feature may import from `api/`,
`components/`, `hooks/`, `lib/` and `styles/`. **A feature must not import from
another feature.** When two features need the same thing it moves *down* into a
shared layer, never sideways. Nothing enforces this mechanically — the same
situation as §6.1 on the backend, and the same honest caveat.

#### The API layer is the part worth reviewing

`src/api/http.ts` is the only place in the client that calls `fetch`, because
this API's contract needs translating in exactly one place:

- **A failed request normally returns HTTP 200.** Business failures arrive as
  `responseType: 'ERROR'` with a code (`E0103`, `E0106`) in the envelope.
- **401 and 403 are the only real error statuses** (§5.4), and they carry the
  envelope too.
- So `http` unwraps `data` on success and throws a single `ApiError` on failure,
  exposing `isUnauthenticated` / `isForbidden` for the two cases where the
  difference matters. A caller never sees the envelope and never branches on a
  status code.

`src/api/types.ts` mirrors `ResponseDto` and `PageDto` field for field, and
flags the two envelope fields that are defects rather than features:
`responseTime` is UTC (§12 #8), and `className` / `methodName` are internal
detail on a public response (§12 #10).

#### Which API it talks to

**The default is the deployed API**, `https://resibo-api.polbinsenthabyer.uk/api`,
set in `src/config/env.ts`. The owner asked for it directly, and it makes the
client runnable on its own: `npm install && npm run dev`, with no backend
process, no database and no JDK. The dev proxy still exists and is used by
setting `VITE_API_BASE_URL=/api`, which makes the browser see one origin.

⚠️ **This points development at production**, which is a trade rather than a
convenience. It is harmless today because the only call the app makes is a
`GET` health check. It stops being harmless at the first screen that writes,
when a product form on a laptop would edit the live catalog. Two mitigations
are in place and neither is sufficient alone: `env.isUsingDeployedApi` is
exported and the status page renders a visible notice from it, and the page
always prints the base URL it is calling. **Revisit the default before the
first mutating screen** — it is one constant.

CORS is not an obstacle: backend `1ad0803` made the allowed origins
configurable through `CORS_ALLOWED_ORIGINS`, defaulting to `*` and naming
`http://localhost:5173` explicitly. Checked against the live host rather than
assumed.

#### Deliberately not built

- **No login screen**, though `features/auth` types and calls both endpoints.
  What each role must *not* be able to do is §1.3, unanswered.
- **No money or date formatting.** Currency, date format and timezone are blank
  in §10, and `responseTime` serialises in UTC. A formatter would have had to
  guess, and a POS prints those values on receipts — so `features/products`
  documents that totals must not be computed client-side instead of shipping a
  helper that quietly does it.
- **No tests and no test runner.** §5 proposes Vitest; nothing is installed.
- **No state or data-fetching library.** A small `useAsync` hook covers one
  health call.
- **No deployment path of any kind** — no CI build step, no nginx vhost, no
  hostname, no container. See §12 #22.

#### Verified

| What | Result |
|---|---|
| `npm run build` (`tsc -b && vite build`) | **passes** — 47 modules, 317 kB JS, 3.5 kB CSS |
| `npm run lint` (oxlint) | **clean**, no warnings |
| `npx prettier --check` | clean |
| `./mvnw -B clean compile` after the client landed | **BUILD SUCCESS**, 32 sources, `src/main/client` untouched |
| The deployed API, which is now the default target | **verified by `curl`** — `GET /api/health/check` returns **200**, and the CORS preflight carrying `Origin: http://localhost:5173` is answered with `access-control-allow-origin: http://localhost:5173`. The default target answers, and it permits the dev server. |
| The client in a browser | **still not verified.** `npm run dev` has not been run, so the page itself has never been loaded. The build, the types, the lint and the endpoint are each evidenced; the two halves have never been put together. |

---

## 6. Architecture

### 6.1 Backend structure — layer-first in the code, modular monolith deferred

**What the code actually is, as of 2026-11-24.** The backend is packaged
**layer-first at the base package**, mirroring gainsoclock:

```
src/main/java/com/api/resibo/
├── ResiboApplication.java
├── configs/         SecurityConfig
│   └── jwt/         JwtUtil, JwtFilter, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler
├── controllers/     AuthController, HealthController
│   └── admin/       AdminProductController
├── dtos/            ResponseDto, PageDto, LoginDto, LoginResponseDto, SignupDto, ProductDto, ProductListDto
├── entities/        Product, User, UserRole
├── repositories/    ProductRepository, UserRepository, UserRoleRepository
├── services/
│   ├── auth/        AuthService + AuthServiceImpl
│   └── product/     ProductService + ProductServiceImpl
├── types/           ResponseType, UserRoleType
└── utilities/       MessageType, ResponseUtility, SecurityUtil, UserUtility
```

There is no `modules/` package and no `shared/` package. `entities/`,
`repositories/` and `services/` arrived flat, in gainsoclock's shape, with the
first features that needed them — services get a sub-package per feature
(`auth/`, `product/`) and controllers a sub-package per audience (`admin/`), as
gainsoclock does.

That is **option A of §6.1.1**, not the modular monolith described below. It was
taken because the owner's most recent and most specific structural instruction
was to use gainsoclock as the reference, and the two instructions cannot both be
honoured as written. **It supersedes the earlier modular-monolith decision — see
§13 — and it is pending the owner's explicit confirmation**, which is why
§6.1.1 stays open rather than being marked answered. **That reversal has stopped
being cheap.** When this was first written the backend was six files; it is now
around thirty, across auth, security and the product catalog, and every one of
them would move. If the answer is B, it needs saying now rather than after the
next feature.

**The deferred alternative, for the record.** What follows is the
modular-monolith design as originally decided. **None of it exists on disk.** It
is kept because it was a deliberate owner decision with a real rationale, and
because it is still the better answer if module boundaries end up mattering more
than parity with the sibling project — if §6.1.1 comes back as B, this is the
target.

What it would mean in practice:

- **Package by feature, not by layer.** No top-level `controllers/`,
  `services/`, `repositories/` — which is precisely what the code has today, so
  this is the bullet that option A trades away. Each module would own its own
  web, service, and persistence code.
- **Each module has a public face and a private inside.** Other modules may use
  only what the module deliberately exposes — its API interfaces and its DTOs.
  Entities and repositories stay package-private to the module.
- **Modules never reach into each other's tables.** Cross-module reads go
  through the owning module's interface. This is the rule that keeps a modular
  monolith from quietly becoming a tangle, and the one that makes extracting a
  module later a possibility rather than a rewrite.
- **One database, one schema, but each table has exactly one owning module.**
  Flyway migrations are named by owning module.

The layout it would produce — **aspirational; not a description of the
repository**:

```
backend/src/main/java/.../resibo/        (note: the real code is at the repo root — §5.1)
├── ResiboApplication.java
├── shared/                  cross-cutting, no business rules
│   ├── money/               money and rounding types
│   ├── error/               error model, exception handling
│   ├── audit/               audit event recording
│   └── config/              Spring, security, web config
└── modules/
    ├── identity/            users, roles, permissions, tokens
    ├── catalog/             products, variants, barcodes, prices, taxes
    ├── pricing/             discounts, promotions, statutory discounts
    ├── sales/               ring up, tender, void, return, receipt numbering
    ├── inventory/           stock movements, on-hand, counts
    ├── cashdrawer/          shifts, cash movements, X and Z readings
    ├── customers/           customers, credit accounts
    ├── purchasing/          suppliers, purchase orders
    └── reporting/           read-side queries across modules
```

### 6.1.1 `[BLOCKER]` Reference project: gainsoclock — and a conflict to resolve

The owner named `%USERPROFILE%\sprintify_solutions\personal\gainsoclock` as the
reference for clean structure (2026-11-24). Its backend is Spring Boot 4.1.2 on
Java 17, Maven, MariaDB — the same shape as Resibo.

**gainsoclock is organised layer-first, not by feature:**

```
com/gainsoclock/backend/
├── configs/        SecurityConfig, CorsConfig + jwt/ (filter, util, entry point, denied handler)
├── controllers/    + admin/ client/ employee/ sub-packages
├── dtos/           flat: XxxDto, XxxListDto, ResponseDto, LoginDto...
├── entities/       JPA entities, flat
├── repositories/   Spring Data interfaces, flat
├── services/       + admin/ client/ employee/ — interface + XxxServiceImpl pairs
├── types/          enums, all suffixed *Type
└── utilities/      ResponseUtility, SecurityUtil, QRCodeUtil, UserUtility...
```

**This directly contradicts §6.1.** A modular monolith is packaged *by feature*
with module-private internals; gainsoclock is packaged *by layer* with
role sub-packages. Both are defensible; they are not combinable as written, and
the choice sets every file path in the backend. So it needs an explicit answer
rather than a quiet compromise:

- [ ] **A — Follow gainsoclock exactly.** Layer-first, as above. Familiar,
      consistent with the sibling project, fastest to start. Abandons the
      modular-monolith decision; module boundaries cannot be enforced, because
      there are no modules.
      **← this is what the code does today (§6.1, §5.1), pending the owner's
      confirmation. The box stays unticked because the owner has not said it.**
- [ ] **B — Modular monolith, with gainsoclock's conventions inside each module.**
      Keep feature modules and their boundaries; inside each one, use
      gainsoclock's familiar layer packages. Reads like gainsoclock locally while
      keeping the `sales` module from reaching into `inventory`'s tables.

      ```
      modules/sales/
      ├── api/            the only package other modules may import
      ├── controllers/
      ├── services/       interface + XxxServiceImpl
      ├── dtos/
      ├── entities/       package-private
      ├── repositories/   package-private
      └── types/
      ```

- [ ] **C — Layer-first for v1, restructure later.** Honest about speed, but
      "later" rarely arrives, and the restructure is the expensive kind.

**My recommendation: B.** It is the only option that honours both instructions —
the modular monolith you chose and the gainsoclock conventions you want the code
to read like. A costs you the boundaries; C defers the cost at interest.

**Answer:** *Still needed from the owner.* **A is what the scaffold did on
2026-11-24**, because "use gainsoclock as the structural reference" was the
later and more specific instruction and something had to be chosen to put a file
on disk. That is an interpretation, not an answer — so this question stays open,
and a one-word reply closes it. **The cheap window has closed.** The backend was
six classes in four packages when this was written; authentication and the
product catalog took it to around thirty across ten, so switching to B is now
hours of mechanical moving rather than minutes. Every further feature adds to
that bill, which is option C's warning arriving on schedule.

### 6.1.2 Conventions adopted from gainsoclock

Independent of the layout question above, these carry over. They are what makes
the reference project read consistently, and none of them conflict with §6.1.

| Convention | Detail |
|---|---|
| Service shape | Interface + `XxxServiceImpl` pair, never a bare class |
| Uniform response | Every service method returns `ResponseDto` |
| `ResponseDto` fields | `responseType`, `code`, `data`, `message`, `className`, `methodName`, `responseTime`, plus `isSuccess()` / `isFail()` |
| Response construction | Static factories — `ResponseUtility.getSuccessResponse(message[, data])`, `getErrorResponse(code, message)` |
| Result codes | Short coded strings (`S0001`, error codes per case) alongside the message |
| DTO pairing | `XxxDto` for a single item or write, `XxxListDto` for a query or list |
| Enums | In `types/`, suffixed `*Type` |
| Controller naming | An admin-only controller is `Admin<Name>Controller` in `controllers/admin/`, so the class name and the `/api/admin/**` path agree (§5.4) |
| Boilerplate | Lombok `@Data` / `@NoArgsConstructor` / `@AllArgsConstructor` on DTOs |
| Shared helpers | `utilities/` for stateless static helpers |
| Config | `application.yaml`; `.env` and `.env.*` git-ignored |
| Deployment | `scripts/deploy.sh`, `scripts/deploy-prelive.sh`; a GitHub Actions workflow (gainsoclock's is `maven.yml`; Resibo's is `ci.yml`) |

**Which of those exist as of 2026-11-24.** All of them except
`scripts/deploy-prelive.sh`. The envelope and its static factories; the success
code `S0001` alongside a growing set of error codes; `ResponseType` and
`UserRoleType` as `*Type` enums in `types/`; Lombok on every DTO; the
`XxxDto` / `XxxListDto` pairing (`ProductDto` and `ProductListDto`); the
interface + `XxxServiceImpl` pair for both services, each returning
`ResponseDto`; message constants in `MessageType` rather than strings at call
sites; `utilities/` holding `ResponseUtility`, `SecurityUtil` and `UserUtility`;
the `Admin<Name>Controller` rule; and `application.yaml` with `.env*`
git-ignored.

`ResponseUtility` fills `className` and `methodName` with `StackWalker`, taking
the first stack frame outside itself — cleaner than gainsoclock's approach, same
result, and verified to resolve correctly (§5.1).

Added since, and recorded elsewhere in this document: `scripts/deploy.sh` in
gainsoclock's shape (§7.3), the committed `.env.example` the §8 rules promised,
carrying variable names with empty values (§5.1, §5.3), and a GitHub Actions
workflow — `.github/workflows/ci.yml`, not gainsoclock's `maven.yml` filename —
whose `build` job runs `mvnw -B clean verify` against a throwaway MariaDB
service container (§5.3).

Still absent: `scripts/deploy-prelive.sh`, because no prelive environment has
been decided (§7, §12 #11).

One deliberate divergence from gainsoclock, recorded so it is not read as a
mistake: `UserUtility` returns **entities** rather than `ResponseDto`, returns
`null` for a missing role row instead of throwing, and its existence checks
actually check — gainsoclock's `isUserExistById` tests an object that is never
null, so it always returns `true`. Building the response is the service's job,
which is why the utility does not do it.

| Question | My recommendation | Your decision |
|---|---|---|
| Keep the `ResponseDto` envelope, including `className`/`methodName`? | Keep it for consistency with gainsoclock. Note that `className`/`methodName` leak internals to the client — I'd keep them out of error responses in production. | **Kept, and implemented 2026-11-24** — all seven fields plus `isSuccess()`/`isFail()`, for gainsoclock consistency. **The caveat still stands, is unaddressed, and its condition has now arrived:** `className`/`methodName` are populated on every response, errors included, and the API became reachable from the public internet on 2026-11-24 (§7.3) — the verified production response names `HealthController` and `healthCheck` in its body. "Before anything is exposed publicly" has passed; tracked as §12 #10. |
| Match gainsoclock's Spring Boot 4.1.2 / Java 17 exactly? | Match Spring Boot 4.1.x. For Java, prefer 21 (LTS, and Boot 4 supports it) unless the server's JDK is 17 — §7 will decide that. | **Java 17 as gainsoclock; Spring Boot 4.2.0, not 4.1.2** — the current latest release, and what Initializr generated. Java 17 because it is the JDK that exists on this machine (§5.1). **The server's JDK has stopped being a question:** the container image carries its own, so the server needs only Docker (§5.2, §7.3). |

**Open questions on the module structure.** All four only arise if §6.1.1 is
answered **B**; under the layer-first code that exists today there are no
modules to bound, which is exactly the cost option A carries.

| Question | My recommendation | Your decision |
|---|---|---|
| Enforce boundaries mechanically, or by convention? | Mechanically — ArchUnit tests, or Spring Modulith. A boundary nobody checks is a boundary that erodes. | |
| Cross-module communication: direct interface calls or in-process events? | Direct calls for queries; events for side effects (e.g. a sale updating stock). Keeps the sale from depending on inventory being fast. | |
| One Maven module or one per feature? | One Maven module to start, with ArchUnit enforcing the rules. Split only if the rules stop holding. | |
| Is `reporting` a module or a separate read layer? | A module, reading through other modules' APIs — unless reports get slow, then dedicated read models. | |

**Your corrections to the layout above:**

**Answer:**

### 6.2 System shape — pending §2.1

To be finalised once §2.1 is answered, because the answer changes it. The sketch
below assumes **2.1 = B (offline-tolerant)** and will be replaced, not patched,
if that changes.

```
Till (browser, React SPA)
  ├── local cache: catalog, prices, open shift      [only if 2.1 = B or C]
  ├── outbox: completed sales awaiting upload       [only if 2.1 = B or C]
  └── HTTPS / JSON
        │
        ▼
Spring Boot application  (one deployable; packaged per §6.1 — layer-first today)
        │
        ▼
MariaDB   ──►  nightly backup, off-machine   [see §10]
```

**Open architectural questions:**

- Does the till print directly, or does the server render and the till relay?
  (Depends on §2.5.)
- Where does receipt numbering live? Per-terminal sequences survive an outage;
  a single server-side sequence does not.

**Answer:**

---

## 7. Environments, hosting, and domain

Production exists as of 2026-11-24, and the honest version of the table is
below. **"Production" here means the API is reachable, not that there is a POS
behind it** — it serves health, auth, and an admin-only product catalog (§5.4).

| | Local | CI | Staging / prelive | Production |
|---|---|---|---|---|
| Needed? | yes | yes | probably, eventually — §12 #11 | yes |
| Status | working | **working since 2026-11-24** | **not built** | **live since 2026-11-24** |
| Where it runs | this machine, from `mvnw` or from the container | a GitHub-hosted runner, `.github/workflows/ci.yml` | — | the shared Ubuntu server, in Docker behind the host's nginx |
| Database | **the shared MariaDB on the server**, via `.env.dev` — the same instance *and schema* production uses, which is the problem recorded as §12 #13 | a throwaway MariaDB 11 service container, `resibo_test` (§5.3) | — | **the `resibo` schema on the shared MariaDB**, as the least-privilege `resibo` account. **3 tables** — `product`, `user_role`, `user` |
| URL | `http://localhost:8083` with the current `.env.dev`, `:8080` without it | — | — | `https://resibo-api.polbinsenthabyer.uk` |
| What it serves | the eight endpoints in §5.1 | nothing — it builds and tests | — | the same eight endpoints |

| Question | Answer |
|---|---|
| Server | Self-hosted on the owner's LAN, Ubuntu, shared with the sibling projects. Address, account, and password are in the handover file (§8) — **deliberately not written here, because this repository is public.** |
| Server OS / specs | Ubuntu (version still unconfirmed — not needed now that the build runs in a container) |
| How we deploy (git pull + restart / Docker / CI pipeline) | **Docker, from a git checkout on the server** — `git pull` then `scripts/deploy.sh`, which builds the image there, restarts, health-gates, and rolls back on failure. Full procedure in §7.3. `.github/workflows/ci.yml` now exists: its `build` job is the test gate (and the reason for the CI service container, §5.3), and it **declares** a `deploy` job that runs the same `scripts/deploy.sh` on a self-hosted runner labelled `resibo-deploy`. The manual command in §7.3 is still the documented path, and the CI-driven one is not described there — see §12 #16. |
| Domain name | `resibo-api.polbinsenthabyer.uk` — a subdomain of the zone that already serves the sibling projects. No new domain was bought. |
| Cloudflare role: DNS only / proxied / Tunnel / Access in front of admin | **Proxied DNS over a Cloudflare Tunnel.** The existing `personal-projects` tunnel is reused rather than a new one created; a `CNAME` points the hostname at it. **No edge authentication sits in front of it** — Cloudflare Access is not configured, so anything the application does not protect itself is open to the internet. Since 2026-11-24 the application does protect itself (§5.4), which changes this from a live hole to a hardening question: §12 #12. Public signup is the part that is still open by design (§8.1 risk 6). |
| TLS: Cloudflare origin cert / Let's Encrypt | **Neither.** TLS terminates at Cloudflare's edge and the tunnel carries the request to the origin, so the origin holds no certificate: nginx listens on plain HTTP on loopback, and the container is published on loopback. Nothing on the LAN speaks TLS and nothing needs to. |
| Who else needs server access | Open — nobody but the owner today. |

### 7.1 Branches (decided)

**Decided by owner, 2026-11-24:**

| Branch | Holds |
|---|---|
| `main` | `README.md` only — the public face of the repository |
| **`dev/resibo`** | **everything**: `client/`, `server/`, `scripts/`, `docker-compose.yml`, `CLAUDE.md` and this plan. The single working branch. |

**Superseded 2026-11-24:** `dev/api/resibo` and `dev/app/resibo` are
retired. The `api`/`app` split was created so backend and frontend work
could proceed independently, and it did not deliver that — independence
comes from the CI path filters and the disjoint deploy targets described in
§7.4, which work equally well on one branch. What the split did deliver was
drift: two copies of this document, both calling themselves version 0.8,
inside a single day. See §13 and §12 #23.

Remote: `https://github.com/polbinsenthabyer/resibo.git` — **public** (see §7.2).

**Naming (decided by owner, 2026-11-24):** the `api` segment distinguishes
backend work from frontend work, matching the sibling project [gainsoclock],
which uses `live/api/...` and `live/app/...`. A frontend branch would therefore
be `dev/app/resibo`. The original `dev/resibo` was renamed to `dev/api/resibo`
and the old name deleted locally and on the remote; the commits are unchanged.

Consequences, so no session is surprised by them:

- **This plan is not on `main`.** A session that clones the default branch sees
  only the README. The README points at the plan and names the branch; follow it.
- `dev/api/resibo` is where work happens. `main` receives only what should be public
  and stable.
- Keeping the plan off `main` means it must be updated on `dev/api/resibo`, and any
  README change the plan triggers has to reach `main` separately.

**Also decided by owner, 2026-11-24:**

- **All work commits directly to `dev/api/resibo`.** No feature branches for now, no
  pull requests. Application code, documentation, migrations — all of it goes to
  `dev/api/resibo`.
- **`main` holds `README.md` and nothing else.** It is not a release branch and
  does not receive application code. The only commits `main` ever gets are README
  updates.

**Amended again 2026-11-24, when the repository became a monorepo.** The
standing instruction is now the simplest it has been: **all work commits
directly to `dev/resibo`.** `main` still receives only README changes.

A commit may touch `client/`, `server/`, or both. Touching both is normal
and correct for a change that spans the contract — an endpoint and its
caller — and it is the thing the two-branch layout made impossible.
Which pipeline runs is decided by the path filters in §7.4, not by where
the commit sits.

On disk there is one checkout, `personal/app/resibo`. ⚠️ A second clone at
`personal/resibo` still exists on the retired `dev/api/resibo` and should
be deleted or re-pointed — two trees for one branch is how the last
divergence started.

Because `dev/app/resibo` was cut *from* `dev/api/resibo` rather than from
`main`, it carries a complete copy of the backend, this plan and `CLAUDE.md`.
Two consequences, and the second one is a trap:

- A session in the frontend checkout has this plan and the project instructions
  in front of it. That is the reason the branch was cut from here rather than
  from `main`, which holds only the README.
- **There are now two copies of this document on two branches, and nothing keeps
  them in step.** It bit on the very first frontend commit: the branch was cut
  at `4883414`, and `0135659` and `e125c6e` landed on the backend branch while
  the client was being written, so §5.4, §5.5 and §12 #17–19 appeared
  underneath it. **Rebase `dev/app/resibo` onto `dev/api/resibo` before every
  frontend commit** — that is now the expected step, and §12 #23 tracks the
  wider question.

On disk the two branches are two separate clones, following gainsoclock's
layout — `personal/resibo` for the API branch, `personal/app/resibo` for the
app branch (§0). They are independent clones rather than `git worktree`s, so
each fetches on its own, and one can be several commits behind the other
without anything saying so.

**Push policy (decided by owner, 2026-11-24):** push to the remote. Sessions do
not need to ask first.

**Commit messages (owner's instruction, 2026-11-24):** meaningful, brief, and
precise. Conventional Commits, imperative mood — `feat:`, `fix:`, `docs:`,
`chore:`, `refactor:`. A one-line subject is the norm; add a body only when the
change genuinely needs explaining, and keep it short when you do.

Never force-push. A rejected push is reported, not overridden.

---

### 7.2 Verified infrastructure (checked 2026-11-24)

What was actually tested, not assumed:

| Item | Result |
|---|---|
| Server reachable | Yes — ICMP and TCP/22 both open |
| SSH host key | ED25519, `SHA256:9b3TMT0pLsHF7uuhLHcHvuPilT3l8xDTHlIiKcfZRPc`, recorded in `known_hosts` |
| SSH auth methods offered | `publickey, password` |
| SSH login by assistant | **Working as of 2026-11-24** — the §7.3 deployment was carried out on the server. It was not working when this table was first written: password auth needs an interactive terminal, which an agent session does not have. Key-based access is still the right end state, and the credentials still need rotating (§8.1 risk 1). |
| Docker and the Compose plugin on the server | Present and working — the image builds there and the container runs (§7.3) |
| nginx on the server | Present, with a `sites-available` / `sites-enabled` layout; `nginx -t` passed before reload (§7.3) |
| Cloudflare tunnel `personal-projects` | Healthy, four connections, already fronting the sibling projects before Resibo was added (§7.3) |
| Database reachable | Yes — TCP/3306 open |
| Database connection | **Working**, verified with a real query |
| MariaDB version | 11.8.6-MariaDB-5ubuntu0.1 (Ubuntu) |
| Server charset / collation | `utf8mb4` / `utf8mb4_uca1400_ai_ci` |
| `resibo` schema | Exists, and **holds three tables** as of 2026-11-24 — `product`, `user_role`, `user`, all applied by hand from `sql/tables/` with the administrative account. None of them was created by Hibernate; `ddl-auto` is still `none` (§5.3) |
| Least-privilege `resibo` account | **Created and in use 2026-11-24.** Grants are `USAGE ON *.*` plus `ALL PRIVILEGES ON resibo.*`, no `GRANT OPTION`; isolation probed, not inferred (§5.3). Resolves §8.1 risk 2 |
| The application's connection, from production | `information_schema.processlist` showed **10 connections from the container as `resibo` on database `resibo`**, superuser none (§5.3) |
| Shared host | ⚠️ The same MariaDB instance also hosts unrelated databases for other projects |
| GitHub repository visibility | ⚠️ **Public** — confirmed via the GitHub API |

**Three consequences worth acting on:**

1. **The repository is public.** Everything committed is world-readable, now and
   in history. No infrastructure addresses, usernames, or internal hostnames go
   into it — hence the redaction in the table above. Verified 2026-11-24 that no
   credential value appears in any commit on any branch.
2. **The database is shared with other projects, and that is why the
   application does not connect as the superuser.** A bug in Resibo running as
   `root` could read or destroy unrelated project data; §8's "use a
   least-privilege app user" was therefore the actual fix rather than good
   practice, and it was **done on 2026-11-24** (§5.3, §8.1 risk 2). What is left
   of the risk is the account's `%` host part (§12 #14), not its privileges.
3. **The `resibo` schema is no longer empty.** It holds `product`, `user_role`
   and `user` (§5.4). Each arrived as a reviewed file in `sql/tables/`, run by
   hand with the administrative account — not from Hibernate, which is still
   forbidden from issuing DDL (§5.3). The §4 domain model can still be designed
   freely; it now has three tables to reconcile with rather than none, and
   nothing records which environment has which (§12 #15).

**Open:** Ubuntu version, and whether MariaDB should stay shared or get a
dedicated instance for Resibo. The server's JDK is no longer open and no longer
matters — the container brings its own (§5.2).

### 7.3 Deployment, as it actually works (2026-11-24)

Written down in operational detail because the next person to deploy will be a
session with no memory of this one, and because one step of it can take the
neighbouring services down silently.

**What is deployed.** A Docker container on the shared Ubuntu server,
reverse-proxied by the host's nginx, fronted by the Cloudflare tunnel that
already serves the sibling projects. It answers the eight endpoints listed in
§5.1: health, actuator, signup, login, and four admin-only product operations.
It connects to the `resibo` schema as the least-privilege `resibo` account
(§5.3) and, since 2026-11-24, **authenticates with JWT** (§5.4) — which means
the env file now also needs `JWT_SECRET`, without which the container will not
start. It still implements no part of a checkout.

#### On the server

| Path or object | What it is |
|---|---|
| `/opt/resibo/app` | A git checkout of `dev/api/resibo`. Cloning needed no credential, because the repository is public (§7.2) — the one convenience that awkward fact buys. |
| `/opt/resibo/resibo.env` | The environment file, mode `0600`, **outside the checkout and not in git**. As of 2026-11-24 it holds `APP_ENV=prod`, **the datasource keys** — `DATASOURCE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (§5.3) — and **`JWT_SECRET`** (§5.4). It is therefore a real secret store now, not a one-line convenience: the `0600` mode and its location outside the checkout stopped being precautionary. Without it the application does not start at all, because neither the datasource keys nor the JWT secret have defaults. **Production's signing key differs from the local one on purpose**, so a token minted on a developer's machine is worthless here. |
| Image `resibo-api:current` | What is running. `:previous` is kept for rollback; `:next` is the staging tag used during a deploy. |
| Container `resibo-api` | `restart: unless-stopped`, published on `127.0.0.1:8083`, reported `Up (healthy)`. |
| `/etc/nginx/sites-available/resibo` | The vhost, symlinked into `sites-enabled`. |

The deploying OS account was added to the `docker` group, which takes effect at
its next login. **This first deployment therefore ran under `sudo`**, and the
command below keeps the `-E` that carries the exported variables through.

#### Deploying

```sh
cd /opt/resibo/app
git pull
sudo -E bash scripts/deploy.sh
```

The script refuses to run if the env file named by `RESIBO_ENV_FILE` (default
`/opt/resibo/resibo.env`) is missing, rather than starting on defaults and
leaving you to work out why production thinks it is `dev`.

#### What the script does, and how rollback works

`scripts/deploy.sh` is deliberately the same shape as
`gainsoclock/scripts/deploy.sh` — stage the new build, restart, health-check,
roll back if it fails — with one substitution: **the artifact kept for rollback
is an image tag, not `app.jar.previous`.**

1. Build `${IMAGE}:next`. The running container is untouched, so a build failure
   costs nothing but time.
2. If `:current` exists, tag it `:previous`. On a first deployment it does not,
   and the script records that it does not.
3. Tag `:next` as `:current`.
4. `docker compose up -d --no-build`.
5. Poll `http://127.0.0.1:8083/actuator/health` up to **30 times at 2-second
   intervals** — about a minute, which comfortably covers Spring Boot's start-up.
6. On success: prune the build-stage images and exit `0`.
7. On failure: print the last 50 lines of the container log, retag `:previous`
   back to `:current`, restart, and exit `1`. **If there was no previous image,
   `compose down` instead** — a failed first deployment leaves nothing running
   rather than something broken.

To roll back by hand afterwards — the script only rolls back the deploy it is
running:

```sh
cd /opt/resibo/app
docker tag resibo-api:previous resibo-api:current
RESIBO_TAG=current docker compose up -d --no-build
```

**Two more scripts exist and are only named here, not yet described.** Recorded
so no session concludes `deploy.sh` is all there is: `scripts/rollback.sh`
performs exactly the manual swap above — for the case where the deploy
succeeded, health passed, and the build turned out to be bad anyway — and
`scripts/setup-server.sh` is an idempotent server bootstrap that creates the
deployment directory, the env file, the nginx vhost, and a **self-hosted GitHub
Actions runner** labelled `resibo-deploy`. The runner is the other half of the
CI `deploy` job, and the whole CI-driven path is the documentation gap tracked
as §12 #16. Until that is closed, the manual command above is the procedure of
record.

#### The nginx vhost

Four directives do the work, and they match the pattern the sibling services
already use — matching it was the point, not a coincidence:

| Directive | Value | Why it matters |
|---|---|---|
| `listen` | `127.0.0.1:80` | nginx is not on the LAN either. Only processes on the host — in practice the tunnel daemon — can reach it. |
| `server_name` | `resibo-api.polbinsenthabyer.uk` | This is how **one** tunnel ingress rule fans out to several applications. |
| `proxy_pass` | `http://127.0.0.1:8083` | The container's published host port. |
| `proxy_set_header Host` | `$host` | Without it the upstream sees nginx's own name instead of the requested hostname. |

`nginx -t` passed before the reload. Do that every time; a bad vhost takes down
every site on the host, not just this one.

#### The Cloudflare tunnel — and the one mistake not to make

The tunnel is named **`personal-projects`**, is healthy on four connections, and
already fronted gainsoclock, gainsoclock's prelive, the gainsoclock app
hostnames, and eloms before Resibo existed. Every one of those ingress rules
points at `http://localhost:80` — that is, at nginx, which sorts them out by
`server_name`. Resibo joins the identical pattern:

- a new ingress rule, `resibo-api.polbinsenthabyer.uk → http://localhost:80`,
  **inserted before the catch-all `http_status:404` rule** — a rule placed after
  the catch-all never matches anything;
- a proxied DNS record, `CNAME resibo-api → <tunnel-uuid>.cfargotunnel.com`,
  TTL auto.

> ⚠️ **The tunnel configuration endpoint takes the whole ingress list, not a
> patch.** A `PUT` carrying only Resibo's rule *replaces* the configuration and
> silently deletes gainsoclock's, its prelive's and eloms' rules. The tunnel
> stays healthy, the neighbours start returning 404, and nothing anywhere
> reports an error — the worst shape an outage can take.
>
> **So: read the current configuration, back it up, append to the list, and PUT
> the complete list with the catch-all last.** That is what was done here; the
> full prior configuration was saved first and every pre-existing rule was
> preserved, which is why the neighbour checks below pass.

Access to all of that is a **scoped API token**, inventoried in §8: Account →
Cloudflare Tunnel:Edit, Zone → DNS:Edit, Zone → Zone:Read, on the one zone and
no other. The value lives in the handover file and is written nowhere else.

#### Ports on this server

| Host port | Service |
|---|---|
| 8081 | gainsoclock |
| 8082 | gainsoclock prelive |
| **8083** | **resibo** |
| 8088 | eloms |

All of them loopback-bound. Two things to keep straight:

- **The container always listens on 8080 internally** (§5.2), whatever the host
  port. 8083 is purely a host-side mapping, set by `HOST_PORT`.
- **The local `.env.dev` sets `SERVER_PORT=8083`, matching the host port the
  server publishes.** It used to set 8088, which is **eloms' port** — harmless,
  because nothing reads `.env.dev` on the server, but it made every
  local-versus-deployed comparison read wrong. That was corrected on 2026-11-24
  (§5.3); the earlier warning here is **resolved**, not live. 8088 is still
  eloms', so it remains a value never to put in a Resibo env file.

#### Verified end to end from the public internet

Already verified on 2026-11-24, not inferred:

| Check | Result |
|---|---|
| `GET https://resibo-api.polbinsenthabyer.uk/api/health/check` | `200`, envelope intact — `responseType: SUCCESS`, `code: S0001`, `message: "Resibo API is healthy!"`, `data: null`, `className`/`methodName` resolved to `HealthController.healthCheck`. `Server: cloudflare`, `CF-RAY` ending `-MNL`. |
| `GET https://resibo-api.polbinsenthabyer.uk/actuator/health` | `200` |
| The container directly, on the server | `200` on `127.0.0.1:8083` |
| Through nginx by `Host` header, on the server | `200` |
| gainsoclock API, after the tunnel change | `200` — unaffected |
| eloms API, after the tunnel change | `401`, which is its normal unauthenticated response — unaffected |
| After the database redeploy (2026-11-24) | Container `Up (healthy)`, zero DDL in the log, **10 connections from the container as `resibo` on database `resibo`** and none from the superuser, and the public endpoint still `200` through the tunnel (§5.3) |

**Two things the deployment did not fix, and was not meant to.** The §10 UTC
`responseTime` defect is now live in production — the verified response read
`03:57` where local time was `11:57`. And `className`/`methodName` now leak to
the public internet, which §6.1.2 said had to stop *before* public exposure
(§12 #10). Neither is cosmetic; both are recorded rather than quietly patched,
for the reasons in §13.

### 7.4 Deploying the client, and why it cannot redeploy the API (2026-11-24)

The React client is **live** at **`https://resibo.polbinsenthabyer.uk`**, and
it is deployed by a path that shares nothing with the API's.

| | Backend | Frontend |
|---|---|---|
| Artifact | Docker image `resibo-api:current` | static files from `client/dist` |
| Lives at | container on `127.0.0.1:8083` | `/var/www/resibo/live` |
| Deployed by | `scripts/deploy.sh` | `scripts/deploy-client.sh` |
| nginx vhost | `resibo` → `proxy_pass` | `resibo-web` → `root` + `try_files` |
| Hostname | `resibo-api.polbinsenthabyer.uk` | `resibo.polbinsenthabyer.uk` |
| On deploy | the container restarts, health-gated | **nothing restarts** |
| Rollback | retag `:previous` → `:current` | rsync back `/var/www/resibo/.previous/live` |

**This is the separation the owner asked for, and it holds at three levels.**

1. **Different things on disk.** A client deploy writes files into
   `/var/www/resibo/live`. It never names the container, the image or port
   8083. nginx serves whatever is on disk at the next request — no reload, no
   restart, no dropped connection. A frontend release cannot restart the API
   any more than copying a file could.
2. **Different vhosts.** `resibo` and `resibo-web` are separate files in
   `/etc/nginx/sites-available`. Editing one does not touch the other.
3. **Different workflows, by path filter.** `.github/workflows/client-ci.yml`
   triggers on `client/**`, `server-ci.yml` on `server/**`. A commit touching
   only `client/` does not *start* the server workflow — the run is never
   queued, so the image is not rebuilt. Shared root files are assigned
   deliberately: `docker-compose.yml` and `scripts/deploy.sh` to the server,
   `scripts/deploy-client.sh` to the client. **A build input listed in
   neither workflow will never trigger a deploy** — right for docs, a silent
   problem for anything else.

#### The one-time setup

```sh
cd /opt/resibo/app && sudo -E bash scripts/setup-client-server.sh live
```

Creates `/var/www/resibo/live` (owned by the deploying account, parent left
root-owned so a compromised deploy cannot rewrite a sibling) and
`/var/www/resibo/.previous` for rollbacks, writes
`/etc/nginx/sites-available/resibo-web`, symlinks it, runs `nginx -t`, reloads
and then checks the neighbouring hostnames still answer.

**The vhost is written by that script and is deliberately not a committed
file.** Every other vhost on this host exists only in
`/etc/nginx/sites-available`; a copy in the repository would be a second
source of truth that drifts. The script is the reproducible artifact.

#### Deploying

CI builds and the server only receives files — **the server has no Node
installed and never builds the frontend**, the same division gainsoclock uses.
Direct `dev/resibo` pushes that touch `client/**`, the client deploy script, or
the client workflow are configured to build and deploy through a dedicated
`resibo-client-deploy` runner. `CLIENT_DEPLOY_ENABLED=true` gates only this
workflow; the backend's `DEPLOY_ENABLED` remains unset. The first automatic
deployment still needs end-to-end verification.
By hand:

```sh
# locally
cd client && npm ci && npm run build
scp -r client/dist/. <server>:~/resibo-client-dist/
# on the server
cd /opt/resibo/app && bash scripts/deploy-client.sh ~/resibo-client-dist live
```

`deploy-client.sh` keeps the current release, rsyncs the new assets, and
**installs `index.html` last**. Vite fingerprints every asset with a content
hash, so new assets sit harmlessly beside the old ones; the moment
`index.html` flips, it points at the new set. Copying it first would leave a
window where the page asks for assets that are not there yet. The script no
longer uses `rsync --delete`: an old page can still ask for its old hashed
assets while a release is being copied. Retired assets require a separate,
delayed cleanup when no old HTML can reference them.

#### Cloudflare

`resibo.polbinsenthabyer.uk` was added to the **existing `personal-projects`
tunnel** — the seventh service on it — and a proxied
`CNAME resibo → <tunnel-id>.cfargotunnel.com` created in the zone.

> 🚨 **The tunnel configuration endpoint takes a whole-config `PUT`.** There is
> no call that appends a rule. The procedure followed, and the one to follow
> again: read the current config, **back it up**, insert the new rule
> **before** the `http_status:404` catch-all, `PUT` the complete list, then
> read it back and check the neighbours. A `PUT` carrying only the new rule
> silently drops the other six services — no error, just four other projects
> offline.

#### Verified, 2026-11-24

| What | Result |
|---|---|
| `https://resibo.polbinsenthabyer.uk/` | **200**, serving the real build — the hashed `/assets/index-*.js` and `.css`, not the placeholder |
| SPA deep link `/some/client/route` | **200** — `try_files` returns `index.html` rather than nginx's 404 |
| CORS from the deployed client origin | preflight **200** with `access-control-allow-origin: https://resibo.polbinsenthabyer.uk`, and a real `GET` returns the `ResponseDto` envelope |
| Tunnel ingress after the write | **8 rules**, all six neighbours intact, new rule before the catch-all |
| Neighbours, publicly | `resibo-api` 200, `gainsoclock-app` 200, `gainsoclock-api` and `eloms-api` 401 — their normal unauthenticated answers |
| Backend redeployed from the new layout | `scripts/deploy.sh` → image built from `server/`, health gate passed, container `Up (healthy)`, `/api/health/check` 200 |

#### Not done

- **Backend CI deploy is still disabled.** Its `DEPLOY_ENABLED` variable is
  unset, and no runner carries its `resibo-deploy` label. The client's new
  runner has a separate `resibo-client-deploy` label and its own gate, so
  enabling frontend delivery does not activate backend deployment. The
  client's first automatic run is pending verification. See §12 #22.
- **No prelive for the client.** `setup-client-server.sh prelive` and
  `deploy-client.sh … prelive` already work, but no hostname or tunnel rule
  exists for it (§12 #11).

---

## 8. Credentials and secrets

> **Rule, no exceptions: no secret value is ever written in this file, in the
> Obsidian vault, in a commit, in a code comment, or pasted into chat.** This
> file records only the *name* of a credential, what it is for, and where its
> value lives.

### How to hand credentials over

1. Put the value in a file **outside the repository** or already git-ignored —
   for example `%USERPROFILE%\.secrets\resibo.env`.
2. Tell me the **path**, not the value.
3. I read it when a task needs it, use it, and never echo it back, never commit
   it, and never record it in a note.

Do not paste credentials into chat. If you already have, treat that credential
as exposed and rotate it.

### How the application will hold them

My proposal, to be confirmed:

- The backend reads every secret from **environment variables**. No secret in
  `application.yml`.
- `application-local.yml` and `.env` are git-ignored. A committed `.env.example`
  lists the variable **names** with empty values, so the required set is visible
  without exposing anything.
- **Decided and in place (§7.3):** production values live in an env file
  **outside the checkout** on the server — `/opt/resibo/resibo.env`, mode
  `0600` — named to the container by `RESIBO_ENV_FILE`. Not Docker secrets:
  there is one host and one container, and a `0600` file is the simpler thing
  that is no weaker here. `.dockerignore` keeps every `.env*` out of the image
  so the file is never baked in (§5.2).
- **Decided and in place (§5.3):** the database user the application connects as
  gets **only the grants it needs** — `USAGE ON *.*` plus `ALL PRIVILEGES ON
  resibo.*`, no `GRANT OPTION`. It is not the superuser, in any environment.
- The Cloudflare API token is **scoped to the single zone**, with the narrowest
  permissions that work.
- GitHub access uses a credential helper or a deploy key — never a token in a
  file inside the repo.
- **Decided and in place (§5.4):** the JWT signing key is supplied as
  `JWT_SECRET`, it has **no default** so a missing one fails startup, and
  **local and production hold different values**. It lives in the same env
  files as the datasource credentials — `.env.dev` locally,
  `/opt/resibo/resibo.env` on the server — and its value appears in neither
  this document nor the repository.

**Your decision / changes:**

**Answer:**

### Inventory — names only

Fill in the last three columns. **Leave the value out.**

All values supplied so far live in one git-ignored file outside every repository:

```
%USERPROFILE%\.secrets\resibo.env     (ACL-restricted to the owner's account)
```

| Credential | Purpose | Where the value lives | Status |
|---|---|---|---|
| GitHub repository access | push, clone, CI | Git Credential Manager (already working) | ✅ working |
| Server SSH account | deploy, administer | `resibo.env` → `RESIBO_SERVER_*` | ✅ **used successfully** for the §7.3 deployment, 2026-11-24. Key-based access remains the goal, and the value still needs rotating — §8.1 risk 1. |
| Database admin user (superuser) | **migrations and backups only, run by a person** — no longer the application's account | `resibo.env` → `RESIBO_DB_*` | ✅ connection verified 2026-11-24, and **retired from application use** the same day (§5.3). Still needs rotating — §8.1 risk 1 |
| Database app user (`resibo`) | the application's connection, least privilege | the per-environment env file → `DATABASE_USERNAME` / `DATABASE_PASSWORD`; on the server `/opt/resibo/resibo.env` | ✅ **created and in use 2026-11-24.** Grants and probed isolation in §5.3. The account **name** is a design fact and is written here on purpose; the password is not, anywhere |
| Cloudflare API token | DNS records and tunnel configuration | handover file | ✅ **working** 2026-11-24 — created for and used by the §7.3 deployment. **Scoped to** Account → Cloudflare Tunnel:Edit, Zone → DNS:Edit, Zone → Zone:Read, on the one zone and no other. |
| Cloudflare account | DNS, tunnel | reached through the scoped token above; no interactive login needed | ✅ working |
| Server application env file | per-environment configuration for the container | `/opt/resibo/resibo.env` on the server, mode `0600`, outside the checkout | ✅ in place — and since 2026-11-24 it holds `APP_ENV=prod`, **the datasource URL, username and password** (§5.3) and **`JWT_SECRET`** (§5.4). It is the one file on the server that must not leak, and the application will not start without it. |
| Local development env file | local configuration, including the database password and the local signing key | `.env.dev` in the working copy — git-ignored, and `.dockerignore`d under any name | ✅ in place, and **no longer value-free**: it carries a real `DATABASE_PASSWORD` and a real `JWT_SECRET`, and points at the shared instance (§12 #13). |
| CI database credentials | the throwaway MariaDB service container in `.github/workflows/ci.yml` | **in the workflow file, in plain sight, deliberately** | ✅ working — and **intentionally not a GitHub Secret**: the container is created for one job and destroyed with it, so there is nothing to protect (§5.3, §13). |
| Domain registrar login | domain ownership | | not yet provided |
| JWT signing key | signs and verifies the API's tokens | the per-environment env file → `JWT_SECRET`; on the server `/opt/resibo/resibo.env` | ✅ **created and in use 2026-11-24** (§5.4). **Local and production values differ on purpose.** At least 32 characters or the application refuses to boot. Rotating it invalidates every token in circulation, which is the intended behaviour, not a side effect |
| Application admin account | the API's own ADMIN login — not a database or server credential | the account lives in the `user` table; its password was supplied in chat | ⚠️ **exposed.** The username `polbinsent` is a design fact and is recorded here; the password is not, anywhere. It was pasted into a chat session, so **it should be changed** — see §8.1 risk 1 |
| SMTP / email provider | receipts, password reset | | not decided |
| Payment provider keys | card / e-wallet | | depends on §2.6 |

| Question | Answer |
|---|---|
| Who holds the master copy of all credentials? | |
| Password manager in use? | |
| Rotation expectation (e.g. every 90 days)? | |

### 8.1 Outstanding security risks

Recorded here because they are real, cheap to fix now, and expensive to fix after
the app is live. Each needs an owner decision.

**1. The supplied credentials were pasted into a chat session, so they are
exposed.** Storing them properly afterwards does not undo that. Both the SSH
account and the database `root` account should be rotated once key-based access
works. Verified 2026-11-24: no credential value appears in any commit on any
branch, so the exposure is to the session transcript only, not to the public
repository.

> **The same now applies to the application's own admin account.** The password
> for the ADMIN user `polbinsent` was supplied in chat, so it is exposed in
> exactly the same way and **should be changed.** It is a different class of
> credential from the other two — it grants `/api/admin/**`, not the server and
> not the database — but it is the login to the only administrative account
> that exists.

**2. ✅ RESOLVED 2026-11-24 — the application no longer connects to MariaDB as
the superuser.** The risk as originally written: three things compounded — the
superuser is authenticated from any host, the same instance hosts unrelated
project databases, and its password is reused with the SSH account. Any one is
tolerable; together, a single SQL injection in Resibo reached every other
project on that server.

> **What was done** (§5.3): a dedicated `resibo` account was created and the
> application switched to it in every environment. Its grants are exactly
> `USAGE ON *.*` plus `ALL PRIVILEGES ON resibo.*`, with **no `WITH GRANT
> OPTION`**, so it cannot widen its own access. Isolation was **probed, not
> assumed**: it sees only `information_schema` and `resibo`, it can do anything
> inside `resibo`, and reads against `gainsoclock`, `eloms` and `mysql` all fail
> with `ER_TABLEACCESS_DENIED_ERROR`, as do `CREATE DATABASE` and `CREATE USER`.
> Production was checked after the redeploy: 10 connections as `resibo`, none as
> the superuser. The superuser is now reserved for migrations and backups run by
> a person.
>
> **What remains, and is not claimed as fixed:** the account's host part is
> `%`, so it is connectable from anywhere that can reach the port — which, since
> nothing Resibo runs is on the LAN and the port is not published to the
> internet, means from the LAN. Narrowing it to the container's gateway or to
> localhost is a further hardening step, **not done**, tracked as §12 #14.
> Risk 1 and risk 3 below are untouched by any of this: the superuser's password
> is still the exposed, reused one, and still needs rotating.

**3. The password is reused between the SSH account and the database
superuser.** One compromise becomes two. Recommend different values at rotation
time. The new `resibo` application account is separate from both, so this is now
a question about the two administrative credentials rather than about the
application.

**4. The repository is public.** Infrastructure addresses and usernames stay out
of it, including out of this file. If the project would rather keep its planning
private, making the repository private is the simpler fix than redacting
documentation forever.

> **Decision needed:** keep the repository public, or make it private?

**5. A local `mvnw verify` connects to the shared production database.** Since
2026-11-24 the Spring context test needs a datasource, and locally it gets the
one in `.env.dev` — which points at the same instance and the same schema
production uses. CI is isolated by its own throwaway container (§5.3); a
developer's machine is not. Nothing writes today only because there are no
entities. The fix is Testcontainers or a local MariaDB container, and it is
recorded as §12 #13 rather than left as a habit.

**Correction, 2026-11-24:** "nothing writes today because there are no
entities" no longer holds. There are three entities, three tables and real rows
in the shared schema, so a local `mvnw verify` now points a test suite at live
data. The risk is unchanged in kind and worse in degree.

**6. Public signup makes the first account an administrator.** This replaces the
larger risk that the whole API was open to the internet, which §5.4 has largely
closed: products are behind ADMIN, every unlisted endpoint defaults to
authenticated, and an anonymous signup cannot grant itself a role. What is left
is the bootstrap.

> **What the exposure is:** `/api/auth/signup` is public, and on a system with
> an empty `user` table the first caller becomes ADMIN. The API is publicly
> reachable (§7.3), so between applying the schema and creating the admin
> account there is a window in which a stranger can take it. For the current
> production deployment the window is closed — an ADMIN account exists, and
> every later anonymous signup is forced to EMPLOYEE — but **the mechanism is
> still in the code and reopens for every new environment**, including a
> prelive if §12 #11 is answered yes.
>
> **What is also still true:** anyone on the internet can create an EMPLOYEE
> account. No endpoint is restricted to EMPLOYEE today, so that currently grants
> nothing beyond what any authenticated user can reach — which is nothing. It
> stops being harmless the first time an `/api/employee/**` endpoint exists.
>
> **The fix** is to require a token for signup, or to gate it behind an invite,
> and to create the admin account by a route that is not a public endpoint.
> Recorded as §12 #17 rather than quietly patched, because it changes how an
> environment is set up.

---

## 9. Reports the owner actually needs

A POS earns its keep in the reports. List what you want to look at, and how
often. Be concrete — "sales report" is not a specification.

| Report | Who reads it | How often | What decision it informs |
|---|---|---|---|
| | | | |

Candidates, for prompting: daily Z-reading · sales by hour · sales by product
and category · cashier performance and over/short · voids and returns and who
made them · stock on hand and reorder list · dead stock · margin by product ·
tax summary for filing · statutory-discount summary.

---

## 10. Non-functional requirements

| Requirement | Answer |
|---|---|
| Acceptable downtime | |
| Target time to ring up one item (scan or keystroke to line on screen) | |
| Backup frequency, and where backups are stored | ⚠️ **Open, and the deadline named in the previous version of this row has arrived.** Nothing is backed up. The `resibo` schema now holds three tables with real rows in them — products and the admin account (§5.4) — so "there is nothing stateful to back up" has stopped being true. `/opt/resibo/resibo.env` carries the datasource credentials and the JWT secret and exists nowhere else on the server (§7.3). Losing either now costs something. This row needs an answer before the first sale is ever recorded, not after. |
| Has a restore ever been tested? (It is not a backup until it has.) | No — and there is now something that would need restoring. |
| How long sales data must be retained | |
| Interface language(s) | |
| Currency, date format, timezone | ⚠️ **Open, and already producing a wrong value.** The running API serialises `responseTime` in **UTC** — `03:27:49` where the machine's clock read `11:27:49` (UTC+08:00). `ResponseDto`'s `@JsonFormat` sets no timezone and Spring's Jackson default is UTC, so every timestamp the API returns is eight hours behind local time. A receipt printed from that is simply wrong, and under §2.3 = B a wrong receipt timestamp is a compliance problem, not a cosmetic one. The fix is one property (`spring.jackson.time-zone`) and it was **deliberately not applied**, because picking the timezone is this row's job, not the code's. Answer the row and the fix lands with it. See §12 #8, §13, §5.1. |
| Receipt paper width | |
| Does the till run on a touchscreen, a keyboard, or both? | |

---

## 11. Delivery phases — my proposal

Reorder or cut as you like. Each phase should end with something you can look at
and judge.

| Phase | Deliverable | Depends on |
|---|---|---|
| 0 | This document filled in; vault notes; git repo initialised | §1, §2 |
| 1 | Application skeleton (plus boundary tests, if §6.1.1 is answered B), MariaDB locally, migrations, login, roles | §5, §6.1 |
| 2 | Product catalog with barcodes and prices | §3 |
| 3 | **Checkout**: ring up, tender, print a receipt | §2.1, §2.3, §2.5 |
| 4 | Shift open/close, cash drawer, X and Z reading | §2.3 |
| 5 | Returns, voids, audit log | §4 |
| 6 | Inventory movements and stock on hand | §3 |
| 7 | Reports from §9 | §9 |
| 8 | Server, domain, deployment, backups | §7, §8 |

**Where this stands (2026-11-24).** Phase 1 is **substantially delivered, except
for migrations**. Done: the application skeleton, a working Maven build, the
`ResponseDto` conventions, a health endpoint, 22 passing tests (§5.1), the
MariaDB connection (§5.3), and — as of 2026-11-24 — **login and roles** (§5.4):
JWT, two roles, a `user` and a `user_role` table, and default-deny on every
endpoint without an explicit rule. Not done: **Flyway and migrations.** The
three tables that exist were applied by hand from `sql/tables/`, and nothing
records which environment has which (§12 #15). Boundary tests are moot —
§6.1.1 went to layer-first, so there are no module boundaries to test.

**Phase 2 is partly delivered, out of order.** There is a product catalog —
create, update, list with filters and pagination, fetch, delete, all behind
ADMIN (§5.1, §5.4). There are **no barcodes and no price lists**; `product`
carries a single price column. Both wait on §1.2 and §3, so the catalog is a
usable administrative surface rather than a finished phase.

Phase 1's remaining work and phases 3 onwards still wait on answers: migrations
wait on the §4 schema, which waits on the §2 blockers, and the role model waits
on §1.3 before it is more than ADMIN and EMPLOYEE.

**Phase 8 is substantially delivered for the API, out of order (2026-11-24).**
Done and verified in §7.3: the server, a Docker deployment with a health-gated
restart and a working rollback path, the nginx vhost, a subdomain on the zone
that already serves the sibling projects, and a public HTTPS URL through the
existing Cloudflare tunnel. It was taken early precisely because it is the one
phase that depends on **none** of the open `[BLOCKER]`s — a health endpoint is
as deployable as a finished POS, and finding out that the nginx-plus-tunnel
pattern works is far cheaper now than during phase 3.

What phase 8 does **not** yet include, so nobody reads the row as finished:

- **Backups.** Nothing is backed up, and **that has become a real gap**: the
  schema holds three tables with rows in them (§5.4), and the server holds the
  datasource credentials plus the JWT secret in `/opt/resibo/resibo.env` and
  nowhere else. The §10 backup rows are still blank. They were due "the moment
  the first table exists", and that moment has passed.
- **Prelive / staging.** Not built. gainsoclock has one on port 8082 and a
  `deploy-prelive.sh`; Resibo has neither, and whether it should is §12 #11.
- **CI, past the test gate.** `.github/workflows/ci.yml` exists: its `build` job
  runs `mvnw -B clean verify` against a throwaway MariaDB service container
  (§5.3), which is a real gate and the reason the database work needed it. It
  also declares a `deploy` job on a self-hosted runner. **That path is not
  documented in §7.3 and is not the procedure a session should follow** — the
  manual deploy is. §12 #16.
- **An application worth deploying.** This is the one that matters. There is now
  a catalog and a login behind that URL, which is more than a health check and
  still nothing a shop could use. Every `[BLOCKER]` and the §6.1.1 confirmation
  are exactly as open as they were the day before the deployment.

**Your changes:**

**Answer:**

---

## 12. Open questions

| # | Question | Blocks | Raised | Status |
|---|---|---|---|---|
| 1 | Business type (§1.2) | screens, schema | 2026-11-24 | open |
| 2 | Offline behaviour (§2.1) | whole architecture | 2026-11-24 | open |
| 3 | BIR / official receipt (§2.3) | sales schema, numbering, receipt | 2026-11-24 | open |
| 4 | Tax model (§2.4) | every total on every screen | 2026-11-24 | open |
| 5 | Stores and terminals (§2.2) | schema, receipt numbering | 2026-11-24 | open |
| 6 | Roles and permissions (§1.3) | auth model | 2026-11-24 | **open — and now partly implemented anyway.** §5.4 shipped two roles, ADMIN and EMPLOYEE, because authentication needed *some* role model to exist. That is a placeholder, not an answer: the §1.3 table is still blank, and a cashier, a shift supervisor and a bookkeeper are three different sets of permissions that EMPLOYEE currently collapses into one. Roles are rows in `user_role`, so adding one needs no migration — but `UserRoleType` and the path rules in `SecurityConfig` both need editing, so this is cheap rather than free. |
| 7 | Module boundary enforcement (§6.1) | backend skeleton | 2026-11-24 | moot until #9 — the code has no modules to enforce |
| 8 | Timezone for serialised timestamps (§10) | every API timestamp, receipt times, Z-reading day boundary, and BIR compliance if §2.3 = B | 2026-11-24 | **open — and now wrong in public, not just locally.** The production response reads `03:57` where local time is `11:57` (§7.3). Deliberately not fixed; see §13. |
| 9 | Confirm the layer-first package structure (§6.1.1) | every file path in the backend | 2026-11-24 | **open — and the cheap window has closed.** The code follows option A. It was six classes when this was raised; authentication and the product catalog took it to around thirty, so reversing to B is now hours of mechanical moving rather than minutes, and it grows with every feature. |
| 10 | Suppress `className` / `methodName` on production responses (§6.1.2) | nothing structural — but it is live internal detail on an open endpoint | 2026-11-24 | **open, and its deadline has passed.** §6.1.2 required both fields gone from production error responses "before anything is exposed publicly"; the API went public on 2026-11-24 (§7.3) and still returns them on every response. The fix is cheap; what needs deciding is whether to suppress by environment or everywhere. |
| 11 | Is a prelive / staging environment wanted (§7, §7.3)? | `scripts/deploy-prelive.sh`, the `/api/health/check/prelive` variant gainsoclock has, and a fourth host port | 2026-11-24 | open — gainsoclock has one on 8082; Resibo deliberately has none, and nothing yet justifies one. |
| 12 | Should Cloudflare Access, or any edge authentication, sit in front of this hostname (§7)? | nothing — it is defence in depth now, not a gate | 2026-11-24 | **open, and much less urgent.** It was raised when the API had no authentication of its own. §5.4 gave it some: products need an ADMIN token and every unlisted endpoint defaults to authenticated. Edge authentication would now be a second layer rather than the only one — and it cannot cover `/api/auth/login`, which has to stay publicly reachable. |
| 13 | **How do local tests get a database?** Right now `mvnw verify` loads `.env.dev` and connects to the **shared production instance** (§5.3, §8.1 risk 5) | whether a careless test can reach real data; also the Testcontainers row in §5 | 2026-11-24 | **open, and no longer theoretical.** CI is isolated by its own throwaway container; a developer's machine is not. The "harmless while there are no entities" caveat expired on 2026-11-24 — there are three entities, three tables and real rows, so a local test run now points at live data. §5 already recommends Testcontainers; a local MariaDB container is the cheaper half-measure. This needed deciding before the first repository and did not get it. |
| 14 | Narrow the `resibo` database account's host part from `%` (§5.3, §8.1 risk 2) | nothing — it is hardening, not a blocker | 2026-11-24 | open, deliberately not done. The privilege half of §8.1 risk 2 is closed; this is the connectivity half. Reaching the port at all requires LAN access, so it is the smaller remaining exposure. Narrowing it to the container gateway would also need the local developer's route considered — see #13. |
| 15 | **Who owns the schema?** `ddl-auto: none` means Hibernate will never create a table, and there is no Flyway and no migration (§5, §5.3) | every environment that has to be set up from scratch | 2026-11-24 | **open, and overtaken by events.** "Decide the tool before the first entity" did not happen: three tables now exist — `product`, `user_role`, `user` — applied by hand from `sql/tables/` with the administrative account. The files are reviewable and idempotent (`CREATE TABLE IF NOT EXISTS`, `INSERT IGNORE`), which is the good half. The bad half: **nothing records which environment has which file**, nothing handles altering an existing table, and the apply order matters (`user_role` before `user`). Adopting Flyway now also means baselining the three tables that already exist. |
| 16 | The CI `deploy` job is not documented (§7, §7.3, §11) | nothing technical — it is a documentation gap that can mislead a session | 2026-11-24 | open. `.github/workflows/ci.yml` declares a `deploy` job that runs `scripts/deploy.sh` on a self-hosted runner labelled `resibo-deploy`, with `APP_DIR=/opt/resibo`, `HOST_PORT=8083`, `APP_ENV=prod`. §7.3 describes only the manual path, and this document should not have two answers to "how does it deploy". Confirm whether the runner is registered and whether CI deploys are the intended path, then write the winner into §7.3 and a decision into §13. |
| 17 | **Should `/api/auth/signup` stay public?** While it is, the first account on an empty system becomes ADMIN, and anyone can create an EMPLOYEE account (§5.4, §8.1 risk 6) | how every new environment is set up, and who can hold an account | 2026-11-24 | open, and the main exposure left after §5.4. The bootstrap window is closed in production because an admin exists, but the mechanism reopens for every new environment. Options: require an ADMIN token for signup and seed the first admin by SQL; keep it public behind an invite code; or leave it and accept that account creation is open. Pick one before a prelive exists or before the first `/api/employee/**` endpoint ships. |
| 18 | **No token refresh, no logout, no revocation** (§5.4) | how long a stolen token is useful; whether a dismissed employee can be locked out mid-shift | 2026-11-24 | open. A token is valid for its full 8 hours and cannot be withdrawn — setting `is_active = 0` stops the next login, not the token already issued. §5's original recommendation was "short-lived access token + refresh", and only the first half was built. Shortening the expiry makes the problem smaller and the frontend's job harder; a refresh token or a deny-list makes it go away and costs real work. |
| 19 | ~~CORS is open to all origins~~ (§5.4) | — | 2026-11-24 | **RESOLVED 2026-11-24.** The API now answers browser calls only from `https://resibo.polbinsenthabyer.uk`, `http://localhost:5173` and `http://127.0.0.1:5173`. Verified on the live API: those three get an `access-control-allow-origin`; `https://evil.example.com` and `http://localhost:3000` get **403 with no CORS headers**; requests carrying no `Origin` at all — Postman, curl, server-to-server — are unaffected at 200. Naming origins also turned `allowCredentials` on, which changes nothing today because the token travels in the `Authorization` header, not a cookie. |
| 20 | **`JWT_SECRET` is missing from `.env.example` and from the CI `build` job** (§5.1, §5.4) | a fresh checkout, and the CI test gate | 2026-11-24 | open, and it looks like an oversight rather than a decision. `jwt.secret` has no default and `ResiboApplicationTests` starts the full context, so a checkout that copies the committed template cannot start the application, and CI sets the datasource keys but no signing key. Both are one line each. Needs confirming against an actual CI run before anyone trusts the gate. |
| 21 | **Is "every repository query is native SQL" a rule or a starting point?** (§5.5) | every repository written from here on | 2026-11-24 | open. It is the owner's instruction and is followed everywhere today. It has already cost one runtime bug (the `Integer` `EXISTS` trap), needs an explicit `countQuery` on every paginated query, and needs a field-to-column map for every sortable listing. Worth confirming it is wanted for simple lookups too, rather than only where JPQL fights the query. |
| 22 | **How does the frontend get built and deployed?** (§7.4) | nothing — **answered and built** | 2026-11-24 | **mostly closed.** CI builds `client/dist`, `scripts/deploy-client.sh` rsyncs it into `/var/www/resibo/live`, nginx vhost `resibo-web` serves it at `https://resibo.polbinsenthabyer.uk`, and it is verified live. **What remains:** no runner carries the `resibo-deploy` label, so both `deploy` jobs are inert and releases are the manual commands in §7.4. Register a runner for this repository and set `DEPLOY_ENABLED=true`. |
| 23 | ~~Two branches each carry a copy of this plan~~ (§7.1) | — | 2026-11-24 | **RESOLVED the same day it was raised.** The monorepo retired `dev/api/resibo` and `dev/app/resibo` in favour of the single `dev/resibo`, so there is one copy of this document again. It cost a rebase and a section renumbering first, which is the evidence for why it was worth resolving rather than managing. |

---

## 13. Decisions log

Append-only. Every entry needs a reason — a decision without its rationale is
trivia the next session cannot reuse.

| Date | Decision | Rationale | Decided by |
|---|---|---|---|
| 2026-11-24 | Backend is Spring Boot | Owner's choice | Owner |
| 2026-11-24 | Frontend is React | Owner's choice | Owner |
| 2026-11-24 | Database is MariaDB | Owner's choice | Owner |
| 2026-11-24 | Backend is structured as a modular monolith, package-by-feature | Owner's choice. One deployable keeps operations simple for a single-store start, while module boundaries keep the option of extracting a service later. | Owner |
| 2026-11-24 | `resibo-plan.md` is the source of truth, updated in the same change as the code | Owner's instruction, so future sessions start from recorded intent rather than re-deriving it from code | Owner |
| 2026-11-24 | No code, scaffolding, or project generation until this plan's blockers are answered | Owner's instruction. The offline and tax-receipt answers change the schema and the architecture; generating a project first would mean discarding it. | Owner |
| 2026-11-24 | Credentials are referenced by name in the plan; values live outside the repo and outside the vault | Secrets in documentation leak through git history, backups, and notes. Names are enough to describe the system. | Claude, per standing rule |
| 2026-11-24 | Remote is `github.com/polbinsenthabyer/resibo` | Owner-supplied | Owner |
| 2026-11-24 | `main` holds `README.md` only; everything else lives on `dev/api/resibo`, committed to directly | Owner's choice. Keeps the public face of the repository to a single curated document while development history stays on one working branch. | Owner |
| 2026-11-24 | Push every commit to the remote; commit messages brief and precise | Owner's instruction. The remote is the project's only off-machine backup, which matters most while the plan is the most valuable thing in the repository. | Owner |
| 2026-11-24 | The working branch is `dev/api/resibo`, not `dev/resibo` | Owner's choice. The `api` segment separates backend from frontend work, matching gainsoclock's `live/api/...` and `live/app/...` convention; a frontend branch becomes `dev/app/resibo`. | Owner |
| 2026-11-24 | `gainsoclock` is the reference for code structure and conventions | Owner's instruction. Consistency across the two personal Spring Boot projects lowers the cost of moving between them. The conventions are adopted in §6.1.2; the **layout** conflicts with the modular-monolith decision and is raised as a blocker in §6.1.1 rather than silently resolved. | Owner, conflict flagged by Claude |
| 2026-11-24 | Infrastructure addresses and usernames are kept out of the repository, not just passwords | The repository was confirmed public. A private LAN address plus an account name is reconnaissance material, and git history is permanent. The handover file holds them instead. | Claude |
| 2026-11-24 | The database connection is verified working; SSH is not | Recorded so no later session assumes server access exists. Password-only SSH cannot be used by an agent session, which has no interactive terminal. | Claude |
| 2026-11-24 | The hold on scaffolding was lifted for the skeleton only, and a Spring Boot project was generated and committed (`2b6dcd6`, §5.1) | The owner supplied the Initializr parameters directly — coordinates, dependencies, Java and Boot versions, configuration format — which is the owner lifting their own hold for this step. The hold's reasoning still applies to everything beyond the skeleton: the schema, auth, and checkout all depend on unanswered `[BLOCKER]` items, and none of them were touched. What was built is deliberately blocker-free: a build, a response envelope, and a health endpoint, none of which the §2 answers can invalidate. | Owner, scope limited by Claude |
| 2026-11-24 | **The backend is packaged layer-first at the base package (§6.1.1 option A), which supersedes the earlier decision to package it as a modular monolith by feature.** The earlier decision is not deleted — it stands as the documented alternative in §6.1 and is what §6.1.1 option B would restore. **This supersession is pending the owner's explicit confirmation (§12 #9).** | The two instructions could not both be honoured: "structure it as a modular monolith" and "use gainsoclock as the structural reference" describe different file trees, and gainsoclock is layer-first. The gainsoclock instruction was the more recent and the more specific of the two, and a file had to be put somewhere, so it won. Recorded as superseding rather than as a compromise because pretending the code is a modular monolith when it is six classes in `controllers/`, `dtos/`, `types/` and `utilities/` would make this document lie — the one failure mode it cannot afford. The cost is explicit: there are no module boundaries, so §6.1.2's enforcement questions do not currently apply. The reversal is minutes of work today and hours after the first domain module, which is why confirmation is wanted before the next one is written. | Claude, on the owner's structural instruction — **confirmation outstanding** |
| 2026-11-24 | The `ResponseDto` envelope is kept in full, `className`/`methodName` included (§6.1.2) | Consistency with gainsoclock was the point of naming it as the reference, and the envelope is the most visible part of that consistency. The leak caveat is not withdrawn: both fields are populated on every response, including errors, and must be suppressed in production error responses before the API is publicly reachable. | Claude, per §6.1.2 |
| 2026-11-24 | The Spring Boot parent is `4.2.0`, not Initializr's `4.2.0.RELEASE` | The `.RELEASE` coordinate Initializr's metadata names does not exist in Maven Central — the suffix was dropped years ago — so the generated pom could not resolve its parent at all. Recorded in §5.1 because regenerating will reproduce it. | Claude |
| 2026-11-24 | The owner's "packaging: yaml" was implemented as YAML configuration with `jar` packaging | Maven packaging admits only `jar` or `war`, so the note cannot have meant Maven packaging. YAML configuration matches gainsoclock, which is the stated reference. Flagged in §5.1 as an assumption awaiting the owner's word, not as a settled decision. | Claude, interpretation — **unconfirmed** |
| 2026-11-24 | The UTC `responseTime` defect is recorded, not fixed | §10 asks the owner for the project's timezone and has no answer yet. Setting `spring.jackson.time-zone` would be inventing that answer in code, where it is far less visible than an unanswered row in this document — and a timestamp that is quietly eight hours off is worse than one that is loudly unconfigured. The defect is written into §5.1, §10 and §12 #8 instead, and the fix is a one-line property the moment the row is filled. | Claude, per §12 #8 |
| 2026-11-24 | **The skeleton is deployed to production and made publicly reachable, before a single `[BLOCKER]` is answered** (§7.3) | Owner's request. It is defensible because phase 8 is the only phase that depends on none of the open questions: the deployment path — image, nginx vhost, tunnel ingress, DNS, health gate, rollback — is identical whether the application serves a health check or a finished POS, so building it now costs nothing that the §2 answers can invalidate, and it removes the whole class of "it works locally" surprise from phase 3. The limit is written into the document in several places on purpose: **what is deployed answers health checks and talks to no database.** A deployed skeleton is not progress on the product and must never be reported as such. | Owner, scope limited and recorded by Claude |
| 2026-11-24 | **The deployment artifact is a Docker image, not a bare JAR** (§5.2) | Owner's instruction — "the gainsoclock pattern, but Docker". It also pays for itself: the build runs inside `eclipse-temurin:17-jdk` through the committed `mvnw`, so the **server needs only Docker** — no JDK to install, no Maven, and no second place where the Java version could drift from the one the project compiles against. The runtime stage is JRE-only with no compiler and no source in it, and the image pins the Java version with the code that needs it rather than relying on whatever the host happens to have. | Owner |
| 2026-11-24 | **Deployment builds on the server from a git checkout (`/opt/resibo/app`), rather than shipping a built artifact to it** (§7.3) | The repository is public, so the server can clone and pull with no credential at all — no deploy key, no registry login, no token on the server to leak or rotate. There is also no registry to run or pay for, and `git pull` makes "what is deployed" answerable by a command on the server instead of by trust. The cost is accepted knowingly: the server spends a few minutes of CPU on each build, and a build failure is discovered on the server rather than in CI. At this size that is the cheaper trade, and the `dependency:go-offline` cache layer (§5.2) keeps the repeat cost small. Revisit if a CI pipeline ever exists. | Claude |
| 2026-11-24 | `scripts/deploy.sh` keeps gainsoclock's shape — stage, restart, health-gate, roll back — with **an image tag as the rollback artifact** instead of `app.jar.previous` (§7.3) | Owner asked for the gainsoclock pattern, and the shape is the valuable part: no deployment is considered done until `/actuator/health` answers, and a failed deployment restores the previous build by itself rather than leaving a human to work out what to undo at the worst possible moment. Docker only changes what "the previous build" is stored as — `resibo-api:previous` instead of a renamed jar — which is strictly better, because an image tag carries its dependencies with it. A session reading either project's script should recognise the other. | Owner's pattern, adapted by Claude |
| 2026-11-24 | **The container and nginx both publish on loopback only; nothing Resibo runs listens on the LAN** (§5.2, §7.3) | The server is shared with unrelated projects and sits on a home network, so the attack surface worth minimising is the LAN, not just the internet. `127.0.0.1:8083:8080` and `listen 127.0.0.1:80` mean the only path in is the Cloudflare tunnel, which is an outbound connection from the host — there is no inbound port to forward, firewall, or forget to firewall. It also makes the public hostname the single front door, so there is exactly one place to put authentication when §12 #12 is answered. | Claude |
| 2026-11-24 | **The existing `personal-projects` tunnel is reused, with a new subdomain, rather than creating a new tunnel or buying a domain** (§7.3) | That tunnel was already healthy and already fronting the sibling projects through nginx by `server_name`, which is a pattern that works and that the owner already operates. A second tunnel would mean a second `cloudflared` service to keep running, patch, and monitor, for no benefit — one tunnel fans out to any number of hostnames through the one nginx. A subdomain of the existing zone likewise needs no purchase, no new registrar login, and no new TLS story. The cost is shared fate: a mistake in the tunnel configuration affects every project behind it, which is exactly why the next row exists. | Claude |
| 2026-11-24 | **Tunnel ingress is changed by read, back up, append, then PUT the complete list — never by PUT-ing one rule** (§7.3) | The Cloudflare tunnel configuration endpoint replaces the whole ingress list; it does not patch it. A `PUT` carrying only Resibo's rule would have silently deleted the rules for gainsoclock, its prelive and eloms, and the failure mode is the dangerous one — the tunnel stays healthy, the neighbours start returning 404, and nothing reports an error. So the prior configuration was saved first, the new rule was appended **before** the `http_status:404` catch-all (a rule after the catch-all can never match), and the neighbours were re-checked afterwards. Written into §7.3 as a procedure rather than a note because the next session will not remember it. | Claude |
| 2026-11-24 | Cloudflare is accessed by a **scoped API token**, recorded in §8 by its scopes and its location only | Per the standing §8 rule: the plan records what a credential is for and where it lives, never its value. The scopes are the part worth writing down, because they are what limits the damage — Account → Cloudflare Tunnel:Edit, Zone → DNS:Edit, Zone → Zone:Read, on the one zone and no other. A token narrow enough to describe in one line is also narrow enough to hand over without auditing. An interactive OAuth login was abandoned in favour of this, because an agent session has no browser and a token can be rotated without one. | Claude, per §8 |
| 2026-11-24 | **Persistence is Spring Data JPA, not plain JDBC** — `spring-boot-starter-data-jpa` plus the MariaDB driver at `runtime` scope (§5.3, commit `9b57c2b`) | §5 recommended JPA and nothing has argued against it: the entities in §4 are ordinary relational shapes, gainsoclock uses the same stack so a session moves between the two projects without re-learning anything, and the starter brings the transaction manager and HikariCP that a hand-rolled `DataSource` would otherwise need assembling. The §5 caveat stands — reporting queries will use plain SQL where JPA fights them, which is a reason to keep JPA, not to avoid it. The driver is `runtime` scope because **nothing in the source imports it**, and a dependency the compiler can see is a dependency something will eventually reference by accident. | Claude, per §5 |
| 2026-11-24 | **`spring.jpa.hibernate.ddl-auto` is `none`, and must never be `update`, `create` or `create-drop` in any environment** (§5.3) | The owner's explicit instruction, and it would be the right answer without one. The schema is owned outside the application, as in gainsoclock, so the thing that creates tables is a reviewed migration or a person — never a side effect of starting the application. The reason it is a rule rather than a preference is §2.3: **if Resibo issues BIR-registered receipts, an ORM silently altering a sales table is exactly the class of change that destroys an audit trail**, and an auditor cannot be told a column changed because a Java field was renamed. `update` is the dangerous setting precisely because it looks helpful. Proven rather than asserted: zero DDL in the log and 0 tables in the schema after the application ran, locally and in production. | Owner, reasoning recorded by Claude |
| 2026-11-24 | **The three datasource properties have no defaults** (§5.3) | A missing configuration must fail startup loudly rather than connect somewhere unintended. The instance hosts unrelated project databases (§7.2), so a fallback to an embedded database or to whatever answers on `localhost:3306` is not a convenience but a way to write Resibo's data into someone else's schema — or to pass a test suite against nothing. The cost is accepted: every environment, including CI, must supply `DATASOURCE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD`, which is why the CI job sets them explicitly. Adding defaults to make something start more easily is the wrong fix; fix the environment. | Claude |
| 2026-11-24 | **`spring.jpa.open-in-view` is `false`**, against Spring's default of `true` (§5.3) | Left at the default, the persistence context stays open for the whole request, a pool connection is held while the response is serialised, and lazy loads fire from the view layer — which is the usual cause of pool exhaustion that looks like a mystery. On a shared instance the pool is not ours alone to waste, and on a checkout screen a request that queues behind a held connection is a cashier waiting. Turning it off now also means no code is ever written that depends on it being on, which is the kind of dependency that is cheap to prevent and expensive to remove. | Claude |
| 2026-11-24 | **A dedicated least-privilege `resibo` database account was created, and the application no longer connects as the superuser** (§5.3; resolves §8.1 risk 2) | The instance is shared with unrelated projects, so a single SQL injection in Resibo running as the superuser reached every other project on the server — the compounding risk §8.1 described. The account holds `USAGE ON *.*` plus `ALL PRIVILEGES ON resibo.*` and **no `WITH GRANT OPTION`**, so it is omnipotent inside its own schema and cannot widen its own access. Isolation was probed rather than inferred: other schemas, `CREATE DATABASE` and `CREATE USER` are all denied. The superuser is kept for migrations and backups run by a person, which is the one use that genuinely needs it. The host part is still `%` and narrowing it is recorded as open (§12 #14) rather than quietly claimed as done. | Owner's call, carried out and verified by Claude |
| 2026-11-24 | **CI gets a throwaway MariaDB service container, and its credentials are deliberately not GitHub Secrets** (§5.3) | The Spring context test now needs a datasource, and **the real database is on a private network that GitHub-hosted runners cannot reach** — so CI needed a database of its own whether or not isolation was wanted, and isolation is wanted anyway (§12 #13 is about the local case, which has none). MariaDB 11 matches the server's major line closely enough to catch dialect surprises, and the health options (`healthcheck.sh --connect --innodb_initialized`) stop the build racing a container that is listening but not ready. Same approach as gainsoclock's `maven.yml`. The credentials are root/test in plain sight on purpose: the database exists for one job and is destroyed with it, so hiding its password protects nothing — and a repository whose "secrets" include things that are not secret teaches everyone to stop reading the list. | Claude |
| 2026-11-24 | **The local `.env.dev` port moved from 8088 to 8083**, matching the server's host port (§5.3, §7.3) | 8088 is **eloms' port** on the shared server. Nothing broke — no server env file reads `.env.dev` — but every comparison between a local run and the deployed one had to be mentally corrected, and a URL that looks like a neighbour's service is a trap left lying around for a session with no memory of this one. Aligning them costs nothing and makes `:8083` mean the same thing everywhere. The §7.3 warning about it is now written as resolved rather than live, because a stale warning is read as a current one. | Claude |
| 2026-11-24 | **Authentication is a stateless JWT — HS256, 8-hour expiry, no server-side session** (§5.4) | §5 recommended it and gainsoclock already works this way, so a session moves between the two projects without re-learning anything. Stateless also means nothing to replicate or stick if there is ever more than one container, and nothing to lose on a restart. 8 hours is chosen to match a shift: a cashier should log in once at the start of the day, not three times. The cost is accepted and recorded, not hidden — a token cannot be revoked before it expires (§12 #18). | Claude, per §5 |
| 2026-11-24 | **jjwt is pinned at 0.13.0, not the 0.11.5 gainsoclock uses** | 0.11's builder and parser API is deprecated, and starting new code on a deprecated API buys nothing. The consequence is written into §5.1 and the `JwtUtil` javadoc because it will otherwise waste someone's afternoon: **JWT code cannot be copied between the two projects.** The version sits in a `jjwt.version` property so the three artifacts cannot drift apart. | Claude |
| 2026-11-24 | **`jwt.secret` has no default, `JwtUtil` refuses to start if it is under 32 characters, and local and production use different keys** (§5.4) | Same reasoning as the datasource properties, with a sharper edge: a hardcoded fallback in a **public** repository would be a signing key an attacker can read, shared by every environment that forgot to set one. Validating the length at boot rather than at the first login means a misconfigured deployment fails the health gate instead of accepting logins it cannot sign. Different keys per environment mean a token minted on a developer's machine is worthless against production, which also stops local testing from accidentally becoming production access. | Claude |
| 2026-11-24 | **The filter chain ends in `anyRequest().authenticated()`** — everything without an explicit rule requires a token (§5.4) | The alternative is a per-controller annotation, and the failure mode of that is silent: a new controller ships public because nobody remembered. Default-deny inverts it — the mistake becomes a 401 on an endpoint that should have been open, which someone notices immediately. It also keeps every access rule in one readable file instead of scattered across annotations. The public list is deliberately short: auth, health, actuator, `/error`, and `OPTIONS`. | Claude |
| 2026-11-24 | **`401`/`E0401` and `403`/`E0403` are the only responses that use a real HTTP error status; every business error stays `200` with a code** (§5.4) | gainsoclock's envelope convention is that the HTTP status says whether the request was handled and the body says what happened, and consistency with it is worth keeping. These two are genuinely different: the request never reached the application, so there is no business outcome to report. The split is also what clients need — a `401` means "get a new token", a `403` means "stop asking", and an `E0106` in a `200` means "that password is wrong". Both still carry the normal envelope so there is one parse path. | Claude |
| 2026-11-24 | **`/error` is in the permitAll list, and must stay there** (§5.4) | Found the hard way. Spring forwards unhandled exceptions to `/error`, so while that path was protected **every 500 came back as a 401** — the status was wrong, the message was wrong, and the actual failure was invisible. A permitted `/error` leaks nothing that the exception handler does not already return. Written into the document rather than left as a line of config because the symptom (a 401 where a 500 belongs) looks like an authentication bug and will send the next session hunting in the wrong place. | Claude |
| 2026-11-24 | **The first account on an empty system becomes ADMIN; after that an anonymous signup is always EMPLOYEE, and only an authenticated ADMIN can create an ADMIN** (§5.4) | Something has to create the first administrator, and the alternatives are worse: a hardcoded default account is a credential in a public repository, and seeding one by SQL means a password in a file in `sql/`. Forcing every later anonymous signup to EMPLOYEE is the part that makes it safe to leave signup public at all — without it, anyone could grant themselves ADMIN by sending `"role": "ADMIN"`. **This is recorded as an exposure, not as a solved problem** (§8.1 risk 6, §12 #17): while signup is public and the table is empty, whoever signs up first becomes the administrator, and on a publicly reachable API that is a race. The window is closed in production and reopens for every new environment. | Claude |
| 2026-11-24 | **Wrong password, unknown username and disabled account all return `E0106` with the same message** (§5.4) | Distinguishing them turns the login endpoint into a username oracle: an attacker learns which accounts exist, and can then spend its effort on passwords for names it knows are real. The cost is a slightly less helpful error for a legitimate user who mistyped their username, which is the right trade for a login endpoint open to the internet. The failed attempt is logged with the username, so the information is available to an operator and not to a caller. | Claude |
| 2026-11-24 | **CORS is open to all origins, with `allowCredentials=false`** (§5.4) | Deliberately temporary, and recorded so it is not mistaken for considered. The frontend does not exist and its hostname is not decided (§5), so a restrictive list would block work for no security benefit today. `allowCredentials=false` is the part that is permanent: browsers reject credentials from a `*` origin anyway, and the token travels in the `Authorization` header rather than a cookie, so nothing here needs them. Must be replaced with real origins before the API holds live data — §12 #19. | Claude |
| 2026-11-24 | **Products moved to `/api/admin/products/**`, and an admin-only controller is named `Admin<Name>Controller` in `controllers/admin/`** (§5.4, §6.1.2) | Grouping by audience in the path means one `SecurityConfig` rule protects a whole area, instead of an annotation per controller that someone can forget. Making the class name match the path means the file tree and the URL tell the same story, and a reviewer can see that a controller is admin-only without opening it. gainsoclock already splits its controllers into `admin/`, `client/` and `employee/`, so this is the sibling project's shape rather than a new invention. The move is breaking, which is cheap now and would not have been later. | Claude |
| 2026-11-24 | **Roles are rows in a `user_role` table, seeded by `INSERT IGNORE`, not an enum column** (§5.4) | §1.3 is unanswered, so the role list is the part of this design most likely to change — and a row is cheaper to add than a column definition plus a migration. `INSERT IGNORE` keeps the file idempotent, so applying the schema twice is safe and re-running it does not disturb existing rows. The application still has to recognise a role by name (`UserRoleType`), so this is not dynamic authorisation; it is a cheap place to put the list. The foreign key is `ON DELETE RESTRICT` because deleting a role must never silently delete the people who hold it. | Claude |
| 2026-11-24 | **Every repository query is native SQL** (§5.5) | The owner's instruction. Three consequences are documented rather than rediscovered: a paginated native query needs its own `countQuery`, a `Sort` in a `Pageable` must carry **column** names so the service translates them through a `SORT_COLUMNS` map that doubles as the sort allow-list, and **`SELECT EXISTS(...)` returns `Integer`** — declaring such a method `boolean` throws `ClassCastException` at runtime, which cost real time to find because it surfaced as a confusing 401. The upside is real too: under `utf8mb4_general_ci` a plain `=` is case-insensitive and still uses the unique index, where a JPQL `LOWER()` habit would have thrown the index away. Every value is a bound parameter, so none of this opens an injection path. | Owner, consequences recorded by Claude |
| 2026-11-24 | **`created_by` and `updated_by` record the signed-in username instead of the literal `manual`** (§5.4) | The columns existed from the first table and were being filled with a placeholder, which made them decoration. With authentication there is an actual actor to record, and audit columns that name a person are the difference between an audit trail and four unused columns — which matters under §2.3 if Resibo ever issues official receipts. `SecurityUtil.currentUsername()` still falls back to `manual` when nobody is authenticated; with the current rules that means only the public signup path. | Claude |
| 2026-11-24 | **`UserUtility` returns entities rather than `ResponseDto`, and its existence checks actually check** (§6.1.2) | Modelled on gainsoclock's utility of the same name, with the parts that do not survive review left out. Returning `ResponseDto` from a lookup helper forces every caller to unwrap a response envelope to get at an entity, so building the response stays the service's job. It returns `null` for a missing role row instead of throwing, because a missing row means the schema was not fully applied — an operator problem the service should log and report, not a crash. And gainsoclock's `isUserExistById` tests an object that is never null, so it always returns `true`; copying that would have imported a bug that never fails loudly. | Claude |
| 2026-11-24 | **The product catalog shipped before §3 and §4 were answered, as an admin surface only** | Recorded because it is a departure from "no code past a blocker". A product with a name, a price and a stock count is the smallest thing that could be built, and it was built to have something real behind authentication rather than to settle the catalog: there are no variants, no barcodes, no price lists and no tax fields, all of which §3 and §4 still own. The listing endpoint is a `POST` despite being a read, because the filter and pagination payload belongs in a body and a `GET` body is not reliably carried by proxies and clients — gainsoclock pairs `GET` with `@RequestBody`, which works until something in the path drops it. If §1.2 comes back as a business that needs variants or batches, this table is expected to be replaced rather than extended. | Claude |
| 2026-11-24 | **The frontend lives at `src/main/client`, on its own branch `dev/app/resibo` cut from `dev/api/resibo`** (§5.6, §7.1) | Owner's choice, made after three options were laid out: a `frontend/` directory beside `pom.xml` on one branch (the common industry layout, and the only one that allows an atomic full-stack commit), a branch cut from `main` with the client at the repository root (what gainsoclock does), or nesting under `src/main`. The chosen location is safe because Maven reads only `src/main/java` and `src/main/resources` — verified by compiling — and because `.dockerignore` now excludes the directory, so the Java image never carries `node_modules`. Cutting the branch from `dev/api/resibo` rather than `main` was deliberate: it puts this plan and `CLAUDE.md` in front of a session working in the frontend checkout. | Owner, options and consequences by Claude |
| 2026-11-24 | **Plain SCSS: no Tailwind and no component library** (§5, §5.6) | Owner's instruction. Design tokens are CSS custom properties on `:root` rather than Sass variables, so a theme or a high-contrast mode for a till screen can change them at runtime instead of requiring a rebuild; breakpoints stay Sass variables, because a media query cannot read a custom property. Component styles are CSS Modules sitting next to their component, so a class name cannot leak out of the component that owns it. A component kit can still be added later, on top of this rather than instead of it. | Owner, structure by Claude |
| 2026-11-24 | **The client has exactly one place that calls `fetch`** — `src/api/http.ts` — and it throws `ApiError` rather than handing the envelope to callers (§5.6) | The API reports business failures with HTTP 200 and `responseType: 'ERROR'`, and uses a real error status only for 401 and 403 (§5.4). A component that calls `fetch` directly has to know that, and the first one to forget will read a failure as a success. Translating the contract in one place means callers get either `data` or an exception, which is the normal shape for the rest of the language. | Claude |
| 2026-11-24 | **No currency, date or timezone formatting helper was written for the client** (§5.6, §10) | §10 is blank and §12 #8 is open: `responseTime` serialises in UTC, and currency and date format have never been chosen. Any formatter would have had to pick those values, and a POS prints them on receipts — possibly BIR-registered ones. Writing none leaves the gap visible; writing one would have buried a guess in a utility that every screen then depends on. The product types document that totals must not be computed client-side, for the same reason. | Claude, per the standing rule not to guess past a blocker |
| 2026-11-24 | **The bearer token is kept in `localStorage`, recorded as provisional rather than settled** (§5.6) | It is what a stateless `Authorization: Bearer` API makes easy, and the backend is deliberately stateless with no session cookie (§5.4). The cost is real: any XSS on the origin can read the token, and there is no revocation (§12 #18), so a stolen token is good for its full eight hours. The alternative — an httpOnly cookie — needs the backend to set it and brings CSRF back with it. Recorded here so that it is a known trade to revisit alongside §1.3, rather than an accident of scaffolding. | Claude |
| 2026-11-24 | **The frontend branch is rebased onto `dev/api/resibo` before committing, rather than left to diverge** (§7.1, §12 #23) | Forced by what happened on the first frontend commit: the branch was cut at `4883414`, and `0135659` and `e125c6e` landed on the backend branch while the client was being written — adding §5.4, §5.5 and §12 #17–19. Committing without rebasing would have produced two different documents both calling themselves version 0.8, with two different §5.4s. Rebasing first is the cheapest way to keep one plan rather than two, and it is now the expected step before any frontend commit. | Claude |
| 2026-11-24 | **The client calls the deployed API by default; a local backend is the opt-in** (§5.6) | Owner's instruction, and it makes the frontend runnable alone — `npm run dev` with no backend, no database and no JDK, which is the difference between a scaffold someone can look at and one they have to assemble first. Verified rather than assumed: the live endpoint returns 200 and the preflight from `http://localhost:5173` is allowed. The accepted cost is that **development points at production**, which is harmless only while every call is a read. Mitigated visibly rather than silently — `env.isUsingDeployedApi` drives a notice on the status page, and the page prints the base URL — and recorded as something to revisit before the first mutating screen, since it is one constant in `src/config/env.ts`. | Owner, trade-off recorded by Claude |
| 2026-11-24 | **One repository, one branch `dev/resibo`, with `client/` and `server/` at the root** — replacing both the root-level backend and the `dev/api` + `dev/app` split (§7.1, §7.4) | Owner's decision, taken after the two-branch layout produced a concrete failure the same day: the frontend branch was cut at `4883414`, two documentation commits landed on the backend branch while work was in progress, and the frontend's plan section had to be renumbered and rebased before it could be committed, because both copies claimed to be version 0.8. Branches model divergent versions of the *same* content that are meant to converge; two applications never converge, so the split bought drift rather than independence. One tree also makes a cross-stack change — an endpoint and its caller — a single reviewable commit, which matters here because `LoginResponseDto` had already changed under the client once. | Owner |
| 2026-11-24 | **Independent deploys come from CI path filters and disjoint deploy targets, not from the repository layout** (§7.4) | The owner's actual requirement was that a frontend release must not redeploy the backend, and separate branches were the assumed way to get it. They are not: two branches still need two workflows, and give drift as well. `client-ci.yml` filters on `client/**` and `server-ci.yml` on `server/**`, so a client-only commit never *starts* the server workflow — the run is not queued, not queued-and-skipped. The stronger guarantee is underneath: the backend deploy builds an image and restarts a container on `127.0.0.1:8083`, the frontend deploy rsyncs files into `/var/www/resibo/live` with nothing restarting. They share no process, port or directory, so one cannot redeploy the other even if a workflow tried. | Owner's requirement, mechanism by Claude |
| 2026-11-24 | **The Docker build context is `./server`, not the repository root** (§7.4) | Before the split, `client/` sat inside the context and was kept out of the image by a `.dockerignore` rule — a rule someone has to remember. Narrowing the context makes it structural: `node_modules` is outside the context and cannot enter the image even if the rule is deleted. The deploy entry points (`docker-compose.yml`, `scripts/`) deliberately stayed at the repository root so the server's existing command, `cd /opt/resibo/app && git pull && bash scripts/deploy.sh`, did not change — verified by redeploying the API from the new layout and watching the health gate pass. | Claude |
| 2026-11-24 | **The client is static files served by nginx, not a container** (§7.4) | Owner's choice between the two patterns already in the house. It is gainsoclock's frontend arrangement — `/var/www/<project>/<env>`, vhost `<project>-web`, `try_files $uri $uri/ /index.html` for SPA routing — and it is the simplest correct thing for a bundle of static files: no image, no port, no healthcheck, and **nothing restarts on deploy**, which is itself the separation the owner wanted. The backend's container pattern buys a health gate and an atomic rollback unit, neither of which a directory of files needs. | Owner |
| 2026-11-24 | **The client's nginx vhost lives only on the server, written by `scripts/setup-client-server.sh`** | Owner's call, and consistent with every other vhost on that host — `resibo`, `gainsoclock-web`, `eloms` all exist only in `/etc/nginx/sites-available`. A committed `.conf` would have been a second source of truth that drifts, which is the exact problem the monorepo had just removed from this document. The script is the reproducible artifact, so a rebuilt server is one command away. It stages the file as the invoking user and moves it with `install`, never through a `sudo` pipe (§8.1). | Owner |
| 2026-11-24 | **The SSH key is authorised on the server; password authentication is no longer used by a session** | The standing task had been open since the first deployment, and it stopped being a convenience item when piping a password into `sudo` wrote a credential into a world-readable nginx config (§8.1). `id_ed25519` is now in `authorized_keys` and key-only authentication is verified with `PasswordAuthentication=no`. Where a password is still unavoidable — `sudo` on this host requires one — it is given to `sudo -S` as the **only** consumer of stdin, with the remote command passed as an ssh argument rather than through `bash -s`. The earlier incident happened precisely because a heredoc and a piped password shared one stdin. | Claude |
| 2026-11-24 | Codex owns frontend work and Claude Code owns backend work on `dev/resibo`; Codex maintains `CODEX.md` through `AGENTS.md` as a short handoff while the plan and vault remain canonical | The role split reduces overlapping edits, and an automatically discoverable handoff lets a new session recover frontend state after an interruption without creating a competing product specification. Shared contract changes require coordination and reviewer acceptance. | Owner |
| 2026-11-24 | **CORS is restricted to named origins, and the *default* is the real list rather than `*`** (§5.4) | Owner's instruction to limit the API to the deployed client and local development. The default matters as much as the value: a wildcard default is the kind of silently-wrong setting that reaches production because nothing ever complains about it, so an environment that forgets `CORS_ALLOWED_ORIGINS` now fails closed — the same reasoning that keeps the datasource properties defaultless. **`localhost:5173` is deliberately in the production list**, because the client defaults to calling the deployed API, so `npm run dev` with no local backend is a cross-origin call from localhost to that host; removing it would break the normal development loop. The override that had been sitting in `/opt/resibo/resibo.env` as `CORS_ALLOWED_ORIGINS=*` was commented out so the committed, reviewable list governs rather than an unversioned file — and it is the reason the first deploy of this change had no effect, which is why it was verified against the live API rather than assumed. | Owner, defaulting and verification by Claude |
| 2026-11-24 | Bundle a SpongeBob GIF on the client health page for a pipeline test (§5.6) | The owner asked for a visible client-only change and a push. Keeping the small GIF in `client/public/` makes the result independent of an external image host and guarantees the `client/**` workflow filter matches. It is temporary decoration on the existing health page, not a product design decision. | Owner, implemented by Codex |
| 2026-11-24 | Give client deployment its own runner label and repository variable (§7.4) | The owner wants every frontend push to build and reach the live site. A shared `DEPLOY_ENABLED` flag would also arm the backend deploy job, which was not requested. `CLIENT_DEPLOY_ENABLED` and `resibo-client-deploy` keep the frontend path independent while the backend gate remains closed. The deploy script retains old hashed assets so an in-flight page does not break during the swap. | Owner, implemented by Codex |

---

## 14. Document change log

| Date | Version | Change | By |
|---|---|---|---|
| 2026-11-24 | 0.1 | Initial template. Stack and modular-monolith structure recorded as decided; all scope, architecture, and deployment questions open. | Claude |
| 2026-11-24 | 0.2 | Added §7.1 branches: remote URL, `main` is README-only, all work commits directly to `dev/api/resibo`. Push policy still open. | Claude |
| 2026-11-24 | 0.3 | Push policy decided (push every commit) and commit-message convention recorded in §7.1. `CLAUDE.md` added to the repository pointing future sessions here. Vault notes created. | Claude |
| 2026-11-24 | 0.4 | Branch renamed to `dev/api/resibo`. Added §6.1.1 (gainsoclock reference and the layout conflict, as a blocker) and §6.1.2 (adopted conventions). Added §7.2 verified infrastructure: DB connection working, SSH not, repository confirmed public. Rewrote §8 with the handover file location and added §8.1 outstanding security risks. Added `.gitignore`. | Claude |
| 2026-11-24 | 0.5 | Reconciled the document with the committed scaffold (`2b6dcd6`). **§6.1 rewritten**: it described a `modules/` tree that does not exist, and now states that the code is layer-first (§6.1.1 option A) with the modular monolith kept as the documented alternative; §6.1.1 stays open as a confirmation item. Added §5.1 recording the scaffold, its dependencies, the `4.2.0.RELEASE` / "packaging: yaml" traps, the local toolchain, and the verified build and endpoints. §5's decision column filled in where code decided it and marked open where nothing has (no JPA, no driver, no Flyway, no auth, no frontend). §6.1.2 answers the `ResponseDto` and Boot/Java questions and lists which conventions actually exist. §10 and §12 #8 record the UTC `responseTime` defect; §12 #9 records the structure confirmation; §11 marks phase 1 partially delivered. Six decisions appended to §13, including the structural supersession. | Claude |
| 2026-11-24 | 0.6 | Recorded the deployment (`c6e826d`). **§7 filled in**: production exists, serves health endpoints only, and the environments table now distinguishes a live production from an unbuilt prelive. Added **§7.3 "Deployment, as it actually works"** — the on-server layout, the deploy command, the rollback mechanism, the nginx vhost directives, the tunnel ingress with the append-don't-overwrite warning, the server port allocation, and the verified public checks. Added **§5.2** for the container runtime (multi-stage build on the committed `mvnw`, JRE-only non-root runtime, loopback-only publishing, capped logs) and two rows to §5's decision table. §7.2 updated: server access, Docker, nginx and the tunnel now verified; the server's JDK is no longer a question. §8 records the Cloudflare token by scope and the server env file by location, and settles how production values are held. §11 marks phase 8 substantially delivered for the API and lists what it does **not** include — backups, prelive, CI, and an application. §12 gains #10 (`className`/`methodName` now leak publicly — the §6.1.2 deadline has passed), #11 (prelive?) and #12 (edge authentication?), and #8 notes the UTC defect is now wrong in public. Corrected two stale §5.1/§6.1.2 claims: `application.yaml` holds five keys including `spring.config.import`, and `.env.example` plus `scripts/deploy.sh` now exist. Eight decisions appended to §13. | Claude |
| 2026-11-24 | 0.7 | Recorded the database integration (`9b57c2b`, docs note `bfafb3a`). Added **§5.3 "The database connection"** — the three new dependencies and why the driver is `runtime` scope, the datasource block key by key, **`ddl-auto: none` with its audit-trail reasoning and a hard rule against ever relaxing it**, `open-in-view: false`, the auto-detected `MariaDBDialect`, the five environment keys (matching gainsoclock's names) and where their values live, the local port move from 8088 to 8083, the **least-privilege `resibo` account** with its probed isolation, the **CI MariaDB service container** and why its credentials are not GitHub Secrets, and what was verified. §5's decision table updated: persistence is JPA **in use**, migrations are now the gap that matters, Testcontainers moved from academic to needed. §5.1 gains the three dependencies and a corrected description of `application.yaml` and the env files — `.env.dev` now holds a real credential. §5.2's `extra_hosts` note is no longer hypothetical. §7's environments table gains a **CI** column and states what each environment's database actually is; §7.2 records the new account and the 10 production connections as `resibo`; §7.3 records the env file's new contents and **rewrites the 8088 trap as resolved**. §8's inventory now distinguishes the retired superuser from the application account and adds the local and CI env files; **§8.1 risk 2 is marked RESOLVED** with what was done and what remains, risk 3 is narrowed to the administrative credentials, and a new risk 5 records that local tests reach the shared instance. §10's backup row and §11's phase notes corrected. §12 gains #13 (local tests hit the shared production database), #14 (narrow the account's `%` host), #15 (nothing owns the schema) and #16 (the undocumented CI `deploy` job). Eight decisions appended to §13. **Throughout: "connected to the database" is stated as plumbing, never as persistence — there are still no entities, no repositories and no tables.** | Claude |
| 2026-11-24 | 0.8 | Recorded authentication (`54c5e21`, `963c737`, `d30000f`, `8b57196`). Added **§5.4 "Authentication and security"** — the five new dependencies and why jjwt is 0.13 and not gainsoclock's 0.11.5, the `user_role` and `user` tables with their apply order and seeded roles, the full access-rules table with its default-deny ending, the two real HTTP error statuses, the signing key and its no-default and 32-character rules, the role model, **the public-signup bootstrap with its warning**, the new `E01xx` error codes and why `E0106` covers three failures, CORS, the move of products behind ADMIN with the `Admin<Name>Controller` naming rule, **three traps** (native `EXISTS` returns `Integer`, `/error` must be permitted, Boot 4 uses Jackson 3), and what was verified against the live public API. Added **§5.5 "Repositories are native SQL"** — `countQuery`, column-name sorting via `SORT_COLUMNS`, and the collation that makes `LOWER()` unnecessary. §5's decision table: auth is **JWT in use**, a password-hashing row added, persistence and migrations rows corrected, tests now 22. §5.1 gains the five dependencies, the `jwt` block, the seven env keys, **the full eight-endpoint API surface**, and a note that `.env.example` and CI are both missing `JWT_SECRET`. §6.1's tree redrawn (around thirty classes, not six) and **the "reversal is cheap" claim withdrawn** in §6.1, §6.1.1 and §12 #9. §6.1.2 records the `Admin<Name>Controller` rule, that almost every gainsoclock convention now exists, and the deliberate `UserUtility` divergence. §4 notes that three of its rows now exist in the database ahead of the section being answered. §7's environments table, §7.2 and §7.3 corrected — three tables, eight endpoints, `JWT_SECRET` in the server env file, and edge authentication reframed as hardening. §8 settles how the signing key is held and adds two inventory rows (the JWT key, and the exposed application admin account — **username only, never the password**). §8.1: risk 1 extended to the admin account, risk 5 corrected now that tables exist, and **new risk 6 for the signup bootstrap**, which replaces the old "the whole API is open" exposure. §10's backup rows and §11's phase notes corrected — phase 1 is substantially delivered and phase 2 partly, and backups are now a real gap. §12 gains #17 (should signup stay public), #18 (no refresh or revocation), #19 (CORS is open), #20 (`JWT_SECRET` missing from `.env.example` and CI) and #21 (is native-SQL-everywhere a rule), with #6, #9, #12, #13 and #15 restated against what now exists. Fifteen decisions appended to §13. **No secret value, server address or OS account name appears anywhere in this document.** | Claude |
| 2026-11-24 | 0.9 | Recorded the **React client** (§5.6): its location at `src/main/client`, the new branch `dev/app/resibo`, the versions actually installed (Vite 8, React 19, TypeScript 7, React Router 7, sass, oxlint, Prettier), the folder structure and its one import rule, the API layer and why it exists, what was deliberately **not** built (no login screen, no formatting helpers, no tests, no state library, no deployment path), and what was verified — including that `./mvnw clean compile` is unaffected and that the client has **never been run against the live API**. §5's six frontend rows updated from "Open" to what is in use, and the "Repo layout" row settled. §7.1 gains the `dev/app/resibo` row, the amended standing instruction, and the warning that two branches now carry this document. §12 gains **#22** (nothing builds or deploys the frontend) and **#23** (two copies of this plan, which already bit once — this version was written on a branch cut before 0.8 landed and had to be rebased and renumbered). Six decisions appended to §13. | Claude |
| 2026-11-24 | 0.10 | The client now defaults to the **deployed** API instead of a local backend (§5.6), with a new *Which API it talks to* subsection covering the trade, the two visible mitigations and the `VITE_API_BASE_URL=/api` escape hatch. §5.6's dev-server row rewritten, and its verification table split: the deployed endpoint and its CORS preflight are now **verified by `curl`**, while the client in a browser is still **not** verified. One decision appended to §13. | Claude |
| 2026-11-24 | 0.11 | **The repository is now one branch, `dev/resibo`, with `client/` and `server/` at the root**, and the client is deployed. Added **§7.4 "Deploying the client, and why it cannot redeploy the API"** — the two deploy paths side by side, the three levels at which they are independent (different directories, different vhosts, different path-filtered workflows), the one-time setup script, the deploy procedure with `index.html` installed last, the Cloudflare whole-config `PUT` warning, and what was verified live. §0 records the single branch and flags the stale second clone. §5.6's location and branch rows updated to `client/` and `dev/resibo`. §5's "Repo layout" row settled. §12 **#22 mostly closed** — built and verified, with only the missing self-hosted runner left — and **#23 resolved**, since one branch means one copy of this document. Six decisions appended to §13, including why path filters rather than branches give independent deploys, and the SSH key now being authorised. | Claude |
| 2026-11-24 | 0.12 | Recorded the owner-assigned Codex frontend and Claude Code backend roles, the shared-file and review method, and the `AGENTS.md` to `CODEX.md` handoff. Corrected the opening frontend status and designated working copy; the four vault notes remain the durable project record. | Codex |
| 2026-11-24 | 0.13 | **CORS restricted** to `https://resibo.polbinsenthabyer.uk`, `http://localhost:5173` and `http://127.0.0.1:5173`, with the `application.yaml` default changed from `*` to that list so a missing `CORS_ALLOWED_ORIGINS` fails closed. §12 **#19 resolved**, with the live evidence: the three named origins allowed, `evil.example.com` and `localhost:3000` both **403 with no CORS headers**, and no-`Origin` clients unaffected. One decision in §13, including why the wildcard override in the server's env file was removed rather than edited. | Claude |
| 2026-11-24 | 0.14 | Added a locally bundled SpongeBob GIF to the client health page as a visible test for the `client/**` pipeline. §5.6 records its temporary scope and §13 the reason for bundling it. | Codex |
| 2026-11-24 | 0.15 | Configured client-only automatic deployment with a dedicated runner and `CLIENT_DEPLOY_ENABLED` gate; removed premature asset deletion from `scripts/deploy-client.sh`. §7.4 records the deployment path and its pending end-to-end verification; §13 records the separation from backend deployment. | Codex |
