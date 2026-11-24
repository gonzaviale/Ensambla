import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { PruebaPremium } from '@/types/planes';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;

const OPCIONES_DE_ACCESIBILIDAD = {
  rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
};

let user: Usuario;
let usuario: UsuarioDePrueba;
let router: Router;
let intentos = 0;
let tokenCsrf: string | null = null;
let cuerpoEnviado = '';

const pruebaDeEjemplo = (paginas = 3, vencida = false): PruebaPremium => {
  const ahora = Date.now();
  return {
    inicioEn: new Date(ahora - (vencida ? 8 : 2) * 86400000).toISOString(),
    finEn: new Date(ahora + (vencida ? -1 : 5) * 86400000).toISOString(),
    paginasIaUtilizadas: paginas,
    limitePaginasIa: 10,
  };
};

describe('planes y activación de prueba', () => {
  it('compara los límites de cada plan', async () => {
    await dadaLaPantallaDePlanes();

    entoncesCadaPlanMuestraSusLimites();
  });

  it('el plan Básico muestra su precio en cero por mes', async () => {
    await dadaLaPantallaDePlanes();

    entoncesElPlanBasicoMuestraSuPrecioEnCero();
  });

  it('cada plan dice si incluye o no cada función, como en el resto de las filas', async () => {
    await dadaLaPantallaDePlanes();

    entoncesCadaPlanDiceQueIncluyeOQueNoIncluye();
  });

  it('muestra el precio aprobado cada treinta días y ofrece la prueba', async () => {
    await dadaLaPantallaDePlanes();

    entoncesPremiumMuestraSuPrecioYSeOfreceLaPrueba();
  });

  it('dice cuántos negocios deja tener cada plan y que el plan es de la cuenta', async () => {
    await dadaLaPantallaDePlanes();

    entoncesSeDiceCuantosNegociosDejaCadaPlan();
  });

  it('se puede entrar desde la navegación principal', async () => {
    dadoUnUsuarioEnElInicio();

    await cuandoSeEligePlanesEnLaNavegacion();

    entoncesSeAbrenLosPlanesDesdeLaNavegacion();
  });

  it('exige sesión antes de mostrar datos de planes', async () => {
    router = renderApp('/planes');

    await screen.findByRole('heading', { name: 'Entrá a tu cuenta' }, ESPERA);

    expect(router.state.location.pathname).toBe('/login');
  });

  it('permite cancelar con Escape y devuelve el foco', async () => {
    await dadaLaPantallaDePlanes();

    await cuandoSeAbreLaConfirmacionYSeCancelaConEscape();

    entoncesSeCerroElDialogoYElFocoVolvioAlBoton();
  });

  it('cancelar con Escape no activa la prueba', async () => {
    await dadaLaPantallaDePlanes();

    await cuandoSeAbreLaConfirmacionYSeCancelaConEscape();

    expect(estado.pruebasPremium.size).toBe(0);
  });

  it('activa la prueba una vez con CSRF aunque se haga doble clic', async () => {
    await dadaLaPantallaDePlanesConUnServidorQueActivaLaPrueba();

    await cuandoSeActivaLaPruebaConDobleClic();

    entoncesSeActivoUnaSolaVezConCsrfYSinCuerpo();
  });

  it('al activar la prueba se refresca el estado', async () => {
    await dadaLaPantallaDePlanesConUnServidorQueActivaLaPrueba();

    await cuandoSeActivaLaPruebaConDobleClic();

    entoncesElEstadoMuestraLaPruebaActiva();
  });

  it('al agotar diez páginas conserva premium y muestra el aviso del cupo total', async () => {
    dadoUnUsuarioConUnaPruebaDe(10);

    cuandoSeAbrenLosPlanes();

    await entoncesSeMuestraElCupoTotalAgotado();
  });

  it('muestra la prueba finalizada, conserva los datos y no ofrece reiniciarla', async () => {
    dadoUnUsuarioConUnaPruebaFinalizada();

    cuandoSeAbrenLosPlanes();

    await entoncesSeMuestraLaPruebaFinalizada();
  });

  it('muestra premium vigente con cien páginas y sin ofrecer otra prueba', async () => {
    dadoUnUsuarioPremium();

    cuandoSeAbrenLosPlanes();

    await entoncesSeMuestraPremiumVigenteConCienPaginas();
  });

  it('un error inicial no muestra un plan inventado', async () => {
    dadoUnServidorDePlanesCaido();

    cuandoSeAbrenLosPlanes();

    await entoncesSeMuestraElErrorSinPlanes();
  });

  it('un error inicial permite reintentar', async () => {
    dadoUnServidorDePlanesCaido();
    cuandoSeAbrenLosPlanes();
    await screen.findByRole('alert', {}, ESPERA);

    await cuandoSeReintentaConElServidorRecuperado();

    await esperarPlanBasico();
  });

  it('una activación rechazada no habilita premium', async () => {
    await dadaLaPantallaDePlanesConUnServidorQueRechazaLaPrueba();

    await cuandoSeIntentaActivarLaPrueba();

    entoncesNoSeHabilitoPremium();
  });

  it('una activación rechazada no se reintenta como CSRF', async () => {
    await dadaLaPantallaDePlanesConUnServidorQueRechazaLaPrueba();

    await cuandoSeIntentaActivarLaPrueba();

    expect(intentos).toBe(1);
  });

  it('la página no introduce problemas de accesibilidad', async () => {
    await dadaLaPantallaDePlanes();

    await entoncesNoHayViolacionesDeAccesibilidad();
  });

  it('la confirmación no introduce problemas de accesibilidad', async () => {
    await dadaLaPantallaDePlanes();
    await user.click(botonPrueba());

    await entoncesNoHayViolacionesDeAccesibilidad();
  });
});

