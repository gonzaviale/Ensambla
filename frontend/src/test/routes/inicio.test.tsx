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

/** Un bloque de Ventas o Gastos (Concepto/Producto + Monto) con una fila de monto conocido. */
async function sembrarBloque(proyectoId: string, nombre: string, monto: number | null) {
  const bloque = await crearBloque({ proyectoId, nombre });
  const campoMonto = await crearCampo({
    bloqueId: bloque.id,
    datos: columna({ nombre: 'Monto', tipo: 'NUMERO' }),
  });
  if (monto !== null) agregarRegistro(bloque.id, { [campoMonto.id]: monto });
  return bloque;
}

async function sistema(nombre = 'Almacén Don Pepe') {
  setupUsuarioConSesion();
  return crearProyecto({ nombre });
}

async function sinViolaciones() {
  const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('Inicio: Mi negocio', () => {
  it('sin sistema invita a armarlo en la bienvenida', async () => {
    setupUsuarioConSesion();

    renderApp('/inicio');

    expect(
      await screen.findByRole('heading', { name: 'Todavía no armaste tu sistema' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Armemos tu sistema' })).toHaveAttribute(
      'href',
      '/bienvenida',
    );
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });

  it('con Ventas y Gastos con datos muestra cuánto te quedó', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);
    await sembrarBloque(proyecto.id, 'Gastos', 30000);

    renderApp('/inicio');

    await verifyHeadingPresentAsync('Almacén Don Pepe');
    const titulo = await screen.findByRole(
      'heading',
      { name: '¿Cuánto te quedó este mes?' },
      ESPERA,
    );
    const tarjeta = titulo.closest('section') as HTMLElement;
    expect(tarjeta).toHaveTextContent(/70\.000/);
    expect(tarjeta).toHaveTextContent(/Vendiste .*100\.000 y gastaste .*30\.000/);
    expect(screen.getByRole('link', { name: 'Contá qué pasó' })).toHaveAttribute('href', '/subir');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });

  it('sumar la pieza que falta desbloquea la respuesta', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 50000);
    const user = userEvent.setup();

    renderApp('/inicio');

    expect(
      await screen.findByRole('heading', { name: '¿Cuánto vendiste este mes?' }, ESPERA),
    ).toBeInTheDocument();
    // Antes: «¿Cuánto te quedó?» está bloqueada y pide Gastos.
    expect(
      screen.queryByRole('heading', { name: '¿Cuánto te quedó este mes?' }),
    ).toBeInTheDocument();
    const boton = await screen.findByRole('button', { name: /Sumar Gastos/ }, ESPERA);
    expect(screen.getByText(/Desbloqueala sumando/)).toHaveTextContent('Gastos');

    await user.click(boton);

    expect(
      await screen.findByText(
        'Listo: ya podés ver ¿Cuánto te quedó este mes?',
        { exact: false },
        ESPERA,
      ),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: /Sumar Gastos/ })).not.toBeInTheDocument(),
    );
    // Ahora está en la lista de respuestas listas.
    const titulo = await screen.findByRole(
      'heading',
      { name: '¿Cuánto te quedó este mes?' },
      ESPERA,
    );
    expect(titulo.closest('section')).toHaveTextContent(/50\.000/);
  });

  it('con la prueba vencida una respuesta que no es básica muestra el candado', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);
    await sembrarBloque(proyecto.id, 'Gastos', 30000);
    await sembrarBloque(proyecto.id, 'Fiados', 15000);
    useSuscripcionStore.getState().simularFinDePrueba();

    renderApp('/inicio');

    expect(
      await screen.findByRole('heading', { name: 'Todas las respuestas' }, ESPERA),
    ).toBeInTheDocument();
    expect(
      screen.getAllByRole('link', { name: /Probar 7 días gratis|Ver planes/ })[0],
    ).toHaveAttribute('href', '/plan');
    // Lo básico se sigue viendo, sin la comparación (que es del plan Negocio).
    expect(
      await screen.findByRole('heading', { name: '¿Cuánto te quedó este mes?' }, ESPERA),
    ).toBeInTheDocument();
    // Ya tiene 3 bloques: sumar otro invita a mejorar (el candado está en la tarjeta de desbloqueo).
    expect(screen.queryByRole('button', { name: /^Sumar / })).not.toBeInTheDocument();
  });

  it('muestra los primeros pasos y los tilda según los datos', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', null);

    renderApp('/inicio');

    const lista = (
      await screen.findByRole('heading', { name: 'Tus primeros pasos' }, ESPERA)
    ).closest('section') as HTMLElement;
    expect(within(lista).getByText(/Armaste tu sistema/)).toBeInTheDocument();
    expect(within(lista).getAllByText(/Falta:/)).toHaveLength(2);
    expect(lista).toHaveAttribute('data-recorrido', 'inicio-checklist');
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const proyecto = await sistema();
    await sembrarBloque(proyecto.id, 'Ventas', 100000);
    await sembrarBloque(proyecto.id, 'Gastos', 30000);

    renderApp('/inicio');
    await screen.findByRole('heading', { name: '¿Cuánto te quedó este mes?' }, ESPERA);

    await sinViolaciones();
  });

  it('sin sistema tampoco tiene violaciones de accesibilidad', async () => {
    setupUsuarioConSesion();

    renderApp('/inicio');
    await screen.findByRole('heading', { name: 'Todavía no armaste tu sistema' }, ESPERA);

    await sinViolaciones();
  });
});
