## AI Interview Gateway Frontend

Premium dark-mode frontend for the AI Interview platform.

- **Framework:** Next.js (App Router)
- **Styling:** Tailwind CSS + shadcn-style components
- **Motion:** Framer Motion
- **Networking:** Axios + JWT interceptor
- **Token storage:** js-cookie

## Implemented in this milestone

- Animated `/login` and `/register` pages with glassmorphism cards
- Dynamic gradient mesh background
- Auth form validation and loading micro-interactions
- JWT persistence in cookies
- Axios client that automatically sends `Authorization: Bearer <token>`
- Redirect from `/` to `/login`
- Protected routing middleware for `/dashboard` and `/interview/*`
- Animated `/dashboard` command-center with Resume + JD inputs
- Realtime `/interview/[sessionId]` WebSocket room with message streams, typing indicator, score glow cards, and smooth auto-scroll

## Project structure

- `src/app/login/page.tsx` – Login page
- `src/app/register/page.tsx` – Register page
- `src/components/auth/*` – Auth shell + form behavior
- `src/components/design/animated-mesh-background.tsx` – Motion backdrop
- `src/components/ui/*` – Reusable core UI components
- `src/lib/api-client.ts` – Axios client + auth endpoints
- `src/lib/token.ts` – Cookie token helpers

## Run locally

### Node version requirement

Use a supported LTS Node runtime (recommended: Node `22`).

If `npm run dev` stays on `Compiling / ...` or build workers crash (for example `SIGBUS`), check your Node version first. Node `25` is not supported by this app's Next.js version.

You can use the included `.nvmrc` in this folder:

```bash
nvm use
```

If Node 22 is not installed yet:

```bash
nvm install 22
nvm use 22
```

```bash
npm run dev
```

Open `http://localhost:3000`.

## Backend contract used

- `POST http://localhost:8080/api/auth/login`
- `POST http://localhost:8080/api/auth/register`

Both endpoints are expected to return either a raw token string or a JSON object containing `token`.

## Next milestone

Add richer session management (real session IDs from backend), dashboard data persistence, and transcript history/export.
