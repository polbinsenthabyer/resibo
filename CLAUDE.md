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
| `dev/api/resibo` | The backend, this file, and the plan. **Backend work commits directly here.** |
| `dev/app/resibo` | The React client at `src/main/client`, on top of a full copy of the backend. **Frontend work commits directly here.** Cut from `dev/api/resibo` at `e125c6e`, 2026-11-24. |

Commit straight to the branch for the half you are changing — no feature
branches, no pull requests — and push every commit. Touch `main` only to update
the README. Keep commit messages brief and precise; Conventional Commits,
imperative mood.

> ⚠️ **This file and `resibo-plan.md` exist on both development branches, and
> nothing keeps the copies in step.** It has already caused one rework: the
> frontend branch was cut at `4883414`, two documentation commits landed on
> `dev/api/resibo` while the client was being written, and the frontend's plan
> section had to be renumbered before it could be committed. **Fetch and rebase
> `dev/app/resibo` onto `dev/api/resibo` before every frontend commit**, and if
> you change either document, say so in the commit message. Plan §12 #23.

## The frontend

`src/main/client` on `dev/app/resibo` — Vite, React, TypeScript, plain SCSS.
Conventions are in `src/main/client/README.md`; the write-up with its reasoning
is plan §5.6.

Two things about the location that must stay true:

- **Maven ignores it.** Only `src/main/java` and `src/main/resources` are build
  inputs, so `src/main/client` is invisible to `./mvnw`. Verified by compiling.
- **`.dockerignore` excludes `src/main/client/`. Do not remove that rule.** The
  `Dockerfile` does `COPY src/ src/`, so without it every backend build drags
  `node_modules` into the build context and busts its dependency cache layer.

The client has **one** place that calls `fetch` — `src/api/http.ts` — because
this API answers a failed request with HTTP 200 and an error code in the
envelope, and only uses a real status for 401 and 403. It unwraps `data` or
throws `ApiError`. **Do not call `fetch` from a component**, and do not add a
second HTTP path.

