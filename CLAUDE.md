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

## Secrets

**No credential value goes into this repository, its history, its documentation,
the vault, or a chat message.** Plan §8 holds an inventory of credential *names*
and where each value lives.

Secrets are supplied as a **path** to a git-ignored file outside the repository.
Read it when a task needs it; never echo it back, never commit it, never record
it in a note. The backend reads secrets from environment variables only — a
committed `.env.example` lists the names with empty values.

## Current state

A skeleton exists and is verified: Spring Boot 4.2.0 on Java 17, Maven,
the `ResponseDto` conventions, `GET /api/health/check`, `/actuator/health`, and
two passing tests. There is **no database connection** — the pom has neither JPA
nor a JDBC driver — and no schema, no migrations, no authentication, and no
frontend. Plan §5.1 records exactly what was generated and what was verified.

**No JDK is on `PATH` and `JAVA_HOME` is unset. Maven is not installed.** Set
`JAVA_HOME` in every shell before building, and use the committed wrapper:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.20"   # Microsoft OpenJDK 17.0.20
.\mvnw.cmd -B clean verify     # compile + tests
.\mvnw.cmd spring-boot:run     # run on :8080, override with SERVER_PORT
```

Everything past the skeleton is still blocked. Generating it was the owner
lifting the scaffolding hold for one step, not for the project — do not start on
the schema, auth, or checkout without answers to §1, §2 and §6.1.1.

Two known gaps, so they are not rediscovered as surprises:

- `responseTime` serialises in **UTC**, not local time. **Deliberately not
  fixed** — the project's timezone is an open owner question (plan §10, §12 #8).
  Do not "fix" it by guessing a zone; answer the question first.
- `className` and `methodName` are returned on every response, errors included.
  They leak internals and must be suppressed in production error responses
  (plan §6.1.2).
