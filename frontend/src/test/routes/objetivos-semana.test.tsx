import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { estado, guardarObjetivosDe } from '@/mocks/db/estado';
import { ID_OBJETIVO_DE_EJEMPLO } from '@/lib/objetivos';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupObjetivoDeEjemplo,
  setupServerError,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;

const DETALLE = `/objetivos/${ID_OBJETIVO_DE_EJEMPLO}`;

let router: Router;
let user: Usuario;
let mediciones = 0;
let lecturasDeMinis = 0;
let lecturasAlAbrir = 0;

function contarLecturaDeMinis({ request }: { request: Request }) {
  if (request.url.endsWith(`/objetivos/${ID_OBJETIVO_DE_EJEMPLO}/mini-objetivos`))
    lecturasDeMinis++;
}

describe('la semana de un objetivo que se autogestiona', () => {
  afterEach(() => {
    server.events.removeListener('request:start', contarLecturaDeMinis);
  });

  it('con seis semanas simuladas muestra los 3 minis', async () => {
    dadoUnObjetivoDeEjemplo();

    cuandoSeAbreElDetalle();

    await entoncesSeMuestranLosTresMinis();
  });

  it('el gráfico muestra la meta y las decisiones de cada semana', async () => {
    dadoUnObjetivoDeEjemplo();

    cuandoSeAbreElDetalle();

    await entoncesElGraficoMuestraLaMetaYLasDecisiones();
  });

  it('el recorrido se abre entero y vuelve a las últimas semanas', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreElDetalle();

    await cuandoSePideVerTodasLasSemanasYDespuesSoloLasUltimas();

    entoncesElRecorridoVuelveAlPrincipio();
  });

  it('muestra los reajustes de Sami con su motivo', async () => {
    dadoUnObjetivoDeEjemplo();

    cuandoSeAbreElDetalle();

    await entoncesSeMuestranLosReajustesConSuMotivo();
  });

  it('avisa sin molestar que el resumen del domingo está listo y no lo repite una vez visto', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreElDetalle();

    await cuandoSeMiraElResumenDelDomingo();

    entoncesElAvisoDesapareceYElFocoVaALaSemana();
  });

  it('desde la tarjeta del objetivo se llega a su semana, con el chip de resumen listo', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreLaListaDeObjetivos();

    await cuandoSeEligeVerLaSemana();

    await entoncesSeLlegaALaSemanaYElResumenSeMarcaComoVisto();
  });

  it('«Lo hago» queda anotado como decisión de la semana', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreElDetalle();

    await cuandoSeDiceLoHagoEnElPrimerMini();

    entoncesLaDecisionQuedaAnotada('PASO_ACEPTADO', 'Subile $250 a Yerba 1 kg');
  });

  it('lo que dejó para después se restaura', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreElDetalle();

    await cuandoSeDejaParaDespuesYSeRestauraElPrimerMini();

    entoncesNoSePuedeCambiarNiQuitar();
  });

  it('lo que dejó para después puede volver a decir «Lo hago»', async () => {
    dadoUnObjetivoDeEjemplo();
    cuandoSeAbreElDetalle();
    await cuandoSeDejaParaDespuesYSeRestauraElPrimerMini();

    await cuandoSeDiceLoHagoEnElPrimerMini();

    entoncesLasUltimasDecisionesSon('PASO_IGNORADO', 'PASO_ACEPTADO');
  });

  it('si el servidor no trae los mini-objetivos lo explica sin romper la pantalla', async () => {
    dadoUnObjetivoDeEjemplo();
    setupServerError('get', `/objetivos/${ID_OBJETIVO_DE_EJEMPLO}/mini-objetivos`, 503, 'Caído');

    cuandoSeAbreElDetalle();

    expect(
      await screen.findByText(/No pudimos traer tus mini-objetivos/, {}, ESPERA),
    ).toBeVisible();
  });

  it('el objetivo de otra cuenta no se muestra', async () => {
    dadoUnObjetivoDeOtraCuenta();

    cuandoSeAbreElObjetivoAjeno();

    expect(
      await screen.findByRole('heading', { name: 'No encontramos ese objetivo' }, ESPERA),
    ).toBeVisible();
  });

  it('al abrir la semana mide el negocio y trae los mini-objetivos', async () => {
    dadoUnObjetivoDeEjemplo();
    dadoUnServidorQueCuentaMedicionesYLecturas();

    cuandoSeAbreElDetalle();

    await entoncesSeMidioUnaVezYSeTrajeronLosMinis();
  });

  it('al volver a la pestaña mide de nuevo y trae los mini-objetivos de nuevo', async () => {
    dadoUnObjetivoDeEjemplo();
    dadoUnServidorQueCuentaMedicionesYLecturas();
    cuandoSeAbreElDetalle();
    await entoncesSeMidioUnaVezYSeTrajeronLosMinis();

    cuandoSeVuelveALaPestana();

    await entoncesSeMidioDosVecesYSeTrajeronLosMinisDeNuevo();
  });

  it('si medir falla, igual muestra lo último que midió Sami', async () => {
    dadoUnObjetivoDeEjemplo();
    setupServerError('post', '/objetivos/medir', 503, 'Caído');

    cuandoSeAbreElDetalle();

    await entoncesSeMuestranLosTresMinis();
  });
});

