import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;
type Usuario = ReturnType<typeof userEvent.setup>;
type UsuarioDePrueba = ReturnType<typeof setupUsuarioConSesion>;

const AVISO_DE_SALIDA = 'Podés retomar la guía desde Sami en el inicio.';

let router: Router;
let user: Usuario;
let usuario: UsuarioDePrueba;

describe('la bienvenida', () => {
  it('aparece la primera vez con el nombre y el foco en empezar la guía', async () => {
    await dadoUnUsuarioConLaBienvenida();

    entoncesLaBienvenidaTieneElFocoEnEmpezarLaGuia();
  });

  it('explorar por cuenta propia la cierra sin activar el tutorial', async () => {
    await dadoUnUsuarioConLaBienvenida();

    await cuandoSeElige(/Explorar por mi cuenta/);

    entoncesNoHayDialogoNiTutorialActivo();
  });

  it('explorar por cuenta propia la deja como vista', async () => {
    await dadoUnUsuarioConLaBienvenida();

    await cuandoSeElige(/Explorar por mi cuenta/);

    await entoncesLaBienvenidaQuedoVista();
  });

  it('con Esc también se cierra y queda como vista', async () => {
    await dadoUnUsuarioConLaBienvenida();

    await cuandoSePresionaEscape();

    await entoncesLaBienvenidaSeCerroYQuedoVista();
  });

  it('si ya fue vista no se muestra', async () => {
    await dadoUnUsuarioConLaBienvenidaYaVista();

    entoncesNoSeMuestraNingunDialogo();
  });
});

