import { describe, expect, it } from 'vitest';
import { sesionExpiradaConfirmada } from '@/api/auth';
import { estado } from '@/mocks/db/estado';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

let vencida: boolean;
let rechazo: Promise<boolean>;

describe('confirmación del vencimiento de sesión', () => {
  it('una respuesta 401 atrasada no expulsa a quien todavía tiene sesión', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeConfirmaElVencimiento();

    entoncesLaSesionNoSeDaPorVencida();
  });

  it('confirma el cierre cuando el servidor ya no reconoce la sesión', async () => {
    dadoUnUsuarioConSesion();
    dadoQueElServidorYaNoReconoceLaSesion();

    await cuandoSeConfirmaElVencimiento();

    entoncesLaSesionSeDaPorVencida();
  });

  it('un error temporal del servidor no se transforma en una sesión vencida', async () => {
    dadoUnUsuarioConSesion();
    dadoQueElServidorFallaTemporalmente();

    cuandoSeIntentaConfirmarElVencimiento();

    await entoncesSePropagaElError503();
  });
});

function dadoUnUsuarioConSesion() {
  setupUsuarioConSesion();
}

function dadoQueElServidorYaNoReconoceLaSesion() {
  estado.sesionUsuarioId = null;
}

function dadoQueElServidorFallaTemporalmente() {
  setupServerError('get', '/auth/me', 503, 'Temporalmente no disponible');
}

async function cuandoSeConfirmaElVencimiento() {
  vencida = await sesionExpiradaConfirmada();
}

function cuandoSeIntentaConfirmarElVencimiento() {
  rechazo = sesionExpiradaConfirmada();
}

function entoncesLaSesionNoSeDaPorVencida() {
  expect(vencida).toBe(false);
}

function entoncesLaSesionSeDaPorVencida() {
  expect(vencida).toBe(true);
}

async function entoncesSePropagaElError503() {
  await expect(rechazo).rejects.toMatchObject({ status: 503 });
}
