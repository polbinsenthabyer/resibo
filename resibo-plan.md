# Resibo — Plan & Source of Truth

> **This document is the source of truth for Resibo.** The code, the database
> schema, and the deployment follow this file. If the code and this file
> disagree, one of them is a bug: either the code drifted, or this file went
> stale and must be corrected in the same change that revealed it.

**Status: planning, with a deployed skeleton that now reaches its database.** A
buildable Spring Boot application exists — scaffold, response envelope, health
endpoint, green tests (§5.1) — it is deployed, running on the server in Docker
behind the host's nginx, fronted by the Cloudflare tunnel that already serves
the sibling projects, answering on `https://resibo-api.polbinsenthabyer.uk`
(§7.3), and as of 2026-11-24 it **connects to MariaDB** as a dedicated
least-privilege account, with Hibernate forbidden from touching the schema
(§5.3).

**Read both "deployed" and "connected" narrowly.** What is live serves the two
health endpoints and nothing else. The schema it connects to is **empty**:
there are **no entities, no repositories, no tables and no migrations**, so
nothing is read and nothing is written. "Connected to the database" is a
plumbing milestone, not persistence, and must never be reported as the latter.
**Every `[BLOCKER]` item below is still unanswered, and nothing that depends on
one should be written until it is.** A deployed skeleton with a live connection
pool unblocks none of them; it only means that when there is something to ship,
the road to the public internet and the road to the data are both built and
tested.

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
| Document version | 0.7 — database connection recorded; scope still unfilled |
| Created | 2026-11-24 |
| Last updated | 2026-11-24 |
| Last updated by | Claude (recorded the JPA datasource, the least-privilege database account, and the CI service container) |
| Plan status | **AWAITING OWNER INPUT** — the skeleton is deployed, publicly reachable (§7.3) and now connected to MariaDB (§5.3), but it serves health endpoints only against an empty schema, and every `[BLOCKER]` in §1, §2 and §6.1.1 is still open |
| Working copy | `%USERPROFILE%\sprintify_solutions\personal\resibo` |
| Remote | `https://github.com/polbinsenthabyer/resibo.git` |
| Production URL | `https://resibo-api.polbinsenthabyer.uk` — health endpoints only; see §7.3 |
| This document lives on | branch `dev/api/resibo` — see §7.1 |
| Vault notes | `Obsidian Vault/Projects/personal/resibo/` |

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
| API style | REST + JSON | REST + JSON **as built** — though only one endpoint exists, so this is cheap to revisit. |
| API docs | OpenAPI generated from the controllers | **Open** — no OpenAPI dependency in the pom. |
| DB migrations | Flyway, versioned SQL committed to the repo | **Open, and now the gap that matters** — no Flyway in the pom and no migrations, while `ddl-auto: none` means Hibernate will not create a table either. The schema has no owner yet; §12 #15. |
| Persistence | Spring Data JPA, with plain SQL where reporting needs it | **Spring Data JPA — in use** (§5.3), with the MariaDB driver at `runtime` scope. The application connects to the `resibo` schema as a dedicated least-privilege account. **There are still no entities and no repositories**, so JPA manages nothing yet; the connection pool is real, the persistence is not. |
| Auth | Short-lived JWT access token + refresh; roles from §1.3 | **Open** — no Spring Security; the health endpoint is unauthenticated. Blocked on §1.3. |
| Frontend build | Vite | **Open** — no frontend code exists. |
| Frontend language | TypeScript | **Open** |
| Server state | TanStack Query | **Open** |
| UI components | shadcn/ui on Tailwind | **Open** |
| Routing | React Router | **Open** |
| Backend tests | JUnit 5 + Testcontainers against real MariaDB | **JUnit 5 in use** (`spring-boot-starter-webmvc-test`, `spring-boot-starter-actuator-test`, `spring-boot-starter-data-jpa-test`). **Testcontainers still open — and it stopped being academic**: the context test now needs a datasource, CI supplies a throwaway one (§5.3), but a local `mvnw verify` loads `.env.dev` and therefore connects to the **shared production database**. §12 #13. |
| Frontend tests | Vitest + React Testing Library; Playwright for checkout | **Open** |
| Local database | Docker Compose for MariaDB | **Decided by default, and not well.** `.env.dev` points local runs *and local tests* at the shared MariaDB on the server, because that is the database that exists. A local container or Testcontainers would isolate it; neither is in place. §12 #13. |
| Repo layout | One repo: `backend/` and `frontend/` | **Diverged.** The backend was scaffolded at the **repository root** — `pom.xml` and `src/` are top-level, there is no `backend/`. Where the frontend lives is therefore open again; see §5.1. |

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

