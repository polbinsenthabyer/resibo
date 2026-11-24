# Codex frontend handoff

Updated: 2026-11-24. This file is the short entry point for a new Codex session. Product decisions live in `resibo-plan.md`; dated history and open work live in the four vault notes at `Projects/personal/resibo/`. Read those before relying on this snapshot.

## Assignment and checkout

- The owner assigned Codex to frontend work in `client/` and Claude Code to backend work in `server/`. This records ownership; it does not claim a Claude Code session is currently running.
- Work on `dev/resibo` at `C:\Users\okarun\sprintify_solutions\personal\resibo`. A second clone at `personal\app\resibo` exists; the vault marks it as a duplicate.
- Shared files such as `resibo-plan.md` and the API contract need one editor at a time. Check branch and working tree before editing, review changes made by the other agent, and have a reviewer accept cross-stack handoffs. Keep frontend and backend deploys independent.

## Current frontend state

- `client/` is a React 19, Vite 8, TypeScript 7, SCSS application. Its UI has API health, login, and signup pages. There is no POS screen and no frontend test runner. New frontend features must work at narrow and desktop widths, per the owner's standing instruction.
- `client/src/api/http.ts` is the single fetch path. It unwraps `ResponseDto`; business errors can arrive with HTTP 200. Authentication uses a provisional `localStorage` token helper.
- Production builds default to the deployed API. Local Vite development now defaults to the `/api` proxy; `VITE_API_BASE_URL` overrides either. The local backend currently uses the shared live database, so its signup path can still create live accounts. Both auth forms show their API target, and signup warns in development.
- The client is served as static files at `https://resibo.polbinsenthabyer.uk`. The latest recorded local checks passed `npm run lint` and `npm run build`; the last rescan observed HTTP 200 for the client root, a deep link, and API health. A browser render and CI deployment were not verified in that rescan.
- The health page includes a locally bundled SpongeBob dancing GIF for the owner's client pipeline test (`b0e35b4`). Local `npm run lint`, `npm run build`, and `npm run format:check` pass; the build copies `spongebob.gif` into `dist/`. The image is temporary and can leave when a real landing page replaces health.
- Login calls `authApi.login`, stores the returned JWT through the existing token helper, displays the returned username and role, and offers local sign-out. Signup sends username, password, and optional name through `authApi.signup`, omits role, and directs the user to login after confirmation. Local lint, build, and format pass. [Client run 45369269628](https://github.com/polbinsenthabyer/resibo/actions/runs/45369269628) passed Build and Deploy; public `/login` and `/signup` serve the matching JS/CSS bundle. The forms were not visually inspected or submitted in a browser, and no account was created for testing.
- Client automatic deployment is live: [run 45366787290](https://github.com/polbinsenthabyer/resibo/actions/runs/45366787290) passed Build and Deploy on the online `resibo-client-deploy` runner. A second, client-only docs push (`99446c1`) also passed [Build and Deploy](https://github.com/polbinsenthabyer/resibo/actions/runs/45367329760), verifying the `client/**` trigger. `CLIENT_DEPLOY_ENABLED=true` is the client-only repository gate; backend `DEPLOY_ENABLED` remains unset. Public `/spongebob.gif` returned `200 image/gif` with the committed SHA-256, the client and API returned 200, and old hashed assets were retained. Repository documentation-only pushes do not trigger the client workflow.

## Open frontend work

- `scripts/deploy-client.sh` now retains old hashed assets during releases, verified on the first automatic deployment. `scripts/setup-client-server.sh prelive` still reuses the live vhost filename; that remains in vault `Tasks.md`.
- The product and permission decisions in plan §1–§2 and §6.1.1 remain open. Do not invent them while building checkout, sale, receipt, or role-specific UI.
- Frontend tests and current documentation reconciliation remain open. Use vault `Tasks.md` for the maintained task list.

**Next action:** when the owner tests the forms, capture any UI or API issues and verify the responsive layout in a browser if desktop control is appropriate. Before setting up prelive, fix its vhost name collision. Do not start checkout or receipt UI until the plan's relevant blockers are answered.

## Update contract for every frontend change

1. Read the vault notes and plan, then reconcile this snapshot with the tree. Record any stale claim.
2. Define the work and verification. Keep a task board with explicit state; use the vault `Tasks.md` for durable open work. Coordinate shared plan or API contract edits with the backend owner.
3. When a decision is made, add its reason to plan §13 and vault `Decisions.md`. When behavior or scope changes, update the plan, bump its version, and append §14 in the same change. Update the README if its summary changes.
4. As work lands, update vault `Changelog.md` with what was requested, changed, and actually verified; update `Tasks.md` and the hub when their facts change. Refresh this file's state and next action before the session ends.
5. Review the diff and run applicable client checks. Commit and push each self-contained change on `dev/resibo`. Record the commit and push result in the vault. Never store secrets, credentials, or a conversation transcript here or in the vault.
6. Make every new frontend feature responsive at mobile and desktop widths; verify the rendered layout when an appropriate browser test is available.
