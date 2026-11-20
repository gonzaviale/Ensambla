import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { afterEach, expect, vi } from 'vitest';
import { axe } from 'vitest-axe';
import { renderApp } from '@/test/renderApp';
import { ESPERA, verifyHeadingPresentAsync } from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;

const TITULO_DE_LA_LANDING = 'Tu negocio de siempre, con tus cuentas más claras';
const SAMI = 'Sami, el pulpito de Ensambla';

let router: Router;
let user: UserEvent;
let irA: ReturnType<typeof vi.fn>;
let poses: string[];
let samisPorCategoria: number[];
let posesTrasOrganizar: string[];

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

/* secciones */

const inicio = () => within(screen.getByRole('region', { name: TITULO_DE_LA_LANDING }));
const ejemplo = () => within(screen.getByRole('region', { name: 'Consultá quién te debe y cuánto' }));
const aTuManera = () => within(screen.getByRole('region', { name: 'Tu negocio, tu manera de anotar' }));
const tusDatos = () =>
  within(
    screen.getByText('¿Puedo descargar una copia de mis datos?').closest('details') as HTMLElement,
  );
const panelDe = (categoria: string) =>
  within(aTuManera().getByRole('region', { name: `Ejemplo de ${categoria}` }));
const importeDelFiado = () => screen.getByLabelText('Importe del fiado');
const botonDeCategoria = (categoria: string) => aTuManera().getByRole('button', { name: categoria });
const clic = (boton: string | RegExp) => user.click(screen.getByRole('button', { name: boton }));
const clicEnEnlace = (enlace: string | RegExp) => user.click(screen.getByRole('link', { name: enlace }));

/* dado */

export const dadoQueSeAbreLaLanding = async () => {
  router = renderApp('/');
  await verifyHeadingPresentAsync(TITULO_DE_LA_LANDING);
  user = userEvent.setup();
};

export const dadoQueElDesplazamientoDeLaPaginaSeEspia = () => {
  irA = vi.fn();
  vi.stubGlobal('scrollTo', irA);
};

export const dadoQueSeRevisanLosDatosDelEjemplo = () => clic('Revisás los datos');

export const dadoQueSeLlegoALaBusqueda = () => clic('Lo encontrás');

export const dadoQueSeComenzoElEjemplo = () => clic('Comenzar el ejemplo');

export const dadoQueSeEligioLaCategoriaConElTeclado = async (categoria: string) => {
  botonDeCategoria('Ventas').focus();
  await user.keyboard('{Tab}{Enter}');
  expect(botonDeCategoria(categoria)).toHaveFocus();
};

export const dadoQueSeOrganizoLaCategoria = (categoria: string) =>
  user.click(panelDe(categoria).getByRole('button', { name: 'Ver cómo se organiza' }));

export const dadoQueSeAbrioElMenuDeSecciones = () => clic('Abrir menú');

/* cuando */

export const cuandoSeAbreLaLanding = dadoQueSeAbreLaLanding;

export const cuandoSeEligeLaPrimeraLlamadaAArmarElNegocio = () =>
  user.click(screen.getAllByRole('link', { name: /Armar mi negocio/ })[0] as HTMLElement);

export const cuandoSeEligeYaTengoCuenta = () => clicEnEnlace('Ya tengo cuenta');

export const cuandoSeConfirmaElImporte = async (importe: string) => {
  await clic('Revisás los datos');
  await user.clear(importeDelFiado());
  await user.type(importeDelFiado(), importe);
  await clic(/Confirmar y buscar/);
};

export const cuandoSeBuscaPorNombre = async (nombre: string) => {
  const buscar = screen.getByRole('searchbox', { name: 'Buscá un nombre en este ejemplo' });
  await user.clear(buscar);
  if (nombre) await user.type(buscar, nombre);
};

export const cuandoSeRecorreElEjemploConElTeclado = async () => {
  screen.getByRole('button', { name: 'Lo tenés en papel' }).focus();
  await user.keyboard('{Tab}{Enter}');
};

export const cuandoSeEligeLaCategoria = (categoria: string) => user.click(botonDeCategoria(categoria));

export const cuandoSePideVerComoSeOrganizaLaCategoria = async (categoria: string) => {
  await cuandoSeEligeLaCategoria(categoria);
  await user.click(panelDe(categoria).getByRole('button', { name: 'Ver cómo se organiza' }));
};

export const cuandoSeRecorrenTodasLasCategorias = async () => {
  poses = [];
  samisPorCategoria = [];
  posesTrasOrganizar = [];
  for (const categoria of ['Ventas', 'Gastos', 'Fiados', 'Productos', 'Ventas']) {
    await user.click(botonDeCategoria(categoria));
    await esperarQueSamiTermineDeCambiar();
    samisPorCategoria.push(aTuManera().getAllByRole('img', { name: SAMI }).length);
    poses.push(unicoSami().getAttribute('viewBox') as string);
    await user.click(aTuManera().getByRole('button', { name: 'Ver cómo se organiza' }));
    posesTrasOrganizar.push(unicoSami().getAttribute('viewBox') as string);
  }
};

