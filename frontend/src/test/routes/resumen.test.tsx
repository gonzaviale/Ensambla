import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { ultimosDias } from '@/lib/sistema/periodoResumen';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let modal: HTMLElement;
let sistemaId = '';
let direcciones: URL[] = [];

describe('el resumen del negocio en PDF', () => {
  beforeEach(() => guardarArchivo.mockClear());

  it('con Premium el modal ofrece los últimos 30 días', async () => {
    dadoUnServidorQueEntregaElPdf();

    await cuandoSeAbreElResumenConPremium();

    entoncesElModalOfreceLosUltimos30Dias();
  });

  it('con Premium se baja el PDF con el nombre que manda el servidor', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeDescargaElPdf();

    await entoncesSeGuardoElArchivo('resumen-almacen-20261019.pdf');
  });

  it('el pedido del PDF lleva la sección y los últimos 30 días', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeDescargaElPdf();

    await entoncesSePidioElPdfDeLosUltimos30Dias();
  });

  it('al bajar el PDF se cierra el modal', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeDescargaElPdf();

    await entoncesSeCerroElModal();
  });

  it('el período se elige entre los últimos 7, 30 o 90 días', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeElijeElPeriodoYSeDescarga('7');

    await entoncesElPedidoEmpiezaEn(ultimosDias(7).desde);
  });

  it('también se pueden elegir las fechas', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();
    await cuandoSeElijenFechasPropias();

    await cuandoSeCorrigeElDesdeYSeDescarga();

    await entoncesSePidioElPeriodo('2026-10-01', '2026-10-07');
  });

  it('se avisa si faltan las fechas', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeElijeOtroPeriodoSinFechas();

    await entoncesSeAvisa('Elegí las dos fechas.');
  });

  it('se avisa si las fechas están al revés y no se pide nada', async () => {
    dadoUnServidorQueEntregaElPdf();
    await cuandoSeAbreElResumenConPremium();
    await cuandoSeElijeOtroPeriodoSinFechas();

    await cuandoSeEscribenLasFechasAlReves();

    await entoncesSeAvisa('La fecha desde no puede ser posterior a la fecha hasta.');
    entoncesNoSePidioNingunPdf();
  });

  it('si el servidor no puede armarlo, lo dice', async () => {
    dadoUnServidorQueFalla();
    await cuandoSeAbreElResumenConPremium();

    await cuandoSeDescargaElPdf();

    await entoncesSeAvisaElErrorYElModalSigueAbierto();
  });

  it('sin Premium el modal explica que es una función paga', async () => {
    await cuandoSeAbreElResumenSinPremium();

    await entoncesElModalExplicaQueEsPremiumSinOfrecerLaDescarga();
  });

  it('sin Premium el modal lleva a los planes', async () => {
    await cuandoSeAbreElResumenSinPremium();

    await cuandoSePideVerLosPlanes();

    await entoncesSeMuestranLosPlanes();
  });

  it('el botón también está al editar la sección', async () => {
    await cuandoSeAbreElEditorDeLaSeccion();

    entoncesEstaElBotonDelResumen();
  });
});

function dadoUnServidorQueEntregaElPdf() {
  direcciones = [];
  server.use(
    http.get('/api/proyectos/:sistemaId/resumen/pdf', ({ request }) => {
      direcciones.push(new URL(request.url));
      return new HttpResponse(new TextEncoder().encode('%PDF-1.4'), {
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="resumen-almacen-20261019.pdf"',
        },
      });
    }),
  );
}

function dadoUnServidorQueFalla() {
  setupServerError('get', '/proyectos/:sistemaId/resumen/pdf', 500, 'Error interno');
}

async function abrirAlmacen(premium: boolean) {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (premium) {
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 1000).toISOString(),
      finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });
  }
  const sistema = crearAlmacenDeEjemplo(hoy());
  sistemaId = sistema.id;
  estado.sistemas.set(usuario.email, [sistema]);
  user = userEvent.setup();
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
  await user.click(await screen.findByRole('button', { name: /Resumen en PDF/ }, ESPERA));
  modal = await screen.findByRole('dialog', {}, ESPERA);
}

async function cuandoSeAbreElResumenConPremium() {
  await abrirAlmacen(true);
  // El botón espera a saber qué plan tiene la persona.
  await waitFor(
    () => expect(within(modal).getByRole('button', { name: /Descargar PDF/ })).toBeEnabled(),
    ESPERA,
  );
}

async function cuandoSeAbreElResumenSinPremium() {
  await abrirAlmacen(false);
}

