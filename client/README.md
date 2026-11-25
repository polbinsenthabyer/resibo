# Resibo — client

The React frontend. Lives at `client/` in the Resibo monorepo on `dev/resibo`.

> The project's source of truth is [`resibo-plan.md`](../resibo-plan.md) at
> the repository root. Read it before making decisions here — product scope,
> the domain model and the open `[BLOCKER]` questions all live there, and
> several of them decide what this app looks like.

## Running it

```sh
cd client
npm ci
npm run dev          # http://localhost:5173
```

Local development uses the deployed API by default. The signup page warns
that it can create a live account. To use the optional local backend on
`:8083`, start it from `server/` and set the base URL to `/api`:

```sh
VITE_API_BASE_URL=/api npm run dev
```

Vite then proxies requests to the local backend. That backend currently uses
the shared live database too, so the proxy does not isolate account writes.
Production builds also use the deployed API by default.

| Script | What it does |
|---|---|
| `npm run dev` | Vite dev server; client calls the deployed API by default |
| `npm run build` | type-check (`tsc -b`) then production build into `dist/` |
| `npm run preview` | serve the built `dist/` |
| `npm run typecheck` | types only, no build |
| `npm run lint` | oxlint |
| `npm run format` | Prettier, write |

Run the dev server from the primary checkout at `personal/resibo/client`.
If edits appear stale, check which Vite process owns port 5173: on Windows,
`localhost` (`::1`) and `127.0.0.1` can reach different processes even when
both display port 5173. The duplicate `personal/app/resibo` checkout must not
be used for development.

## Deployment