describe('el tutorial', () => {
  it('empieza desde la bienvenida con el primer paso y la etapa 1 de 5', async () => {
    await dadoUnUsuarioConLaBienvenida();

    await cuandoSeEmpiezaLaGuia();

    entoncesEstaEnLaEtapa1DelPrimerPaso();
  });

  it('empieza resaltando una sola pieza y sin ofrecer reiniciar la demo', async () => {
    await dadoUnUsuarioConLaBienvenida();

    await cuandoSeEmpiezaLaGuia();

    entoncesHayUnaSolaPiezaResaltadaYNoSeOfreceReiniciar();
  });

  it('bloquea lo que no corresponde al paso y recuerda qué hacer', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSeIntentaIrALaZona();

    await entoncesSeRecuerdaElPrimerPaso();
  });

  it('el camino de Sami: describir el negocio muestra la idea con forma', async () => {
    await dadoUnaGuiaEnElCaminoDeSami();

    await cuandoSeDescribeElNegocio('Tengo un almacén de barrio y quiero anotar mis ventas');

    await esperarPaso('Tu idea ya tiene forma.');
  });

  it('el camino de Sami: ajustar el ancho lleva a confirmar la creación', async () => {
    await dadoUnaGuiaConLaIdeaDeSamiConForma();

    await cuandoSeAjustaElAnchoDelSegundoBloque();

    await esperarPaso('Cuando te cierre, la creamos.');
  });

  it('el camino de Sami: crear la sección lleva al editor', async () => {
    await dadoUnaGuiaConElAnchoAjustado();

    await cuandoSeCreaLaSeccion();

    await entoncesSeAbreElEditorEnElPasoDeCambiar();
  });

  it('el camino de Sami: personalizar el bloque lleva a ponerle nombre', async () => {
    await dadoUnaGuiaEnElEditor();

    await cuandoSePersonalizaElBloque();

    await entoncesElNombreTieneElFoco();
  });

  it('el camino de Sami: renombrar el bloque lleva a moverlo', async () => {
    await dadoUnaGuiaPersonalizandoElBloque();

    await cuandoSeRenombraElBloque('Mercadería');

    await esperarPaso('De acá se mueve.');
  });

  it('el camino de Sami: mover el bloque lleva a llenarlo', async () => {
    await dadoUnaGuiaConElBloqueRenombrado();

    await cuandoSeMueveElBloqueAbajo();

    await esperarPaso('De acá se llena.');
  });

  it('el camino de Sami: registrar abre la carga de un dato', async () => {
    await dadoUnaGuiaConElBloqueMovido();

    await cuandoSeEligeRegistrarEnElBloque();

    await esperarPaso('Cargá un dato tuyo.');
  });

  it('el camino de Sami: guardar un dato termina en la etapa 5 de 5', async () => {
    await dadoUnaGuiaCargandoUnDato();

    await cuandoSeGuardaElDato('Yerba mate', '8');

    await entoncesTerminaEnLaEtapa5();
  });

  it('salir del tutorial cierra el cartel y avisa cómo retomarlo', async () => {
    await dadoUnaGuiaEnLaUltimaEtapa();

    await cuandoSeSaleDelTutorial();

    await entoncesElCartelSeCerroYSeAvisaComoRetomarlo();
  });

  it('salir del tutorial deja de resaltar piezas', async () => {
    await dadoUnaGuiaEnLaUltimaEtapa();

    await cuandoSeSaleDelTutorial();

    entoncesNoQuedaNingunaPiezaResaltada();
  });

  it('el camino de las plantillas: elegir una plantilla muestra qué es', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSeElige(/Elegir una plantilla/);

    await entoncesSeExplicaQueEsUnaPlantilla();
  });

  it('el camino de las plantillas: ver una plantilla muestra sus piezas', async () => {
    await dadoUnaGuiaEnLasPlantillas();

    await cuandoSeMiraLaPlantillaDeAlmacen();

    await esperarPaso('Mirá las piezas antes de empezar.');
  });

  it('el camino de las plantillas: seguir mirando vuelve a la lista', async () => {
    await dadoUnaGuiaMirandoLaPlantillaDeAlmacen();

    await cuandoSeElige('Seguir mirando');

    await esperarPaso('Una plantilla es un conjunto.');
  });

  it('el camino de las plantillas: empezar con el conjunto lleva al editor', async () => {
    await dadoUnaGuiaMirandoLaPlantillaDeAlmacen();

    await cuandoSeElige(/Empezar con este conjunto/);

    await esperarPaso('De acá se cambia.');
  });

  it('el camino desde cero: confirmar muestra el lienzo propio', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSeElige(/Empezar de cero/);

    await esperarPaso('Tu lienzo, desde cero.');
  });

  it('el camino desde cero: seguir pide traer la primera pieza', async () => {
    await dadoUnaGuiaEnElLienzoDesdeCero();

    await cuandoSeElige(/Seguir desde cero/);

    await esperarPaso('Traé tu primera pieza.');
  });

  it('el camino desde cero: agregar una pieza lleva a personalizarla', async () => {
    await dadoUnaGuiaPidiendoLaPrimeraPieza();

    await cuandoSeAgregaLaPiezaClientes();

    await esperarPaso('De acá se cambia.');
  });

  it('cerrar el cartel de «desde cero» vuelve a elegir el camino', async () => {
    await dadoUnaGuiaEnElLienzoDesdeCero();

    await cuandoSeElige('Cerrar');

    await esperarPaso('Tres maneras de empezar.');
  });

  it('Esc sale de la guía y avisa cómo retomarla', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSePresionaEscape();

    await entoncesLaGuiaSalioYSeAvisaComoRetomarla();
  });

  it('la ✕ cierra la guía', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSeSaleDelTutorial();

    entoncesNoHayCartelDeGuia();
  });

  it('desde el inicio se retoma la guía', async () => {
    await dadoUnaGuiaCerradaConLaX();

    await cuandoSeElige(/Primera vez/);

    await esperarPaso('Tres maneras de empezar.');
  });

  it('ir al inicio durante la guía la termina sin preguntar nada', async () => {
    await dadoUnaGuiaEnLasPlantillasYAsentada();

    await cuandoSeVaAlInicioPorElLogo();

    await entoncesLaGuiaTerminoSinPreguntar();
  });

  it('el tutorial no ofrece reiniciar la demo en ningún paso', async () => {
    await dadoUnaGuiaEnElPrimerPaso();

    await cuandoSeElige(/Elegir una plantilla/);

    entoncesNoSeOfreceReiniciarLaDemo();
  });
});

function cartel() {
  return screen.getByLabelText('Guía interactiva de Sami');
}

