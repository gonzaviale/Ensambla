import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;

const NOMBRE_DEL_PANEL = 'Tu flujo de caja';

let router: Router;
let user: Usuario;

describe('inicio: panel del negocio', () => {
  it('con ventas muestra el flujo de caja cerrado, con lo que te va quedando', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);

    await entoncesElFlujoDeCajaEstaCerradoYDiceLoQueTeQueda();
  });

  it('al abrir el flujo de caja muestra el período y aclara lo fiado', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);

    await cuandoSeAbreElFlujoDeCaja();

    entoncesElPanelMuestraElPeriodoYLoFiado();
  });

  it('el flujo de caja es una fila por semana con lo que entró y salió', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await cuandoSeAbreElFlujoDeCaja();

    entoncesHayUnaFilaPorSemanaConLoCobradoYLoGastado();
  });

  it('el flujo de caja escribe el resultado y lo que te queda', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await cuandoSeAbreElFlujoDeCaja();

    entoncesLaCabeceraNombraLasColumnasYDiceCuantoTeQueda();
  });

  it('con panel el saludo sigue siendo el h1 y el título del panel es un h2', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await esperarLasCifras();

    entoncesElSaludoEsElH1YElPanelUnH2();
  });

  it('quien ya tiene una sección ve una bienvenida compacta con las tres formas de empezar', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await esperarLasCifras();

    entoncesLaBienvenidaCompactaOfreceLasTresFormasDeEmpezar();
  });

  it('desde la bienvenida compacta «Elegir una plantilla» lleva a Plantillas', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await esperarLasCifras();

    await cuandoSeEligeEnLaFranja(/Elegir una plantilla/);

    await verifyPathAsync(router, '/plantillas');
  });

  it('«Cargar desde una foto» y «Ver mi zona» van en la franja de bienvenida, una sola vez', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await esperarLasCifras();

    entoncesLosAtajosVanEnLaFranjaUnaSolaVez();
  });

  it('desde la franja, «Ver mi zona» lleva a Mi zona', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await esperarLasCifras();

    await cuandoSeEligeEnLaFranja(/Ver mi zona/);

    await verifyPathAsync(router, '/zona');
  });

  it('quien recién empieza ve los atajos completos debajo de la bienvenida grande', async () => {
    await dadoElInicioDe([]);

    entoncesLosAtajosSonCompletos();
  });

  it('quien recién empieza ve la bienvenida grande, sin panel', async () => {
    await dadoElInicioDe([]);

    entoncesSeVeLaBienvenidaGrandeSinPanel();
  });

  it('sin secciones el saludo sigue siendo el h1', async () => {
    await dadoElInicioDe([]);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/En qué puedo/);
  });

  it('mientras llegan los números avisa que está haciendo las cuentas', async () => {
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);

    entoncesAvisaQueEstaHaciendoLasCuentas();
    await esperarLasCifras();
    entoncesYaNoAvisaQueEstaHaciendoLasCuentas();
  });

  it('mira sólo el negocio activo: si no tiene ventas no muestra el panel de otro', async () => {
    await dadoElInicioDe([makeTemplate('shop', {}, true), crearAlmacenCargado(hoy())]);

    await entoncesSeVeLaBienvenidaSinPanel();
  });

  it('sin secciones no aparece el panel: la home queda como siempre', async () => {
    await dadoElInicioDe([]);

    expect(titulo()).toBeNull();
  });

  it('con una sección sin ventas no muestra el panel: la home se ve igual', async () => {
    await dadoElInicioDe([makeTemplate('shop', {}, true)]);

    await entoncesSeVeLaBienvenidaSinPanel();
    expect(screen.queryByText(/Todavía no cargaste ventas/)).toBeNull();
  });

  it('si el servidor falla avisa', async () => {
    dadoUnServidorQueFallaUnaVez();

    await dadoElInicioDe([crearAlmacenCargado(hoy())]);

    expect(await screen.findByRole('alert', undefined, ESPERA)).toHaveTextContent(
      /No pudimos traer tus números/,
    );
  });

  it('si el servidor falla permite reintentar', async () => {
    dadoUnServidorQueFallaUnaVez();
    await dadoElInicioDe([crearAlmacenCargado(hoy())]);
    await screen.findByRole('alert', undefined, ESPERA);

    await clickButton(user, 'Probar de nuevo');

    expect(await screen.findByRole('heading', { name: 'Semana a semana' }, ESPERA)).toBeVisible();
    expect(screen.queryByRole('alert')).toBeNull();
  });
});

const titulo = () => screen.queryByRole('heading', { name: NOMBRE_DEL_PANEL });
const franja = () => within(screen.getByRole('region', { name: 'Empezar algo nuevo' }));

/** El título aparece mientras carga: lo que hay que esperar es el flujo de caja. */
const esperarLasCifras = () =>
  screen.findByText(/Lo fiado no entra hasta que lo cobrás/, undefined, ESPERA);

async function dadoElInicioDe(sistemas: Sistema[]) {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  user = userEvent.setup();
}

function dadoUnServidorQueFallaUnaVez() {
  server.use(
    http.get(
      '*/api/negocio/panel',
      () => HttpResponse.json({ message: 'Error' }, { status: 500 }),
      {
        once: true,
      },
    ),
  );
}