export const cuandoSeVuelveALaAnotacionConElTeclado = async (categoria: string) => {
  const volver = panelDe(categoria).getByRole('button', { name: 'Volver a la anotación' });
  volver.focus();
  await user.keyboard('{Enter}');
};

export const cuandoSeAbreLaPreguntaSobreLaDescarga = () =>
  user.click(screen.getByText('¿Puedo descargar una copia de mis datos?'));

export const cuandoSeAbreLaPreguntaSobreElCuaderno = () => user.click(preguntaDelCuaderno());

export const cuandoSeCierraLaPreguntaSobreElCuaderno = () => user.click(preguntaDelCuaderno());

export const cuandoSeAvanzaAlPasoDeLaBusqueda = () => clic(/Confirmar y buscar/);

export const cuandoSeAbreElMenuDeSecciones = () => clic('Abrir menú');

export const cuandoSePulsaEscape = () => user.keyboard('{Escape}');

export const cuandoSeVuelveAlInicioDesdeElLogo = () => {
  irA.mockClear();
  return clicEnEnlace('Ensambla, ir al inicio');
};

export const cuandoSeVuelveArribaConElBoton = () => {
  irA.mockClear();
  return user.click(screen.getByRole('button', { name: 'Volver arriba', hidden: true }));
};

/* entonces */

export const entoncesHayUnSoloTituloPrincipal = () => {
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
};

export const entoncesLasLlamadasPrincipalesApuntanAlRegistro = () => {
  for (const enlace of screen.getAllByRole('link', { name: /Armar mi negocio/ })) {
    expect(enlace).toHaveAttribute('href', '/registro');
  }
};

export const entoncesSeAbreElRegistro = async () => {
  await screen.findByRole('heading', { name: 'Creá tu cuenta' }, ESPERA);
  expect(router.state.location.pathname).toBe('/registro');
  expect(screen.getByText('Empezá a armar tu negocio en Ensambla.')).toBeVisible();
};

export const entoncesSeAbreElIngreso = async () => {
  await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
  expect(router.state.location.pathname).toBe('/login');
};

export const entoncesSeEncuentraElFiadoConElImporteCorregido = () => {
  const resultados = screen.getByRole('list', { name: 'Fiados encontrados' });
  expect(within(resultados).getByText('Ana López')).toBeVisible();
  expect(resultados).toHaveTextContent(/4\.500,5/);
  expect(within(resultados).getAllByRole('listitem')).toHaveLength(1);
  expect(screen.getByRole('button', { name: 'Lo encontrás' })).toHaveAttribute('aria-pressed', 'true');
};

export const entoncesNoSeGuardoNadaEnElNavegador = () => {
  expect(localStorage.length).toBe(0);
};

export const entoncesLaListaMuestraSoloA = (nombre: string) => {
  const lista = screen.getByRole('list', { name: 'Fiados encontrados' });
  expect(lista).toHaveTextContent(nombre);
  expect(within(lista).getAllByRole('listitem')).toHaveLength(1);
};

export const entoncesLaListaIncluyeA = (nombre: string) => {
  expect(screen.getByRole('list', { name: 'Fiados encontrados' })).toHaveTextContent(nombre);
};

export const entoncesLaListaMuestraLosTresNombres = () => {
  const lista = screen.getByRole('list', { name: 'Fiados encontrados' });
  expect(within(lista).getAllByRole('listitem')).toHaveLength(3);
};

export const entoncesNoHayCoincidencias = () => {
  const lista = screen.getByRole('list', { name: 'Fiados encontrados' });
  expect(within(lista).queryByRole('listitem')).not.toBeInTheDocument();
  expect(ejemplo().getByRole('status')).toHaveTextContent('No hay coincidencias');
};

export const entoncesSeRechazaElImporteSinPasarALaConfirmacion = () => {
  expect(screen.getByRole('alert')).toHaveTextContent('Ingresá un importe mayor a cero.');
  expect(importeDelFiado()).toHaveFocus();
  expect(importeDelFiado()).toHaveAttribute('aria-invalid', 'true');
  expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
};

export const entoncesElFocoPasaAlPasoSiguiente = () => {
  expect(screen.getByRole('button', { name: 'Revisás los datos' })).toHaveFocus();
  expect(importeDelFiado()).toBeVisible();
};

export const entoncesElFocoVaAlImporteEnElPasoDos = () => {
  expect(importeDelFiado()).toHaveFocus();
  expect(screen.getByText('Paso 2 de 3')).toBeVisible();
};

export const entoncesElFocoVaALaBusquedaEnElPasoTres = () => {
  expect(screen.getByRole('searchbox')).toHaveFocus();
  expect(screen.getByText('Paso 3 de 3')).toBeVisible();
};