async function esperarPaso(titulo: string) {
  await waitFor(
    () => expect(within(cartel()).getByRole('heading', { level: 2 })).toHaveTextContent(titulo),
    ESPERA,
  );
}

function pasoActual() {
  return useSistemaStore.getState().tutorial.paso;
}

function bloqueDelTutorial() {
  const id = useSistemaStore.getState().tutorial.bloqueId;
  const nodo = document.querySelector<HTMLElement>(`[data-block="${id}"]`);
  if (!nodo) throw new Error('Falta el bloque del tutorial');
  return nodo;
}

function botonDeLaBienvenida() {
  return screen.getByRole('button', { name: /Hacer mi primera sección con guía/ });
}

async function dadoUnUsuarioConLaBienvenida() {
  usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' }, { bienvenidaVista: false });
  router = renderApp('/inicio');
  await screen.findByRole('dialog', { name: /Hola, Ana Pérez/ }, ESPERA);
  user = userEvent.setup();
}

async function dadoUnUsuarioConLaBienvenidaYaVista() {
  setupUsuarioConSesion({}, { bienvenidaVista: true });
  router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
}

async function dadoUnaGuiaEnElPrimerPaso() {
  await dadoUnUsuarioConLaBienvenida();
  await cuandoSeEmpiezaLaGuia();
}

async function dadoUnaGuiaEnElCaminoDeSami() {
  await dadoUnaGuiaEnElPrimerPaso();
  await cuandoSeElige(/Crear con Sami/);
  await esperarPaso('Contame lo que necesitás.');
}

async function dadoUnaGuiaConLaIdeaDeSamiConForma() {
  await dadoUnaGuiaEnElCaminoDeSami();
  await cuandoSeDescribeElNegocio('Tengo un almacén de barrio y quiero anotar mis ventas');
  await esperarPaso('Tu idea ya tiene forma.');
}

async function dadoUnaGuiaConElAnchoAjustado() {
  await dadoUnaGuiaConLaIdeaDeSamiConForma();
  await cuandoSeAjustaElAnchoDelSegundoBloque();
  await esperarPaso('Cuando te cierre, la creamos.');
}

async function dadoUnaGuiaEnElEditor() {
  await dadoUnaGuiaConElAnchoAjustado();
  await cuandoSeCreaLaSeccion();
  await esperarPaso('De acá se cambia.');
  await waitFor(() => expect(() => bloqueDelTutorial()).not.toThrow(), ESPERA);
}

async function dadoUnaGuiaPersonalizandoElBloque() {
  await dadoUnaGuiaEnElEditor();
  await cuandoSePersonalizaElBloque();
  await esperarPaso('Ponelo en tus palabras.');
}

async function dadoUnaGuiaConElBloqueRenombrado() {
  await dadoUnaGuiaPersonalizandoElBloque();
  await cuandoSeRenombraElBloque('Mercadería');
  await esperarPaso('De acá se mueve.');
}

async function dadoUnaGuiaConElBloqueMovido() {
  await dadoUnaGuiaConElBloqueRenombrado();
  await cuandoSeMueveElBloqueAbajo();
  await esperarPaso('De acá se llena.');
}

async function dadoUnaGuiaCargandoUnDato() {
  await dadoUnaGuiaConElBloqueMovido();
  await cuandoSeEligeRegistrarEnElBloque();
  await esperarPaso('Cargá un dato tuyo.');
}

async function dadoUnaGuiaEnLaUltimaEtapa() {
  await dadoUnaGuiaCargandoUnDato();
  await cuandoSeGuardaElDato('Yerba mate', '8');
  await esperarPaso('Tu primera sección ya es tuya.');
}

async function dadoUnaGuiaEnLasPlantillas() {
  await dadoUnaGuiaEnElPrimerPaso();
  await cuandoSeElige(/Elegir una plantilla/);
  await esperarPaso('Una plantilla es un conjunto.');
  await screen.findByRole('heading', { name: /Elegí una base/ }, ESPERA);
}

