import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

type Usuario = ReturnType<typeof userEvent.setup>;

const VELAS = 'Vendo velas y anoto quién me debe';
const NEGOCIO_VAGO = 'quiero ordenar mi negocio';

let user: Usuario;

describe('«Armalo con Sami» (crear un proyecto real)', () => {
  it('empieza con el saludo de Sami', async () => {
    await dadaLaPantallaDeSami();

    entoncesSamiSaludaAAna();
  });

  it('empieza sin propuesta', async () => {
    await dadaLaPantallaDeSami();

    entoncesNoHayPropuestaTodavia();
  });

  it('«vendo velas y anoto quién me debe» propone Ventas', async () => {
    await dadaLaPantallaDeSami();

    await cuandoSeLeCuentaASami(VELAS);

    await entoncesSePropone('Ventas', /sumarle la columna Saldo pendiente/);
  });

  it('la propuesta de Ventas trae la columna de saldo pendiente armada por Sami', async () => {
    await dadaLaPantallaDeSami();

    await cuandoSeLeCuentaASami(VELAS);

    await entoncesLaVistaMuestraLasColumnasDeVentas();
  });

  it('con una descripción vaga hace una pregunta con respuestas rápidas', async () => {
    await dadaLaPantallaDeSami();

    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);

    await entoncesSamiPreguntaQueQuiereAnotar();
  });

  it('después de la respuesta rápida propone', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);

    await cuandoSeEligeLaRespuestaRapida('Mis gastos');

    await entoncesSePropone('Gastos');
    expect(screen.queryByRole('group', { name: 'Respuestas rápidas' })).toBeNull();
  });

  it('si la persona elige «otra cosa», Sami lo piensa con la IA y lo dice', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);

    await cuandoSeEligeLaRespuestaRapida('Otra cosa');

    await entoncesLaPropuestaFueArmadaConAyudaDeIa();
  });

  it('si el servidor no tiene propuesta, Sami pide más detalle', async () => {
    await dadaLaPantallaDeSami();
    dadoUnServidorSinPropuesta();

    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);

    await entoncesSamiPideMasDetalleYDejaVolverAEscribir();
  });

  it('pide un poco más cuando lo escrito es muy corto', async () => {
    await dadaLaPantallaDeSami();

    await cuandoSeLeCuentaASami('hola');

    await verifyToastAsync(/Necesito más detalles/);
  });

  it('cuando lo escrito es muy corto no borra lo escrito', async () => {
    await dadaLaPantallaDeSami();

    await cuandoSeLeCuentaASami('hola');

    entoncesLoEscritoSeConserva('hola');
  });

  it('se puede quitar una columna sugerida y volver a sumarla', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);
    await nombreDelProyecto();

    await cuandoSeQuitaYSeVuelveASumarLaColumnaDeSaldo();

    entoncesLaColumnaSugeridaSigueEnLaPropuesta();
  });

  it('se crea el proyecto sin la columna sugerida', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);
    await nombreDelProyecto();

    await cuandoSeQuitaLaColumnaDeSaldoYSeCrea();

    await entoncesElProyectoQuedoCreado('Ventas', '5 columnas');
  });

  it('crea el proyecto con la columna sugerida y el nombre elegido', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);

    await cuandoSeCreaConElNombre('Velas de Ana');

    await entoncesElProyectoQuedoCreado('Velas de Ana', '6 columnas');
  });

  it('si el servidor rechaza la creación, avisa', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);
    await nombreDelProyecto();
    setupServerError('post', '/sami/proyectos', 400, 'Revisá la configuración del campo');

    await clickButton(user, /Sí, crear mi proyecto/);

    await verifyToastAsync(/Revisá la configuración del campo/);
  });

  it('si el servidor rechaza la creación, deja la propuesta para reintentar', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);
    await nombreDelProyecto();
    setupServerError('post', '/sami/proyectos', 400, 'Revisá la configuración del campo');
    await clickButton(user, /Sí, crear mi proyecto/);
    await verifyToastAsync(/Revisá la configuración del campo/);

    expect(screen.getByRole('button', { name: /Sí, crear mi proyecto/ })).toBeEnabled();
  });

  it('si Sami no puede pensar, avisa con el mensaje del servidor', async () => {
    await dadaLaPantallaDeSami();
    dadoUnSamiSinConsultasDeIa();

    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);

    await verifyToastAsync(/ya usaste todas las consultas con IA/);
  });

  it('si Sami no puede pensar, conserva lo escrito', async () => {
    await dadaLaPantallaDeSami();
    dadoUnSamiSinConsultasDeIa();

    await cuandoSeLeCuentaASami(NEGOCIO_VAGO);
    await verifyToastAsync(/ya usaste todas las consultas con IA/);

    entoncesLoEscritoSeConserva(NEGOCIO_VAGO);
  });

  it('«Empezar de nuevo» vuelve al saludo', async () => {
    await dadaLaPantallaDeSami();
    await cuandoSeLeCuentaASami(VELAS);
    await nombreDelProyecto();

    await clickButton(user, 'Empezar de nuevo');

    entoncesVuelveAlSaludoSinNadaEscrito();
  });

  // La home ya no enlaza a /sami («Crear con Sami» abre el taller): se retoma si vuelve el acceso.
  it.skip('se llega desde el inicio', async () => {
    setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    const router = renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);

    await clickButton(userEvent.setup(), 'Armalo con Sami');

    await verifyPathAsync(router, '/sami');
  });

  it('es accesible', async () => {
    await dadaLaPantallaDeSami();

    await entoncesNoHayViolacionesDeAccesibilidad();
  });
});

