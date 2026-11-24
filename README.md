# Resibo

Resibo is a point-of-sale application in development, built with Spring Boot, React, and MariaDB.

The API is deployed at <https://resibo-api.polbinsenthabyer.uk>. It currently has JWT authentication, ADMIN and EMPLOYEE roles, and an admin-only product catalog. The React client at <https://resibo.polbinsenthabyer.uk> is a one-page API health screen. Checkout, sales, payments, receipts, shifts, and stock movement are not built yet.

## Working branch

`main` contains this public summary only. The application, plan, and project instructions live together on [`dev/resibo`](https://github.com/polbinsenthabyer/resibo/tree/dev/resibo):

- [`client/`](https://github.com/polbinsenthabyer/resibo/tree/dev/resibo/client) — React, Vite, TypeScript, and SCSS
- [`server/`](https://github.com/polbinsenthabyer/resibo/tree/dev/resibo/server) — Spring Boot API and SQL table definitions
- [`resibo-plan.md`](https://github.com/polbinsenthabyer/resibo/blob/dev/resibo/resibo-plan.md) — product scope, decisions, open questions, and delivery plan

The client and server have separate CI workflows and deployment targets. A client release does not redeploy the API.

To inspect or run the current client from a checkout:

```sh
git switch dev/resibo
cd client
npm ci
npm run dev
```

The development client calls the deployed API by default. The project plan records the unresolved choices that affect checkout and receipts, including offline behavior, tax, permissions, and official-receipt requirements.

## Credentials

Do not commit credentials or put them in documentation. The working branch has names-only environment templates; real values belong outside the repository.