Dependencies, exactly as they stand in `pom.xml` — the bottom three were added
on 2026-11-24 for the database connection (§5.3):

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
| `spring-boot-starter-data-jpa-test` | `test` | The JPA test slice, so repository tests do not need the full context when there are repositories to test |

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

`application.yaml` holds six things and no secrets: the application name
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
did); and `logging.level.com.api.resibo: INFO`.

The env files that go with it: **`.env.example` is committed and carries names
only**, as §8 promised; `.env.dev`, `.env.prelive` and `.env.prod` are
git-ignored. They are parsed as a `.properties` file, which is a real
constraint worth remembering — no `export`, no shell expansion, no quotes
around values. **This has stopped being a file of harmless values.** The five
keys are `SERVER_PORT`, `DATASOURCE_URL`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD` and `DB_POOL_MAX`, the datasource keys are no longer
commented out, and `DATABASE_PASSWORD` is a real credential — so `.env.dev` is
now exactly the kind of file the §8 rules exist for.

**The API surface, such as it is.** `HealthController` carries
`@RequestMapping("api/health")` and `@GetMapping("/check")` — so `GET
/api/health/check`, returning the standard envelope. gainsoclock also exposes a
`/check/prelive` variant; it was deliberately left out, because no prelive
environment has been decided for Resibo (§7). It costs one method to add when
one exists.

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
statement. **What it is not:** persistence. There are no entities, no
repositories, no tables and no migrations — the pool connects to an empty
schema and the application still serves only health endpoints.

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

Verified rather than trusted: **zero DDL statements appear in the application
log, and the `resibo` schema still contains zero tables after the application
has run** — locally and on the server.

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
connects to the shared production database. Nothing writes today, because there
are no entities, but that is a property of the code being empty rather than of
the setup being safe. §5 already recommended Testcontainers; this is what makes
it concrete, and it is recorded as an open question (§12 #13) rather than a
footnote.

#### Verified, not assumed (2026-11-24)

| Check | Result |
|---|---|
| Local `./mvnw -B clean verify` | **BUILD SUCCESS**, 2/2 tests. Hikari connected; the log reports `Database dialect: MariaDBDialect` |
| `GET /actuator/health` with details | `db: {status: UP, database: MariaDB, validationQuery: isValid()}` |
| DDL issued by the application | **None.** Zero DDL statements in the log, and the `resibo` schema still holds **0 tables** after a run — `ddl-auto: none` proven, not assumed |
| Isolation of the `resibo` account | Probed directly; results in the table above |
| On the server after redeploy | Container `Up (healthy)`, zero DDL, and `information_schema.processlist` showed **10 live connections from the container as user `resibo` on database `resibo`**, with the superuser holding none — the account switch is confirmed in production, not merely configured |
| Public endpoint through the tunnel | Still `200` |

---

## 6. Architecture

### 6.1 Backend structure — layer-first in the code, modular monolith deferred

**What the code actually is, as of 2026-11-24.** The backend is packaged
**layer-first at the base package**, mirroring gainsoclock:

```
src/main/java/com/api/resibo/
├── ResiboApplication.java
├── controllers/     HealthController
├── dtos/            ResponseDto
├── types/           ResponseType
└── utilities/       MessageType, ResponseUtility
```

There is no `modules/` package and no `shared/` package. There are no
`entities/`, `repositories/` or `services/` packages yet either — those arrive
flat, in gainsoclock's shape, with the first feature that needs them.

That is **option A of §6.1.1**, not the modular monolith described below. It was
taken because the owner's most recent and most specific structural instruction
was to use gainsoclock as the reference, and the two instructions cannot both be
honoured as written. **It supersedes the earlier modular-monolith decision — see
§13 — and it is pending the owner's explicit confirmation**, which is why
§6.1.1 stays open rather than being marked answered. Six files is the cheapest
this reversal will ever be; if the answer is B, say so before the first domain
module lands.

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
├── configs/        SecurityConfig + jwt/ (filter, util, entry point, denied handler)
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
and a one-word reply closes it. Right now the whole backend is six classes in
four packages; switching to B is minutes of work. After the first domain module
it is hours, and after three it is the expensive kind of restructure option C
warns about.

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
| Boilerplate | Lombok `@Data` / `@NoArgsConstructor` / `@AllArgsConstructor` on DTOs |
| Shared helpers | `utilities/` for stateless static helpers |
| Config | `application.yaml`; `.env` and `.env.*` git-ignored |
| Deployment | `scripts/deploy.sh`, `scripts/deploy-prelive.sh`; a GitHub Actions workflow (gainsoclock's is `maven.yml`; Resibo's is `ci.yml`) |

**Which of those exist as of 2026-11-24.** Implemented: the `ResponseDto`
envelope and its fields; `ResponseUtility.getSuccessResponse(message[, data])`
and `getErrorResponse(code, message)` as static factories; the success code
`S0001`; `ResponseType` as a `*Type` enum in `types/`; Lombok `@Data` /
`@NoArgsConstructor` / `@AllArgsConstructor` on the DTO; `utilities/` for
stateless helpers, which now holds `ResponseUtility` and a `MessageType` class
of message constants; and `application.yaml` with `.env*` git-ignored.

`ResponseUtility` fills `className` and `methodName` with `StackWalker`, taking
the first stack frame outside itself — cleaner than gainsoclock's approach, same
result, and verified to resolve correctly (§5.1).

Added since, and recorded elsewhere in this document: `scripts/deploy.sh` in
gainsoclock's shape (§7.3), the committed `.env.example` the §8 rules promised,
carrying variable names with empty values (§5.1, §5.3), and a GitHub Actions
workflow — `.github/workflows/ci.yml`, not gainsoclock's `maven.yml` filename —
whose `build` job runs `mvnw -B clean verify` against a throwaway MariaDB
service container (§5.3).

Not yet present, and not yet contradicted either: the interface +
`XxxServiceImpl` service shape and "every service method returns `ResponseDto`"
(there are no services), the `XxxDto` / `XxxListDto` pairing (there is one DTO),
error codes beyond `S0001`, and `scripts/deploy-prelive.sh` (no prelive
environment has been decided — §7, §12 #11).

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
below. **"Production" here means the skeleton is reachable, not that there is a
product behind it** — it serves the two health endpoints and nothing else. It
now also holds a connection pool to an **empty** schema (§5.3), which is a
different statement from storing anything.

| | Local | CI | Staging / prelive | Production |
|---|---|---|---|---|
| Needed? | yes | yes | probably, eventually — §12 #11 | yes |
| Status | working | **working since 2026-11-24** | **not built** | **live since 2026-11-24** |
| Where it runs | this machine, from `mvnw` or from the container | a GitHub-hosted runner, `.github/workflows/ci.yml` | — | the shared Ubuntu server, in Docker behind the host's nginx |
| Database | **the shared MariaDB on the server**, via `.env.dev` — the same instance production uses, which is the problem recorded as §12 #13 | a throwaway MariaDB 11 service container, `resibo_test` (§5.3) | — | **the `resibo` schema on the shared MariaDB**, as the least-privilege `resibo` account. **0 tables** — connected, empty, and read by nothing |
| URL | `http://localhost:8083` with the current `.env.dev`, `:8080` without it | — | — | `https://resibo-api.polbinsenthabyer.uk` |
| What it serves | `/api/health/check` and `/actuator/health` | nothing — it builds and tests | — | the same two endpoints, and nothing else |

