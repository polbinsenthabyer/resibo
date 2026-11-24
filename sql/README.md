# SQL

The database schema lives here, **one file per table**, and this directory is
the authority for what the schema should be.

```
sql/
└── tables/
    └── product.sql
```

## Why the schema is here and not in the application

Hibernate runs with `ddl-auto: none` — see `src/main/resources/application.yaml`.
The application never creates, alters or drops a table. That is deliberate and
is recorded with its reasoning in §5.3 of [`resibo-plan.md`](../resibo-plan.md):
if Resibo ends up issuing BIR-registered receipts, an ORM quietly altering a
sales table on startup is exactly the kind of change that destroys an audit
trail, and it leaves no migration, no review and no record of what it did.

So schema changes are explicit, reviewable files — not a side effect of
deploying.

## Conventions

- **One file per table**, named after the table: `tables/<table_name>.sql`.
- Each file is **idempotent** — `CREATE TABLE IF NOT EXISTS`, so re-running it
  is safe.
- Column order in the file matches column order in the table.
- `ENGINE = InnoDB`, `utf8mb4` / **`utf8mb4_general_ci`** on **every** table.
  State it explicitly in each file: this is deliberately *not* the MariaDB 11.8
  server default (`utf8mb4_uca1400_ai_ci`), so a table that omits it will be
  created with the wrong collation and compare differently from its neighbours.
- Audit columns on every table: `created_at`, `created_by`, `updated_at`,
  `updated_by`.
- If a JPA entity and its file disagree, **the file wins** and the entity is
  the bug.

## Applying a file

Use the **administrative** database account, not the application's. The
`resibo` account could run DDL inside its own schema, but schema changes are a
deliberate, human-run operation and keeping them off the application account
means a bug in the app can never reshape the database.

```sh
mariadb -h <host> -u <admin-user> -p resibo < sql/tables/product.sql
```

## Known gap

There is **no migration tool**, so nothing tracks which files have been applied
to which environment, and nothing handles altering an existing table — these
files only describe the desired end state of a fresh table. Flyway is proposed
in §5 of the plan and has not been adopted. Until it is, applying a change is a
manual step that has to be remembered for every environment, and `ALTER`
statements have to be written and run by hand.
