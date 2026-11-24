# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read the plan first

**`resibo-plan.md` is this project's source of truth.** Read it in full before
answering questions, planning, or writing code — every session, not just the
first. It records the product intent, the scope decisions and their rationale,
the domain model, the architecture, and the deployment and secrets rules. The
code follows it.

This is the owner's standing instruction (2026-11-24): future sessions are to
work from that document rather than re-deriving intent from source files.

Supporting context lives in the Obsidian vault at
`Projects/personal/resibo/` (hub, `Decisions`, `Changelog`, `Tasks`) — read it
per the user-level protocol. The vault indexes and narrates; `resibo-plan.md`
specifies. Where they disagree about the specification, the plan wins.

## Keep the plan current

**Any change to behaviour, scope, schema, stack, or deployment updates
`resibo-plan.md` in the same turn as the code** — not at session end, and not
"later".

- A decision made or an approach rejected → append to §13 with its **rationale**.
- Any edit to the document itself → append to §14 and bump the version in §0.
- Anything the README states that the plan has changed → update `README.md` too,
  on `main` (see branches below).

A plan that has quietly gone stale is worse than no plan, because the next
session will trust it.

## Do not guess past a blocker

Items tagged `[BLOCKER]` in the plan are unanswered questions where guessing
wrong means rewriting code. The open ones are listed in §12. **Ask the owner;
do not invent an answer**, and do not quietly pick the easy option.

Two of them govern the whole design:

- **§2.1 — offline behaviour.** Online-only, offline-tolerant, and offline-first
  are three different applications. Retrofitting the third onto the first is
  close to a rewrite.
- **§2.3 — official-receipt compliance.** If Resibo issues BIR-registered
  receipts, then append-only sales, never-reset sequential numbering, Z-readings,
  and statutory senior/PWD discount handling are structural requirements. They
  cannot be added afterwards. The plan's description of them is explicitly
  unverified — it needs confirming with the owner's bookkeeper, not treating as
  settled.

## Branches

| Branch | Contents |
|---|---|
| `main` | `README.md` only. Never application code. |
| `dev/api/resibo` | Everything else. **All work commits directly here.** |

Commit straight to `dev/api/resibo` — no feature branches, no pull requests — and
push every commit. Touch `main` only to update the README. Keep commit messages
brief and precise; Conventional Commits, imperative mood.

## Architecture

The backend lives at the **repository root** — `pom.xml` and `src/`, not under
`backend/` — and is packaged **layer-first** at the base package
`com.api.resibo`: `controllers/`, `dtos/`, `types/`, `utilities/`, mirroring the
sibling project gainsoclock. `entities/`, `repositories/` and `services/` join
them, flat, when a feature needs them. There is no `modules/` package and no
`shared/` package.

That is plan §6.1.1 **option A**, and it **supersedes** the earlier
modular-monolith decision. Anything describing feature modules with enforced
boundaries — an older copy of this file, or the plan before document version
0.5 — is describing what was intended, not what exists. Nothing enforces module
boundaries, because there are no modules.

