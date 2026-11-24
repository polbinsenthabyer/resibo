# Resibo — Plan & Source of Truth

> **This document is the source of truth for Resibo.** The code, the database
> schema, and the deployment follow this file. If the code and this file
> disagree, one of them is a bug: either the code drifted, or this file went
> stale and must be corrected in the same change that revealed it.

**Status: planning. No code exists yet, and none should be written until the
`[BLOCKER]` items below are answered.**

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
| Document version | 0.4 — template, not yet filled in |
| Created | 2026-11-24 |
| Last updated | 2026-11-24 |
| Last updated by | Claude (initial template) |
| Plan status | **AWAITING OWNER INPUT** |
| Working copy | `C:\Users\okarun\sprintify_solutions\personal\resibo` |
| Remote | `https://github.com/polbinsenthabyer/resibo.git` |
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
MariaDB database · **modular monolith** for the backend structure. Those are
settled. The rest below are the choices that hang off them.

| Choice | My recommendation | Your decision |
|---|---|---|
| Java version | 21 (LTS) | |
| Spring Boot version | latest 3.x | |
| Build tool | Maven — more examples, easier handover | |
| API style | REST + JSON | |
| API docs | OpenAPI generated from the controllers | |
| DB migrations | Flyway, versioned SQL committed to the repo | |
| Persistence | Spring Data JPA, with plain SQL where reporting needs it | |
| Auth | Short-lived JWT access token + refresh; roles from §1.3 | |
| Frontend build | Vite | |
| Frontend language | TypeScript | |
| Server state | TanStack Query | |
| UI components | shadcn/ui on Tailwind | |
| Routing | React Router | |
| Backend tests | JUnit 5 + Testcontainers against real MariaDB | |
| Frontend tests | Vitest + React Testing Library; Playwright for checkout | |
| Local database | Docker Compose for MariaDB | |
| Repo layout | One repo: `backend/` and `frontend/` | |

**Accept all recommendations, or state your changes:**

**Answer:**

> Note: checkout is a keyboard-driven, latency-sensitive screen. Whatever the UI
> library, the sale screen needs deliberate keyboard handling — a design task in
> its own right, not a component choice.

---

## 6. Architecture

### 6.1 Backend structure — modular monolith (decided)

**Decided by owner, 2026-11-24:** the backend is a **modular monolith** — one
deployable Spring Boot application, internally divided into modules with
enforced boundaries, rather than either a layer-first package tree or separate
services.

What that means in practice, as I propose to apply it:

- **Package by feature, not by layer.** No top-level `controllers/`,
  `services/`, `repositories/`. Each module owns its own web, service, and
  persistence code.
- **Each module has a public face and a private inside.** Other modules may use
  only what the module deliberately exposes — its API interfaces and its DTOs.
  Entities and repositories stay package-private to the module.
- **Modules never reach into each other's tables.** Cross-module reads go
  through the owning module's interface. This is the rule that keeps a modular
  monolith from quietly becoming a tangle, and the one that makes extracting a
  module later a possibility rather than a rewrite.
- **One database, one schema, but each table has exactly one owning module.**
  Flyway migrations are named by owning module.

Proposed layout — **a proposal, correct it before any code is written**:

