import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto, obtenerProyecto } from '@/api/proyectos';
import { listarRegistros } from '@/api/registros';
import type { TipoCampo } from '@/types/campo';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const COLUMNAS_VENTAS: [string, TipoCampo][] = [
  ['Fecha', 'FECHA'],
  ['Producto', 'TEXTO'],
  ['Cantidad', 'NUMERO'],
  ['Monto', 'NUMERO'],
];
const COLUMNAS_GASTOS: [string, TipoCampo][] = [
  ['Fecha', 'FECHA'],
  ['Concepto', 'TEXTO'],
  ['Monto', 'NUMERO'],
];

async function crearTabla(proyectoId: string, nombre: string, columnas: [string, TipoCampo][]) {
  const bloque = await crearBloque({ proyectoId, nombre });
  const ids: Record<string, string> = {};
  for (const [columna, tipo] of columnas) {
    const campo = await crearCampo({
      bloqueId: bloque.id,
      datos: {
        nombre: columna,
        tipo,
        obligatorio: false,
        valorInicial: null,
        opciones: [],
        bloqueRelacionadoId: null,
        formula: null,
      },
    });
    ids[columna] = campo.id;
  }
  return { bloque, ids };
}

async function setupNegocio({ conGastos = true }: { conGastos?: boolean } = {}) {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi negocio' });
  const ventas = await crearTabla(proyecto.id, 'Ventas', COLUMNAS_VENTAS);
  const gastos = conGastos ? await crearTabla(proyecto.id, 'Gastos', COLUMNAS_GASTOS) : undefined;
  return { proyecto, ventas, gastos };
}

async function abrirSubir() {
  renderApp('/subir');
  await verifyHeadingPresentAsync('Contanos qué pasó');
  return userEvent.setup();
}

async function contar(user: ReturnType<typeof userEvent.setup>, relato: string) {
  await user.type(await screen.findByLabelText(/qué pasó en tu negocio/i, {}, ESPERA), relato);
  await user.click(screen.getByRole('button', { name: 'Entender' }));
}

describe('Sumá datos: «Contá qué pasó»', () => {
  it('sin sistema invita a armar el negocio en la bienvenida', async () => {
    setupUsuarioConSesion();
    renderApp('/subir');

    const enlace = await screen.findByRole('link', { name: /armar mi negocio/i }, ESPERA);
    expect(enlace).toHaveAttribute('href', '/bienvenida');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });

  it('«Entender» muestra lo entendido y, aparte, lo que no entendió', async () => {
    await setupNegocio();
    const user = await abrirSubir();

    await contar(user, 'Vendí 3 velas a 4500. Gasté 12000 en cera. Hoy llovió mucho');

    expect(await screen.findByText(/Venta · 3 Velas/, {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByText(/Gasto · Cera/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'No lo entendí' })).toBeInTheDocument();
    expect(screen.getByText(/«Hoy llovió mucho»/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar 2 cosas' })).toBeInTheDocument();
  });

  it('«Quitar» saca la cosa y «Deshacer» la devuelve', async () => {
    await setupNegocio();
    const user = await abrirSubir();
    await contar(user, 'Vendí 3 velas a 4500. Gasté 12000 en cera');
    await screen.findByText(/Gasto · Cera/, {}, ESPERA);

    await user.click(screen.getByRole('button', { name: /^Quitar: Gasto/ }));
    expect(screen.queryByText(/Gasto · Cera/)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Deshacer' }));
    expect(await screen.findByText(/Gasto · Cera/)).toBeInTheDocument();
  });

  it('confirmar guarda las filas en las tablas del motor simulado', async () => {
    const { ventas, gastos } = await setupNegocio();
    const user = await abrirSubir();

    await contar(user, 'Vendí 3 velas a 4500. Gasté 12000 en cera');
    await user.click(await screen.findByRole('button', { name: 'Guardar 2 cosas' }, ESPERA));

    expect(await screen.findByText(/Listo, guardé 2 cosas/, {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver mis respuestas' })).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText(/Venta · 3 Velas/)).not.toBeInTheDocument());

    const filasVentas = await listarRegistros(ventas.bloque.id, {});
    expect(filasVentas.content).toHaveLength(1);
    expect(filasVentas.content[0]?.valores).toMatchObject({
      [ventas.ids['Producto'] as string]: 'Velas',
      [ventas.ids['Cantidad'] as string]: 3,
      [ventas.ids['Monto'] as string]: 4500,
    });
    const filasGastos = await listarRegistros(gastos?.bloque.id ?? '', {});
    expect(filasGastos.content[0]?.valores).toMatchObject({
      [gastos?.ids['Concepto'] as string]: 'Cera',
      [gastos?.ids['Monto'] as string]: 12000,
    });
  });

  it('corregir el monto cambia lo que se guarda', async () => {
    const { ventas } = await setupNegocio();
    const user = await abrirSubir();
    await contar(user, 'Vendí 3 velas a 4500');

    await user.click(await screen.findByRole('button', { name: /^Corregir el monto/ }, ESPERA));
    const monto = screen.getByLabelText('Monto de la venta');
    await user.clear(monto);
    await user.type(monto, '5000');
    await user.click(screen.getByRole('button', { name: 'Guardar 1 cosa' }));
    await screen.findByText(/Listo, guardé 1 cosa/, {}, ESPERA);

    const filas = await listarRegistros(ventas.bloque.id, {});
    expect(filas.content[0]?.valores[ventas.ids['Monto'] as string]).toBe(5000);
  });

  it('si falta una pieza, ofrece sumarla y guardar', async () => {
    const { proyecto } = await setupNegocio({ conGastos: false });
    const user = await abrirSubir();

    await contar(user, 'Gasté 12000 en cera');
    const boton = await screen.findByRole('button', { name: 'Sumar Gastos y guardar' }, ESPERA);
    await user.click(boton);

    expect(await screen.findByText(/Listo, guardé 1 cosa/, {}, ESPERA)).toBeInTheDocument();
    const sistema = await obtenerProyecto(proyecto.id);
    const gastos = sistema.bloques.find((bloque) => bloque.nombre === 'Gastos');
    expect(gastos).toBeDefined();
    const filas = await listarRegistros(gastos?.id ?? '', {});
    expect(filas.content).toHaveLength(1);
  });

  it('muestra «Pronto» en la planilla y enlaces al cuaderno', async () => {
    await setupNegocio();
    await abrirSubir();

    expect(await screen.findByText('Pronto')).toBeInTheDocument();
    const enlaces = screen.getAllByRole('link', { name: /subir (foto|lista)/i });
    expect(enlaces).toHaveLength(2);
    for (const enlace of enlaces) expect(enlace).toHaveAttribute('href', '/cuaderno');
  });

  it('no tiene violaciones de accesibilidad (vacía y con filas entendidas)', async () => {
    await setupNegocio();
    const user = await abrirSubir();
    const reglas = { rules: { 'color-contrast': { enabled: false } } };

    expect((await axe(document.body, reglas)).violations).toEqual([]);

    await contar(user, 'Vendí 3 velas a 4500. Hoy llovió mucho');
    await screen.findByText(/Venta · 3 Velas/, {}, ESPERA);
    expect((await axe(document.body, reglas)).violations).toEqual([]);
  });
});
