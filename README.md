# Resibo

A point-of-sale application — Spring Boot, React, and MariaDB.

> **Status: planning, with a deployed API that has login and a product table.**
> The backend builds, runs, and is publicly reachable at
> **<https://resibo-api.polbinsenthabyer.uk>**. It has **JWT authentication**
> with two roles, three tables in MariaDB, and an admin-only product catalog.
> That is all it does. There is **no checkout, no sale, no receipt, no payment,
> no shift, no stock movement and no frontend** — so there is no POS yet. Scope,
> the data model, and the architecture are still being settled in
> [`resibo-plan.md`](resibo-plan.md) before the rest is built. That document is
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
| Backend | Spring Boot 4.2.0 on Java 17, built with Maven, packaged as a `jar` |
| Frontend | React — not started |
| Database | MariaDB via Spring Data JPA — three tables: `product`, `user_role`, `user` |
| Auth | Spring Security with a stateless JWT, HS256, 8-hour token |
| Hosting | Docker on a self-hosted Ubuntu server, reverse-proxied by nginx, fronted by a Cloudflare tunnel |

The backend's dependencies: `webmvc`, Actuator, Lombok, DevTools, Spring Data
JPA with the MariaDB driver at `runtime` scope, Spring Security, and `jjwt` at
**0.13.0** — pinned by a `jjwt.version` property, and deliberately not the
0.11.5 the sibling project `gainsoclock` uses, because the builder and parser
API differs between the two. Four test starters go with them. **No migrations
tool is in the build**, which is now a gap rather than an absence: three tables
exist and nothing tracks which environment has them.