```
backend/src/main/java/.../resibo/
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

The owner named `C:\Users\okarun\sprintify_solutions\personal\gainsoclock` as the
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

**Answer:**

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
| Deployment | `scripts/deploy.sh`, `scripts/deploy-prelive.sh`; GitHub Actions at `.github/workflows/maven.yml` |

| Question | My recommendation | Your decision |
|---|---|---|
| Keep the `ResponseDto` envelope, including `className`/`methodName`? | Keep it for consistency with gainsoclock. Note that `className`/`methodName` leak internals to the client — I'd keep them out of error responses in production. | |
| Match gainsoclock's Spring Boot 4.1.2 / Java 17 exactly? | Match Spring Boot 4.1.x. For Java, prefer 21 (LTS, and Boot 4 supports it) unless the server's JDK is 17 — §7 will decide that. | |

**Open questions on the module structure:**

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
Spring Boot modular monolith  (one deployable; modules per §6.1)
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

| | Local | Staging | Production |
|---|---|---|---|
| Needed? | yes | | |
| Where it runs | this machine | | |
| Database | Docker MariaDB | | |
| URL | `localhost` | | |

| Question | Answer |
|---|---|
| Server | Self-hosted on the owner's LAN, Ubuntu. Address, user, and password are in the handover file (§8) — **deliberately not written here, because this repository is public.** |
| Server OS / specs | Ubuntu (version TBC — needs a shell session to confirm) |
| How we deploy (git pull + restart / Docker / CI pipeline) | |
| Domain name | |
| Cloudflare role: DNS only / proxied / Tunnel / Access in front of admin | |
| TLS: Cloudflare origin cert / Let's Encrypt | |
| Who else needs server access | |

### 7.2 Verified infrastructure (checked 2026-11-24)

What was actually tested, not assumed:

| Item | Result |
|---|---|
| Server reachable | Yes — ICMP and TCP/22 both open |
| SSH host key | ED25519, `SHA256:9b3TMT0pLsHF7uuhLHcHvuPilT3l8xDTHlIiKcfZRPc`, recorded in `known_hosts` |
| SSH auth methods offered | `publickey, password` |
| SSH login by assistant | **Not yet working.** Password auth needs an interactive terminal, which an agent session does not have. Pending public-key install — see §8. |
| Database reachable | Yes — TCP/3306 open |
| Database connection | **Working**, verified with a real query |
| MariaDB version | 11.8.6-MariaDB-5ubuntu0.1 (Ubuntu) |
| Server charset / collation | `utf8mb4` / `utf8mb4_uca1400_ai_ci` |
| `resibo` schema | Exists, and is **empty** — no tables |
| Shared host | ⚠️ The same MariaDB instance also hosts unrelated databases for other projects |
| GitHub repository visibility | ⚠️ **Public** — confirmed via the GitHub API |

**Three consequences worth acting on:**

1. **The repository is public.** Everything committed is world-readable, now and
   in history. No infrastructure addresses, usernames, or internal hostnames go
   into it — hence the redaction in the table above. Verified 2026-11-24 that no
   credential value appears in any commit on any branch.
2. **The database is shared with other projects.** Connecting as `root` means a
   bug in Resibo can read or destroy unrelated project data. This turns §8's
   "use a least-privilege app user" from good practice into the actual fix.
3. **The `resibo` schema is empty**, so there is no legacy structure to work
   around — the schema in §4 can be designed freely once the blockers are
   answered.

**Open:** Ubuntu version, JDK available on the server, and whether MariaDB should
stay shared or get a dedicated instance for Resibo.

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
be `dev/app/resibo`. The original `dev/api/resibo` was renamed to `dev/api/resibo`
and deleted locally and on the remote; the commits are unchanged.

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

## 8. Credentials and secrets

> **Rule, no exceptions: no secret value is ever written in this file, in the
> Obsidian vault, in a commit, in a code comment, or pasted into chat.** This
> file records only the *name* of a credential, what it is for, and where its
> value lives.

### How to hand credentials over

1. Put the value in a file **outside the repository** or already git-ignored —
   for example `C:\Users\okarun\.secrets\resibo.env`.
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
- Production values are set on the server as environment variables or Docker
  secrets — decided in §7.
- The database user the application connects as gets **only the grants it needs**.
  It is not `root`.
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
C:\Users\okarun\.secrets\resibo.env     (ACL-restricted to the owner's account)
```