| Question | Answer |
|---|---|
| Server | Self-hosted on the owner's LAN, Ubuntu, shared with the sibling projects. Address, account, and password are in the handover file (§8) — **deliberately not written here, because this repository is public.** |
| Server OS / specs | Ubuntu (version still unconfirmed — not needed now that the build runs in a container) |
| How we deploy (git pull + restart / Docker / CI pipeline) | **Docker, from a git checkout on the server** — `git pull` then `scripts/deploy.sh`, which builds the image there, restarts, health-gates, and rolls back on failure. Full procedure in §7.3. `.github/workflows/ci.yml` now exists: its `build` job is the test gate (and the reason for the CI service container, §5.3), and it **declares** a `deploy` job that runs the same `scripts/deploy.sh` on a self-hosted runner labelled `resibo-deploy`. The manual command in §7.3 is still the documented path, and the CI-driven one is not described there — see §12 #16. |
| Domain name | `resibo-api.polbinsenthabyer.uk` — a subdomain of the zone that already serves the sibling projects. No new domain was bought. |
| Cloudflare role: DNS only / proxied / Tunnel / Access in front of admin | **Proxied DNS over a Cloudflare Tunnel.** The existing `personal-projects` tunnel is reused rather than a new one created; a `CNAME` points the hostname at it. **Access is not in front of anything** — the API is open to the internet, which is tolerable only while it serves health checks. Revisit before the first real endpoint. |
| TLS: Cloudflare origin cert / Let's Encrypt | **Neither.** TLS terminates at Cloudflare's edge and the tunnel carries the request to the origin, so the origin holds no certificate: nginx listens on plain HTTP on loopback, and the container is published on loopback. Nothing on the LAN speaks TLS and nothing needs to. |
| Who else needs server access | Open — nobody but the owner today. |

