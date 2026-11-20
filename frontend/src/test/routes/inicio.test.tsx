import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  verifyToastAsync,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;

const ATAJOS_DEL_PIE = [
  'Inicio',
  'Avisos',
  'Objetivos',
  'Mi zona',
  'Plantillas',
  'Configuración',
  'Planes',
];

let router: Router;
let user: Usuario;
let usuario: UsuarioDePrueba;

describe('inicio', () => {
  it('saluda con el nombre de la cuenta', async () => {
    await dadoUnUsuarioEnElInicio();

    entoncesSaludaConElNombreDeLaCuenta();
  });

  it('muestra las tres maneras de empezar', async () => {
    await dadoUnUsuarioEnElInicio();

    entoncesMuestraLasTresManerasDeEmpezar();
  });

  // La sección «Así se ve un sistema» está comentada en InicioPage: se retoma si vuelve.
  it.skip('cambia la distribución del ejemplo entre mosaico y una columna', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeEligeUnaColumna();

    entoncesTodosLosBloquesDelEjemploOcupanElAncho();
  });

  it('el menú de arriba ya no tiene «Mis cuadernos»: el negocio y sus secciones están en el Inicio', async () => {
    await dadoUnUsuarioEnElInicioConUnSistema();

    entoncesElMenuNoTieneMisCuadernos();
  });

  it('un enlace viejo a /cuadernos lleva al Inicio', async () => {
    dadoUnUsuarioConSesion();

    cuandoSeAbreElEnlaceViejoACuadernos();

    await entoncesLlevaAlInicio();
  });

  it('el menú hamburguesa (celular) se abre desde el botón', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeAbreElMenuHamburguesa();

    entoncesElMenuHamburguesaEstaAbierto();
  });

  it('el menú hamburguesa se cierra al elegir una sección', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSeAbreElMenuHamburguesa();

    await cuandoSeEligeObjetivosEnElMenu();

    await entoncesSeAbreObjetivosYElMenuQuedaCerrado();
  });

  it('muestra la zona sin mapa', async () => {
    await dadoUnUsuarioEnElInicio();

    entoncesLaZonaSeMuestraSinMapa();
  });

  it('lleva a «Mi zona»', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeEligeVerMiZona();

    await verifyPathAsync(router, '/zona');
  });
});

describe('pie de página', () => {
  it('tiene los atajos agrupados', async () => {
    await dadoUnUsuarioEnElInicio();

    entoncesElPieTieneTodosLosAtajos();
  });

  it('cada atajo lleva a su pantalla', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeEligeElAtajoDeAvisos();

    await entoncesSeAbreAvisosConElPieVisible();
  });
});

describe('plantillas', () => {
  it('abre la vista previa de una plantilla', async () => {
    await dadoUnUsuarioEnLasPlantillas();

    await cuandoSeAbreLaVistaPreviaDeLaPlantillaDeNegocio();

    await entoncesSeVeLaVistaPreviaDePedidosYClientes();
  });

  it('elegir opciones y empezar crea el sistema con ejemplos', async () => {
    await dadoUnUsuarioEnLasPlantillas();
    await cuandoSeAbreLaVistaPreviaDeLaPlantillaDeNegocio();

    await cuandoSeEmpiezaConEntradasYSalidas();

    await entoncesSeCreaElSistemaConEntradasYSalidas();
  });

  it('filtra por tipo', async () => {
    await dadoUnUsuarioEnLasPlantillas();

    await cuandoSeFiltraPorStockYMovimientos();

    entoncesHayPlantillasVisibles(1);
  });

  it('busca por texto', async () => {
    await dadoUnUsuarioEnLasPlantillas();

    await cuandoSeBuscaUnaPlantilla('kiosco');

    entoncesHayPlantillasVisibles(1);
  });

  it('sin coincidencias sugiere otra búsqueda', async () => {
    await dadoUnUsuarioEnLasPlantillas();

    await cuandoSeBuscaUnaPlantilla('turnos');

    entoncesNoHayPlantillasYSeSugiereOtraBusqueda();
  });
});

