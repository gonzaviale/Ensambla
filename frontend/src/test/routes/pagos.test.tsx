import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, delay, http } from 'msw';
import { axe } from 'vitest-axe';
import { consultarRenovacion } from '@/api/pagos';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import type { Renovacion } from '@/types/pagos';

type Usuario = ReturnType<typeof userEvent.setup>;
type Contrato = NonNullable<Renovacion['contrato']>;

const ESPERA = { timeout: 10_000 };
const FIN_PRUEBA = new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString();
const ENLACE_DE_MERCADO_PAGO =
  'https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=contrato-local';

let user: Usuario;
let datos: Renovacion;
let contrato: Contrato;
let intentos = 0;
let tokenCsrf: string | null = null;
let cuerpoEnviado = '';
let cancelaciones = 0;
let consulta: Promise<unknown>;

const contratoDeEjemplo = (): Contrato => ({
  id: crypto.randomUUID(),
  estado: 'PENDIENTE',
  importe: 14900,
  moneda: 'ARS',
  primerCobroEn: FIN_PRUEBA,
  proximoCobroEn: null,
  cancelacionSolicitada: false,
  enlace: ENLACE_DE_MERCADO_PAGO,
});

const esperarPlanes = () =>
  screen.findByRole('heading', { name: /tu espacio actual|Tu prueba Premium está activa/ }, ESPERA);

describe('contratación y renovación automática', () => {
  it('muestra el precio aprobado', async () => {
    await dadaLaPantallaDePlanesSinMercadoPagoConfigurado();

    entoncesSeMuestraElPrecioAprobado();
  });

  it('mantiene deshabilitada la contratación sin configurar Mercado Pago', async () => {
    await dadaLaPantallaDePlanesSinMercadoPagoConfigurado();

    expect(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA)).toBeDisabled();
  });

  it('explica que el primer cobro conserva los días de prueba', async () => {
    await dadaLaContratacionEnLaPrueba();

    await cuandoSeAbreLaContratacion();

    entoncesElDialogoExplicaElPrimerCobroYElPlan();
  });

  it('permite volver sin contratar', async () => {
    await dadaLaContratacionEnLaPrueba();
    await cuandoSeAbreLaContratacion();

    await cuandoSePresionaEscape();

    entoncesSeCerroElDialogoYElFocoVolvioAContratar();
  });

  it('prepara una sola contratación con CSRF y sin enviar precio', async () => {
    await dadaUnaContratacionPorPrepararDesdeElRetornoDePago();

    await cuandoSePreparaLaContratacionConDobleClic();

    await entoncesSePrepararonUnaVezConCsrfYSinCuerpo();
  });

  it('el retorno del pago no da acceso pago ni muestra el enlace de otro lado', async () => {
    await dadaUnaContratacionPorPrepararDesdeElRetornoDePago();

    await cuandoSePreparaLaContratacionConDobleClic();

    await entoncesSeOfreceContinuarEnMercadoPagoSinDarAccesoPago();
  });

  it('una autorización confirmada sigue esperando el pago', async () => {
    await dadaUnaAutorizacionConfirmada();

    await entoncesSeEsperaElPagoYNoSePresentaComoCobrado();
  });

  it('cancelar pide confirmación antes de cancelar', async () => {
    await dadoUnPremiumPagoConRenovacionAutomatica();

    await cuandoSePideCancelarLaRenovacion();

    entoncesSePideConfirmacionYTodaviaNoSeCancelo();
  });

  it('cancelar conserva el Premium ya pagado', async () => {
    await dadoUnPremiumPagoConRenovacionAutomatica();
    await cuandoSePideCancelarLaRenovacion();

    await cuandoSeConfirmaLaCancelacion();

    await entoncesLaRenovacionQuedaCanceladaYElPeriodoPagoSeConserva();
  });

  it('una baja no confirmada muestra el error', async () => {
    await dadaUnaBajaQueMercadoPagoNoConfirma();

    await cuandoSePideYSeConfirmaLaCancelacion();

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent('todavía no confirmó');
  });

  it('una baja no confirmada deja la cancelación pendiente sin prometer que terminó', async () => {
    await dadaUnaBajaQueMercadoPagoNoConfirma();

    await cuandoSePideYSeConfirmaLaCancelacion();

    await entoncesLaCancelacionQuedaPendiente();
  });

  it('rechaza enlaces de pago que no pertenecen al checkout de Mercado Pago', async () => {
    dadoUnContratoConEnlaceAjeno();

    cuandoSeConsultaLaRenovacion();

    await expect(consulta).rejects.toThrow();
  });

  it('una falla al consultar permite reintentar sin mostrar un botón habilitado con datos inventados', async () => {
    dadoUnServidorDeRenovacionCaido();

    await cuandoSeAbrenLosPlanes();

    await entoncesSeOfreceReintentarYNoSeOfreceContratar();
  });

  it('la confirmación de contratación conserva las reglas de accesibilidad', async () => {
    await dadaLaPantallaDePlanesConContratacionDisponible();

    await cuandoSeAbreLaContratacion();

    await entoncesNoHayViolacionesDeAccesibilidad();
  });
});