export const entoncesLaCategoriaQuedaElegidaConSuAnotacion = (categoria: string) => {
  expect(botonDeCategoria(categoria)).toHaveAttribute('aria-pressed', 'true');
  expect(aTuManera().getAllByRole('button', { pressed: true })).toHaveLength(1);
  expect(panelDe(categoria).getByText('Tu anotación')).toBeVisible();
  expect(panelDe(categoria).queryByRole('definition')).not.toBeInTheDocument();
};

export const entoncesSamiOrganizaLaCategoria = (categoria: string, campo: string, valor: string) => {
  const panel = panelDe(categoria);
  expect(panel.getByText(campo)).toBeVisible();
  expect(panel.getByText(valor)).toBeVisible();
  expect(panel.getAllByRole('definition')).toHaveLength(3);
  expect(panel.getByRole('status')).toHaveTextContent('organizado para revisar');
};

export const entoncesHayUnSoloSamiEnCadaCategoria = () => {
  expect(samisPorCategoria).toEqual([1, 1, 1, 1, 1]);
};

export const entoncesCadaCategoriaTieneUnaPoseDistinta = () => {
  expect(new Set(poses).size).toBe(4);
};

export const entoncesLaPoseNoCambiaAlOrganizar = () => {
  expect(posesTrasOrganizar).toEqual(poses);
};

export const entoncesLaCategoriaElegidaTieneElFocoYEstaPresionada = (categoria: string) => {
  expect(botonDeCategoria(categoria)).toHaveFocus();
  expect(botonDeCategoria(categoria)).toHaveAttribute('aria-pressed', 'true');
};

export const entoncesSeVuelveAlApunteOriginalDe = (categoria: string, apunte: RegExp) => {
  const panel = panelDe(categoria);
  expect(panel.getByText(apunte)).toBeVisible();
  expect(panel.queryByRole('definition')).not.toBeInTheDocument();
  expect(panel.getByRole('button', { name: 'Ver cómo se organiza' })).toHaveFocus();
};

export const entoncesElEjemploSeAlcanzaDesdeLaPrimeraPantalla = () => {
  expect(screen.getByRole('link', { name: /Ver un ejemplo/ })).toHaveAttribute('href', '#como-funciona');
  expect(inicio().getByRole('link', { name: /Ana López/ })).toHaveAttribute('href', '#como-funciona');
};

export const entoncesCadaEnlaceInternoTieneUnDestino = () => {
  const destinos = new Set([...document.querySelectorAll('[id]')].map((nodo) => nodo.id));
  for (const enlace of document.querySelectorAll<HTMLAnchorElement>('a[href^="#"]')) {
    expect(destinos.has(decodeURIComponent(enlace.hash.slice(1))), enlace.hash).toBe(true);
  }
};

export const entoncesLaDescargaFiguraComoPendienteSinAccionFalsa = () => {
  expect(tusDatos().getByText(/Todavía no/)).toBeVisible();
  expect(tusDatos().queryByRole('button')).not.toBeInTheDocument();
  expect(tusDatos().queryByRole('link')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Publicar comentario' })).not.toBeInTheDocument();
  expect(screen.queryByRole('tab', { name: 'Voz' })).not.toBeInTheDocument();
};

export const entoncesLaPreguntaSobreElCuadernoEstaAbierta = () => {
  expect(detalleDelCuaderno()).toHaveAttribute('open');
  expect(screen.getByText(/Podés seguir anotando como hasta ahora/)).toBeVisible();
  expect(router.state.location.pathname).toBe('/');
};

export const entoncesLaPreguntaSobreElCuadernoEstaCerrada = () => {
  expect(detalleDelCuaderno()).not.toHaveAttribute('open');
  expect(router.state.location.pathname).toBe('/');
};

export const entoncesSeVeElMenuDeSecciones = () => {
  expect(screen.getByRole('navigation', { name: 'Menú de secciones' })).toBeVisible();
};

export const entoncesElMenuQuedaCerradoConElFocoEnSuBoton = () => {
  const boton = screen.getByRole('button', { name: 'Abrir menú' });
  expect(boton).toHaveAttribute('aria-expanded', 'false');
  expect(boton).toHaveFocus();
};

export const entoncesLaPaginaVuelveAlInicio = () => {
  expect(irA).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }));
};

export const entoncesNoHayProblemasDeAccesibilidad = async () => {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  expect(resultado.violations.map((violacion) => violacion.id)).toEqual([]);
};

/* datos */

const unicoSami = () => aTuManera().getByRole('img', { name: SAMI });

const preguntaDelCuaderno = () => screen.getByText('¿Tengo que dejar de usar mi cuaderno?');

const detalleDelCuaderno = () => preguntaDelCuaderno().closest('details');

const esperarQueSamiTermineDeCambiar = () =>
  waitFor(() =>
    expect(document.querySelector('.ens-sami-character')).toHaveAttribute('data-cambiando', 'false'),
  );