describe('modales', () => {
  it('se abren bloqueando el scroll', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeAbreElModalDeAyuda();

    entoncesElModalEstaAbiertoYElScrollBloqueado();
  });

  it('se cierran con Esc y devuelven el foco', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSeAbreElModalDeAyuda();

    await cuandoSePresionaEscape();

    entoncesElModalSeCerroYElFocoVolvioAlBoton();
  });

  it('el lienzo en blanco crea un sistema vacío en modo edición', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeEmpiezaDeCero();

    await entoncesSeCreaUnSistemaEnModoEdicion();
  });

  it('el avatar despliega un menú que lleva a la configuración', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeEligeConfiguracionEnElMenuDelAvatar();

    await verifyPathAsync(router, '/cuenta');
  });

  it('en la configuración se cambia el nombre que se muestra y se guarda la preferencia', async () => {
    await dadoUnUsuarioEnLaConfiguracion();

    await cuandoSeGuardaElNombre('Marta');

    await entoncesQuedaGuardadoElNombre('Marta');
  });

  it('el menú del avatar se cierra con Escape devolviendo el foco', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSeAbreElMenuDelAvatar();

    await cuandoSePresionaEscape();

    entoncesElMenuDelAvatarSeCerroYElFocoVolvioAlAvatar();
  });

  it('el menú del avatar se cierra al tocar afuera', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSeAbreElMenuDelAvatar();

    await cuandoSeTocaAfuera();

    entoncesElMenuDelAvatarSeCerro();
  });

  it('el menú del avatar ofrece configuración y cerrar sesión', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSeAbreElMenuDelAvatar();

    await entoncesElMenuOfreceConfiguracionYCerrarSesion();
  });

  it('desde el menú del avatar la sesión se cierra recién al confirmar', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSePideCerrarSesionDesdeElAvatar();

    await cuandoSeConfirmaCerrarSesion();

    await verifyPathAsync(router, '/login');
  });

  it('antes de confirmar la sesión sigue abierta', async () => {
    await dadoUnUsuarioEnElInicio();

    await cuandoSePideCerrarSesionDesdeElAvatar();

    entoncesLaSesionSigueAbierta();
  });

  it('cancelar la confirmación mantiene la sesión y devuelve el foco al avatar', async () => {
    await dadoUnUsuarioEnElInicio();
    await cuandoSePideCerrarSesionDesdeElAvatar();

    await cuandoSeCancelaCerrarSesion();

    entoncesLaSesionSigueAbiertaYElFocoVolvioAlAvatar();
  });

  it('desde la cuenta también se puede cerrar la sesión', async () => {
    await dadoUnUsuarioEnLaConfiguracion();

    await cuandoSeCierraLaSesionDesdeLaCuenta();

    await verifyPathAsync(router, '/login');
  });
});

function dadoUnUsuarioConSesion() {
  usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
}

async function dadoUnUsuarioEnElInicio() {
  dadoUnUsuarioConSesion();
  await abrirInicio();
}

async function dadoUnUsuarioEnElInicioConUnSistema() {
  dadoUnUsuarioConSesion();
  estado.sistemas.set(usuario.email, [makeTemplate('shop', {}, true)]);
  await abrirInicio();
}

async function dadoUnUsuarioEnLasPlantillas() {
  await dadoUnUsuarioEnElInicio();
  await clickButton(user, /Elegir una plantilla/);
  await verifyHeadingPresentAsync(/Elegí una base/);
}

async function dadoUnUsuarioEnLaConfiguracion() {
  await dadoUnUsuarioEnElInicio();
  await cuandoSeEligeConfiguracionEnElMenuDelAvatar();
  await verifyPathAsync(router, '/cuenta');
}

async function abrirInicio() {
  router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  user = userEvent.setup();
}

function cuandoSeAbreElEnlaceViejoACuadernos() {
  router = renderApp('/cuadernos');
}

async function cuandoSeEligeUnaColumna() {
  await clickButton(user, /Una columna/);
}

async function cuandoSeAbreElMenuHamburguesa() {
  await user.click(screen.getByRole('button', { name: 'Abrir el menú' }));
}

async function cuandoSeEligeObjetivosEnElMenu() {
  await clickButton(user, 'Objetivos');
}

async function cuandoSeEligeVerMiZona() {
  await clickButton(user, /Ver mi zona/);
}

async function cuandoSeEligeElAtajoDeAvisos() {
  await user.click(elPie().getByRole('link', { name: 'Avisos' }));
}

