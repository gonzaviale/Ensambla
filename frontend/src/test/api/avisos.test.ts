import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import { aplicarAviso, descartarAviso, listarAvisosPendientes } from '@/api/avisos';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion, setupUsuarioSinSesion } from '@/test/testHelpers';
import { avisoDelServidor } from '@/test/avisosData';

const ID_DEL_AVISO = 'e0000000-0000-4000-8000-000000000001';

type Aviso = ReturnType<typeof avisoDelServidor>;
type Pendientes = Awaited<ReturnType<typeof listarAvisosPendientes>>;

let aviso: Aviso;
let pendientes: Pendientes;
let resuelto: Aviso | Awaited<ReturnType<typeof aplicarAviso>>;
let error: unknown;

describe('API de avisos', () => {
  it('lista sólo los avisos pendientes de la cuenta', async () => {
    dadoUnAvisoPendienteYOtroDescartado();

    await cuandoSeListanLosPendientes();

    entoncesSoloSaleElPendiente();
  });

  it('aplicar cierra el aviso y devuelve su estado', async () => {
    dadoUnAvisoPendiente();

    await cuandoSeAplicaElAviso();

    entoncesElAvisoQuedaResueltoComo('APLICADO');
    await entoncesYaNoHayAvisosPendientes();
  });

  it('descartar cierra el aviso y deja de listarse', async () => {
    dadoUnAvisoPendiente();

    await cuandoSeDescartaElAviso();

    entoncesElAvisoQuedaResueltoComo('DESCARTADO');
    await entoncesYaNoHayAvisosPendientes();
  });

  it('un aviso de otra cuenta responde como si no existiera', async () => {
    dadoUnAvisoDeOtraCuenta();

    await cuandoSeIntentaAplicarElAvisoAjeno();

    entoncesElErrorEs(404);
  });

  it('no se puede resolver dos veces el mismo aviso', async () => {
    dadoUnAvisoPendiente();
    await dadoQueYaSeDescarto();

    await cuandoSeIntentaDescartarDeNuevo();

    entoncesElErrorEs(400, 'El aviso ya fue resuelto');
  });

  it('exige una sesión válida', async () => {
    dadoUnUsuarioSinSesion();

    await cuandoSeIntentanListarLosPendientes();

    entoncesElErrorEs(401);
  });
});

function dadoUnAvisoPendiente() {
  const usuario = setupUsuarioConSesion();
  aviso = avisoDelServidor();
  estado.avisosDelServidor.set(usuario.email, [aviso]);
}

function dadoUnAvisoPendienteYOtroDescartado() {
  const usuario = setupUsuarioConSesion();
  estado.avisosDelServidor.set(usuario.email, [
    avisoDelServidor(),
    avisoDelServidor({ id: 'otro', estado: 'DESCARTADO', resueltoEn: '2026-10-19T13:00:00Z' }),
  ]);
}

function dadoUnAvisoDeOtraCuenta() {
  estado.avisosDelServidor.set('otra@example.com', [avisoDelServidor()]);
  setupUsuarioConSesion({ email: 'lucia@example.com' });
}

function dadoUnUsuarioSinSesion() {
  setupUsuarioSinSesion();
}

async function dadoQueYaSeDescarto() {
  await descartarAviso(aviso.id);
}

async function cuandoSeListanLosPendientes() {
  pendientes = await listarAvisosPendientes();
}

async function cuandoSeAplicaElAviso() {
  resuelto = await aplicarAviso(aviso.id);
}

async function cuandoSeDescartaElAviso() {
  resuelto = await descartarAviso(aviso.id);
}

async function cuandoSeIntentaAplicarElAvisoAjeno() {
  error = await aplicarAviso(ID_DEL_AVISO).catch((e: unknown) => e);
}

async function cuandoSeIntentaDescartarDeNuevo() {
  error = await descartarAviso(aviso.id).catch((e: unknown) => e);
}

async function cuandoSeIntentanListarLosPendientes() {
  error = await listarAvisosPendientes().catch((e: unknown) => e);
}

function entoncesSoloSaleElPendiente() {
  expect(pendientes.map((pendiente) => pendiente.id)).toEqual([ID_DEL_AVISO]);
}

function entoncesElAvisoQuedaResueltoComo(esperado: string) {
  expect(resuelto.estado).toBe(esperado);
}

async function entoncesYaNoHayAvisosPendientes() {
  expect(await listarAvisosPendientes()).toEqual([]);
}

function entoncesElErrorEs(estadoHttp: number, mensaje?: string) {
  expect(error).toBeInstanceOf(ApiError);
  expect((error as ApiError).status).toBe(estadoHttp);
  if (mensaje) expect((error as ApiError).message).toBe(mensaje);
}