### 7.1 Branches (decided)

**Decided by owner, 2026-11-24:**

| Branch | Holds |
|---|---|
| `main` | `README.md` only — the public face of the repository |
| `dev/api/resibo` | backend development branch; carries `README.md`, `CLAUDE.md` **and** this plan |

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

So the standing instruction for every session is: **work on `dev/api/resibo`.** Touch
`main` only to update the README, and then only the README.

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
| `resibo` schema | Exists, and is **still empty** — no tables, even after the connected application has run (§5.3) |
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
3. **The `resibo` schema is empty**, so there is no legacy structure to work
   around — the schema in §4 can be designed freely once the blockers are
   answered. It stays empty on purpose: `ddl-auto: none` means the application
   will not create a table even now that it is connected (§5.3).

**Open:** Ubuntu version, and whether MariaDB should stay shared or get a
dedicated instance for Resibo. The server's JDK is no longer open and no longer
matters — the container brings its own (§5.2).

### 7.3 Deployment, as it actually works (2026-11-24)

Written down in operational detail because the next person to deploy will be a
session with no memory of this one, and because one step of it can take the
neighbouring services down silently.

**What is deployed is the skeleton.** Docker container on the shared Ubuntu
server, reverse-proxied by the host's nginx, fronted by the Cloudflare tunnel
that already serves the sibling projects. It answers `/api/health/check` and
`/actuator/health`. Since 2026-11-24 it also holds a connection pool to the
empty `resibo` schema as the least-privilege `resibo` account (§5.3) — which
changes what the deployment needs in its env file, and nothing about what it
serves. It has no authentication and implements no part of a POS.

#### On the server

