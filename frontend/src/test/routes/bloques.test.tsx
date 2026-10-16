import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { agregarRegistro } from '@/mocks/db/registros';
import { useSuscripcionStore } from '@/stores/suscripcion';
import type { GuardarCampo } from '@/types/campo';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const columna = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'TEXTO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

async function sembrarTabla(proyectoId: string, nombre: string, nombreDelDato = 'Concepto') {
  const bloque = await crearBloque({ proyectoId, nombre });
  const campo = await crearCampo({
    bloqueId: bloque.id,
    datos: columna({ nombre: nombreDelDato }),
  });
  return { bloque, campo };
}

/** Un sistema con las tablas dadas; `primera` es la tabla del primer nombre. */
async function setupSistema(primera: string, ...otras: string[]) {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi negocio' });
  const tabla = await sembrarTabla(proyecto.id, primera);
  for (const nombre of otras) await sembrarTabla(proyecto.id, nombre);
  return { proyecto, bloque: tabla.bloque, campo: tabla.campo };
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('Mis bloques (/bloques)', () => {
  it('sin sistema invita a la bienvenida', async () => {
    setupUsuarioConSesion();
    renderApp('/bloques');

    await verifyHeadingPresentAsync('Tu sistema, bloque a bloque');
    const invitacion = await screen.findByRole('link', { name: 'Armar mi sistema' }, ESPERA);
    expect(invitacion).toHaveAttribute('href', '/bienvenida');
  });

  it('muestra una tarjeta por bloque con su cantidad de registros', async () => {
    const ventas = await setupSistema('Ventas', 'Gastos');
    agregarRegistro(ventas.bloque.id, { [ventas.campo.id]: 'Pan' }, '2026-10-01T10:00:00Z');
    agregarRegistro(ventas.bloque.id, { [ventas.campo.id]: 'Leche' }, '2026-10-02T10:00:00Z');
    renderApp('/bloques');

    const enlaceVentas = await screen.findByRole('link', { name: 'Ventas' }, ESPERA);
    expect(enlaceVentas).toHaveAttribute('href', `/bloques/${ventas.bloque.id}`);
    const tarjetas = document.querySelector('[data-recorrido="bloques-tarjetas"]') as HTMLElement;
    await waitFor(() => expect(within(tarjetas).getByText('2 registros')).toBeInTheDocument());
    expect(within(tarjetas).getByText('Todavía sin datos')).toBeInTheDocument();
    expect(within(tarjetas).getAllByText('Desbloquea:').length).toBeGreaterThan(0);
    expect(within(tarjetas).getAllByRole('link')).toHaveLength(2);
  });

  it('«Sumar» agrega el bloque y aparece entre las tarjetas', async () => {
    await setupSistema('Ventas');
    const user = userEvent.setup();
    renderApp('/bloques');

    await user.click(await screen.findByRole('button', { name: 'Sumar Gastos' }, ESPERA));

    const tarjetas = document.querySelector('[data-recorrido="bloques-tarjetas"]') as HTMLElement;
    expect(await within(tarjetas).findByRole('link', { name: 'Gastos' }, ESPERA)).toBeVisible();
    expect(await screen.findByText('Sumaste «Gastos» a tu sistema.', {}, ESPERA)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Sumar Gastos' })).not.toBeInTheDocument();
  });

  it('con 3 bloques y la prueba vencida muestra el candado en vez de «Sumar»', async () => {
    await setupSistema('Ventas', 'Gastos', 'Productos');
    useSuscripcionStore.getState().simularFinDePrueba();
    renderApp('/bloques');

    expect(
      await screen.findByRole('heading', { name: 'Todos los bloques que quieras' }, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: /^Sumar / })).not.toBeInTheDocument();
    expect(
      within(screen.getByRole('region', { name: 'Todos los bloques que quieras' })).getByRole(
        'link',
        { name: /Probar 7 días gratis|Ver planes/ },
      ),
    ).toHaveAttribute('href', '/plan');
  });

  it('no tiene violaciones de accesibilidad', async () => {
    await setupSistema('Ventas', 'Gastos');
    renderApp('/bloques');
    await screen.findByRole('link', { name: 'Ventas' }, ESPERA);
    await screen.findByRole('button', { name: 'Sumar Productos' }, ESPERA);
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    await sinViolaciones();
  });
});

describe('Un bloque (/bloques/$bloqueId)', () => {
  it('muestra el formulario y las últimas filas como tarjetas', async () => {
    const { proyecto, bloque, campo } = await setupSistema('Ventas');
    agregarRegistro(bloque.id, { [campo.id]: 'Pan lactal' }, '2026-10-01T10:00:00Z');
    agregarRegistro(bloque.id, { [campo.id]: 'Yerba' }, '2026-10-02T10:00:00Z');
    renderApp(`/bloques/${bloque.id}`);

    await verifyHeadingPresentAsync('Ventas');
    expect(
      within(screen.getByRole('navigation', { name: 'Dónde estás' })).getByRole('link', {
        name: 'Mis bloques',
      }),
    ).toHaveAttribute('href', '/bloques');
    expect(await screen.findByLabelText('Concepto', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar fila' })).toBeVisible();

    const ultimas = screen.getByRole('region', { name: 'Lo último que cargaste' });
    const filas = await within(ultimas).findAllByRole('listitem', {}, ESPERA);
    expect(filas).toHaveLength(2);
    expect(within(ultimas).getByText('Pan lactal')).toBeVisible();
    expect(within(ultimas).getAllByText('Concepto:')).toHaveLength(2);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();

    const verTodo = screen.getByRole('link', { name: 'Ver todo' });
    expect(verTodo).toHaveAttribute(
      'href',
      expect.stringContaining(`/proyectos/${proyecto.id}/usar`),
    );
    expect(verTodo).toHaveAttribute('href', expect.stringContaining(`bloque=${bloque.id}`));
  });

  it('un bloque sin filas lo dice, y al guardar aparece la fila nueva', async () => {
    const { bloque } = await setupSistema('Gastos');
    const user = userEvent.setup();
    renderApp(`/bloques/${bloque.id}`);

    expect(
      await screen.findByText('Todavía no cargaste nada en este bloque', {}, ESPERA),
    ).toBeVisible();
    await user.type(await screen.findByLabelText('Concepto', {}, ESPERA), 'Luz');
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));

    expect(
      await screen.findByText('Fila guardada. Ya podés cargar otra.', {}, ESPERA),
    ).toBeVisible();
    const ultimas = screen.getByRole('region', { name: 'Lo último que cargaste' });
    expect(await within(ultimas).findByText('Luz', {}, ESPERA)).toBeVisible();
  });

  it('un bloque que no existe invita a volver a Mis bloques', async () => {
    await setupSistema('Ventas');
    renderApp('/bloques/00000000-0000-4000-8000-000000000000');

    expect(await screen.findByText('No encontramos este bloque', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Volver a Mis bloques' })).toHaveAttribute(
      'href',
      '/bloques',
    );
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { bloque, campo } = await setupSistema('Ventas');
    agregarRegistro(bloque.id, { [campo.id]: 'Pan' }, '2026-10-01T10:00:00Z');
    renderApp(`/bloques/${bloque.id}`);

    await screen.findByText('Pan', {}, ESPERA);
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    await sinViolaciones();
  });
});
