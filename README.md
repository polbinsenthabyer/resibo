# Resibo

A point-of-sale application — Spring Boot, React, and MariaDB.

> **Status: planning.** There is no application code in this repository yet.
> Scope, architecture, and the data model are being settled in
> [`resibo-plan.md`](resibo-plan.md) before anything is built. That document is
> the project's source of truth; this README is the summary of it.

---

## What Resibo is

Resibo is a POS system for ringing up sales, taking payment, issuing a receipt,
and reconciling the till at the end of a shift — with the catalog, stock, and
reporting behind it that make those things possible.

The specifics of *which* business it serves, and how far the first version
reaches, are open questions recorded in the plan. They are deliberately not
guessed at here: a POS for a cafe and a POS for a hardware store share a
checkout screen and little else.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot (Java), structured as a **modular monolith** |
| Frontend | React |
| Database | MariaDB |
| Hosting | To be decided — server and Cloudflare-managed domain |

Supporting choices — build tooling, migrations, auth, test frameworks — are
proposed with recommendations in [§5 of the plan](resibo-plan.md#5-tech-stack)
and await sign-off.

## Backend architecture

One deployable Spring Boot application, divided internally into feature modules
with enforced boundaries rather than a layer-first package tree.

```
backend/src/main/java/.../resibo/
├── shared/      money, errors, audit, configuration
└── modules/
    ├── identity/      users, roles, permissions, tokens
    ├── catalog/       products, variants, barcodes, prices, taxes
    ├── pricing/       discounts and promotions
    ├── sales/         ring up, tender, void, return, receipt numbering
    ├── inventory/     stock movements, on-hand, counts
    ├── cashdrawer/    shifts, cash movements, X and Z readings
    ├── customers/     customers and credit accounts
    ├── purchasing/    suppliers and purchase orders
    └── reporting/     read-side queries
```

Each module keeps its entities and repositories private and exposes only an
`api/` package of interfaces and DTOs. Modules do not read each other's tables.
The full rules, and the open questions about how to enforce them, are in
[§6.1 of the plan](resibo-plan.md#61-backend-structure--modular-monolith-decided).

## Repository

- `resibo-plan.md` — the plan and source of truth. Read this first.
- `README.md` — this summary.
- Remote: `https://github.com/polbinsenthabyer/resibo.git`

Application code arrives once the plan's blocking questions are answered.

## Getting started

Not yet applicable — there is nothing to build or run. Setup instructions will
be written alongside the first working skeleton, and will cover the JDK and Node
versions, starting MariaDB, applying migrations, and running the two halves
locally.

## Configuration and secrets

No credential value belongs in this repository, in its history, or in its
documentation. The backend will read every secret from environment variables; a
committed `.env.example` will list the required variable **names** with empty
values. Real values live outside the repository.

See [§8 of the plan](resibo-plan.md#8-credentials-and-secrets) for the full
handling rules and the inventory of credentials the project needs.

## Contributing

Any change that alters behaviour, scope, schema, stack, or deployment updates
`resibo-plan.md` in the same change. The plan is not a historical artifact of
the kickoff — it is the current description of the system, and it is expected to
stay accurate.