async function cuandoSeAbreElEditorDeLaSeccion() {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp(`/sistemas/${sistema.id}?modo=construir`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
}

const botonDeDescarga = () => within(modal).getByRole('button', { name: /Descargar PDF/ });

async function cuandoSeDescargaElPdf() {
  await user.click(botonDeDescarga());
}

async function cuandoSeElijeElPeriodoYSeDescarga(periodo: string) {
  await user.selectOptions(within(modal).getByLabelText('Período'), periodo);
  await cuandoSeDescargaElPdf();
}

async function cuandoSeElijeOtroPeriodoSinFechas() {
  await user.selectOptions(within(modal).getByLabelText('Período'), 'otro');
  await cuandoSeDescargaElPdf();
}

async function cuandoSeEscribenLasFechasAlReves() {
  await user.type(within(modal).getByLabelText('Desde'), '2026-10-08');
  await user.type(within(modal).getByLabelText('Hasta'), '2026-10-07');
}

async function cuandoSeElijenFechasPropias() {
  await cuandoSeElijeOtroPeriodoSinFechas();
  await cuandoSeEscribenLasFechasAlReves();
}

async function cuandoSeCorrigeElDesdeYSeDescarga() {
  await user.clear(within(modal).getByLabelText('Desde'));
  await user.type(within(modal).getByLabelText('Desde'), '2026-10-01');
  await cuandoSeDescargaElPdf();
}

async function cuandoSePideVerLosPlanes() {
  await user.click(await within(modal).findByRole('link', { name: 'Ver planes' }, ESPERA));
}

function entoncesElModalOfreceLosUltimos30Dias() {
  const titulo = within(modal).getByRole('heading', { name: 'Resumen de tu negocio en PDF' });
  expect(titulo).toBeVisible();
  expect(within(modal).getByLabelText('Período')).toHaveValue('30');
}

async function entoncesSeGuardoElArchivo(nombre: string) {
  await waitFor(() => expect(guardarArchivo).toHaveBeenCalledTimes(1), ESPERA);
  expect(guardarArchivo.mock.calls[0]?.[0]).toMatchObject({ nombre });
}

async function entoncesSePidioElPdfDeLosUltimos30Dias() {
  await waitFor(() => expect(direcciones).toHaveLength(1), ESPERA);
  expect(direcciones[0]?.pathname).toBe(`/api/proyectos/${sistemaId}/resumen/pdf`);
  expect(direcciones[0]?.searchParams.get('desde')).toBe(ultimosDias(30).desde);
  expect(direcciones[0]?.searchParams.get('hasta')).toBe(hoy());
}

async function entoncesSeCerroElModal() {
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument(), ESPERA);
}

async function entoncesElPedidoEmpiezaEn(desde: string) {
  await waitFor(() => expect(direcciones).toHaveLength(1), ESPERA);
  expect(direcciones[0]?.searchParams.get('desde')).toBe(desde);
}

async function entoncesSePidioElPeriodo(desde: string, hasta: string) {
  await waitFor(() => expect(direcciones).toHaveLength(1), ESPERA);
  expect(direcciones[0]?.searchParams.get('desde')).toBe(desde);
  expect(direcciones[0]?.searchParams.get('hasta')).toBe(hasta);
}

async function entoncesSeAvisa(mensaje: string) {
  expect(await within(modal).findByRole('alert')).toHaveTextContent(mensaje);
}

function entoncesNoSePidioNingunPdf() {
  expect(direcciones).toHaveLength(0);
}

async function entoncesSeAvisaElErrorYElModalSigueAbierto() {
  expect(await within(modal).findByRole('alert')).toBeVisible();
  expect(guardarArchivo).not.toHaveBeenCalled();
  expect(screen.getByRole('dialog')).toBeVisible();
}

async function entoncesElModalExplicaQueEsPremiumSinOfrecerLaDescarga() {
  expect(await within(modal).findByText('El resumen en PDF es parte de Premium.')).toBeVisible();
  expect(within(modal).queryByRole('button', { name: /Descargar PDF/ })).not.toBeInTheDocument();
}

async function entoncesSeMuestranLosPlanes() {
  const titulo = await screen.findByRole(
    'heading',
    { name: 'Gratuito, tu espacio actual' },
    ESPERA,
  );
  expect(titulo).toBeVisible();
}

function entoncesEstaElBotonDelResumen() {
  expect(screen.getByRole('button', { name: /Resumen en PDF/ })).toBeVisible();
}