const esperarPlanBasico = () =>
  screen.findByRole('heading', { name: 'Básico, tu espacio actual' }, ESPERA);
const esperarPrueba = () =>
  screen.findByRole('heading', { name: 'Tu prueba Premium está activa' }, ESPERA);
const botonPrueba = () => screen.getByRole('button', { name: 'Probar Premium por 7 días' });

async function dadaLaPantallaDePlanes() {
  usuario = setupUsuarioConSesion();
  router = renderApp('/planes');
  await esperarPlanBasico();
  user = userEvent.setup();
}

async function dadaLaPantallaDePlanesConUnServidorQueActivaLaPrueba() {
  usuario = setupUsuarioConSesion();
  intentos = 0;
  server.use(
    http.post('/api/planes/prueba', async ({ request }) => {
      intentos++;
      tokenCsrf = request.headers.get('X-CSRF-TOKEN');
      cuerpoEnviado = await request.text();
      await delay(180);
      const prueba = pruebaDeEjemplo(0);
      estado.pruebasPremium.set(usuario.email, prueba);
      return HttpResponse.json(prueba);
    }),
  );
  router = renderApp('/planes');
  await esperarPlanBasico();
  user = userEvent.setup();
}

async function dadaLaPantallaDePlanesConUnServidorQueRechazaLaPrueba() {
  usuario = setupUsuarioConSesion();
  intentos = 0;
  server.use(
    http.post('/api/planes/prueba', () => {
      intentos++;
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 403,
          error: 'Forbidden',
          message: 'Esta cuenta ya utilizó su prueba premium.',
          path: '/api/planes/prueba',
          traceId: 'rechazo',
          codigo: 'PRUEBA_UTILIZADA',
        },
        { status: 403 },
      );
    }),
  );
  router = renderApp('/planes');
  await esperarPlanBasico();
  user = userEvent.setup();
}

function dadoUnUsuarioEnElInicio() {
  usuario = setupUsuarioConSesion();
  router = renderApp('/inicio');
  user = userEvent.setup();
}

function dadoUnUsuarioConUnaPruebaDe(paginas: number) {
  usuario = setupUsuarioConSesion();
  estado.pruebasPremium.set(usuario.email, pruebaDeEjemplo(paginas));
}

function dadoUnUsuarioConUnaPruebaFinalizada() {
  usuario = setupUsuarioConSesion();
  estado.pruebasPremium.set(usuario.email, pruebaDeEjemplo(10, true));
}

function dadoUnUsuarioPremium() {
  usuario = setupUsuarioConSesion();
  const datos = planesDe(usuario.email);
  datos.suscripcion = {
    ...datos.suscripcion,
    codigoPlanActual: 'PREMIUM',
    puedeIniciarPrueba: false,
  };
  datos.uso.limitePaginasIa = 100;
  server.use(http.get('/api/planes', () => HttpResponse.json(datos)));
}

function dadoUnServidorDePlanesCaido() {
  usuario = setupUsuarioConSesion();
  setupServerError('get', '/planes', 503, 'No pudimos consultar los planes.');
}

function cuandoSeAbrenLosPlanes() {
  router = renderApp('/planes');
  user = userEvent.setup();
}

async function cuandoSeEligePlanesEnLaNavegacion() {
  await user.click(await screen.findByRole('button', { name: 'Planes' }, ESPERA));
  await esperarPlanBasico();
}

async function cuandoSeAbreLaConfirmacionYSeCancelaConEscape() {
  await user.click(botonPrueba());
  const dialogo = screen.getByRole('dialog', { name: 'Probá Premium durante 7 días' });
  expect(within(dialogo).getByText(/10 análisis de foto en total/)).toBeVisible();
  await user.keyboard('{Escape}');
}

async function cuandoSeActivaLaPruebaConDobleClic() {
  await user.click(botonPrueba());
  await user.dblClick(screen.getByRole('button', { name: 'Activar prueba gratis' }));
  await esperarPrueba();
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull(), ESPERA);
}

async function cuandoSeIntentaActivarLaPrueba() {
  await user.click(botonPrueba());
  await user.click(screen.getByRole('button', { name: 'Activar prueba gratis' }));
  expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(/ya utilizó su prueba/);
}

async function cuandoSeReintentaConElServidorRecuperado() {
  server.use(http.get('/api/planes', () => HttpResponse.json(planesDe(usuario.email))));
  await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }));
}