async function cuandoSeAbreLaVistaPreviaDeLaPlantillaDeNegocio() {
  const tarjeta = document.querySelector<HTMLElement>('[data-template="business"]');
  if (!tarjeta) throw new Error('Falta la tarjeta de la plantilla');
  await user.click(within(tarjeta).getByRole('button', { name: /Ver la sección/ }));
}

async function cuandoSeEmpiezaConEntradasYSalidas() {
  const modal = await screen.findByRole('dialog');
  await user.click(within(modal).getByLabelText(/Entradas y salidas/));
  await user.click(within(modal).getByRole('button', { name: /Empezar con este conjunto/ }));
}

async function cuandoSeFiltraPorStockYMovimientos() {
  await clickButton(user, 'Stock y movimientos');
}

async function cuandoSeBuscaUnaPlantilla(texto: string) {
  await user.type(screen.getByLabelText('Buscar una plantilla'), texto);
}

async function cuandoSeAbreElModalDeAyuda() {
  await user.click(elBotonDeAyuda());
  await screen.findByRole('dialog');
}

async function cuandoSePresionaEscape() {
  await user.keyboard('{Escape}');
}

async function cuandoSeEmpiezaDeCero() {
  await clickButton(user, /Empezar de cero/);
  await user.click(await screen.findByRole('button', { name: /Seguir desde cero/ }));
}

async function cuandoSeAbreElMenuDelAvatar() {
  await user.click(await screen.findByRole('button', { name: 'Menú de tu cuenta' }));
  await screen.findByRole('menuitem', { name: 'Configuración' });
}

async function cuandoSeEligeConfiguracionEnElMenuDelAvatar() {
  await clickButton(user, 'Menú de tu cuenta');
  await user.click(await screen.findByRole('menuitem', { name: 'Configuración' }));
}

async function cuandoSeGuardaElNombre(nombre: string) {
  const campo = await screen.findByLabelText('¿Cómo querés que te llame Sami?');
  await user.clear(campo);
  await user.type(campo, nombre);
  await clickButton(user, 'Guardar nombre');
}

async function cuandoSeTocaAfuera() {
  await user.click(document.body);
}

async function cuandoSePideCerrarSesionDesdeElAvatar() {
  await clickButton(user, 'Menú de tu cuenta');
  await user.click(await screen.findByRole('menuitem', { name: 'Cerrar sesión' }));
  await screen.findByRole('dialog', { name: '¿Cerrar la sesión?' });
}

async function cuandoSeConfirmaCerrarSesion() {
  const dialogo = await screen.findByRole('dialog', { name: '¿Cerrar la sesión?' });
  await user.click(within(dialogo).getByRole('button', { name: 'Cerrar sesión' }));
}

async function cuandoSeCancelaCerrarSesion() {
  const dialogo = await screen.findByRole('dialog', { name: '¿Cerrar la sesión?' });
  await user.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));
}

async function cuandoSeCierraLaSesionDesdeLaCuenta() {
  await user.click(await screen.findByRole('link', { name: 'Cerrar sesión' }));
  await user.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
}

function entoncesSaludaConElNombreDeLaCuenta() {
  expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
  expect(screen.queryByRole('heading', { name: 'Seguí donde lo dejaste' })).toBeNull();
}

