# Codex frontend handoff

Updated: 2026-11-24. This file is the short entry point for a new Codex session. Product decisions live in `resibo-plan.md`; dated history and open work live in the four vault notes at `Projects/personal/resibo/`. Read those before relying on this snapshot.

## Assignment and checkout

- The owner assigned Codex to frontend work in `client/` and Claude Code to backend work in `server/`. This records ownership; it does not claim a Claude Code session is currently running.
- Work on `dev/resibo` at `C:\Users\okarun\sprintify_solutions\personal\resibo`. A second clone at `personal\app\resibo` exists; the vault marks it as a duplicate.
- Shared files such as `resibo-plan.md` and the API contract need one editor at a time. Check branch and working tree before editing, review changes made by the other agent, and have a reviewer accept cross-stack handoffs. Keep frontend and backend deploys independent.

## Current frontend state

- `client/` is a React 19, Vite 8, TypeScript 7, SCSS application. Its current UI is one API health page. There is no login or POS screen and no frontend test runner.
- `client/src/api/http.ts` is the single fetch path. It unwraps `ResponseDto`; business errors can arrive with HTTP 200. Authentication uses a provisional `localStorage` token helper.
- The client defaults to the deployed API. Set `VITE_API_BASE_URL=/api` for the local Vite proxy. Revisit the default before a screen can write data.
- The client is served as static files at `https://resibo.polbinsenthabyer.uk`. The latest recorded local checks passed `npm run lint` and `npm run build`; the last rescan observed HTTP 200 for the client root, a deep link, and API health. A browser render and CI deployment were not verified in that rescan.

## Open frontend work

- `scripts/deploy-client.sh` can delete old hashed assets before swapping `index.html`; `scripts/setup-client-server.sh prelive` reuses the live vhost filename. Both are recorded in vault `Tasks.md` and need review before those paths are relied on.
- The product and permission decisions in plan §1–§2 and §6.1.1 remain open. Do not invent them while building checkout, sale, receipt, or role-specific UI.
- Authentication UI, frontend tests, and current documentation reconciliation remain open. Use vault `Tasks.md` for the maintained task list.

**Next action:** take the next frontend task the owner assigns. Before a client deployment, fix and verify the asset cleanup issue in `scripts/deploy-client.sh`. Before setting up prelive, fix its vhost name collision. Do not start checkout or receipt UI until the plan's relevant blockers are answered.

## Update contract for every frontend change

1. Read the vault notes and plan, then reconcile this snapshot with the tree. Record any stale claim.
2. Define the work and verification. Keep a task board with explicit state; use the vault `Tasks.md` for durable open work. Coordinate shared plan or API contract edits with the backend owner.
3. When a decision is made, add its reason to plan §13 and vault `Decisions.md`. When behavior or scope changes, update the plan, bump its version, and append §14 in the same change. Update the README if its summary changes.
4. As work lands, update vault `Changelog.md` with what was requested, changed, and actually verified; update `Tasks.md` and the hub when their facts change. Refresh this file's state and next action before the session ends.
5. Review the diff and run applicable client checks. Commit and push each self-contained change on `dev/resibo`. Record the commit and push result in the vault. Never store secrets, credentials, or a conversation transcript here or in the vault.