async function dadoUnaGuiaMirandoLaPlantillaDeAlmacen() {
  await dadoUnaGuiaEnLasPlantillas();
  await cuandoSeMiraLaPlantillaDeAlmacen();
  await esperarPaso('Mirá las piezas antes de empezar.');
}

async function dadoUnaGuiaEnElLienzoDesdeCero() {
  await dadoUnaGuiaEnElPrimerPaso();
  await cuandoSeElige(/Empezar de cero/);
  await esperarPaso('Tu lienzo, desde cero.');
}

async function dadoUnaGuiaPidiendoLaPrimeraPieza() {
  await dadoUnaGuiaEnElLienzoDesdeCero();
  await cuandoSeElige(/Seguir desde cero/);
  await esperarPaso('Traé tu primera pieza.');
}

async function dadoUnaGuiaCerradaConLaX() {
  await dadoUnaGuiaEnElPrimerPaso();
  await cuandoSeSaleDelTutorial();
}

async function dadoUnaGuiaEnLasPlantillasYAsentada() {
  await dadoUnaGuiaEnLasPlantillas();
  await waitFor(() => expect(router.state.status).toBe('idle'), ESPERA);
}

async function cuandoSeElige(nombre: string | RegExp) {
  await user.click(screen.getByRole('button', { name: nombre }));
}

async function cuandoSePresionaEscape() {
  await user.keyboard('{Escape}');
}

async function cuandoSeEmpiezaLaGuia() {
  await cuandoSeElige(/Hacer mi primera sección con guía/);
  await esperarPaso('Tres maneras de empezar.');
}

async function cuandoSeIntentaIrALaZona() {
  await cuandoSeElige('Zona');
}

async function cuandoSeDescribeElNegocio(descripcion: string) {
  await user.type(await screen.findByLabelText('Contame un poco de tu negocio'), descripcion);
  await cuandoSeElige(/Pensalo conmigo/);
}

async function cuandoSeAjustaElAnchoDelSegundoBloque() {
  const botones = screen.getAllByRole('button', { name: 'Fila completa' });
  await user.click(botones[1] as HTMLElement);
}

async function cuandoSeCreaLaSeccion() {
  await cuandoSeElige(/Sí, crear mi sección/);
}

async function cuandoSePersonalizaElBloque() {
  const boton = within(bloqueDelTutorial()).getByRole('button', { name: 'Personalizar bloque' });
  await user.click(boton);
}

async function cuandoSeRenombraElBloque(nombre: string) {
  const campo = screen.getByLabelText('Nombre');
  await user.clear(campo);
  await user.type(campo, nombre);
  await user.tab();
}

async function cuandoSeMueveElBloqueAbajo() {
  await user.click(within(bloqueDelTutorial()).getByRole('button', { name: 'Mover abajo' }));
}

async function cuandoSeEligeRegistrarEnElBloque() {
  await user.click(within(bloqueDelTutorial()).getByRole('button', { name: /Registrar/ }));
}

async function cuandoSeGuardaElDato(producto: string, cantidad: string) {
  const modal = await screen.findByRole('dialog');
  await user.type(within(modal).getByLabelText(/Producto/), producto);
  await user.type(within(modal).getByLabelText(/Cantidad/), cantidad);
  await user.click(within(modal).getByRole('button', { name: 'Guardar' }));
}

async function cuandoSeSaleDelTutorial() {
  await user.click(within(cartel()).getByRole('button', { name: 'Salir del tutorial' }));
}

async function cuandoSeMiraLaPlantillaDeAlmacen() {
  const tarjeta = document.querySelector<HTMLElement>('.template-page [data-template="shop"]');
  if (!tarjeta) throw new Error('Falta la tarjeta de la plantilla');
  await user.click(within(tarjeta).getByRole('button', { name: /Ver la sección/ }));
}

