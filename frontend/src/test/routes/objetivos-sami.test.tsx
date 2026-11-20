import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado, objetivosDe } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;

const FRASE_DEL_BARRIO = 'Quiero vender más y que me conozcan en el barrio';
const ELIGE_RECOMENDAR = /Editar «Pedile a 3 clientes que te recomienden/;

let user: Usuario;
let usuario: UsuarioDePrueba;
let router: Router;

describe('definir un objetivo conversando con Sami', () => {
  it('con una frase propone lo que entendió antes de guardar', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await cuandoLeCuentaASami(FRASE_DEL_BARRIO);

    await entoncesSamiMuestraLoQueEntendio();
  });

  it('propone mini-objetivos que se leen antes de guardar y muestra la cuota', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await cuandoLeCuentaASami(FRASE_DEL_BARRIO);

    await entoncesSeProponenTresMinisYLaCuota();
  });

  it('todavía no se guardó nada ni se gastó cuota', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await cuandoLeCuentaASami(FRASE_DEL_BARRIO);

    await entoncesTodaviaNoSeGuardoNada();
  });

  it('funciona como un chat: Sami saluda', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await entoncesSamiSaluda();
  });

  it('Shift+Enter hace un salto de línea', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await cuandoSeEscribeConUnSaltoDeLinea();

    expect(campoDeSami()).toHaveValue('Quiero vender más\ny que me conozcan');
  });

  it('Enter envía y el mensaje queda en el hilo', async () => {
    dadoUnUsuarioQueAbreObjetivos();

    await cuandoSeEnviaConEnter('Quiero vender más');

    await entoncesElMensajeQuedaEnElHilo('Quiero vender más');
  });

  it('se puede quitar y retocar un mini-objetivo antes de guardar', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami(FRASE_DEL_BARRIO);
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);

    await cuandoSeQuitaUnMiniSeRetocaOtroYSeGuarda();

    await entoncesElObjetivoGuardadoNoTieneMetaInventada();
  });

  it('al guardar quedan los mini-objetivos elegidos y se gasta una cuota', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami(FRASE_DEL_BARRIO);
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);

    await cuandoSeQuitaUnMiniSeRetocaOtroYSeGuarda();

    await entoncesQuedanDosMinisYSeGastoUnaCuota();
  });

  it('una acción sin datos no puede afirmar un importe ni un porcentaje', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami('Quiero que más gente conozca mi local');
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);

    await cuandoSeEscribeUnPorcentajeEnUnaAccionSinDatos();

    entoncesSeRechazaLaAccionYNoSePuedeGuardar();
  });

  it('una meta en pesos se define con el formulario de siempre, con lo que ya escribió', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami('Quiero ganar $400.000 por mes');

    await cuandoSePideLaMetaEnPesos();

    entoncesSeAbreElFormularioClasicoConLaMeta();
  });

  it('si la IA no responde ofrece guardar como seguimiento manual', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    dadaUnaIaCaida();

    await cuandoLeCuentaASami('Quiero vender más');

    await entoncesSoloOfreceElSeguimientoManual();
  });

  it('el seguimiento manual se guarda sin gastar cuota', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    dadaUnaIaCaida();
    await cuandoLeCuentaASami('Quiero vender más');
    await screen.findByText(/Sami no pudo responder ahora/, {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Guardar como seguimiento manual' }));

    await entoncesQuedoGuardadoElSeguimientoManualSinGastarCuota();
  });

  it('con la cuota agotada explica y ofrece guardar igual como seguimiento manual', async () => {
    dadoUnUsuarioConLaCuotaAgotada();

    await cuandoLeCuentaASami('Quiero vender más');

    await entoncesSeExplicaQueSeAgotoLaCuota();
  });

  it('con la cuota agotada lleva a Planes', async () => {
    dadoUnUsuarioConLaCuotaAgotada();
    await cuandoLeCuentaASami('Quiero vender más');
    await screen.findByText(/Usaste todos los objetivos con IA de este mes/, {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Ver planes' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/planes'));
  });

  it('con Premium la cuota es de 20', async () => {
    dadoUnUsuarioConPremium();

    await cuandoLeCuentaASami('Quiero gastar menos en luz');

    expect(
      await screen.findByText('Te quedan 20 de 20 objetivos con IA este mes', {}, ESPERA),
    ).toBeVisible();
  });

  it('avisa si el servidor rechaza lo que se quiere guardar y no lo da por guardado', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami('Quiero gastar menos en luz');
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
    setupServerError('post', '/objetivos/sami', 400, 'Revisá los mini-objetivos que elegiste.');

    await user.click(screen.getByRole('button', { name: 'Guardar objetivo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Revisá los mini-objetivos que elegiste.',
    );
    expect(objetivosDe(usuario.email)).toHaveLength(0);
  });

  it('tiene un tope de mensajes por conversación', async () => {
    dadoUnUsuarioQueAbreObjetivos();
    await cuandoLeCuentaASami('Quiero gastar menos en luz');

    await cuandoSeCambiaLaPropuestaCincoVeces();

    await entoncesSeLlegoAlMaximoDeMensajes();
  });
});