A push to `dev/resibo` that changes `client/**` starts the client workflow.
It runs lint and build on GitHub-hosted Ubuntu, then sends the build artifact
to the `resibo-client-deploy` runner, which serves it from
`/var/www/resibo/live`. The repository variable `CLIENT_DEPLOY_ENABLED=true`
controls this deploy job. The backend workflow has its own path filter and
deploy gate, so a client-only commit does not redeploy the API. The first
automatic client release passed in [run 45366787290](https://github.com/polbinsenthabyer/resibo/actions/runs/45366787290).

The health page currently displays a temporary SpongeBob GIF to make the
pipeline result easy to see. It is bundled in `public/spongebob.gif`, sourced
from the [SpongeBob SquarePants GIPHY channel](https://giphy.com/gifs/dancing-hipster-spongebob-9d1Fo0XyIYXzW).

## Visual assets

Routine interface icons come from Lucide React; call Magnific only when the
owner explicitly asks for a generated visual. The receipt-and-arrow mark in
`public/resibo-logo.svg` was requested from Magnific. Its source PNG, original
SVG, and creation link are in [`design/magnific/`](design/magnific/README.md).
The web version is cropped and adapts its blue to dark browser themes. The
header and favicon use that SVG. The temporary GIF above predates this rule.
The admin sidebar, action buttons, and theme toggle use Lucide icons. Icon-only
controls have accessible names and titles.

## Authentication pages

`/login` and `/signup` use the existing `authApi` calls. Login stores the JWT
returned by `AuthController` in local storage and redirects ADMIN to `/admin`
or EMPLOYEE to `/employee`. Each route shows a simple role-specific welcome
page with a Sign out button. Sign out clears the stored token and returns to
`/login`. The client reads the token role and expiry for navigation; the API
still validates the token for every protected request. Signup sends username,
password, and optional name; it does not request a role or sign the user in.
Both forms handle server errors and fit narrow screens.
Backend success and error messages appear in a shared popup stack. Mutating
requests show both; read requests show errors only. The automatic health check
keeps its inline status, and login success appears after the token is saved.
The signup page warns in development because the default deployed API creates
live accounts. Even the optional local `/api` proxy reaches a backend whose
current `server/.env.dev` uses the shared live database.

`/` opens the matching role page when a valid-looking session is stored, or
`/login` otherwise. The health page remains at `/status`. The header has no
page navigation; its light/dark toggle saves the choice in local storage and
defaults to the device preference on first visit. The forms, welcome pages,
and header use the same theme tokens and mobile layout rules.

`/admin` now opens a simple admin workspace with a far-left side menu and catalog count.
`/admin/products` uses the existing ADMIN-only API routes to list, search,
create, edit, and delete products. Its joined Row/Card icon toggle switches the
current product page between a compact list and a responsive grid; the choice
is saved in the browser. Add and Edit open a keyboard-accessible popup. Editing loads the full product first and
sends all editable fields back; deletion asks for confirmation. Prices are
displayed without an assumed currency or client-side totals. The EMPLOYEE
route remains a welcome placeholder. Local development calls the deployed API
by default, so product writes affect live data.
The side menu stays visible and uses labeled section icons. Admin action
buttons and the light/dark toggle show icons instead of text; accessible
names and hover titles describe their actions.

## Why it is separate from the backend

The backend is a Maven project under `server/`. The Docker build context is
`server/`, so `client/` and `node_modules` cannot enter the API image.

The current monorepo layout and its rationale are recorded in `resibo-plan.md`
§7.1, §7.4 and §13. The earlier `src/main/client` layout is historical.

> Both halves share `dev/resibo`. Coordinate API contract changes and edits to
> shared files such as `resibo-plan.md`; the retired `dev/api/resibo` and
> `dev/app/resibo` branches are no longer work targets.

## Structure

```
src/
├── main.tsx                  entry: mounts App, loads the one global stylesheet
├── App.tsx                   app-wide providers (router today)
├── app/                      routing: the route table and path constants
├── api/                      the HTTP layer — one place that calls fetch
│   ├── types.ts              ResponseDto / PageDto, mirrored from the backend
│   ├── http.ts               request wrapper: unwraps the envelope, throws ApiError
│   ├── errors.ts             ApiError / NetworkError
│   ├── endpoints.ts          every API path, in one file
│   └── tokenStorage.ts       where the bearer token lives
├── features/<feature>/       a feature owns its api, types, hooks, components, pages
│   ├── auth/
│   ├── health/
│   └── products/
├── components/               shared, presentational, feature-agnostic
├── layouts/                  page chrome with an <Outlet />
├── hooks/                    shared hooks
├── lib/                      framework-free helpers
├── pages/                    pages belonging to no feature (404)
└── styles/                   global only: tokens, reset, element defaults
```

**The rule that keeps this clean:** a feature may import from `api/`,
`components/`, `hooks/`, `lib/` and `styles/`. A feature must not import from
another feature. When two features need the same thing, it moves down into the
shared layer rather than sideways.

Anything a component owns — markup, styles, its own small hooks — sits in that
component's folder next to it, which is why components are folders and not
loose files.

## Styling

Plain SCSS. Two kinds of stylesheet and no third:

- **`src/styles/`** — global, loaded once: design tokens as CSS custom
  properties on `:root`, a small reset, element defaults.
- **`*.module.scss`** — everything else. A CSS Module next to the component
  that owns it, so a class name cannot leak.

Tokens and mixins are injected into every stylesheet by `vite.config.ts`, so a
component's styles can use `var(--space-4)`, `$breakpoint-md` and
`@include focus-ring` with no import. **Never write `@use 'abstracts' as *`
yourself** — Sass rejects the same module being loaded twice in one file.

Class names are camelCased on the way into TypeScript
(`.product-row` → `styles.productRow`).

## Talking to the API

Everything goes through `http` in `src/api/http.ts`. Call it through an API
module, never directly from a component.

The backend's contract is unusual enough to be worth knowing:
**a failed request normally still returns HTTP 200.** Business failures are
reported in the envelope as `responseType: 'ERROR'` plus a code such as
`E0103`; the only genuine HTTP error statuses are **401** (no or bad token) and
**403** (valid token, wrong role). `http` normalises both into a thrown
`ApiError`, so a caller sees one failure path.

```ts
import { productApi } from '@/api/products/api'
import { ApiError } from '@/api/errors'

try {
  const page = await productApi.list({ size: 20 })
} catch (error) {
  if (error instanceof ApiError && error.isForbidden) {
    // valid token, but not an ADMIN
  }
}
```

Roles are enforced by URL prefix: `/api/admin/**` needs ADMIN,
`/api/employee/**` needs either role. `/api/auth/**` and `/api/health/**` are
public.

## Configuration

No env file is needed. `src/config/env.ts` defaults to the deployed API in
both environments; `VITE_API_BASE_URL` overrides it. See `.env.example`.

| `VITE_API_BASE_URL` | Talks to |
|---|---|
| unset in local development | the deployed API — `https://resibo-api.polbinsenthabyer.uk/api` |
| unset in a production build | the deployed API — `https://resibo-api.polbinsenthabyer.uk/api` |
| `/api` | the local backend through the dev proxy; use in production only if a same-origin API proxy is configured |
| an absolute URL | whatever you name |

Neither auth page prints the effective target. The signup page warns about
live account creation in development; the health page indicates when it uses
the deployed API.

⚠️ **Every `VITE_`-prefixed value is compiled into the bundle the browser
downloads.** Nothing secret may go in one — see `resibo-plan.md` §8.

## Known gaps, deliberately

- **No checkout, sales, receipt, or employee workflow.** Product management is
  ADMIN-only; broader permissions still need the decisions in plan §1.3.
- **No money or date formatting helpers.** Currency, date format and timezone
  are blank in plan §10, and `responseTime` currently serialises in UTC. A
  formatter here would have had to guess, and a POS prints timestamps on
  receipts.
- **No tests.** No test runner is installed yet.
- **No state management or data-fetching library.** `useAsync` covers the
  current need; TanStack Query is proposed in plan §5 and not adopted.
