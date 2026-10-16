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
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const columna = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'TEXTO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

async function sembrarBloque(proyectoId: string, nombre: string, monto: number | null) {
  const bloque = await crearBloque({ proyectoId, nombre });
  const campoMonto = await crearCampo({
    bloqueId: bloque.id,
    datos: columna({ nombre: 'Monto', tipo: 'NUMERO' }),
  });
  if (monto !== null) agregarRegistro(bloque.id, { [campoMonto.id]: monto });
  return bloque;
}

async function sistema() {
  setupUsuarioConSesion();
  return crearProyecto({ nombre: 'Almacén Don Pepe' });
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('Respuestas', () => {
  it('sin sistema invita a armarlo', async () => {
    setupUsuarioConSesion();

    renderApp('/respuestas');

    expect(
      await screen.findByRole('heading', { name: 'Todavía no hay respuestas' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Armemos tu sistema' })).toHaveAttribute(
      'href',
      '/bienvenida',
    );
  });

  it('agrupa lo que ya sabés y lo que falta desbloquear, y abre el detalle', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);
    await sembrarBloque(proyecto.id, 'Gastos', 30000);
    const user = userEvent.setup();

    renderApp('/respuestas');

    const sabes = (await screen.findByRole('heading', { name: 'Lo que ya sabés' }, ESPERA)).closest(
      'section',
    ) as HTMLElement;
    expect(within(sabes).getByText('¿Cuánto te quedó este mes?')).toBeInTheDocument();
    const faltan = screen.getByRole('heading', { name: 'Para desbloquear' }).closest('section');
    expect(
      within(faltan as HTMLElement).getAllByRole('button', { name: /^Sumar / }).length,
    ).toBeGreaterThan(0);

    await user.click(within(sabes).getByRole('button', { name: /¿Cuánto te quedó este mes\?/ }));

    const detalle = await screen.findByRole(
      'dialog',
      { name: '¿Cuánto te quedó este mes?' },
      ESPERA,
    );
    expect(detalle).toHaveTextContent(/70\.000/);
    expect(detalle).toHaveTextContent(/Vendiste/);
    await sinViolaciones();
  });

  it('sumar la pieza que falta pasa la respuesta a la lista', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 50000);
    const user = userEvent.setup();

    renderApp('/respuestas');

    const faltan = (
      await screen.findByRole('heading', { name: 'Para desbloquear' }, ESPERA)
    ).closest('section') as HTMLElement;
    await user.click(
      within(faltan).getAllByRole('button', { name: 'Sumar Gastos' })[0] as HTMLElement,
    );

    await waitFor(() => {
      const sabes = screen.getByRole('heading', { name: 'Lo que ya sabés' }).closest('section');
      expect(
        within(sabes as HTMLElement).getByText('¿Cuánto te quedó este mes?'),
      ).toBeInTheDocument();
    }, ESPERA);
  });

  it('con la prueba vencida las respuestas completas quedan con candado y la planilla también', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);
    await sembrarBloque(proyecto.id, 'Gastos', 30000);
    await sembrarBloque(proyecto.id, 'Fiados', 15000);
    useSuscripcionStore.getState().simularFinDePrueba();

    renderApp('/respuestas');

    expect(
      await screen.findByRole('heading', { name: 'Todas las respuestas' }, ESPERA),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Descargar tus datos en planilla' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Descargar planilla/ })).not.toBeInTheDocument();
    await sinViolaciones();
  });

  it('con el plan completo ofrece descargar la planilla de Ventas', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);

    renderApp('/respuestas');

    expect(
      await screen.findByRole('button', { name: 'Descargar planilla de Ventas' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });
});
