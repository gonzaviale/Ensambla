import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { objetivosDe } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupObjetivoDeEjemplo,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;

const AVISO_DE_META_LEJANA = 'Esta meta no se alcanza con lo que vendés hoy';
const META_EN_PESOS = 'Quiero poner una meta en pesos por mes';

let router: Router;
let user: Usuario;
let usuario: UsuarioDePrueba;

describe('objetivos', () => {
  it('se abre desde la navegación principal', async () => {
    dadoUnUsuarioEnElInicio();

    await cuandoSeEligeObjetivosEnLaNavegacion();

    entoncesSeAbreObjetivosYQuedaMarcado();
  });

  it('ya no hay un laboratorio', async () => {
    dadoUnUsuarioEnElInicio();

    await cuandoSeEligeObjetivosEnLaNavegacion();

    entoncesNoHayLaboratorio();
  });

  it('sin objetivos propone definir uno', async () => {
    dadoUnUsuarioSinObjetivos();

    cuandoSeAbreObjetivos();

    await entoncesSePropone('Definí tu primer objetivo');
  });

  it('un objetivo definido se muestra con lo que falta, sin porcentajes', async () => {
    dadoUnUsuarioSinObjetivos();
    cuandoSeAbreObjetivos();

    await cuandoSeDefineLaMetaDe400Mil();

    await entoncesSeMuestraConLoQueFaltaSinPorcentajes();
  });

  it('todo está en una pantalla: sin pestañas ni saltos a otras páginas', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();

    cuandoSeAbreObjetivos();

    await entoncesTodoEstaEnUnaPantalla();
  });

  it('sin ventas no muestra números de ejemplo', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();

    cuandoSeAbreObjetivos();

    await entoncesNoSeMuestranNumerosDeEjemplo();
  });

  it('con ventas muestra cómo viene el negocio y el avance de las semanas reales', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemploYVentasReales();

    cuandoSeAbreObjetivos();

    await entoncesSeMuestraComoVieneElNegocioYElAvance();
  });

  it('la tarjeta no repite qué hacer: lo muestra el seguimiento de la semana', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();
    cuandoSeAbreObjetivos();

    await cuandoSeEligeVerLaSemana();

    await entoncesLaTarjetaNoRepiteQueHacerYSeAbreLaSemana();
  });

  it('un objetivo que no existe invita a volver', async () => {
    dadoUnUsuarioSinObjetivos();

    cuandoSeAbreElObjetivo('/objetivos/no-existe');

    await entoncesSeInvitaAVolverAMisObjetivos();
  });

  it('pide confirmación antes de quitar un objetivo', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();
    cuandoSeAbreObjetivos();

    await cuandoSePideQuitarElObjetivo();

    await entoncesSePideConfirmacionAntesDeQuitar();
  });

  it('al confirmar se quita el objetivo', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();
    cuandoSeAbreObjetivos();
    await cuandoSePideQuitarElObjetivo();

    await cuandoSeConfirmaQuitarElObjetivo();

    await entoncesElObjetivoSeQuito();
  });

  it('un objetivo definido queda guardado en el servidor de la cuenta, con su texto y su meta', async () => {
    dadoUnUsuarioSinObjetivos();
    cuandoSeAbreObjetivos();

    await cuandoSeDefineLaMetaDe400Mil();

    await entoncesQuedaGuardadoElObjetivoDe400Mil();
  });

  it('muestra qué entendió Sami y pide escribir la meta si no la encuentra', async () => {
    dadoUnUsuarioSinObjetivos();
    cuandoSeAbreObjetivos();

    await cuandoSeEscribeUnObjetivoSinMeta();

    entoncesSamiEntendioQueQuiereCubrirGastosYNoSePuedeDefinir();
  });

  it('una meta escrita después de la lectura de Sami se guarda', async () => {
    dadoUnUsuarioSinObjetivos();
    cuandoSeAbreObjetivos();
    await cuandoSeEscribeUnObjetivoSinMeta();

    await cuandoSeEscribeLaMetaYSeDefine('250000');

    await entoncesQuedaGuardadaLaMeta(250_000);
  });

  it('si la meta no se alcanza con lo que vende hoy, lo avisa y propone un primer escalón', async () => {
    dadoUnUsuarioQueVendeUnPocoAlMes();
    cuandoSeAbreObjetivos();

    await cuandoSeEscribeUnObjetivoDeUnMillon();

    await entoncesSeAvisaQueLaMetaNoSeAlcanzaYSePropone36Mil();
  });

  it('aceptar el primer escalón lo guarda como meta', async () => {
    dadoUnUsuarioQueVendeUnPocoAlMes();
    cuandoSeAbreObjetivos();
    await cuandoSeEscribeUnObjetivoDeUnMillon();

    await cuandoSeAceptaElPrimerEscalon();

    await entoncesQuedaGuardadaLaMeta(36_000);
  });

  it('un objetivo guardado se ve al volver a abrir la pantalla', async () => {
    dadoUnUsuarioConUnObjetivoDeEjemplo();

    cuandoSeAbreObjetivos();

    await entoncesSeVeElObjetivoGuardado();
  });

  it('no muestra los objetivos de otra cuenta', async () => {
    dadoUnObjetivoDeOtraCuenta();

    cuandoSeAbreObjetivos();

    await entoncesSePropone('Definí tu primer objetivo');
  });

  it('avisa si el servidor rechaza el objetivo y no lo da por definido', async () => {
    dadoUnServidorQueRechazaElObjetivo();
    cuandoSeAbreObjetivos();

    await cuandoSeDefineLaMetaDe400Mil();

    await entoncesSeAvisaElRechazoSinDarloPorDefinido();
  });
});

