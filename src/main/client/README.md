# Resibo — client

The React frontend. Lives at `src/main/client` inside the backend repository,
on branch `dev/app/resibo`.

> The project's source of truth is [`resibo-plan.md`](../../../resibo-plan.md) at
> the repository root. Read it before making decisions here — product scope,
> the domain model and the open `[BLOCKER]` questions all live there, and
> several of them decide what this app looks like.

## Running it

```sh
cd src/main/client
npm install
npm run dev          # http://localhost:5173
```

**That is all you need.** By default the client talks to the **deployed** API
at `https://resibo-api.polbinsenthabyer.uk/api`, so there is no backend to
start, no database to reach, and no `JAVA_HOME` to set.

To run against a **local** backend instead, point it at the relative path and
let the dev proxy forward to `:8083`:

```sh
VITE_API_BASE_URL=/api npm run dev      # PowerShell: $env:VITE_API_BASE_URL="/api"; npm run dev
```

Then start the backend too (`./mvnw spring-boot:run` from the repository root).
The browser sees a single origin that way, so CORS never applies.

> ⚠️ **The default points development at production.** Harmless today — the
> only call the app makes is a GET health check — and the status page says
> which API it is using. It stops being harmless the moment a screen can
> write, so revisit this before the first product form.

| Script | What it does |
|---|---|
| `npm run dev` | Vite dev server with the `/api` proxy |
| `npm run build` | type-check (`tsc -b`) then production build into `dist/` |
| `npm run preview` | serve the built `dist/` |
| `npm run typecheck` | types only, no build |
| `npm run lint` | oxlint |
| `npm run format` | Prettier, write |

## Why it is here and not at the repository root

The backend is a Maven project rooted at the repository root, and Maven treats
only `src/main/java` and `src/main/resources` as inputs — so everything under
`src/main/client` is invisible to the Java build. `.dockerignore` excludes this
directory outright, so the backend image never carries it.

This is the owner's call, recorded with its alternatives in `resibo-plan.md`
§5.6 and §13. The sibling project gainsoclock puts its frontend at the root of
a separate branch instead.

> ⚠️ **This branch carries a full copy of the backend, the plan and
> `CLAUDE.md`.** Nothing keeps those copies in step with `dev/api/resibo`, so
> **fetch and rebase onto it before every commit here** — plan §12 #23.

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

Everything goes through `http` in `src/api/http.ts`. Call it through a
feature's `api.ts`, never directly from a component.

The backend's contract is unusual enough to be worth knowing:
**a failed request normally still returns HTTP 200.** Business failures are
reported in the envelope as `responseType: 'ERROR'` plus a code such as
`E0103`; the only genuine HTTP error statuses are **401** (no or bad token) and
**403** (valid token, wrong role). `http` normalises both into a thrown
`ApiError`, so a caller sees one failure path.

```ts
import { productApi } from '@/features/products/api'
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

No env file is needed. `src/config/env.ts` defaults the API to the deployed
host; `VITE_API_BASE_URL` overrides it. See `.env.example`.

| `VITE_API_BASE_URL` | Talks to |
|---|---|
| unset | the deployed API — `https://resibo-api.polbinsenthabyer.uk/api` |
| `/api` | a local backend on `:8083`, through the dev proxy. Also correct for a production build served from the same origin as the API |
| an absolute URL | whatever you name |

`env.isUsingDeployedApi` is true in the first case, and the status page renders
a notice from it. Keep that notice on any screen that can write.

⚠️ **Every `VITE_`-prefixed value is compiled into the bundle the browser
downloads.** Nothing secret may go in one — see `resibo-plan.md` §8.

## Known gaps, deliberately

- **No login screen.** The auth API is typed and wired; the screen is not
  built, because what each role may and may not do is an open question
  (`resibo-plan.md` §1.3).
- **No money or date formatting helpers.** Currency, date format and timezone
  are blank in plan §10, and `responseTime` currently serialises in UTC. A
  formatter here would have had to guess, and a POS prints timestamps on
  receipts.
- **No tests.** No test runner is installed yet.
- **No state management or data-fetching library.** `useAsync` covers the
  current need; TanStack Query is proposed in plan §5 and not adopted.
