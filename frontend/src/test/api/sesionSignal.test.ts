import { expect, it } from 'vitest';

let respuesta: Response;

it('acepta el signal usado por las consultas de sesión', async () => {
  await cuandoSeConsultaLaSesionConUnSignal();

  entoncesElServidorResponde401PorqueNoHaySesion();
});

async function cuandoSeConsultaLaSesionConUnSignal() {
  respuesta = await fetch(new URL('/api/auth/me', window.location.origin), {
    signal: new AbortController().signal,
  });
}

function entoncesElServidorResponde401PorqueNoHaySesion() {
  expect(respuesta.status).toBe(401);
}