// --- dado ---

function dadoUnObjetivoDeEjemplo() {
  const usuario = setupUsuarioConSesion();
  setupObjetivoDeEjemplo(usuario.email);
  user = userEvent.setup();
}

function dadoUnObjetivoDeOtraCuenta() {
  setupUsuarioConSesion();
  guardarObjetivosDe('otra@ensambla.test', [
    {
      id: 'ajeno',
      texto: 'Ajeno',
      tipo: 'MONETARIO',
      meta: 100000,
      armadoConSami: false,
      creadoEn: '2026-10-01T12:00:00',
    },
  ]);
}

function dadoUnServidorQueCuentaMedicionesYLecturas() {
  mediciones = 0;
  lecturasDeMinis = 0;
  server.events.on('request:start', contarLecturaDeMinis);
  server.use(
    http.post('/api/objetivos/medir', () => {
      mediciones++;
      return new HttpResponse(null, { status: 204 });
    }),
  );
}

// --- cuando ---

function cuandoSeAbreElDetalle() {
  router = renderApp(DETALLE);
}

function cuandoSeAbreLaListaDeObjetivos() {
  router = renderApp('/objetivos');
}

function cuandoSeAbreElObjetivoAjeno() {
  renderApp('/objetivos/ajeno');
}

async function cuandoSePideVerTodasLasSemanasYDespuesSoloLasUltimas() {
  const semana = within(await laSemana());
  await semana.findByRole('img', { name: /meta de \$500\.000/ }, ESPERA);
  await user.click(semana.getByRole('button', { name: 'Ver todas las semanas' }));
  screen.getByRole('region', { name: 'Recorrido de las semanas' }).scrollTop = 200;
  await user.click(semana.getByRole('button', { name: 'Ver solo las últimas' }));
}