/** El flujo de caja arranca cerrado: se espera a que llegue y se abre como lo haría la persona. */
async function cuandoSeAbreElFlujoDeCaja() {
  await user.click(await screen.findByRole('heading', { name: 'Semana a semana' }, ESPERA));
}

async function cuandoSeEligeEnLaFranja(nombre: RegExp) {
  await user.click(franja().getByRole('button', { name: nombre }));
}

async function entoncesElFlujoDeCajaEstaCerradoYDiceLoQueTeQueda() {
  const encabezado = await screen.findByRole('heading', { name: 'Semana a semana' }, ESPERA);
  const flujo = encabezado.closest('details') as HTMLElement;
  expect(flujo).not.toHaveAttribute('open');
  expect(within(flujo).getByText(/^Te queda \$/)).toBeVisible();
  expect(within(flujo).getByText(/Lo fiado no entra hasta que lo cobrás/)).not.toBeVisible();
}

function entoncesElPanelMuestraElPeriodoYLoFiado() {
  const panel = screen.getByRole('region', { name: NOMBRE_DEL_PANEL });
  expect(within(panel).getByText(/Los últimos 30 días/)).toBeVisible();
  expect(within(panel).getByText(/Lo fiado no entra hasta que lo cobrás/)).toBeVisible();
  expect(document.querySelector('.panel-tarjetas')).toBeNull();
}

function entoncesHayUnaFilaPorSemanaConLoCobradoYLoGastado() {
  const semanas = within(screen.getByRole('list', { name: 'Flujo de caja por semana' }));
  const filas = semanas.getAllByRole('listitem');
  expect(filas).toHaveLength(5);
  for (const fila of filas) {
    expect(within(fila).getByText('Cobraste')).toBeVisible();
    expect(within(fila).getByText('Gastaste')).toBeVisible();
    expect(within(fila).queryByText('Te quedó')).toBeNull();
    expect(fila).toHaveTextContent(/\$\d/);
  }
}

function entoncesLaCabeceraNombraLasColumnasYDiceCuantoTeQueda() {
  for (const columna of ['Cobraste', 'Gastaste', 'Resultado']) {
    expect(screen.getByText(columna, { selector: '.panel-semanas-cabecera span' })).toBeVisible();
  }
  expect(screen.getByText('Te queda')).toBeVisible();
}

function entoncesElSaludoEsElH1YElPanelUnH2() {
  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/En qué puedo/);
  expect(screen.getByRole('heading', { level: 2, name: NOMBRE_DEL_PANEL })).toBeVisible();
}

function entoncesLaBienvenidaCompactaOfreceLasTresFormasDeEmpezar() {
  const bienvenida = franja();
  expect(bienvenida.getByText('Hola, Ana Pérez.')).toBeVisible();
  expect(bienvenida.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
  expect(bienvenida.getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
  expect(bienvenida.getByRole('button', { name: /Empezar de cero/ })).toBeVisible();
  expect(bienvenida.getByRole('button', { name: /Primera vez/ })).toBeVisible();
  expect(screen.queryByText(/Tocá una ficha para arrancar/)).toBeNull();
}

function entoncesLosAtajosVanEnLaFranjaUnaSolaVez() {
  expect(franja().getByRole('button', { name: /Cargar desde una foto/ })).toBeVisible();
  expect(franja().getByRole('button', { name: /Ver mi zona/ })).toBeVisible();
  expect(screen.getAllByRole('button', { name: /Cargar desde una foto/ })).toHaveLength(1);
  expect(screen.getAllByRole('button', { name: /Ver mi zona/ })).toHaveLength(1);
  expect(screen.getAllByRole('region', { name: 'Más para tu negocio' })).toHaveLength(1);
  expect(document.querySelector('.inicio-atajos.compactos')).not.toBeNull();
}

function entoncesLosAtajosSonCompletos() {
  const atajos = within(screen.getByRole('region', { name: 'Más para tu negocio' }));
  expect(atajos.getByRole('button', { name: /Cargar desde una foto/ })).toBeVisible();
  expect(atajos.getByRole('button', { name: /Ver mi zona/ })).toBeVisible();
  expect(document.querySelector('.inicio-atajos')).not.toBeNull();
  expect(document.querySelector('.inicio-atajos.compactos')).toBeNull();
}

function entoncesSeVeLaBienvenidaGrandeSinPanel() {
  expect(screen.getByRole('region', { name: 'Empezar algo nuevo' })).toBeVisible();
  expect(screen.getByText(/Tocá una ficha para arrancar/)).toBeVisible();
  expect(titulo()).toBeNull();
}

function entoncesAvisaQueEstaHaciendoLasCuentas() {
  const panel = screen.getByRole('region', { name: NOMBRE_DEL_PANEL });
  expect(within(panel).getByRole('status')).toHaveTextContent('Estamos haciendo tus cuentas');
}

function entoncesYaNoAvisaQueEstaHaciendoLasCuentas() {
  expect(screen.queryByText('Estamos haciendo tus cuentas…')).toBeNull();
}

async function entoncesSeVeLaBienvenidaSinPanel() {
  expect(await screen.findByRole('region', { name: 'Empezar algo nuevo' }, ESPERA)).toBeVisible();
  expect(titulo()).toBeNull();
}