function simularRenovacion(renovacion: Renovacion) {
  datos = renovacion;
  server.use(http.get('/api/planes/renovacion', () => HttpResponse.json(datos)));
}

async function abrirPlanes(ruta = '/planes') {
  renderApp(ruta);
  await esperarPlanes();
  user = userEvent.setup();
}

async function dadaLaPantallaDePlanesSinMercadoPagoConfigurado() {
  setupUsuarioConSesion();
  await abrirPlanes();
}

async function dadaLaPantallaDePlanesConContratacionDisponible() {
  setupUsuarioConSesion();
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato: null });
  await abrirPlanes();
}

async function dadaLaContratacionEnLaPrueba() {
  const usuario = setupUsuarioConSesion();
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 3 * 86400000).toISOString(),
    finEn: FIN_PRUEBA,
    paginasIaUtilizadas: 8,
    limitePaginasIa: 10,
  });
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato: null });
  await abrirPlanes();
}

async function dadaUnaContratacionPorPrepararDesdeElRetornoDePago() {
  setupUsuarioConSesion();
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato: null });
  intentos = 0;
  server.use(
    http.post('/api/planes/renovacion', async ({ request }) => {
      intentos++;
      tokenCsrf = request.headers.get('X-CSRF-TOKEN');
      cuerpoEnviado = await request.text();
      await delay(180);
      datos.contrato = contratoDeEjemplo();
      return HttpResponse.json(datos);
    }),
  );
  await abrirPlanes('/planes?status=approved');
  await user.click(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA));
}

async function dadaUnaAutorizacionConfirmada() {
  setupUsuarioConSesion();
  contrato = contratoDeEjemplo();
  contrato.estado = 'AUTORIZADA';
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
  await abrirPlanes();
}

async function dadoUnPremiumPagoConRenovacionAutomatica() {
  const usuario = setupUsuarioConSesion();
  const planes = planesDe(usuario.email);
  planes.suscripcion = {
    codigoPlanActual: 'PREMIUM',
    pruebaActiva: false,
    puedeIniciarPrueba: false,
    inicioEn: new Date().toISOString(),
    finEn: FIN_PRUEBA,
  };
  planes.uso.limitePaginasIa = 100;
  contrato = contratoDeEjemplo();
  contrato.estado = 'AUTORIZADA';
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
  cancelaciones = 0;
  server.use(
    http.get('/api/planes', () => HttpResponse.json(planes)),
    http.post('/api/planes/renovacion/cancelar', () => {
      cancelaciones++;
      contrato.cancelacionSolicitada = true;
      contrato.estado = 'CANCELADA';
      return HttpResponse.json(datos);
    }),
  );
  await abrirPlanes();
}

async function dadaUnaBajaQueMercadoPagoNoConfirma() {
  setupUsuarioConSesion();
  contrato = contratoDeEjemplo();
  contrato.estado = 'AUTORIZADA';
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
  server.use(
    http.post('/api/planes/renovacion/cancelar', () => {
      contrato.cancelacionSolicitada = true;
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 502,
          error: 'Bad Gateway',
          message: 'Mercado Pago todavía no confirmó la baja.',
          path: '/api/planes/renovacion/cancelar',
          traceId: 'local',
        },
        { status: 502 },
      );
    }),
  );
  await abrirPlanes();
}

function dadoUnContratoConEnlaceAjeno() {
  setupUsuarioConSesion();
  contrato = contratoDeEjemplo();
  contrato.enlace = 'https://un-sitio-ajeno.example/robar-datos';
  simularRenovacion({ habilitada: true, periodoDias: 30, contrato });
}

function dadoUnServidorDeRenovacionCaido() {
  setupUsuarioConSesion();
  server.use(http.get('/api/planes/renovacion', () => new HttpResponse(null, { status: 503 })));
}

async function cuandoSeAbreLaContratacion() {
  await user.click(await screen.findByRole('button', { name: 'Contratar Premium' }, ESPERA));
}

async function cuandoSePresionaEscape() {
  await user.keyboard('{Escape}');
}

async function cuandoSePreparaLaContratacionConDobleClic() {
  await user.dblClick(screen.getByRole('button', { name: 'Preparar contratación' }));
}

