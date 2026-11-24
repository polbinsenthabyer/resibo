# Resibo

A point-of-sale application — Spring Boot, React, and MariaDB.

> **Status: planning, with a deployed skeleton.** The backend builds, runs, and
> answers a health check — and that is all it does. It is deployed and publicly
> reachable at **<https://resibo-api.polbinsenthabyer.uk>**, which proves the
> road to production works and nothing about the product: there is **no database
> connection**, no schema, no authentication, and no frontend. Scope, the data
> model, and the architecture are still being settled in
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
| Database | MariaDB — chosen, not yet connected to |
| Hosting | Docker on a self-hosted Ubuntu server, reverse-proxied by nginx, fronted by a Cloudflare tunnel |

The backend's dependencies today are deliberately few: `webmvc`, Actuator,
Lombok, DevTools, and the matching test starters. No persistence layer, no
migrations tool, and no security starter are in the build yet, because the
decisions they would encode are still open.

The remaining choices — migrations, auth, frontend tooling, test frameworks —
are proposed with recommendations in
[§5 of the plan](resibo-plan.md#5-tech-stack) and await sign-off.

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

You need a **JDK 17**. You do not need Maven — the `mvnw` wrapper is committed
and will fetch it.

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

It listens on port 8080 unless `SERVER_PORT` says otherwise. Two endpoints
answer:

```sh
curl http://localhost:8080/api/health/check
curl http://localhost:8080/actuator/health
```

If you have Docker and would rather not deal with `JAVA_HOME` at all, the same
image that runs in production builds and runs locally — it brings its own JDK:

```sh
docker compose up --build        # then http://localhost:8083/api/health/check
```

Configuration comes from a per-environment env file chosen by `APP_ENV` —
`.env.dev` by default, loaded if it is there and ignored if it is not. Copy the
committed `.env.example`, which carries variable names and no values.

No database is needed to build or run any of this — the application does not
connect to one yet. Instructions for MariaDB, migrations, and the frontend
follow the decisions that produce them.

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
> no database — no JPA dependency, no JDBC driver, no schema in use. It answers
> health checks, and every question about what Resibo actually does is still
> open in the plan.

Deploying is manual, from a git checkout on the server:

```sh
cd /opt/resibo/app
git pull
sudo -E bash scripts/deploy.sh
```

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

See [§8 of the plan](resibo-plan.md#8-credentials-and-secrets) for the full
handling rules and the inventory of credentials the project needs.

## Contributing

Any change that alters behaviour, scope, schema, stack, or deployment updates
`resibo-plan.md` in the same change. The plan is not a historical artifact of
the kickoff — it is the current description of the system, and it is expected to
stay accurate.