const campoDeSami = () => screen.getByLabelText('Contale tu objetivo a Sami');
const hilo = () => within(screen.getByRole('list', { name: 'Conversación con Sami' }));
const elMes = () => `${new Date().toISOString().slice(0, 7)}`;

function dadoUnUsuarioQueAbreObjetivos() {
  usuario = setupUsuarioConSesion();
  router = renderApp('/objetivos');
  user = userEvent.setup();
}

function dadoUnUsuarioConLaCuotaAgotada() {
  usuario = setupUsuarioConSesion();
  estado.objetivosConIa.set(`${usuario.email}|${elMes()}`, 5);
  router = renderApp('/objetivos');
  user = userEvent.setup();
}

function dadoUnUsuarioConPremium() {
  usuario = setupUsuarioConSesion();
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 86_400_000).toISOString(),
    finEn: new Date(Date.now() + 86_400_000 * 5).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  router = renderApp('/objetivos');
  user = userEvent.setup();
}

function dadaUnaIaCaida() {
  setupServerError('post', '/objetivos/sami/propuesta', 503, 'La IA no está disponible');
}

async function cuandoLeCuentaASami(texto: string) {
  await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA);
  await user.type(campoDeSami(), texto);
  await user.click(screen.getByRole('button', { name: 'Enviar' }));
}

async function cuandoSeEscribeConUnSaltoDeLinea() {
  await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA);
  await user.type(campoDeSami(), 'Quiero vender más{Shift>}{Enter}{/Shift}y que me conozcan');
}

async function cuandoSeEnviaConEnter(texto: string) {
  await user.type(
    await screen.findByLabelText('Contale tu objetivo a Sami', {}, ESPERA),
    `${texto}{Enter}`,
  );
}

async function cuandoSeQuitaUnMiniSeRetocaOtroYSeGuarda() {
  await user.click(screen.getByRole('button', { name: /Quitar «Poné un cartel en la vereda/ }));
  await user.click(screen.getByRole('button', { name: ELIGE_RECOMENDAR }));
  const titulo = screen.getByLabelText('Qué vas a hacer');
  await user.clear(titulo);
  await user.type(titulo, 'Pedile a 5 clientes que te recomienden');
  await user.click(screen.getByRole('button', { name: 'Listo' }));
  await user.click(screen.getByRole('button', { name: 'Guardar objetivo' }));
  await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
}

async function cuandoSeEscribeUnPorcentajeEnUnaAccionSinDatos() {
  await user.click(screen.getByRole('button', { name: ELIGE_RECOMENDAR }));
  await user.type(screen.getByLabelText('Para qué'), ' y vender 20 %');
}

async function cuandoSePideLaMetaEnPesos() {
  await user.click(await screen.findByRole('button', { name: 'Poner mi meta en pesos' }, ESPERA));
}

async function cuandoSeCambiaLaPropuestaCincoVeces() {
  for (let vuelta = 0; vuelta < 5; vuelta += 1) {
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
    await user.type(screen.getByLabelText('¿Querés cambiar algo?'), `Más barato ${vuelta}`);
    await user.click(screen.getByRole('button', { name: 'Enviar' }));
    await waitFor(() => expect(hilo().getAllByRole('listitem')).toHaveLength(2 * (vuelta + 2)));
  }
}

async function entoncesSamiSaluda() {
  await screen.findByRole('list', { name: 'Conversación con Sami' }, ESPERA);
  expect(await hilo().findByText(/Hola, soy Sami/)).toBeVisible();
}

async function entoncesSamiMuestraLoQueEntendio() {
  const lectura = within(
    await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA),
  );
  expect(lectura.getByText(FRASE_DEL_BARRIO)).toBeVisible();
  expect(lectura.getByText('Una parte la mide Sami y otra me la contás vos')).toBeVisible();
  expect(lectura.getByText(/Se apoyó en: ventas, margen, inventario, gastos\./)).toBeVisible();
  expect(lectura.getByText(/Sin datos de: zona\./)).toBeVisible();
}