async function cuandoSePideCancelarLaRenovacion() {
  await user.click(await screen.findByRole('button', { name: 'Cancelar renovación' }, ESPERA));
}

async function cuandoSeConfirmaLaCancelacion() {
  const dialogo = screen.getByRole('dialog', { name: 'Cancelar renovación automática' });
  await user.click(within(dialogo).getByRole('button', { name: 'Confirmar cancelación' }));
}

async function cuandoSePideYSeConfirmaLaCancelacion() {
  await cuandoSePideCancelarLaRenovacion();
  await user.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));
}

function cuandoSeConsultaLaRenovacion() {
  consulta = consultarRenovacion();
}

async function cuandoSeAbrenLosPlanes() {
  await abrirPlanes();
}

function entoncesSeMuestraElPrecioAprobado() {
  expect(screen.getByText('Cada 30 días · renovación automática')).toBeVisible();
  expect(screen.getByText(/14\.900/)).toBeVisible();
}

function entoncesElDialogoExplicaElPrimerCobroYElPlan() {
  const dialogo = screen.getByRole('dialog', { name: 'Contratar Premium' });
  expect(within(dialogo).getByText(/14\.900.*finales cada 30 días/)).toBeVisible();
  expect(within(dialogo).getByText(/El primer cobro se programa para/)).toBeVisible();
  expect(within(dialogo).getByText(/diez análisis de foto totales/)).toBeVisible();
  expect(
    within(dialogo).getByText('El plan es de tu cuenta: incluye todos los negocios que quieras.'),
  ).toBeVisible();
}

function entoncesSeCerroElDialogoYElFocoVolvioAContratar() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(screen.getByRole('button', { name: 'Contratar Premium' })).toHaveFocus();
}

async function entoncesSePrepararonUnaVezConCsrfYSinCuerpo() {
  await screen.findByRole('link', { name: 'Continuar en Mercado Pago' }, ESPERA);
  expect(intentos).toBe(1);
  expect(tokenCsrf).toBeTruthy();
  expect(cuerpoEnviado).toBe('');
}

async function entoncesSeOfreceContinuarEnMercadoPagoSinDarAccesoPago() {
  const continuar = await screen.findByRole('link', { name: 'Continuar en Mercado Pago' }, ESPERA);
  expect(continuar).toHaveAttribute('href', ENLACE_DE_MERCADO_PAGO);
  expect(screen.getByRole('heading', { name: 'Gratuito, tu espacio actual' })).toBeVisible();
  expect(estado.pruebasPremium.size).toBe(0);
}

async function entoncesSeEsperaElPagoYNoSePresentaComoCobrado() {
  expect(await screen.findByText('Renovación automática autorizada.', {}, ESPERA)).toBeVisible();
  expect(screen.getByText(/El acceso pago se habilita al confirmar/)).toBeVisible();
  expect(screen.getByRole('heading', { name: 'Gratuito, tu espacio actual' })).toBeVisible();
}

function entoncesSePideConfirmacionYTodaviaNoSeCancelo() {
  const dialogo = screen.getByRole('dialog', { name: 'Cancelar renovación automática' });
  expect(within(dialogo).getByText(/período ya pagado/)).toBeVisible();
  expect(
    within(dialogo).getByText(/Tus negocios, secciones y registros se mantienen/),
  ).toBeVisible();
  expect(cancelaciones).toBe(0);
}

async function entoncesLaRenovacionQuedaCanceladaYElPeriodoPagoSeConserva() {
  expect(
    await screen.findByText('La renovación automática está cancelada.', {}, ESPERA),
  ).toBeVisible();
  expect(screen.getByRole('heading', { name: 'Premium, tu espacio actual' })).toBeVisible();
  expect(screen.getByText(/Tu período pago llega hasta/)).toBeVisible();
  expect(cancelaciones).toBe(1);
}

async function entoncesLaCancelacionQuedaPendiente() {
  await screen.findByRole('alert', {}, ESPERA);
  expect(screen.getByText('Cancelación pendiente de confirmación.')).toBeVisible();
  expect(screen.queryByText('La renovación automática está cancelada.')).toBeNull();
}

async function entoncesSeOfreceReintentarYNoSeOfreceContratar() {
  expect(
    await screen.findByRole('button', { name: 'Reintentar renovación' }, ESPERA),
  ).toBeVisible();
  expect(screen.queryByRole('button', { name: 'Contratar Premium' })).toBeNull();
}

async function entoncesNoHayViolacionesDeAccesibilidad() {
  const resultado = await axe(document.body, {
    rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
  });
  expect(resultado.violations.map((item) => item.id)).toEqual([]);
}