async function cuandoSeMiraElResumenDelDomingo() {
  await screen.findByText('Tu resumen de la semana está listo.', {}, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Ver resumen' }));
}

async function cuandoSeEligeVerLaSemana() {
  await screen.findByText('Resumen de la semana listo', {}, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Ver mi semana' }));
}

const primerMini = async () =>
  within((await (await minis()).findAllByRole('article'))[0] as HTMLElement);

async function cuandoSeDiceLoHagoEnElPrimerMini() {
  const tarjeta = await primerMini();
  await user.click(tarjeta.getByRole('button', { name: 'Lo hago' }));
  await tarjeta.findByText('Esta semana dijiste que lo hacés', {}, ESPERA);
}

async function cuandoSeDejaParaDespuesYSeRestauraElPrimerMini() {
  const tarjeta = await primerMini();
  await user.click(tarjeta.getByRole('button', { name: 'Dejarlo para después' }));
  const restaurar = /^Restaurar «.*» para volver a decidir$/;
  await user.click(await tarjeta.findByRole('button', { name: restaurar }, ESPERA));
}

function cuandoSeVuelveALaPestana() {
  fireEvent(window, new Event('focus'));
}

// --- entonces ---

const minis = async () =>
  within(await screen.findByRole('region', { name: 'Tus mini-objetivos' }, ESPERA));

const laSemana = async () => {
  await minis();
  return screen.getByRole('region', { name: 'Cómo te fue cada semana' });
};

async function entoncesSeMuestranLosTresMinis() {
  const lista = await minis();
  expect(await lista.findAllByRole('article')).toHaveLength(3);
}

async function entoncesElGraficoMuestraLaMetaYLasDecisiones() {
  const lista = await minis();
  await lista.findAllByRole('article');
  expect(
    screen.getByText(/Si cumplís estos mini-objetivos sumás cerca de \$[\d.]+ por mes/),
  ).toBeVisible();
  expect(lista.getByRole('heading', { name: 'Subile $250 a Yerba 1 kg' })).toBeVisible();
  expect(lista.getByRole('heading', { name: 'Volvé a vender 6 Fernet por semana' })).toBeVisible();
  const semana = within(screen.getByRole('region', { name: 'Cómo te fue cada semana' }));
  expect(
    await semana.findByRole('img', { name: /meta de \$500\.000/ }, ESPERA),
  ).toBeInTheDocument();
  expect(within(semana.getByRole('table')).getAllByRole('row')).toHaveLength(8);
  expect(
    semana.getByText(/Aplicaste: Subí el precio de Yerba 1 kg/, { selector: 'strong' }),
  ).toBeVisible();
  expect(semana.getByText(/Después vendiste \$4\.500 más por semana/)).toBeVisible();
  expect(semana.getByText(/Te faltan \$295\.000 para tu meta/)).toBeVisible();
}

function entoncesElRecorridoVuelveAlPrincipio() {
  expect(screen.getByRole('region', { name: 'Recorrido de las semanas' }).scrollTop).toBe(0);
}

async function entoncesSeMuestranLosReajustesConSuMotivo() {
  const cambios = within(await screen.findByRole('region', { name: 'Qué cambió Sami' }, ESPERA));
  expect(
    await cambios.findByText(/Ahora el objetivo es \$16\.000 por semana/, {}, ESPERA),
  ).toBeVisible();
  expect(
    cambios.getByText(/no avanza\. Lo cambiamos por «Volvé a vender 6 Fernet por semana»/),
  ).toBeVisible();
  expect(screen.queryByText(/Camino combinado/)).not.toBeInTheDocument();
}

function entoncesElAvisoDesapareceYElFocoVaALaSemana() {
  expect(screen.queryByText('Tu resumen de la semana está listo.')).not.toBeInTheDocument();
  expect(screen.getByRole('region', { name: 'Cómo te fue cada semana' })).toHaveFocus();
}

async function entoncesSeLlegaALaSemanaYElResumenSeMarcaComoVisto() {
  await waitFor(() => expect(router.state.location.pathname).toBe(DETALLE));
  await user.click(await screen.findByRole('button', { name: 'Listo' }, ESPERA));
  expect(screen.queryByText('Tu resumen de la semana está listo.')).not.toBeInTheDocument();
}

function entoncesLaDecisionQuedaAnotada(tipo: string, descripcion: string) {
  const decisiones = estado.autogestion.get(ID_OBJETIVO_DE_EJEMPLO)?.decisiones ?? [];
  expect(decisiones.at(-1)).toMatchObject({ tipo, descripcion });
}

function entoncesNoSePuedeCambiarNiQuitar() {
  expect(screen.queryByRole('button', { name: /Cambiar|Quitar/ })).not.toBeInTheDocument();
}

function entoncesLasUltimasDecisionesSon(...tipos: string[]) {
  const decisiones = estado.autogestion.get(ID_OBJETIVO_DE_EJEMPLO)?.decisiones ?? [];
  expect(decisiones.slice(-tipos.length).map((decision) => decision.tipo)).toEqual(tipos);
}

async function entoncesSeMidioUnaVezYSeTrajeronLosMinis() {
  await minis();
  await waitFor(() => expect(mediciones).toBe(1), ESPERA);
  await waitFor(() => expect(lecturasDeMinis).toBeGreaterThan(0), ESPERA);
  lecturasAlAbrir = lecturasDeMinis;
}

async function entoncesSeMidioDosVecesYSeTrajeronLosMinisDeNuevo() {
  await waitFor(() => expect(mediciones).toBe(2), ESPERA);
  await waitFor(() => expect(lecturasDeMinis).toBeGreaterThan(lecturasAlAbrir), ESPERA);
}
