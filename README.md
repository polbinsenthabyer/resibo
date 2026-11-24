# Resibo

A point-of-sale application — Spring Boot, React, and MariaDB.

> **Status: planning, with a skeleton.** The backend builds, runs, and answers a
> health check — and that is all it does. There is no database connection, no
> schema, no authentication, and no frontend. Scope, the data model, and the
> architecture are still being settled in
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
| Hosting | To be decided — server and Cloudflare-managed domain |

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

No database is needed to build or run any of this — the application does not
connect to one yet. Instructions for MariaDB, migrations, and the frontend
follow the decisions that produce them.

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
