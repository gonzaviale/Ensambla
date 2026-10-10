import { expect, it } from 'vitest';

it('acepta el signal usado por las consultas de sesión', async () => {
  const response = await fetch(new URL('/api/auth/me', window.location.origin), {
    signal: new AbortController().signal,
  });

  expect(response.status).toBe(401);
});
