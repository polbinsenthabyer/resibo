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
| `dev/resibo` | Everything else. **All work commits directly here.** |

Commit straight to `dev/resibo` — no feature branches, no pull requests — and
push every commit. Touch `main` only to update the README. Keep commit messages
brief and precise; Conventional Commits, imperative mood.

## Architecture

The backend is a **modular monolith**: one deployable Spring Boot application,
packaged by feature rather than by layer. There is no top-level `controllers/`,
`services/`, or `repositories/`.

Each module keeps its entities and repositories package-private and exposes only
an `api/` package of interfaces and DTOs. **Modules never read each other's
tables** — cross-module access goes through the owning module's interface. Every
table has exactly one owning module, and Flyway migrations are named by owner.
That rule is the whole point of the structure; without it this becomes a tangle
with extra folders. The layout and the open questions about enforcing it are in
plan §6.1.

## Secrets

**No credential value goes into this repository, its history, its documentation,
the vault, or a chat message.** Plan §8 holds an inventory of credential *names*
and where each value lives.

Secrets are supplied as a **path** to a git-ignored file outside the repository.
Read it when a task needs it; never echo it back, never commit it, never record
it in a note. The backend reads secrets from environment variables only — a
committed `.env.example` lists the names with empty values.

## Current state

No application code exists. There is no build, no test suite, and nothing to
run; commands will be documented here once a skeleton exists. Scaffolding is
blocked on the plan's blocking questions, by the owner's explicit instruction —
do not generate a Spring Boot or React project until those are answered.