function entoncesMuestraLasTresManerasDeEmpezar() {
  expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
  expect(screen.getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
  expect(screen.getByRole('button', { name: /Empezar de cero/ })).toBeVisible();
}

function entoncesTodosLosBloquesDelEjemploOcupanElAncho() {
  const bloques = [...document.querySelectorAll('.hero-system .preview-block')];
  expect(bloques.every((bloque) => bloque.getAttribute('data-width') === 'full')).toBe(true);
}

function entoncesElMenuNoTieneMisCuadernos() {
  const menu = within(screen.getByRole('navigation', { name: 'Principal' }));
  expect(menu.queryByRole('button', { name: /Mis (cuadernos|secciones)/ })).toBeNull();
  expect(menu.getByRole('button', { name: 'Inicio' })).toBeVisible();
}

async function entoncesLlevaAlInicio() {
  await verifyPathAsync(router, '/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
}

function entoncesElMenuHamburguesaEstaAbierto() {
  const boton = screen.getByRole('button', { name: 'Cerrar el menú' });
  expect(boton).toHaveAttribute('aria-expanded', 'true');
}

async function entoncesSeAbreObjetivosYElMenuQuedaCerrado() {
  await verifyPathAsync(router, '/objetivos');
  const boton = screen.getByRole('button', { name: 'Abrir el menú' });
  expect(boton).toHaveAttribute('aria-expanded', 'false');
}

function entoncesLaZonaSeMuestraSinMapa() {
  expect(document.querySelector('.leaflet-container')).toBeNull();
}

function elPie() {
  return within(screen.getByRole('navigation', { name: 'Pie de página' }));
}

function entoncesElPieTieneTodosLosAtajos() {
  ATAJOS_DEL_PIE.forEach((nombre) => {
    expect(elPie().getByRole('link', { name: nombre })).toBeVisible();
  });
  expect(elPie().getByRole('link', { name: 'Avisos' })).toHaveAttribute('href', '/avisos');
}

async function entoncesSeAbreAvisosConElPieVisible() {
  await verifyPathAsync(router, '/avisos');
  expect(screen.getByRole('navigation', { name: 'Pie de página' })).toBeVisible();
}

async function entoncesSeVeLaVistaPreviaDePedidosYClientes() {
  const modal = await screen.findByRole('dialog');
  const titulo = within(modal).getByRole('heading', { name: /Pedidos y clientes, a tu manera/ });
  expect(titulo).toBeVisible();
}

async function entoncesSeCreaElSistemaConEntradasYSalidas() {
  await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
  const [sistema] = useSistemaStore.getState().sistemas;
  expect(sistema?.sections[0]?.blocks.map((bloque) => bloque.type)).toContain('entries');
  expect(useSistemaStore.getState().listo).toBe(true);
  expect(screen.queryByRole('dialog')).toBeNull();
  await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
}

function entoncesHayPlantillasVisibles(cantidad: number) {
  const visibles = document.querySelectorAll('[data-template]:not([hidden])');
  expect(visibles).toHaveLength(cantidad);
}

function entoncesNoHayPlantillasYSeSugiereOtraBusqueda() {
  entoncesHayPlantillasVisibles(0);
  expect(screen.getByText(/Probá con Almacén/)).toBeVisible();
}

function elBotonDeAyuda() {
  return screen.getByRole('button', { name: 'Ayuda' });
}

function entoncesElModalEstaAbiertoYElScrollBloqueado() {
  expect(screen.getByRole('dialog')).toBeVisible();
  expect(document.body.style.overflow).toBe('hidden');
}

function entoncesElModalSeCerroYElFocoVolvioAlBoton() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(document.body.style.overflow).toBe('');
  expect(elBotonDeAyuda()).toHaveFocus();
}

async function entoncesSeCreaUnSistemaEnModoEdicion() {
  await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
  expect(useSistemaStore.getState().modo).toBe('build');
}

async function entoncesQuedaGuardadoElNombre(nombre: string) {
  await verifyToastAsync('Guardamos tu nombre.');
  expect(await screen.findByText(nombre)).toBeVisible();
  await waitFor(() => expect(estado.preferencias.get(usuario.email)?.nombre).toBe(nombre), ESPERA);
}

function entoncesElMenuDelAvatarSeCerroYElFocoVolvioAlAvatar() {
  expect(screen.queryByRole('menu')).toBeNull();
  expect(screen.getByRole('button', { name: 'Menú de tu cuenta' })).toHaveFocus();
}

function entoncesElMenuDelAvatarSeCerro() {
  expect(screen.queryByRole('menu')).toBeNull();
}

async function entoncesElMenuOfreceConfiguracionYCerrarSesion() {
  const avatar = screen.getByRole('button', { name: 'Menú de tu cuenta' });
  expect(avatar).toHaveAttribute('aria-expanded', 'true');
  expect(await screen.findByRole('menuitem', { name: 'Configuración' })).toHaveFocus();
  expect(screen.getByRole('menuitem', { name: 'Cerrar sesión' })).toBeVisible();
}

function entoncesLaSesionSigueAbierta() {
  expect(router.state.location.pathname).not.toBe('/login');
}

function entoncesLaSesionSigueAbiertaYElFocoVolvioAlAvatar() {
  expect(screen.queryByRole('dialog')).toBeNull();
  entoncesLaSesionSigueAbierta();
  expect(screen.getByRole('button', { name: 'Menú de tu cuenta' })).toHaveFocus();
}