| Path or object | What it is |
|---|---|
| `/opt/resibo/app` | A git checkout of `dev/api/resibo`. Cloning needed no credential, because the repository is public (§7.2) — the one convenience that awkward fact buys. |
| `/opt/resibo/resibo.env` | The environment file, mode `0600`, **outside the checkout and not in git**. As of 2026-11-24 it holds `APP_ENV=prod` **and the datasource keys** — `DATASOURCE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (§5.3). It is therefore a real secret store now, not a one-line convenience: the `0600` mode and its location outside the checkout stopped being precautionary. Without it the application does not start at all, because the datasource keys have no defaults. |
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
- The JWT signing key is generated per environment and differs between them.

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
| Server application env file | per-environment configuration for the container | `/opt/resibo/resibo.env` on the server, mode `0600`, outside the checkout | ✅ in place — and since 2026-11-24 it holds `APP_ENV=prod` **plus the datasource URL, username and password** (§5.3). It is now the one file on the server that must not leak, and the application will not start without it. |
| Local development env file | local configuration, including the database password | `.env.dev` in the working copy — git-ignored, and `.dockerignore`d under any name | ✅ in place, and **no longer value-free**: it carries a real `DATABASE_PASSWORD` and points at the shared instance (§12 #13). |
| CI database credentials | the throwaway MariaDB service container in `.github/workflows/ci.yml` | **in the workflow file, in plain sight, deliberately** | ✅ working — and **intentionally not a GitHub Secret**: the container is created for one job and destroyed with it, so there is nothing to protect (§5.3, §13). |
| Domain registrar login | domain ownership | | not yet provided |
| JWT signing key | session tokens | generate per environment | not created |
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
| Backup frequency, and where backups are stored | **Still open, and one step closer to urgent.** Nothing is backed up. There is still nothing stateful to back up — the `resibo` schema holds **0 tables** even though the application is now connected to it (§5.3) — so the deadline has not arrived; but the on-server state is no longer a git checkout and a one-line env file. `/opt/resibo/resibo.env` now carries the datasource credentials and exists nowhere else on the server (§7.3), which makes it the first thing on that machine whose loss costs something. This row becomes urgent the day the first table is created, which is also the day a restore test stops being theoretical. |
| Has a restore ever been tested? (It is not a backup until it has.) | No — there is no backup to restore. |
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

**Where this stands (2026-11-24).** Phase 1 is **partially delivered, and moved
once since**. Done: the application skeleton, a working Maven build, the
`ResponseDto` conventions, a health endpoint, two passing tests (§5.1), and —
as of 2026-11-24 — **the MariaDB connection** (§5.3): JPA starter, driver,
Hikari pool, a least-privilege account, and `ddl-auto: none`. Not done, and not
started: Flyway and the first migration, any entity or repository, login, and
roles. Boundary tests are not merely undone but currently moot — §6.1.1 went to
layer-first, so there are no module boundaries to test.

"The database connection was the one piece that could move now" was the previous
version of this paragraph, and it moved. **It moved alone**: the pool connects
to an empty schema, so the application reads and writes nothing. Of what remains
in phase 1, migrations wait on the §4 schema, which waits on the §2 blockers;
login and roles wait on §1.3. The next piece that could move without an answer
is choosing the schema's owner — a migration tool, not a table (§12 #15).

**Phase 8 is substantially delivered for the API, out of order (2026-11-24).**
Done and verified in §7.3: the server, a Docker deployment with a health-gated
restart and a working rollback path, the nginx vhost, a subdomain on the zone
that already serves the sibling projects, and a public HTTPS URL through the
existing Cloudflare tunnel. It was taken early precisely because it is the one
phase that depends on **none** of the open `[BLOCKER]`s — a health endpoint is
as deployable as a finished POS, and finding out that the nginx-plus-tunnel
pattern works is far cheaper now than during phase 3.

What phase 8 does **not** yet include, so nobody reads the row as finished:

- **Backups.** Nothing is backed up, and nothing stateful yet needs to be: the
  schema the application now connects to holds no tables. What the server does
  hold, since 2026-11-24, is the datasource credentials in
  `/opt/resibo/resibo.env` and nowhere else. The §10 backup rows are still
  blank, and they become urgent the moment the first table exists — not before,
  and not after.
- **Prelive / staging.** Not built. gainsoclock has one on port 8082 and a
  `deploy-prelive.sh`; Resibo has neither, and whether it should is §12 #11.
- **CI, past the test gate.** `.github/workflows/ci.yml` exists: its `build` job
  runs `mvnw -B clean verify` against a throwaway MariaDB service container
  (§5.3), which is a real gate and the reason the database work needed it. It
  also declares a `deploy` job on a self-hosted runner. **That path is not
  documented in §7.3 and is not the procedure a session should follow** — the
  manual deploy is. §12 #16.
- **An application worth deploying.** This is the one that matters. The road to
  the public internet is now paved and tested; that moves phases 2 through 7 not
  one step. Every `[BLOCKER]` and the §6.1.1 confirmation are exactly as open as
  they were the day before the deployment.

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
| 6 | Roles and permissions (§1.3) | auth model | 2026-11-24 | open |
| 7 | Module boundary enforcement (§6.1) | backend skeleton | 2026-11-24 | moot until #9 — the code has no modules to enforce |
| 8 | Timezone for serialised timestamps (§10) | every API timestamp, receipt times, Z-reading day boundary, and BIR compliance if §2.3 = B | 2026-11-24 | **open — and now wrong in public, not just locally.** The production response reads `03:57` where local time is `11:57` (§7.3). Deliberately not fixed; see §13. |
| 9 | Confirm the layer-first package structure (§6.1.1) | every file path in the backend | 2026-11-24 | **open — but the code already follows option A.** Cheap to reverse while the backend is six classes; expensive once domain code exists. |
| 10 | Suppress `className` / `methodName` on production responses (§6.1.2) | nothing structural — but it is live internal detail on an open endpoint | 2026-11-24 | **open, and its deadline has passed.** §6.1.2 required both fields gone from production error responses "before anything is exposed publicly"; the API went public on 2026-11-24 (§7.3) and still returns them on every response. The fix is cheap; what needs deciding is whether to suppress by environment or everywhere. |
| 11 | Is a prelive / staging environment wanted (§7, §7.3)? | `scripts/deploy-prelive.sh`, the `/api/health/check/prelive` variant gainsoclock has, and a fourth host port | 2026-11-24 | open — gainsoclock has one on 8082; Resibo deliberately has none, and nothing yet justifies one. |
| 12 | Should Cloudflare Access, or any edge authentication, sit in front of this hostname before the first non-health endpoint ships (§7)? | whether the first real endpoint can go live as-is | 2026-11-24 | open — tolerable today only because the API serves health checks and holds no data. Distinct from the §1.3 application auth question; this one is at the edge. |
| 13 | **How do local tests get a database?** Right now `mvnw verify` loads `.env.dev` and connects to the **shared production instance** (§5.3, §8.1 risk 5) | whether a careless test can reach real data; also the Testcontainers row in §5 | 2026-11-24 | **open, and the one new risk the database work introduced.** CI is isolated by its own throwaway container; a developer's machine is not. Harmless only while there are no entities — the moment a repository test writes, it writes to the live instance. §5 already recommends Testcontainers; a local MariaDB container is the cheaper half-measure. Needs a decision before the first repository, not after. |
| 14 | Narrow the `resibo` database account's host part from `%` (§5.3, §8.1 risk 2) | nothing — it is hardening, not a blocker | 2026-11-24 | open, deliberately not done. The privilege half of §8.1 risk 2 is closed; this is the connectivity half. Reaching the port at all requires LAN access, so it is the smaller remaining exposure. Narrowing it to the container gateway would also need the local developer's route considered — see #13. |
| 15 | **Who owns the schema?** `ddl-auto: none` means Hibernate will never create a table, and there is no Flyway and no migration (§5, §5.3) | the first table, and therefore every phase from 2 onwards | 2026-11-24 | open. Not urgent — there is nothing to migrate until §4 is settled — but it is now a *gap* rather than an absence: the application is connected to a schema that nothing is responsible for creating. Decide the tool (Flyway was the recommendation) before the first entity, so the first table arrives as a reviewed migration rather than as a hand-run statement nobody recorded. |
| 16 | The CI `deploy` job is not documented (§7, §7.3, §11) | nothing technical — it is a documentation gap that can mislead a session | 2026-11-24 | open. `.github/workflows/ci.yml` declares a `deploy` job that runs `scripts/deploy.sh` on a self-hosted runner labelled `resibo-deploy`, with `APP_DIR=/opt/resibo`, `HOST_PORT=8083`, `APP_ENV=prod`. §7.3 describes only the manual path, and this document should not have two answers to "how does it deploy". Confirm whether the runner is registered and whether CI deploys are the intended path, then write the winner into §7.3 and a decision into §13. |

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