function dadoUnUsuarioEnElInicio() {
  usuario = setupUsuarioConSesion();
  router = renderApp('/inicio');
  user = userEvent.setup();
}

function dadoUnUsuarioSinObjetivos() {
  usuario = setupUsuarioConSesion();
}

function dadoUnUsuarioConUnObjetivoDeEjemplo() {
  usuario = setupUsuarioConSesion();
  setupObjetivoDeEjemplo(usuario.email);
}

function dadoUnUsuarioConUnObjetivoDeEjemploYVentasReales() {
  dadoUnUsuarioConUnObjetivoDeEjemplo();
  setupServerResponse('get', '/objetivos/negocio', 200, {
    hayVentas: true,
    vendiste: 400_000,
    mercaderia: 250_000,
    gastos: 30_000,
    queda: 120_000,
    teDeben: 15_000,
    semanas: [
      { semana: 'Hace 2 sem.', queda: 80_000 },
      { semana: 'Hace 1 sem.', queda: 100_000 },
      { semana: 'Hoy', queda: 120_000 },
    ],
  });
}

function dadoUnUsuarioQueVendeUnPocoAlMes() {
  usuario = setupUsuarioConSesion();
  setupServerResponse('get', '/objetivos/negocio', 200, {
    hayVentas: true,
    vendiste: 83_000,
    mercaderia: 55_000,
    gastos: 0,
    queda: 28_000,
    teDeben: 0,
    semanas: [{ semana: 'Hoy', queda: 28_000 }],
  });
}

function dadoUnObjetivoDeOtraCuenta() {
  setupObjetivoDeEjemplo('otra@example.com');
  usuario = setupUsuarioConSesion();
}

function dadoUnServidorQueRechazaElObjetivo() {
  usuario = setupUsuarioConSesion();
  setupServerError('post', '/objetivos', 400, 'Revisá los datos del objetivo.', {
    texto: 'El texto es obligatorio',
  });
}

function cuandoSeAbreObjetivos() {
  router = renderApp('/objetivos');
  user = userEvent.setup();
}