const nombreDelProyecto = () => screen.findByLabelText('Nombre de tu proyecto', {}, ESPERA);
const campoDeNegocio = () => screen.getByLabelText('Contame un poco de tu negocio');
const proyecto = () => within(screen.getByRole('region', { name: 'Tu proyecto' }));

async function dadaLaPantallaDeSami() {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  renderApp('/sami');
  await verifyHeadingPresentAsync('Armemos tu proyecto.');
  user = userEvent.setup();
}

function dadoUnServidorSinPropuesta() {
  setupServerResponse('post', '/sami/conversacion', 200, {
    tipo: 'RESPALDO_IA',
    pregunta: null,
    propuesta: null,
    preguntasHechas: 3,
  });
}

function dadoUnSamiSinConsultasDeIa() {
  setupServerError('post', '/sami/conversacion', 400, 'Hoy ya usaste todas las consultas con IA.');
}

async function cuandoSeLeCuentaASami(texto: string) {
  await user.type(campoDeNegocio(), texto);
  await clickButton(user, /Enviar a Sami/);
}

async function cuandoSeEligeLaRespuestaRapida(nombre: string) {
  const rapidas = await screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);
  await user.click(within(rapidas).getByRole('button', { name: nombre }));
}

async function cuandoSeQuitaYSeVuelveASumarLaColumnaDeSaldo() {
  const quitar = /Quitar la columna Saldo pendiente/;
  await user.click(await screen.findByRole('button', { name: quitar }, ESPERA));
  expect(screen.getByText('sin esta')).toBeVisible();
  await user.click(
    screen.getByRole('button', { name: /Volver a sumar la columna Saldo pendiente/ }),
  );
}

async function cuandoSeQuitaLaColumnaDeSaldoYSeCrea() {
  await cuandoSeQuitaYSeVuelveASumarLaColumnaDeSaldo();
  await user.click(screen.getByRole('button', { name: /Quitar la columna Saldo pendiente/ }));
  await clickButton(user, /Sí, crear mi proyecto/);
}

async function cuandoSeCreaConElNombre(nombre: string) {
  const campo = await nombreDelProyecto();
  await user.clear(campo);
  await user.type(campo, nombre);
  await clickButton(user, /Sí, crear mi proyecto/);
}

function entoncesSamiSaludaAAna() {
  expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
  expect(screen.getByText('Escribí qué hacés y cómo te organizás hoy.')).toBeVisible();
  expect(screen.getByText(/Yo pienso una base y vos me decís si te sirve/)).toBeVisible();
}

function entoncesNoHayPropuestaTodavia() {
  expect(screen.getByText('TU PROPUESTA VA A APARECER ACÁ')).toBeVisible();
  expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
}

async function entoncesSePropone(nombre: string, explicacion?: RegExp) {
  expect(await nombreDelProyecto()).toHaveValue(nombre);
  if (explicacion) expect(screen.getByText(explicacion)).toBeVisible();
}

async function entoncesLaVistaMuestraLasColumnasDeVentas() {
  await nombreDelProyecto();
  expect(await proyecto().findByText('Producto', {}, ESPERA)).toBeVisible();
  expect(proyecto().getByText('Saldo pendiente')).toBeVisible();
  expect(proyecto().getByText('Armada por Sami')).toBeVisible();
}

async function entoncesSamiPreguntaQueQuiereAnotar() {
  await screen.findByRole('group', { name: 'Respuestas rápidas' }, ESPERA);
  expect(screen.getByText('¿Qué es lo principal que querés anotar?')).toBeVisible();
}

async function entoncesLaPropuestaFueArmadaConAyudaDeIa() {
  expect(await nombreDelProyecto()).toBeVisible();
  expect(proyecto().getByText('Armada con ayuda de IA')).toBeVisible();
}

async function entoncesSamiPideMasDetalleYDejaVolverAEscribir() {
  expect(
    await screen.findByText(/todavía no encuentro un punto de partida/, {}, ESPERA),
  ).toBeVisible();
  expect(campoDeNegocio()).toBeEnabled();
  expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
}

function entoncesLoEscritoSeConserva(texto: string) {
  expect(campoDeNegocio()).toHaveValue(texto);
}

function entoncesLaColumnaSugeridaSigueEnLaPropuesta() {
  expect(screen.getByRole('button', { name: /Quitar la columna Saldo pendiente/ })).toBeVisible();
}

async function entoncesElProyectoQuedoCreado(nombre: string, columnas: string) {
  expect(
    await screen.findByText(`Tu proyecto «${nombre}» ya está creado.`, {}, ESPERA),
  ).toBeVisible();
  expect(screen.getByText(columnas)).toBeVisible();
}

function entoncesVuelveAlSaludoSinNadaEscrito() {
  expect(campoDeNegocio()).toHaveValue('');
  expect(screen.queryByLabelText('Nombre de tu proyecto')).toBeNull();
  expect(screen.queryByText(VELAS)).toBeNull();
}

async function entoncesNoHayViolacionesDeAccesibilidad() {
  const resultado = await axe(document.body, {
    rules: {
      'color-contrast': { enabled: false },
      region: { enabled: false },
      'heading-order': { enabled: false },
    },
  });
  expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html}`)).toEqual([]);
}