Nothing builds or deploys the client: no CI step, no vhost, no hostname
(plan §12 #22). There is no login screen, no formatting helper for money or
dates — §10 is still blank — and no test runner.

## Architecture

The backend lives at the **repository root** — `pom.xml` and `src/`, not under
`backend/` — and is packaged **layer-first** at the base package
`com.api.resibo`, mirroring the sibling project gainsoclock:

```
configs/        SecurityConfig, and jwt/ for the filter, util and two handlers
controllers/    AuthController, HealthController, and admin/ for admin-only ones
dtos/           flat: ResponseDto, PageDto, LoginDto, SignupDto, ProductDto...
entities/       Product, User, UserRole
repositories/   one Spring Data interface per entity
services/       auth/ and product/, each an interface plus an *Impl
types/          ResponseType, UserRoleType
utilities/      MessageType, ResponseUtility, SecurityUtil, UserUtility
```

There is no `modules/` package and no `shared/` package.

That is plan §6.1.1 **option A**, and it **supersedes** the earlier
modular-monolith decision. Anything describing feature modules with enforced
boundaries — an older copy of this file, or the plan before document version
0.5 — is describing what was intended, not what exists. Nothing enforces module
boundaries, because there are no modules.

**The owner has not confirmed it** (plan §12 #9), and the cheap window has
closed: it was six classes when the question was raised and is now around
thirty. Raise it, but do not expect the reversal to be free any more.

Conventions that do hold, and that new code must follow (plan §6.1.2):

- Every controller and service returns `ResponseDto`, built through
  `ResponseUtility.getSuccessResponse(message[, data])` or
  `getErrorResponse(code, message)` — never assembled by hand.
- Enums live in `types/`, suffixed `*Type`. Stateless helpers live in
  `utilities/`.
- Response messages are constants in `MessageType`, not strings at call sites.
- Services are an interface plus an `XxxServiceImpl`, never a bare class.
- Lombok `@Data` / `@NoArgsConstructor` / `@AllArgsConstructor` on DTOs.
- **An admin-only controller is `Admin<Name>Controller` in
  `controllers/admin/`.** The class name and the `/api/admin/**` path then say
  the same thing, and one `SecurityConfig` rule covers the whole area — no
  per-controller annotation to forget.

**Boot 4 uses Jackson 3.** `ObjectMapper` is
`tools.jackson.databind.ObjectMapper`, **not** `com.fasterxml.jackson.databind`.
The annotations did not move: `@JsonFormat` and friends are still
`com.fasterxml.jackson.annotation`. So a file can legitimately import from both
packages, which is why this is easy to get wrong.

## Repositories: native SQL only

**Every repository query is native SQL** — `@Query(..., nativeQuery = true)` —
on the owner's instruction. No JPQL, no derived method names. Three rules follow
from that, and all three have already cost a bug (plan §5.5):

1. **A paginated native query needs its own `countQuery`.** Spring Data can
   derive a count from JPQL but not from SQL.
2. **A `Sort` in a `Pageable` must carry column names, not entity field
   names.** The string is appended to `ORDER BY` verbatim, so
   `Sort.by("productName")` produces `ORDER BY productName` and the column is
   `product_name`. `ProductServiceImpl` keeps a `SORT_COLUMNS` map and
   translates; it doubles as the sort allow-list, falling back to `id` on
   anything unrecognised. Any new sortable listing needs the same.
3. **A native `SELECT EXISTS(...)` returns `Integer`, not `Boolean`.** Declaring
   the method `boolean` throws
   `ClassCastException: Integer cannot be cast to Boolean` at runtime, and it
   surfaces as a confusing 401 rather than an obvious error. Return `Integer`
   and convert in a `default` method, as `UserRepository` does. Expect this on
   the next `EXISTS` or `COUNT`.

Columns are compared with a plain `=` rather than `LOWER(...)`: under
`utf8mb4_general_ci` that is already case-insensitive **and** still uses the
unique index. Everything is a bound parameter — never concatenate a value into
a query string.

## Authentication

Stateless JWT (plan §5.4). HS256, 8-hour token, no session and no cookie. The
token arrives as `Authorization: Bearer <token>`.

| Path | Who |
|---|---|
| `/api/auth/**`, `/api/health/**`, `/actuator/**`, `/error`, `OPTIONS /**` | anyone |
| `/api/admin/**` | ADMIN |
| `/api/employee/**` | ADMIN or EMPLOYEE |
| **everything else** | **any authenticated user** |

The chain ends in `anyRequest().authenticated()`, so **a new controller is
protected the moment it exists.** Do not add a `permitAll` rule to make
something work without understanding why it is being challenged.

> ⚠️ **`/error` must stay in the permitAll list.** Spring forwards unhandled
> exceptions there. With it protected, **every 500 comes back as a 401** and the
> real failure is invisible. A 401 where you expected a 500 is this bug.

`401`/`E0401` and `403`/`E0403` are the only responses that use a real HTTP
error status; **every business error is still 200 with a code**, per the
envelope convention. Both are written by `JwtAuthenticationEntryPoint` and
`JwtAccessDeniedHandler` and still carry the normal `ResponseDto`.

`JwtFilter` rejects nothing — it reads the token, sets the security context if
the token is good, and lets `SecurityConfig` decide what an anonymous request
means. Keep it that way: every access rule belongs in the one file.

Other things not to rediscover:

- **`jwt.secret` has no default and must be at least 32 characters.** `JwtUtil`
  checks at boot, so a short or missing secret fails startup rather than the
  first login. Do not add a fallback.
- **jjwt is 0.13.0, gainsoclock is on 0.11.5.** The builder and parser API
  differs, so **do not copy JWT code between the two projects.**
- Two roles, `ADMIN` and `EMPLOYEE`, in the `user_role` table. That is a
  placeholder for plan §1.3, which is still open — do not treat it as the
  permission model.
- Roles are stored without the `ROLE_` prefix; `JwtFilter` adds it when building
  the authority, because `hasRole("ADMIN")` looks for `ROLE_ADMIN`.
- **`E0106` covers unknown username, wrong password and disabled account
  alike**, with an identical message, so usernames cannot be enumerated. Do not
  "improve" the error by splitting it.
- **Signup is public and the first account on an empty system becomes ADMIN.**
  After that an anonymous signup is always EMPLOYEE whatever it asks for. This
  is a known open exposure (plan §8.1 risk 6, §12 #17), not a finished design —
  and on a new environment the admin account must be created immediately.
- `created_by` / `updated_by` record the signed-in username via
  `SecurityUtil.currentUsername()`, which falls back to `manual` only when
  nobody is authenticated.
- **No refresh token, no logout, no revocation.** A token is good for its full
  8 hours; disabling an account stops the next login, not the current token.
- CORS is open to all origins with `allowCredentials=false`, deliberately, so
  frontend work is not blocked. It needs real origins before live data.

**The application admin account is `polbinsent` (ADMIN).** Its password was
supplied in chat, so it is exposed and should be changed. **Never write that
password anywhere** — not in this file, the plan, the vault, a commit, or a chat
message.

## Database

The application connects to MariaDB (plan §5.3) through Spring Data JPA, as a
**dedicated least-privilege account scoped to the `resibo` schema** — never the
instance superuser, which is reserved for migrations and backups run by a
person.

**Three tables exist**, each as a reviewed file in `sql/tables/` applied by hand
with the administrative account: `product`, `user_role`, `user`. **There is
still no migration tool**, so nothing records which environment has which file
and nothing handles altering a table (plan §12 #15).

> **Apply `user_role.sql` before `user.sql`** — `user` has a foreign key to it.
> `user_role.sql` seeds its ADMIN and EMPLOYEE rows with `INSERT IGNORE`, so
> re-running it is safe. Without those rows signup fails with `E9999`.

Every table is `utf8mb4` / **`utf8mb4_general_ci`**, stated explicitly because
it is *not* the server default. See `sql/README.md`; if a file and an entity
disagree, the file wins.

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
`DATABASE_PASSWORD`, plus `SERVER_PORT`, `DB_POOL_MAX`, `JWT_SECRET` and
`JWT_EXPIRATION` — the first five matching gainsoclock's `.env`. `.env.dev` is
git-ignored and holds a **real password and a real signing key**; the server
reads `/opt/resibo/resibo.env`.

`.env.example` carries names only — and **it is missing `JWT_SECRET` and
`JWT_EXPIRATION`**, so copying it is not enough to start the application. The CI
`build` job has the same hole: it sets the datasource keys and no signing key,
while `ResiboApplicationTests` starts the full context. Both are one line each,
tracked as plan §12 #20, and neither has been changed.

> ⚠️ **Before running tests, know what the datasource points at.**
> `ResiboApplicationTests` starts the Spring context, so `mvnw verify` needs a
> database — and the `.env.dev` in this working copy points at the **shared
> server instance**, the same schema production uses. CI is isolated by a
> throwaway MariaDB service container; a local run is not (plan §12 #13, §8.1
> risk 5). **"Nothing writes because there are no entities" no longer applies**
> — there are three entities, three tables and real rows, including the only
> admin account. Do not be the session that discovers this the hard way.

## Secrets

**No credential value goes into this repository, its history, its documentation,
the vault, or a chat message.** Plan §8 holds an inventory of credential *names*
and where each value lives.

Secrets are supplied as a **path** to a git-ignored file outside the repository.
Read it when a task needs it; never echo it back, never commit it, never record
it in a note. The backend reads secrets from environment variables only — a
committed `.env.example` lists the names with empty values.

That now includes **`JWT_SECRET`**, the key the API signs tokens with. Record it
by name and location only. **Local and production hold different values on
purpose**, so a token minted locally is worthless against production; rotating
it invalidates every token in circulation, which is intended. It also includes
the password for the `polbinsent` ADMIN account, which must never be written
down.

## Deployment

The API is deployed and publicly reachable. **It serves health, actuator,
signup, login and four admin-only product endpoints, and implements no part of
a checkout** — never describe it as more than that, to the owner or in a note.
Plan §7.3 is the full procedure, §5.2 covers the image, §5.3 the database
connection and §5.4 authentication; this is the short version, so a session does
not improvise one.

| | |
|---|---|
| Public URL | `https://resibo-api.polbinsenthabyer.uk` |
| Runtime | Docker container `resibo-api` on the shared Ubuntu server, published on `127.0.0.1:8083` **only** — never on the LAN |
| In front of it | the host's nginx vhost, matched by `server_name` → the existing `personal-projects` Cloudflare tunnel |
| Checkout on the server | `/opt/resibo/app`, on `dev/api/resibo` |
| Env file | `/opt/resibo/resibo.env`, mode `0600`, outside the checkout and not in git — holds `APP_ENV=prod`, **the datasource credentials** and **`JWT_SECRET`**; the application will not start without it |

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

Spring Boot 4.2.0 on Java 17, Maven, the `ResponseDto` conventions, **deployed**
(see Deployment above), **connected to MariaDB** as a least-privilege account
with `ddl-auto: none` (see Database above), and since 2026-11-24 **authenticated
with JWT** and serving an admin-only product catalog. **22 tests, build green.**
Eight endpoints exist:

| Endpoint | Who |
|---|---|
| `GET /api/health/check`, `GET /actuator/health` | anyone |
| `POST /api/auth/signup`, `POST /api/auth/login` | anyone |
| `POST /api/admin/products/set` | ADMIN |
| `POST /api/admin/products/list` | ADMIN |
| `GET /api/admin/products/get/{productId}` | ADMIN |
| `DELETE /api/admin/products/delete/{productId}` | ADMIN |

Three tables: `product`, `user_role`, `user`. One account: `polbinsent`, ADMIN.
Test accounts were removed.

Verified on the live public API (plan §5.4): the first signup became ADMIN, a
second requesting ADMIN became EMPLOYEE, duplicate usernames were caught
case-insensitively, a short password gave `E0103`, an invalid role `E0105`,
login returned the role and an 8-hour expiry, both a wrong password and an
unknown user gave `E0106`, no token and a garbage token both gave 401, an
EMPLOYEE token gave 403 and an ADMIN token 200, a CORS preflight from an
arbitrary origin returned the right headers, and the stored password is a BCrypt
hash.

There is still **no checkout, no sale, no receipt, no payment, no shift, no
stock movement, no migration tool and no frontend.** Plan §5.1 records the
scaffold and the API surface, §5.3 the database connection, §5.4 authentication,
§5.5 the native-SQL repository rules, §5.2 and §7.3 the container and how it
reaches the internet.

**None of this unblocks the product.** Every `[BLOCKER]` is as open as it was
before. A login and a product list are not a POS, and must not be relayed as
one.

**No JDK is on `PATH` and `JAVA_HOME` is unset. Maven is not installed.** Set
`JAVA_HOME` in every shell before building, and use the committed wrapper —
**and note that both commands need a reachable database and a `JWT_SECRET` of at
least 32 characters**, because neither has a default and one test starts the
Spring context:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.20"   # Microsoft OpenJDK 17.0.20
.\mvnw.cmd -B clean verify     # compile + tests; needs DATASOURCE_URL, JWT_SECRET et al.
.\mvnw.cmd spring-boot:run     # run on :8083 per .env.dev, or SERVER_PORT
```

`docker compose up --build` is the alternative that needs no `JAVA_HOME` at all
— the build stage carries its own JDK — and serves on `:8083` locally. It is
the same image that runs in production.

The frontend is a separate toolchain and needs no `JAVA_HOME`. Node 26 and npm
are on `PATH`:

```powershell
cd src\main\client
npm install
npm run dev        # :5173, proxies /api to :8083 — so run the backend too
npm run build      # tsc -b, then vite build into dist/
npm run lint       # oxlint, not ESLint
```

**The checkout is still blocked.** The scaffold, the database connection, the
product table and authentication were each the owner lifting their own hold for
one step — not for the project. Do not start on sales, receipts, payments,
shifts or stock without answers to §1, §2 and §6.1.1: those are the parts the
§2 answers can invalidate, and a sales table built on a guess is a rewrite.

Three known gaps, so they are not rediscovered as surprises. All are visible on
the public internet, which raises the stakes without changing the answer:

- **Signup is public and the first account on an empty system becomes ADMIN**
  (plan §8.1 risk 6, §12 #17). Closed for production because an admin exists;
  reopens for every new environment.

- `responseTime` serialises in **UTC**, not local time — the production response
  reads `03:57` where local time is `11:57`. **Deliberately not fixed:** the
  project's timezone is an open owner question (plan §10, §12 #8). Do not "fix"
  it by guessing a zone; answer the question first.
- `className` and `methodName` are returned on every response, errors included.
  §6.1.2 required them suppressed in production error responses **before**
  anything was publicly exposed, and that moment has now passed (plan §12 #10).
  Raise it with the owner; do not quietly leave it, and do not quietly change
  the envelope either.