The remaining choices — migrations, frontend tooling, test frameworks — are
proposed with recommendations in
[§5 of the plan](resibo-plan.md#5-tech-stack) and await sign-off.

## The database connection

The application opens a HikariCP pool (`resibo-pool`) to the `resibo` schema as
a **dedicated least-privilege account** — not the instance superuser, which that
shared server reserves for migrations and backups run by a person. The account
can do anything inside its own schema and nothing outside it.

Three things about it are deliberate, and two of them are rules:

- **`spring.jpa.hibernate.ddl-auto` is `none`, and must stay `none`.** Hibernate
  never creates, alters, or drops anything; the schema is owned outside the
  application. This is not a preference: if Resibo ends up issuing
  BIR-registered receipts, an ORM silently altering a sales table is exactly the
  class of change that destroys an audit trail.
- **The datasource URL, username, and password have no defaults.** A missing
  configuration fails startup loudly rather than connecting somewhere
  unintended — which matters, because that MariaDB instance also hosts unrelated
  projects.
- `spring.jpa.open-in-view` is `false`, keeping the persistence context out of
  the view layer.

The schema holds three tables — `product`, `user_role` and `user`. Each was
created from a reviewed file in [`sql/`](sql/README.md), applied by hand with
the administrative account. **Nothing tracks which environment has which file**,
and there is no tool for altering an existing table. See
[§5.3](resibo-plan.md#53-the-database-connection-2026-11-24) and
[§5.4](resibo-plan.md#54-authentication-and-security-2026-11-24) of the plan.

One rule for anyone writing a repository: **every query is native SQL**, on the
owner's instruction. That carries consequences — a paginated query needs its own
`countQuery`, a `Sort` must carry column names rather than entity field names,
and `SELECT EXISTS(...)` returns an `Integer` and not a `Boolean`. All three are
written up in [§5.5 of the plan](resibo-plan.md#55-repositories-are-native-sql-2026-11-24).

## Backend architecture

One deployable Spring Boot application. The code sits at the repository root and
is packaged **layer-first** at the base package `com.api.resibo`, matching the
sibling project `gainsoclock` that Resibo takes its conventions from:

```
src/main/java/com/api/resibo/
├── ResiboApplication.java
├── configs/         SecurityConfig, and jwt/ for the filter, util and handlers
├── controllers/     AuthController, HealthController, and admin/ for admin-only
├── dtos/            ResponseDto — the single response envelope — and the rest
├── entities/        Product, User, UserRole
├── repositories/    one Spring Data interface per entity
├── services/        auth/ and product/, each an interface plus an *Impl
├── types/           ResponseType, UserRoleType
└── utilities/       MessageType, ResponseUtility, SecurityUtil, UserUtility
```

Every endpoint returns the same envelope — `responseType`, `code`, `data`,
`message`, `className`, `methodName`, `responseTime` — built through
`ResponseUtility`'s static factories rather than assembled at the call site.

Naming rule: an admin-only controller is `Admin<Name>Controller` and lives in
`controllers/admin/`, so the class name and the `/api/admin/**` path agree.

Eight endpoints exist. The "Who" column is enforced in `SecurityConfig`, not by
annotations on the controllers:

| Endpoint | Who | Purpose |
|---|---|---|
| `GET /api/health/check` | anyone | The application's own health check, returning the standard envelope |
| `GET /actuator/health` | anyone | Spring Boot Actuator, for infrastructure probes |
| `POST /api/auth/signup` | anyone | Register an account — read the warning below first |
| `POST /api/auth/login` | anyone | Exchange username and password for a token |
| `POST /api/admin/products/set` | ADMIN | Create when the payload has no `id`, update when it does |
| `POST /api/admin/products/list` | ADMIN | Filtered, sorted, paginated listing |
| `GET /api/admin/products/get/{productId}` | ADMIN | One product |
| `DELETE /api/admin/products/delete/{productId}` | ADMIN | Delete one product |

The listing is a `POST` despite being a read: its filter and pagination payload
belongs in a request body, and a `GET` body is not reliably carried by proxies,
browsers and HTTP clients.

An earlier decision called for a modular monolith packaged by feature instead.
That structure is **not** what the code is, and the plan now records why, what
the layer-first choice costs, and that it is awaiting the owner's confirmation —
see [§6.1 of the plan](resibo-plan.md#61-backend-structure--layer-first-in-the-code-modular-monolith-deferred).

## Authentication

Stateless JWT. Log in, get a token, send it on every later call:

```sh
# 1. log in
curl -X POST https://resibo-api.polbinsenthabyer.uk/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"...","password":"..."}'

# 2. use the token it returns
curl https://resibo-api.polbinsenthabyer.uk/api/admin/products/get/1 \
  -H 'Authorization: Bearer <token>'
```

Login returns `username`, `role` and `token`. Tokens are
HS256 and last **8 hours** — roughly one shift on a till. There is no refresh
token, no logout and no revocation: a token is valid until it expires.

Who can reach what:

| Path | Who |
|---|---|
| `/api/auth/**`, `/api/health/**`, `/actuator/**`, `/error`, `OPTIONS /**` | anyone |
| `/api/admin/**` | ADMIN |
| `/api/employee/**` | ADMIN or EMPLOYEE |
| **everything else** | **any authenticated user** |

That last row is deliberate: the filter chain ends in
`anyRequest().authenticated()`, so a new controller is protected the moment it
exists and nobody has to remember to add a rule.

Two roles exist, `ADMIN` and `EMPLOYEE`. They are a working minimum, not an
answer to the plan's unanswered question about who uses Resibo.

**Errors.** Only two responses use a real HTTP error status — `401` with code
`E0401` for a missing, malformed or expired token, and `403` with `E0403` for a
valid token whose role is not allowed. Every other error is still `200` with an
error code in the envelope. Login and signup failures use `E0101`–`E0106`;
notably **`E0106` covers unknown username, wrong password and disabled account
alike**, with an identical message, so the endpoint cannot be used to find out
which usernames are real.

**CORS** is open to all origins so frontend work is not blocked, with
`allowCredentials=false` — browsers reject credentials from a `*` origin, and
the token travels in a header rather than a cookie. It needs replacing with
real origins before this API holds live data.

> ⚠️ **Signup is public, and the first account on an empty system becomes
> ADMIN.** After that an anonymous signup is always EMPLOYEE, whatever role it
> asks for, and only an authenticated ADMIN can create another ADMIN. So on a
> fresh environment there is a window in which whoever signs up first takes the
> administrator account. **Create the admin account immediately after applying
> the schema.** This is a known open exposure, not a finished design — see
> [§8.1 of the plan](resibo-plan.md#81-outstanding-security-risks).

The full detail is in
[§5.4 of the plan](resibo-plan.md#54-authentication-and-security-2026-11-24).

## Repository layout

This repository keeps its branches deliberately separate:

| Branch | Contents |
|---|---|
| `main` | This README, and nothing else. |
| `dev/api/resibo` | Backend working branch — `resibo-plan.md`, `CLAUDE.md`, and the application itself. |

The `api` segment distinguishes backend work from frontend work; a frontend
branch would be `dev/app/resibo`.

**The plan and the code are not on `main`.** To get them:

```sh
git switch dev/api/resibo
```

The rest of the application arrives as the plan's blocking questions are
answered.

## Getting started

You need a **JDK 17** and a **MariaDB you are willing to point at** — see the
database note below, because the build's tests need one too. You do not need
Maven: the `mvnw` wrapper is committed and will fetch it.

`JAVA_HOME` must point at that JDK, and it must be set in **every** shell you
build from — it is the one real piece of friction here. Without it the wrapper
stops with *"The java/javac command does not exist in PATH nor is JAVA_HOME set,
so mvnw cannot run."* On the machine this was built on, nothing Java is on
`PATH` at all, and the JDK is the Microsoft Build of OpenJDK 17.0.20 under
`%USERPROFILE%\.jdks\ms-17.0.20`:

```powershell
# PowerShell, per shell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.20"
.\mvnw.cmd -B clean verify
```

```sh
# bash — point JAVA_HOME at your own JDK 17
export JAVA_HOME="$HOME/.jdks/ms-17.0.20"
./mvnw -B clean verify
```

Then run it:

```sh
.\mvnw.cmd spring-boot:run                    # PowerShell
./mvnw spring-boot:run                        # bash
java -jar target/resibo-0.0.1-SNAPSHOT.jar    # or the built jar, either shell
```

It listens on port 8080 unless `SERVER_PORT` says otherwise; the project's own
`.env.dev` sets **8083**, matching the host port the deployed container uses, so
a local URL and a deployed one mean the same thing. These two answer without a
token:

```sh
curl http://localhost:8083/api/health/check     # 8080 if SERVER_PORT is unset
curl http://localhost:8083/actuator/health
```

Everything else needs one — see [Authentication](#authentication).

If you have Docker and would rather not deal with `JAVA_HOME` at all, the same
image that runs in production builds and runs locally — it brings its own JDK:

```sh
docker compose up --build        # then http://localhost:8083/api/health/check
```

Configuration comes from a per-environment env file chosen by `APP_ENV` —
`.env.dev` by default, loaded if it is there and ignored if it is not. Copy the
committed `.env.example`, which carries variable names and no values.

**You do need a MariaDB now.** The datasource properties have no defaults, so
`DATASOURCE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD` must be set — in
`.env.dev` or in the environment — or the application fails to start, on
purpose. That applies to `./mvnw verify` as well as to running it: one test
starts the Spring context, so **the test suite needs a reachable database too**.

**You also need `JWT_SECRET`, and it must be at least 32 characters.** It has no
default either — a fallback signing key in a public repository would be no
secret at all — and `JwtUtil` checks the length at startup, so a short one fails
the boot rather than the first login. `JWT_EXPIRATION` is optional and defaults
to 8 hours in milliseconds. Use a different value locally from the one
production uses, so a token minted here is worthless there.

> Note: the committed `.env.example` does **not** yet list `JWT_SECRET` or
> `JWT_EXPIRATION`, so copying the template alone is not enough to start the
> application. Add them by hand. Tracked in
> [§12 of the plan](resibo-plan.md#12-open-questions).

The tables have to exist before anything but the health endpoints works. Apply
them in order, with the administrative database account — `user_role` first,
because `user` has a foreign key to it:

```sh
mariadb -h <host> -u <admin-user> -p resibo < sql/tables/user_role.sql
mariadb -h <host> -u <admin-user> -p resibo < sql/tables/user.sql
mariadb -h <host> -u <admin-user> -p resibo < sql/tables/product.sql
```

Then create the admin account straight away, before anyone else can: the first
signup on an empty `user` table becomes ADMIN. See
[Authentication](#authentication).

A caution that belongs here rather than in the plan's small print: the
`.env.dev` used while this was built points at the **shared server instance**,
which is the same database the deployed application uses. CI runs against a
throwaway MariaDB service container instead. Pointing local work at a local
MariaDB — or at Testcontainers — is the open item in
[§12 of the plan](resibo-plan.md#12-open-questions); until it is closed, know
what your `DATASOURCE_URL` is aimed at before you run a test that writes.

The application never creates a table: `ddl-auto` is `none` and there is no
migration tool. The files in [`sql/`](sql/README.md) are the authority for what
the schema should be, and applying them is a deliberate manual step.

## Deployment

The application runs on a self-hosted Ubuntu server as a Docker container,
reverse-proxied by the host's nginx and published through a Cloudflare tunnel.

| | |
|---|---|
| Public URL | <https://resibo-api.polbinsenthabyer.uk> |
| What it serves | The eight endpoints above — health, auth, and admin-only products. **Nothing else** |
| Runtime | Docker container, `restart: unless-stopped`, published on the host's loopback interface only |
| TLS | Terminated by Cloudflare. The tunnel carries the request to the origin, so the origin holds no certificate and listens on plain HTTP on loopback. |

```sh
curl https://resibo-api.polbinsenthabyer.uk/api/health/check
```

> **"Deployed" does not mean "working POS".** The live application can
> authenticate a user and manage a list of products. It cannot ring up a sale,
> take a payment, print a receipt, open a shift or move stock, and there is no
> frontend. Every question about what Resibo actually does is still open in the
> plan.

Deploying is manual, from a git checkout on the server:

```sh
cd /opt/resibo/app
git pull
sudo -E bash scripts/deploy.sh
```

A GitHub Actions workflow, `.github/workflows/ci.yml`, builds and tests every
push to the working branch against a throwaway MariaDB service container — the
real database sits on a private network a hosted runner cannot reach. It is the
gate, not the artifact producer: the server still builds its own image.

`scripts/deploy.sh` builds the image on the server using the committed `mvnw`,
so the server needs only Docker — no JDK, no Maven. It then tags the running
image as `:previous`, starts the new one, and polls `/actuator/health`. If the
health check never passes, it retags `:previous`, restarts, and exits non-zero:
a bad build does not leave a broken service behind.

The Dockerfile is covered in
[§5.2 of the plan](resibo-plan.md#52-the-container-runtime-2026-11-24); the
on-server layout, the rollback path, the nginx vhost, the tunnel ingress and the
server's port allocation are in
[§7.3](resibo-plan.md#73-deployment-as-it-actually-works-2026-11-24). **Read
§7.3 before touching the Cloudflare tunnel** — its configuration endpoint
replaces the entire ingress list rather than patching it, and unrelated services
share that tunnel.

## Configuration and secrets

No credential value belongs in this repository, in its history, or in its
documentation. The backend reads its configuration from environment variables,
and the committed `.env.example` lists the variable **names** with empty values.
Every other `.env.*` file is git-ignored, and `.dockerignore` keeps all of them
out of the container image under any name. Real values live outside the
repository — on the server, in a `0600` env file outside the checkout.

The database credential that file holds belongs to a **least-privilege account
scoped to the `resibo` schema**, not to the instance superuser, so a compromise
of the application cannot reach the unrelated projects that share the server.

The same file holds `JWT_SECRET`, the key the API signs its tokens with. It is
referred to by name only, here and in the plan. **Local and production use
different values on purpose**, so a token minted on a developer's machine cannot
be used against production. Rotating it invalidates every token in circulation,
which is the intended behaviour.

One apparent exception, which is not one: the MariaDB credentials in the CI
workflow are in plain sight deliberately. They belong to a container created for
a single job and destroyed with it, so there is nothing there to protect — and
a list of "secrets" containing things that are not secret is a list nobody
reads carefully.

See [§8 of the plan](resibo-plan.md#8-credentials-and-secrets) for the full
handling rules and the inventory of credentials the project needs.

## Contributing

Any change that alters behaviour, scope, schema, stack, or deployment updates
`resibo-plan.md` in the same change. The plan is not a historical artifact of
the kickoff — it is the current description of the system, and it is expected to
stay accurate.