function entoncesCadaPlanMuestraSusLimites() {
  const premium = screen.getByRole('article', { name: 'Plan Premium' });
  const basico = screen.getByRole('article', { name: 'Plan Básico' });
  expect(within(basico).getByText('1.000')).toBeVisible();
  expect(within(premium).getByText('100')).toBeVisible();
}

function entoncesElPlanBasicoMuestraSuPrecioEnCero() {
  const basico = screen.getByRole('article', { name: 'Plan Básico' });
  expect(within(basico).getByText('$0')).toBeVisible();
  expect(within(basico).getByText('Por mes · sin vencimiento')).toBeVisible();
}

function entoncesCadaPlanDiceQueIncluyeOQueNoIncluye() {
  const basico = within(screen.getByRole('article', { name: 'Plan Básico' }));
  const premium = within(screen.getByRole('article', { name: 'Plan Premium' }));
  expect(valorDeLaFila(basico, 'Carga manual, edición y consulta')).toHaveTextContent('Incluido');
  expect(valorDeLaFila(basico, 'Resúmenes en PDF')).toHaveTextContent('No incluido');
  expect(valorDeLaFila(premium, 'Resúmenes en PDF')).toHaveTextContent('Incluido');
}

function valorDeLaFila(plan: ReturnType<typeof within>, titulo: string) {
  return plan.getByText(titulo).nextElementSibling;
}

function entoncesPremiumMuestraSuPrecioYSeOfreceLaPrueba() {
  const premium = screen.getByRole('article', { name: 'Plan Premium' });
  expect(within(premium).getByText(/14\.900/)).toBeVisible();
  expect(within(premium).getByText('Cada 30 días · renovación automática')).toBeVisible();
  expect(botonPrueba()).toBeEnabled();
  expect(document.title).toBe('Planes · Ensambla');
}

function entoncesSeDiceCuantosNegociosDejaCadaPlan() {
  const premium = screen.getByRole('article', { name: 'Plan Premium' });
  const basico = screen.getByRole('article', { name: 'Plan Básico' });
  expect(within(basico).getByText('1 negocio')).toBeVisible();
  expect(within(premium).getAllByText('Ilimitado')[0]).toBeVisible();
  expect(screen.getByText('El plan es de tu cuenta y vale para todos tus negocios.')).toBeVisible();
}

function entoncesSeAbrenLosPlanesDesdeLaNavegacion() {
  expect(router.state.location.pathname).toBe('/planes');
  expect(screen.getByRole('button', { name: 'Planes' })).toHaveAttribute('aria-current', 'page');
}

function entoncesSeCerroElDialogoYElFocoVolvioAlBoton() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(botonPrueba()).toHaveFocus();
}

function entoncesSeActivoUnaSolaVezConCsrfYSinCuerpo() {
  expect(intentos).toBe(1);
  expect(tokenCsrf).toBe('csrf-token-de-prueba');
  expect(cuerpoEnviado).toBe('');
}

function entoncesElEstadoMuestraLaPruebaActiva() {
  expect(screen.getByRole('button', { name: 'Prueba activa' })).toBeDisabled();
  expect(screen.getByText(/Los 10 análisis de foto valen para toda la prueba/)).toBeVisible();
  expect(screen.getByRole('main')).toHaveFocus();
}

async function entoncesSeMuestraElCupoTotalAgotado() {
  await esperarPrueba();
  expect(screen.getByText(/Usaste los análisis de foto incluidos/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Prueba activa' })).toBeDisabled();
  expect(screen.getByRole('progressbar', { name: 'Análisis de foto de la prueba' })).toHaveValue(
    10,
  );
}

async function entoncesSeMuestraLaPruebaFinalizada() {
  await esperarPlanBasico();
  expect(screen.getByText(/Tu prueba terminó/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Prueba ya utilizada' })).toBeDisabled();
  expect(screen.getByText(/conservás tus secciones, tus datos y tus recetas/)).toBeVisible();
}

async function entoncesSeMuestraPremiumVigenteConCienPaginas() {
  await screen.findByRole('heading', { name: 'Premium, tu espacio actual' }, ESPERA);
  expect(screen.getByRole('button', { name: 'Premium activo' })).toBeDisabled();
  expect(screen.getByRole('progressbar', { name: 'Análisis de foto de este mes' })).toHaveAttribute(
    'max',
    '100',
  );
}

async function entoncesSeMuestraElErrorSinPlanes() {
  expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
    'No pudimos consultar los planes.',
  );
  expect(screen.queryByRole('article', { name: 'Plan Premium' })).toBeNull();
}

function entoncesNoSeHabilitoPremium() {
  expect(screen.queryByRole('heading', { name: 'Tu prueba Premium está activa' })).toBeNull();
  expect(estado.pruebasPremium.size).toBe(0);
}

async function entoncesNoHayViolacionesDeAccesibilidad() {
  const resultado = await axe(document.body, OPCIONES_DE_ACCESIBILIDAD);
  expect(resultado.violations.map((item) => item.id)).toEqual([]);
}