| Credential | Purpose | Where the value lives | Status |
|---|---|---|---|
| GitHub repository access | push, clone, CI | Git Credential Manager (already working) | ✅ working |
| Server SSH account | deploy, administer | `resibo.env` → `RESIBO_SERVER_*` | ⚠️ supplied, **login not yet working** (see below) |
| Database admin user (`root`) | currently the only DB access | `resibo.env` → `RESIBO_DB_*` | ✅ **connection verified** 2026-11-24 |
| Database app user | application connection, least privilege | not created | ❌ **to create — see risk 2 below** |
| Cloudflare account | DNS, tunnel | OAuth, pending browser authorisation | ⚠️ in progress |
| Cloudflare API token | unattended/CI use | not created | ❌ not yet needed |
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

**2. The application must not connect to MariaDB as `root`.** Three reasons
compound here: `root` is authenticated as `root@%` (connectable from any host),
the same instance hosts unrelated project databases, and the password is reused
with the SSH account. Any one of those is tolerable; together, a single SQL
injection in Resibo reaches every other project on that server.

> **Proposed fix:** create a dedicated user granted only what it needs on
> `resibo.*` — no `GRANT OPTION`, no access to other schemas, and not
> connectable from `%`. Keep `root` for migrations and backups only, run by a
> person. I have **not** done this; creating a database user on a shared server
> affects other projects, so it is your call.
>
> **Decision needed:**

**3. The password is reused between the SSH account and the database.** One
compromise becomes two. Recommend different values at rotation time.

**4. The repository is public.** Infrastructure addresses and usernames stay out
of it, including out of this file. If the project would rather keep its planning
private, making the repository private is the simpler fix than redacting
documentation forever.

> **Decision needed:** keep the repository public, or make it private?

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
| Backup frequency, and where backups are stored | |
| Has a restore ever been tested? (It is not a backup until it has.) | |
| How long sales data must be retained | |
| Interface language(s) | |
| Currency, date format, timezone | |
| Receipt paper width | |
| Does the till run on a touchscreen, a keyboard, or both? | |

---

## 11. Delivery phases — my proposal

Reorder or cut as you like. Each phase should end with something you can look at
and judge.

| Phase | Deliverable | Depends on |
|---|---|---|
| 0 | This document filled in; vault notes; git repo initialised | §1, §2 |
| 1 | Module skeleton with boundary tests, MariaDB locally, migrations, login, roles | §5, §6.1 |
| 2 | Product catalog with barcodes and prices | §3 |
| 3 | **Checkout**: ring up, tender, print a receipt | §2.1, §2.3, §2.5 |
| 4 | Shift open/close, cash drawer, X and Z reading | §2.3 |
| 5 | Returns, voids, audit log | §4 |
| 6 | Inventory movements and stock on hand | §3 |
| 7 | Reports from §9 | §9 |
| 8 | Server, domain, deployment, backups | §7, §8 |

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
| 7 | Module boundary enforcement (§6.1) | backend skeleton | 2026-11-24 | open |

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

---

## 14. Document change log

| Date | Version | Change | By |
|---|---|---|---|
| 2026-11-24 | 0.1 | Initial template. Stack and modular-monolith structure recorded as decided; all scope, architecture, and deployment questions open. | Claude |
| 2026-11-24 | 0.2 | Added §7.1 branches: remote URL, `main` is README-only, all work commits directly to `dev/api/resibo`. Push policy still open. | Claude |
| 2026-11-24 | 0.3 | Push policy decided (push every commit) and commit-message convention recorded in §7.1. `CLAUDE.md` added to the repository pointing future sessions here. Vault notes created. | Claude |
| 2026-11-24 | 0.4 | Branch renamed to `dev/api/resibo`. Added §6.1.1 (gainsoclock reference and the layout conflict, as a blocker) and §6.1.2 (adopted conventions). Added §7.2 verified infrastructure: DB connection working, SSH not, repository confirmed public. Rewrote §8 with the handover file location and added §8.1 outstanding security risks. Added `.gitignore`. | Claude |
