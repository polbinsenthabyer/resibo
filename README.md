# Resibo

A point-of-sale application — Spring Boot, React, and MariaDB.

> **Status: planning, with a deployed skeleton.** The backend builds, runs,
> answers a health check, and connects to MariaDB — and that is all it does. It
> is deployed and publicly reachable at
> **<https://resibo-api.polbinsenthabyer.uk>**, which proves the road to
> production works and nothing about the product. The database connection is
> likewise plumbing, not persistence: there are **no entities, no repositories,
> no schema and no migrations**, so the application connects to an empty
> database and reads and writes nothing. There is no authentication and no
> frontend. Scope, the data model, and the architecture are still being settled
> in [`resibo-plan.md`](resibo-plan.md) before the rest is built. That document
> is the project's source of truth; this README is the summary of it.

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
| Database | MariaDB — connected via Spring Data JPA, with **no schema and no entities yet** |
| Hosting | Docker on a self-hosted Ubuntu server, reverse-proxied by nginx, fronted by a Cloudflare tunnel |

The backend's dependencies today are deliberately few: `webmvc`, Actuator,
Lombok, DevTools, the matching test starters, and — since the database
integration — Spring Data JPA with the MariaDB driver at `runtime` scope. No
migrations tool and no security starter are in the build yet, because the
decisions they would encode are still open.

The remaining choices — migrations, auth, frontend tooling, test frameworks —
are proposed with recommendations in
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

**None of this is persistence.** The schema holds zero tables, nothing maps to
it, and no request touches it. See
[§5.3 of the plan](resibo-plan.md#53-the-database-connection-2026-11-24).

## Backend architecture

One deployable Spring Boot application. The code sits at the repository root and
is packaged **layer-first** at the base package `com.api.resibo`, matching the
sibling project `gainsoclock` that Resibo takes its conventions from:

```
src/main/java/com/api/resibo/
├── ResiboApplication.java
├── controllers/     HealthController
├── dtos/            ResponseDto — the single response envelope
├── types/           ResponseType
└── utilities/       MessageType, ResponseUtility
```

`entities/`, `repositories/` and `services/` join them, flat, when the first
feature needs them.

Every endpoint returns the same envelope — `responseType`, `code`, `data`,
`message`, `className`, `methodName`, `responseTime` — built through
`ResponseUtility`'s static factories rather than assembled at the call site.

Two endpoints exist:

| Endpoint | Purpose |
|---|---|
| `GET /api/health/check` | The application's own health check; returns the standard envelope, proving the request path and the envelope both work |
| `GET /actuator/health` | Spring Boot Actuator, for infrastructure probes |

An earlier decision called for a modular monolith packaged by feature instead.
That structure is **not** what the code is, and the plan now records why, what
the layer-first choice costs, and that it is awaiting the owner's confirmation —
see [§6.1 of the plan](resibo-plan.md#61-backend-structure--layer-first-in-the-code-modular-monolith-deferred).

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
a local URL and a deployed one mean the same thing. Two endpoints answer:

```sh
curl http://localhost:8083/api/health/check     # 8080 if SERVER_PORT is unset
curl http://localhost:8083/actuator/health
```

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
purpose. That applies to `./mvnw verify` as well as to running it: one of the
two tests starts the Spring context, so **the test suite needs a reachable
database too**.

A caution that belongs here rather than in the plan's small print: the
`.env.dev` used while this was built points at the **shared server instance**,
which is the same database the deployed application uses. CI runs against a
throwaway MariaDB service container instead. Pointing local work at a local
MariaDB — or at Testcontainers — is the open item in
[§12 of the plan](resibo-plan.md#12-open-questions); until it is closed, know
what your `DATASOURCE_URL` is aimed at before you run a test that writes.

Nothing creates tables: `ddl-auto` is `none` and there are no migrations yet.
Instructions for the schema, migrations, and the frontend follow the decisions
that produce them.

## Deployment

The application runs on a self-hosted Ubuntu server as a Docker container,
reverse-proxied by the host's nginx and published through a Cloudflare tunnel.

| | |
|---|---|
| Public URL | <https://resibo-api.polbinsenthabyer.uk> |
| What it serves | `GET /api/health/check` and `GET /actuator/health` — **nothing else** |
| Runtime | Docker container, `restart: unless-stopped`, published on the host's loopback interface only |
| TLS | Terminated by Cloudflare. The tunnel carries the request to the origin, so the origin holds no certificate and listens on plain HTTP on loopback. |

```sh
curl https://resibo-api.polbinsenthabyer.uk/api/health/check
```

> **"Deployed" does not mean "working POS".** The live application connects to
> its database and holds an idle connection pool against an **empty schema** —
> no tables, no entities, no migrations. It answers health checks, and every
> question about what Resibo actually does is still open in the plan.

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