function cuandoSeAbreElObjetivo(ruta: string) {
  router = renderApp(ruta);
}

async function cuandoSeEligeObjetivosEnLaNavegacion() {
  await user.click(await screen.findByRole('button', { name: 'Objetivos' }, ESPERA));
  await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);
}

async function cuandoSeAbreElFormularioDeMetaEnPesos() {
  await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);
  await user.click(await screen.findByRole('button', { name: META_EN_PESOS }, ESPERA));
}

async function cuandoSeDefineLaMetaDe400Mil() {
  await cuandoSeAbreElFormularioDeMetaEnPesos();
  expect(screen.getByRole('button', { name: 'Definir objetivo' })).toBeDisabled();
  await user.click(await screen.findByRole('button', { name: /Quiero ganar \$400\.000/ }, ESPERA));
  await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));
}

async function cuandoSeEligeVerLaSemana() {
  await user.click(await screen.findByRole('button', { name: /Ver mi semana/ }, ESPERA));
}

async function cuandoSePideQuitarElObjetivo() {
  await user.click(await screen.findByRole('button', { name: /Quitar objetivo/ }, ESPERA));
}

async function cuandoSeConfirmaQuitarElObjetivo() {
  const dialogo = await screen.findByRole('dialog');
  await user.click(within(dialogo).getByRole('button', { name: 'Quitar objetivo' }));
}

async function cuandoSeEscribeUnObjetivoSinMeta() {
  await user.click(await screen.findByRole('button', { name: META_EN_PESOS }, ESPERA));
  await user.type(
    await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA),
    'Quiero subir la yerba a $4.000 y recuperar mis gastos',
  );
}

async function cuandoSeEscribeLaMetaYSeDefine(meta: string) {
  const lectura = within(screen.getByRole('region', { name: 'Lo que entendió Sami' }));
  await user.type(lectura.getByLabelText(/Tu meta/), meta);
  await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));
}

async function cuandoSeEscribeUnObjetivoDeUnMillon() {
  await user.click(await screen.findByRole('button', { name: META_EN_PESOS }, ESPERA));
  await user.type(
    await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA),
    'Quiero ganar un millon al mes',
  );
}

async function cuandoSeAceptaElPrimerEscalon() {
  const aviso = await screen.findByRole('note', { name: AVISO_DE_META_LEJANA }, ESPERA);
  await user.click(within(aviso).getByRole('button', { name: 'Empezar por $36.000' }));
  expect(screen.getByLabelText(/Tu meta/)).toHaveValue('36.000');
  expect(screen.queryByRole('note', { name: AVISO_DE_META_LEJANA })).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));
}

function entoncesSeAbreObjetivosYQuedaMarcado() {
  expect(router.state.location.pathname).toBe('/objetivos');
  expect(screen.getByRole('button', { name: 'Objetivos' })).toHaveAttribute('aria-current', 'page');
}

function entoncesNoHayLaboratorio() {
  expect(screen.queryByRole('button', { name: 'Laboratorio' })).not.toBeInTheDocument();
  expect(screen.queryByText(/laboratorio/i)).not.toBeInTheDocument();
}

async function entoncesSePropone(titulo: string) {
  expect(await screen.findByRole('heading', { name: titulo }, ESPERA)).toBeVisible();
}

async function entoncesSeMuestraConLoQueFaltaSinPorcentajes() {
  expect(
    await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }),
  ).toBeVisible();
  expect(screen.getByText(/Te faltan \$400\.000/)).toBeVisible();
  expect(screen.getByText(/Todavía no hay semanas para comparar/)).toBeVisible();
  expect(screen.queryByText(/%/)).not.toBeInTheDocument();
}

async function entoncesTodoEstaEnUnaPantalla() {
  await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }, ESPERA);
  expect(screen.queryByRole('tab')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Ver cómo funcionaría' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Ver plan de acción' })).not.toBeInTheDocument();
  expect(router.state.location.pathname).toBe('/objetivos');
}

