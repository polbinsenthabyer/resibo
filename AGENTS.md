# Codex instructions for Resibo

Read the global Agent Protocol and its Multi-Agent Work Management companion, then the four vault notes at `Projects/personal/resibo/` in hub, Decisions, Changelog, Tasks order. Read `resibo-plan.md` and `CODEX.md` before changing the frontend. The plan is the product source of truth; `CODEX.md` is the short Codex handoff, not a second specification.

The owner assigned Codex to frontend work in `client/` and Claude Code to backend work in `server/`. Work on `dev/resibo` in `C:\Users\okarun\sprintify_solutions\personal\resibo`. The other clone at `personal\app\resibo` is a duplicate; do not edit it. Coordinate any API contract or shared-file change before editing overlapping work. Do not claim Claude Code is running or has accepted a handoff without evidence.

For each material change, update `resibo-plan.md` in the same change, with the rationale in §13, a version bump and §14 entry. Write decisions, dated verification, and open work to the four vault notes as the work happens. Refresh `CODEX.md` with the current frontend state and next action before ending the task. Review the diff, run applicable checks, commit deliberately on `dev/resibo`, and push per the project policy. Never record secrets or full conversation transcripts.

Visual assets for Resibo: generate new logos, icons, illustrations, and other original graphics with Magnific. Save them in the Magnific `pol` project, keep the original export under `client/design/magnific/`, and put web-ready assets in `client/public/` as needed. Reuse the generated logo mark for branding and favicon. Record the source and any adaptation in the plan and client README. This is the owner's standing asset-source instruction; do not silently substitute another generator or icon pack.