async function entoncesSeProponenTresMinisYLaCuota() {
  await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
  const minis = screen.getAllByRole('listitem').filter((item) => item.className === 'sami-mini');
  expect(minis).toHaveLength(3);
  expect(screen.getByText('Vender un 10 % más por semana')).toBeVisible();
  expect(screen.getByText('Lo mide Sami')).toBeVisible();
  expect(screen.getAllByText('Me lo contás vos')).toHaveLength(2);
  expect(screen.getByText('Te quedan 5 de 5 objetivos con IA este mes')).toBeVisible();
}

async function entoncesTodaviaNoSeGuardoNada() {
  await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
  expect(objetivosDe(estado.usuarios.values().next().value?.email ?? '')).toHaveLength(0);
}

async function entoncesElMensajeQuedaEnElHilo(texto: string) {
  await screen.findByRole('region', { name: 'Lo que entendió Sami' }, ESPERA);
  expect(hilo().getByText(texto)).toBeVisible();
  expect(hilo().queryByText(/Hola, soy Sami/)).not.toBeInTheDocument();
  expect(screen.getByLabelText('¿Querés cambiar algo?')).toHaveValue('');
}

async function entoncesElObjetivoGuardadoNoTieneMetaInventada() {
  const [guardado] = objetivosDe(usuario.email);
  expect(guardado).toMatchObject({
    texto: FRASE_DEL_BARRIO,
    tipo: 'HIBRIDO',
    meta: null,
    armadoConSami: true,
  });
  // Un objetivo sin importe no recibe una meta falsa: la tarjeta no habla de pesos ni de lo que falta.
  expect(screen.queryByText(/\$500\.000/)).not.toBeInTheDocument();
  expect(screen.queryByText(/te faltan/)).not.toBeInTheDocument();
}

async function entoncesQuedanDosMinisYSeGastoUnaCuota() {
  const mini = await screen.findByRole('list', { name: 'Mini-objetivos de esta semana' }, ESPERA);
  expect(within(mini).getAllByRole('listitem')).toHaveLength(2);
  expect(within(mini).getByText('Pedile a 5 clientes que te recomienden')).toBeVisible();
  expect(estado.objetivosConIa.get(`${usuario.email}|${elMes()}`)).toBe(1);
}

function entoncesSeRechazaLaAccionYNoSePuedeGuardar() {
  expect(screen.getByRole('alert')).toHaveTextContent('Sin importes ni porcentajes');
  expect(screen.getByRole('button', { name: 'Listo' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Guardar objetivo' })).toBeDisabled();
}

function entoncesSeAbreElFormularioClasicoConLaMeta() {
  expect(campoDeSami()).toHaveValue('Quiero ganar $400.000 por mes');
  expect(screen.getByLabelText(/Tu meta/)).toHaveValue('400.000');
  expect(screen.queryByRole('button', { name: 'Guardar objetivo' })).not.toBeInTheDocument();
}

async function entoncesSoloOfreceElSeguimientoManual() {
  expect(await screen.findByText(/Sami no pudo responder ahora/, {}, ESPERA)).toBeVisible();
  expect(screen.queryByRole('button', { name: 'Guardar objetivo' })).not.toBeInTheDocument();
}

async function entoncesQuedoGuardadoElSeguimientoManualSinGastarCuota() {
  await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
  expect(objetivosDe(usuario.email)).toMatchObject([
    { texto: 'Quiero vender más', tipo: 'SIN_DATOS', meta: null, armadoConSami: false },
  ]);
  expect(estado.objetivosConIa.size).toBe(0);
}

async function entoncesSeExplicaQueSeAgotoLaCuota() {
  expect(
    await screen.findByText(/Usaste todos los objetivos con IA de este mes/, {}, ESPERA),
  ).toBeVisible();
  expect(screen.getByText('Usaste los 5 objetivos con IA de este mes')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Guardar como seguimiento manual' })).toBeVisible();
}

async function entoncesSeLlegoAlMaximoDeMensajes() {
  expect(
    await screen.findByText(/Llegaste al máximo de mensajes de esta conversación/, {}, ESPERA),
  ).toBeVisible();
  expect(screen.queryByLabelText('¿Querés cambiar algo?')).not.toBeInTheDocument();
}
