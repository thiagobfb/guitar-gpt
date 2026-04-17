# GuitarGPT Frontend

Web frontend for GuitarGPT. Not yet initialized.

## Planned stack

- React + Vite + TypeScript
- TanStack Query for API calls
- MSW (Mock Service Worker) for mocking the backend during SDD
- shadcn/ui + Tailwind CSS

## Approach

Spec Driven Development: specs in `../docs/specs/` precede implementation.
Mocks via MSW simulate the backend so the frontend can be developed independently.