**The owner has not confirmed it** (plan §12 #9). Do not quietly entrench it: if
you are about to write the first domain feature, that is the moment to ask.
Reversing six classes takes minutes; reversing a built-out backend does not.

Conventions that do hold, and that new code must follow (plan §6.1.2):

- Every controller and service returns `ResponseDto`, built through
  `ResponseUtility.getSuccessResponse(message[, data])` or
  `getErrorResponse(code, message)` — never assembled by hand.
- Enums live in `types/`, suffixed `*Type`. Stateless helpers live in
  `utilities/`.
- Response messages are constants in `MessageType`, not strings at call sites.
- Services are an interface plus an `XxxServiceImpl`, never a bare class.
- Lombok `@Data` / `@NoArgsConstructor` / `@AllArgsConstructor` on DTOs.

## Database

The application connects to MariaDB (plan §5.3) through Spring Data JPA, as a
**dedicated least-privilege account scoped to the `resibo` schema** — never the
instance superuser, which is reserved for migrations and backups run by a
person. **There are no entities, no repositories, no tables and no migrations.**
Connected is not the same as persistent; do not describe or report it as
persistence.

> ⚠️ **`spring.jpa.hibernate.ddl-auto` is `none` and must never be changed.**
> Not to `update`, not to `create`, not to `create-drop`, not "just locally",
> not to make a test pass. The schema is owned outside the application, and if
> Resibo issues BIR-registered receipts (plan §2.3), an ORM silently altering a
> sales table is exactly the class of change that destroys an audit trail.
> `update` is the dangerous one because it looks helpful. If a schema change is
> needed, it is written as reviewable SQL and run deliberately — see plan §12
> #15, which is the open question about which tool owns that.

Two more settings that are decisions, not defaults, and should not be
"tidied up": `spring.datasource.url` / `username` / `password` have **no
defaults**, so a missing configuration fails startup loudly instead of
connecting somewhere unintended — do not add fallbacks to make something start.
And `spring.jpa.open-in-view` is `false`; leave it off.

Where the values come from: `DATASOURCE_URL`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD`, plus `SERVER_PORT` and `DB_POOL_MAX` — names matching
gainsoclock's `.env`. `.env.example` carries names only; `.env.dev` is
git-ignored and now holds a **real password**; the server reads
`/opt/resibo/resibo.env`.

**Before running tests, know what the datasource points at.** One of the two
tests starts the Spring context, so `mvnw verify` needs a database — and the
`.env.dev` in this working copy points at the **shared server instance**, the
same one production uses. CI is isolated by a throwaway MariaDB service
container; a local run is not (plan §12 #13, §8.1 risk 5). Nothing writes today
because there are no entities. Do not be the session that changes that by
accident.

## Secrets

**No credential value goes into this repository, its history, its documentation,
the vault, or a chat message.** Plan §8 holds an inventory of credential *names*
and where each value lives.

Secrets are supplied as a **path** to a git-ignored file outside the repository.
Read it when a task needs it; never echo it back, never commit it, never record
it in a note. The backend reads secrets from environment variables only — a
committed `.env.example` lists the names with empty values.

## Deployment

The skeleton is deployed and publicly reachable. **It serves
`/api/health/check` and `/actuator/health`, connects to an empty database, and
implements no part of a POS** — never describe it as more than that, to the
owner or in a note. Plan §7.3 is the full procedure, §5.2 covers the image and
§5.3 the database connection; this is the short version, so a session does not
improvise one.

| | |
|---|---|
| Public URL | `https://resibo-api.polbinsenthabyer.uk` |
| Runtime | Docker container `resibo-api` on the shared Ubuntu server, published on `127.0.0.1:8083` **only** — never on the LAN |
| In front of it | the host's nginx vhost, matched by `server_name` → the existing `personal-projects` Cloudflare tunnel |
| Checkout on the server | `/opt/resibo/app`, on `dev/api/resibo` |
| Env file | `/opt/resibo/resibo.env`, mode `0600`, outside the checkout and not in git — holds `APP_ENV=prod` **and the datasource credentials**; the application will not start without it |

**To deploy** — manual, on the server. `.github/workflows/ci.yml` exists and its
`build` job is the test gate (it runs `mvnw verify` against a throwaway MariaDB
service container); it also declares a `deploy` job on a self-hosted runner,
which is **not documented in plan §7.3 and not the path to follow** until plan
§12 #16 is answered. The manual procedure is:

```sh
cd /opt/resibo/app
git pull
sudo -E bash scripts/deploy.sh
```

The script builds `resibo-api:next`, tags the running `:current` as
`:previous`, promotes `:next` to `:current`, restarts via Compose, then polls
`/actuator/health` up to 30 times at 2-second intervals. On failure it prints
the container log, retags `:previous` back, restarts, and exits non-zero. It
refuses to run at all if the env file named by `RESIBO_ENV_FILE` is missing.
**Do not hand-roll a deployment around it** — if it needs to do something
different, change the script and record why in plan §13.

**To roll back** after the fact (the script only rolls back its own run):

```sh
cd /opt/resibo/app
docker tag resibo-api:previous resibo-api:current
RESIBO_TAG=current docker compose up -d --no-build
```

**Host ports on that server:** 8081 gainsoclock, 8082 gainsoclock prelive,
**8083 resibo**, 8088 eloms — all loopback-bound. The container always listens
on **8080 internally**, whatever the host port is. The local `.env.dev` sets
`SERVER_PORT=8083` to match, so a local URL and a deployed one mean the same
thing; it used to set 8088, which is **eloms'** port, and that value should
never appear in a Resibo env file.

> ⚠️ **Cloudflare tunnel configuration: append, never overwrite.** The tunnel
> configuration endpoint takes the **entire** ingress list and replaces it — it
> does not patch. A `PUT` carrying only Resibo's rule deletes the rules for
> gainsoclock, its prelive and eloms; the tunnel stays healthy and the
> neighbours silently return 404, with no error anywhere to tell you. So
> always: read the current configuration, **back it up**, append your rule
> **before** the `http_status:404` catch-all, PUT the complete list, and
> re-check the neighbouring hostnames afterwards.

Cloudflare access is a scoped API token in the handover file (Account →
Cloudflare Tunnel:Edit, Zone → DNS:Edit, Zone → Zone:Read). Per the Secrets
rules above: use it, never echo it, never commit it. The same goes for the
server's address and account — they belong in the handover file, not in this
repository.

## Current state

A skeleton exists, is verified, and is **deployed** (see Deployment above):
Spring Boot 4.2.0 on Java 17, Maven, the `ResponseDto` conventions,
`GET /api/health/check`, `/actuator/health`, and two passing tests. Since
2026-11-24 it also **connects to MariaDB** — JPA starter, MariaDB driver at
`runtime` scope, Hikari pool, a least-privilege `resibo` account, and
`ddl-auto: none` (see Database above). Verified: build green, `/actuator/health`
reports `db: UP`, **zero DDL issued and zero tables in the schema**, and
production runs its 10 pool connections as `resibo`, not as the superuser.

There is still **no schema, no entity, no repository, no migration, no
authentication and no frontend.** Plan §5.1 records what was generated, §5.3 the
database connection and what was proven about it, §5.2 and §7.3 how it builds
into a container and how it reaches the internet.

Being deployed and being connected both unblock nothing about the product.
Every `[BLOCKER]` is as open as it was before, and "it connects to the database"
must never be relayed as "it has persistence".

**No JDK is on `PATH` and `JAVA_HOME` is unset. Maven is not installed.** Set
`JAVA_HOME` in every shell before building, and use the committed wrapper —
**and note that both commands now need a reachable database**, because the
datasource has no defaults and one test starts the Spring context:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.20"   # Microsoft OpenJDK 17.0.20
.\mvnw.cmd -B clean verify     # compile + tests; needs DATASOURCE_URL et al.
.\mvnw.cmd spring-boot:run     # run on :8083 per .env.dev, or SERVER_PORT
```

`docker compose up --build` is the alternative that needs no `JAVA_HOME` at all
— the build stage carries its own JDK — and serves on `:8083` locally. It is
the same image that runs in production.

Everything past the skeleton is still blocked. Generating it was the owner
lifting the scaffolding hold for one step, not for the project — do not start on
the schema, auth, or checkout without answers to §1, §2 and §6.1.1. The database
connection was a second such step and deliberately as narrow: a connection pool
and a forbidden `ddl-auto` are identical whatever the §2 answers turn out to be,
which is exactly why it could be built now. **An entity is not**, so do not
write one.

Two known gaps, so they are not rediscovered as surprises. Both are now visible
on the public internet, which raises the stakes without changing the answer:

- `responseTime` serialises in **UTC**, not local time — the production response
  reads `03:57` where local time is `11:57`. **Deliberately not fixed:** the
  project's timezone is an open owner question (plan §10, §12 #8). Do not "fix"
  it by guessing a zone; answer the question first.
- `className` and `methodName` are returned on every response, errors included.
  §6.1.2 required them suppressed in production error responses **before**
  anything was publicly exposed, and that moment has now passed (plan §12 #10).
  Raise it with the owner; do not quietly leave it, and do not quietly change
  the envelope either.