async function entoncesNoSeMuestranNumerosDeEjemplo() {
  await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }, ESPERA);
  expect(screen.getByText(/Todavía no hay semanas para comparar/)).toBeVisible();
  expect(screen.getByText(/Te faltan \$500\.000/)).toBeVisible();
  expect(screen.queryByRole('heading', { name: 'Estado de tu negocio' })).not.toBeInTheDocument();
}

async function entoncesSeMuestraComoVieneElNegocioYElAvance() {
  expect(
    await screen.findByRole('heading', { name: 'Estado de tu negocio' }, ESPERA),
  ).toBeVisible();
  expect(
    await screen.findByText(/Subió \$40\.000 en las últimas 2 semanas/, {}, ESPERA),
  ).toBeVisible();
  expect(screen.getByText(/Te faltan \$380\.000/)).toBeVisible();
  expect(screen.getByText(/Te deben \(fiado\)/)).toBeVisible();
}

async function entoncesLaTarjetaNoRepiteQueHacerYSeAbreLaSemana() {
  expect(screen.queryByRole('button', { name: /Qué podés hacer/ })).not.toBeInTheDocument();
  expect(screen.queryByText('Lo voy a hacer')).not.toBeInTheDocument();
  await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/objetivos\/.+/));
}

async function entoncesSeInvitaAVolverAMisObjetivos() {
  const titulo = await screen.findByRole(
    'heading',
    { name: 'No encontramos ese objetivo' },
    ESPERA,
  );
  expect(titulo).toBeVisible();
  expect(screen.getByRole('button', { name: 'Ir a mis objetivos' })).toBeVisible();
}

async function entoncesSePideConfirmacionAntesDeQuitar() {
  const dialogo = await screen.findByRole('dialog');
  expect(within(dialogo).getByText('¿Quitar este objetivo?')).toBeVisible();
}

async function entoncesElObjetivoSeQuito() {
  expect(await screen.findByRole('heading', { name: 'Definí tu primer objetivo' })).toBeVisible();
  await waitFor(() => expect(objetivosDe(usuario.email)).toHaveLength(0));
}

async function entoncesQuedaGuardadoElObjetivoDe400Mil() {
  await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }, ESPERA);
  expect(objetivosDe(usuario.email)).toMatchObject([
    { texto: expect.stringContaining('$400.000') as string, meta: 400_000 },
  ]);
}

function entoncesSamiEntendioQueQuiereCubrirGastosYNoSePuedeDefinir() {
  const lectura = within(screen.getByRole('region', { name: 'Lo que entendió Sami' }));
  expect(lectura.getByText(/Querés cubrir tus gastos/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Definir objetivo' })).toBeDisabled();
}

async function entoncesQuedaGuardadaLaMeta(meta: number) {
  await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }, ESPERA);
  expect(objetivosDe(usuario.email)).toMatchObject([{ meta }]);
}

async function entoncesSeAvisaQueLaMetaNoSeAlcanzaYSePropone36Mil() {
  const aviso = await screen.findByRole('note', { name: AVISO_DE_META_LEJANA }, ESPERA);
  expect(within(aviso).getByText(/Hoy vendés unos \$83\.000 por mes/)).toBeVisible();
  expect(within(aviso).getByRole('button', { name: 'Empezar por $36.000' })).toBeVisible();
}

async function entoncesSeVeElObjetivoGuardado() {
  await screen.findByRole('heading', { name: 'Seguimiento de tus objetivos' }, ESPERA);
  expect(screen.getByText(/sin subir demasiado los precios/)).toBeVisible();
}

async function entoncesSeAvisaElRechazoSinDarloPorDefinido() {
  expect(await screen.findByRole('alert')).toHaveTextContent('El texto es obligatorio');
  expect(
    screen.queryByRole('heading', { name: 'Seguimiento de tus objetivos' }),
  ).not.toBeInTheDocument();
}