async function cuandoSeAgregaLaPiezaClientes() {
  await screen.findByRole('complementary', { name: 'Piezas disponibles' }, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Agregar y organizar' }));
  await user.click(elementoDeLaBiblioteca());
  await user.click(await screen.findByRole('button', { name: 'Agregar Clientes' }, ESPERA));
}

function elementoDeLaBiblioteca() {
  const nodo = document.querySelector<HTMLElement>('[data-accion="mobile-library"]');
  if (!nodo) throw new Error('Falta el acceso a las piezas');
  return nodo;
}

async function cuandoSeVaAlInicioPorElLogo() {
  await user.click(screen.getByRole('button', { name: 'Ensambla, ir al inicio' }));
}

function entoncesLaBienvenidaTieneElFocoEnEmpezarLaGuia() {
  expect(botonDeLaBienvenida()).toHaveFocus();
}

function entoncesNoHayDialogoNiTutorialActivo() {
  expect(screen.queryByRole('dialog')).toBeNull();
  expect(useSistemaStore.getState().tutorial.activo).toBe(false);
}

async function entoncesLaBienvenidaQuedoVista() {
  await waitFor(
    () => expect(estado.preferencias.get(usuario.email)?.bienvenidaVista).toBe(true),
    ESPERA,
  );
}

async function entoncesLaBienvenidaSeCerroYQuedoVista() {
  expect(screen.queryByRole('dialog')).toBeNull();
  await entoncesLaBienvenidaQuedoVista();
}

function entoncesNoSeMuestraNingunDialogo() {
  expect(screen.queryByRole('dialog')).toBeNull();
}

function entoncesEstaEnLaEtapa1DelPrimerPaso() {
  expect(within(cartel()).getByLabelText('Etapa 1 de 5')).toBeVisible();
  expect(within(cartel()).getByText('Tocá una de las tres opciones.')).toBeVisible();
}

function entoncesHayUnaSolaPiezaResaltadaYNoSeOfreceReiniciar() {
  expect(screen.queryByText(/Reiniciar la demo/)).toBeNull();
  expect(document.querySelector('.mk-root')).toHaveClass('tutorial-running');
  expect(document.querySelectorAll('.tour-target')).toHaveLength(1);
}

async function entoncesSeRecuerdaElPrimerPaso() {
  await verifyToastAsync('Tocá una de las tres opciones.');
  expect(pasoActual()).toBe('choose');
  expect(screen.getByRole('heading', { name: /En qué puedo/ })).toBeVisible();
}

async function entoncesSeAbreElEditorEnElPasoDeCambiar() {
  await esperarPaso('De acá se cambia.');
  expect(router.state.location.pathname).toMatch(/^\/sistemas\//);
}

async function entoncesElNombreTieneElFoco() {
  await esperarPaso('Ponelo en tus palabras.');
  expect(screen.getByLabelText('Nombre')).toHaveFocus();
}

async function entoncesTerminaEnLaEtapa5() {
  await esperarPaso('Tu primera sección ya es tuya.');
  expect(within(cartel()).getByLabelText('Etapa 5 de 5')).toBeVisible();
}

async function entoncesElCartelSeCerroYSeAvisaComoRetomarlo() {
  expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
  await verifyToastAsync(AVISO_DE_SALIDA);
}

function entoncesNoQuedaNingunaPiezaResaltada() {
  expect(document.querySelector('.mk-root')).not.toHaveClass('tutorial-running');
  expect(document.querySelectorAll('.tour-target')).toHaveLength(0);
}

async function entoncesSeExplicaQueEsUnaPlantilla() {
  await esperarPaso('Una plantilla es un conjunto.');
}

async function entoncesLaGuiaSalioYSeAvisaComoRetomarla() {
  expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
  expect(useSistemaStore.getState().tutorial.activo).toBe(false);
  await verifyToastAsync(AVISO_DE_SALIDA);
}

function entoncesNoHayCartelDeGuia() {
  expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
}

async function entoncesLaGuiaTerminoSinPreguntar() {
  await verifyPathAsync(router, '/inicio');
  expect(screen.queryByLabelText('Guía interactiva de Sami')).toBeNull();
  expect(screen.queryByText(/¿Volvemos o empezamos de nuevo\?/)).toBeNull();
  expect(screen.queryByRole('dialog')).toBeNull();
}

function entoncesNoSeOfreceReiniciarLaDemo() {
  const texto = /Reiniciar la demo|Borrar todos los sistemas de esta demo/;
  expect(screen.queryByText(texto)).toBeNull();
}
