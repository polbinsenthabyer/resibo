# Codex instructions for Resibo

Read the global Agent Protocol and its Multi-Agent Work Management companion, then the four vault notes at `Projects/personal/resibo/` in hub, Decisions, Changelog, Tasks order. Read `resibo-plan.md` and `CODEX.md` before changing the frontend. The plan is the product source of truth; `CODEX.md` is the short Codex handoff, not a second specification.

The owner assigned Codex to frontend work in `client/` and Claude Code to backend work in `server/`. Work on `dev/resibo` in `C:\Users\okarun\sprintify_solutions\personal\resibo`. The other clone at `personal\app\resibo` is a duplicate; do not edit it. Coordinate any API contract or shared-file change before editing overlapping work. Do not claim Claude Code is running or has accepted a handoff without evidence.

For each material change, update `resibo-plan.md` in the same change, with the rationale in §13, a version bump and §14 entry. Write decisions, dated verification, and open work to the four vault notes as the work happens. Refresh `CODEX.md` with the current frontend state and next action before ending the task. Review the diff, run applicable checks, commit deliberately on `dev/resibo`, and push per the project policy. Never record secrets or full conversation transcripts.
