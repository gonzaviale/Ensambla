import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { axe } from 'vitest-axe';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nombreDeArchivo } from '@/api/client';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { descargarReporte, generarReporte } from '@/api/reportes';
import { agregarRegistro } from '@/mocks/db/registros';
import { errorResponse } from '@/mocks/errores';
import { server } from '@/mocks/node';
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

/* setup: Pedidos con Cliente, Cantidad, Estado (Pendiente | Entregado) y Pagado (sí/no) */

async function setupPedidos() {
  setupUsuarioConSesion();
  const proyecto = await crearProyecto({ nombre: 'Mi negocio' });
  const pedidos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Pedidos' });
  const cliente = await crearCampo({ bloqueId: pedidos.id, datos: columna({ nombre: 'Cliente' }) });
  const cantidad = await crearCampo({
    bloqueId: pedidos.id,
    datos: columna({ nombre: 'Cantidad', tipo: 'NUMERO' }),
  });
  const estado = await crearCampo({
    bloqueId: pedidos.id,
    datos: columna({ nombre: 'Estado', tipo: 'OPCION', opciones: ['Pendiente', 'Entregado'] }),
  });
  const pagado = await crearCampo({
    bloqueId: pedidos.id,
    datos: columna({ nombre: 'Pagado', tipo: 'SI_NO' }),
  });
  const fila = (
    nombre: string,
    cant: number,
    estadoFila: string,
    pagada: boolean,
    creadoEn: string,
  ) =>
    agregarRegistro(
      pedidos.id,
      {
        [cliente.id]: nombre,
        [cantidad.id]: cant,
        [estado.id]: estadoFila,
        [pagado.id]: pagada,
      },
      creadoEn,
    );
  fila('Beto', 10, 'Entregado', true, '2026-10-01T15:00:00Z');
  fila('Ana', 5.5, 'Pendiente', false, '2026-10-05T15:00:00Z');
  fila('Carla', 20, 'Entregado', false, '2026-10-10T15:00:00Z');
  return { proyecto, pedidos, cliente, cantidad, estado, pagado, fila };
}

async function abrirReportes(proyectoId: string) {
  renderApp(`/proyectos/${proyectoId}/reportes`);
  await verifyHeadingPresentAsync('Mi negocio');
  return screen.findByRole('form', { name: 'Filtros del reporte' }, ESPERA);
}

async function sinViolaciones(elemento: HTMLElement) {
  const resultado = await axe(elemento, { rules: { 'color-contrast': { enabled: false } } });
  const resumen = resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`);
  expect(resumen).toEqual([]);
}

describe('ENS-161: la API simulada de reportes', () => {
  it('trae las filas más nuevas primero y los totales de todas las que cumplen el filtro', async () => {
    const { pedidos, cantidad, cliente } = await setupPedidos();

    const reporte = await generarReporte(pedidos.id, {});

    expect(reporte.filas.map((fila) => fila.valores[cliente.id])).toEqual(['Carla', 'Ana', 'Beto']);
    expect(reporte.totales).toEqual({ cantidad: 3, sumas: { [cantidad.id]: 35.5 } });
    expect(reporte).toMatchObject({ truncado: false, maximoFilas: 50 });
  });

  it('filtra por texto, por fechas inclusive (hora de Argentina) y por el valor de una columna', async () => {
    const { pedidos, cantidad, estado, pagado, cliente } = await setupPedidos();
    const clientes = async (filtro: Parameters<typeof generarReporte>[1]) =>
      (await generarReporte(pedidos.id, filtro)).filas.map((fila) => fila.valores[cliente.id]);

    expect(await clientes({ q: 'ana' })).toEqual(['Ana']);
    expect(await clientes({ desde: '2026-10-05', hasta: '2026-10-10' })).toEqual(['Carla', 'Ana']);
    expect(await clientes({ desde: '2026-10-06' })).toEqual(['Carla']);
    expect(await clientes({ hasta: '2026-10-01' })).toEqual(['Beto']);
    expect(await clientes({ campoId: estado.id, valor: 'Entregado' })).toEqual(['Carla', 'Beto']);
    expect(await clientes({ campoId: pagado.id, valor: 'false' })).toEqual(['Carla', 'Ana']);
    const entregadosDeOctubre = await generarReporte(pedidos.id, {
      desde: '2026-10-01',
      hasta: '2026-10-31',
      campoId: estado.id,
      valor: 'Entregado',
    });
    expect(entregadosDeOctubre.totales).toEqual({ cantidad: 2, sumas: { [cantidad.id]: 30 } });
  });

  it('rechaza fechas al revés y columnas que no se pueden filtrar', async () => {
    const { pedidos, cliente, estado } = await setupPedidos();
    const error = (filtro: Parameters<typeof generarReporte>[1]) =>
      generarReporte(pedidos.id, filtro).catch((e: unknown) => e);

    expect(await error({ desde: '2026-10-10', hasta: '2026-10-01' })).toMatchObject({
      status: 400,
      message: 'La fecha desde no puede ser posterior a la fecha hasta',
    });
    expect(await error({ campoId: cliente.id, valor: 'Beto' })).toMatchObject({
      message: 'No se puede filtrar por ese campo',
    });
    expect(await error({ campoId: estado.id, valor: 'Perdido' })).toMatchObject({ status: 400 });
  });

  it('muestra las primeras 50 filas y avisa que hay más, con los totales de todas', async () => {
    const { pedidos, fila, cantidad } = await setupPedidos();
    for (let n = 0; n < 57; n++)
      fila(`Cliente ${n}`, 1, 'Pendiente', false, '2026-10-12T10:00:00Z');

    const reporte = await generarReporte(pedidos.id, {});

    expect(reporte.filas).toHaveLength(50);
    expect(reporte.truncado).toBe(true);
    expect(reporte.totales).toMatchObject({ cantidad: 60, sumas: { [cantidad.id]: 92.5 } });
  });

  it('el archivo trae todas las filas del filtro y los mismos totales que el reporte', async () => {
    const { pedidos, cantidad, estado } = await setupPedidos();
    const filtro = { campoId: estado.id, valor: 'Entregado' };

    const reporte = await generarReporte(pedidos.id, filtro);
    const archivo = await descargarReporte(pedidos.id, filtro);

    const lineas = (await archivo.blob.text()).split('\n');
    expect(archivo.nombre).toMatch(/^reporte-pedidos-\d{8}\.xlsx$/);
    expect(lineas[0]).toBe('Cliente\tCantidad\tEstado\tPagado\tCargado');
    expect(lineas).toHaveLength(1 + reporte.totales.cantidad + 1);
    expect(lineas.at(-1)?.split('\t')[1]).toBe(String(reporte.totales.sumas[cantidad.id]));
  });
});

describe('ENS-161: pedir un reporte y descargarlo', () => {
  let guardados: { nombre: string; contenido: Promise<string> }[];

  beforeEach(() => {
    guardados = [];
    let ultimoBlob: Blob | undefined;
    // jsdom no implementa estas dos: se las agrega para guardar lo que la pantalla intenta descargar.
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      writable: true,
      value: (blob: Blob) => {
        ultimoBlob = blob;
        return 'blob:reporte';
      },
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      writable: true,
      value: () => undefined,
    });
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      guardados.push({
        nombre: this.download,
        contenido: ultimoBlob?.text() ?? Promise.resolve(''),
      });
    });
  });

  afterEach(() => {
    Reflect.deleteProperty(URL, 'createObjectURL');
    Reflect.deleteProperty(URL, 'revokeObjectURL');
    vi.restoreAllMocks();
  });

  it('muestra los totales y las filas de la tabla y descarga el mismo reporte', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    await abrirReportes(proyecto.id);

    expect(await screen.findByText('3 filas en el reporte', {}, ESPERA)).toBeVisible();
    const resumen = screen.getByLabelText('Totales del reporte');
    expect(within(resumen).getByText('Total de Cantidad')).toBeVisible();
    expect(within(resumen).getByText('35,5')).toBeVisible();
    const tabla = screen.getByRole('table', { name: 'Filas del reporte de Pedidos' });
    expect(within(tabla).getAllByRole('row')).toHaveLength(4);
    expect(within(tabla).getByText('Carla')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Descargar Excel (XLSX)' }));

    await waitFor(() => expect(guardados).toHaveLength(1), ESPERA);
    expect(guardados[0]?.nombre).toMatch(/^reporte-pedidos-\d{8}\.xlsx$/);
    const lineas = (await guardados[0]?.contenido)?.split('\n') ?? [];
    expect(lineas.at(-1)).toContain('Total (3 filas)');
    expect(lineas.at(-1)?.split('\t')[1]).toBe('35.5');
  });

  it('aplica los filtros y los totales en pantalla coinciden con los del archivo', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    const filtros = await abrirReportes(proyecto.id);
    await screen.findByText('3 filas en el reporte', {}, ESPERA);

    await user.type(within(filtros).getByLabelText('Cargado desde'), '2026-10-02');
    await user.selectOptions(within(filtros).getByLabelText('Filtrar por una columna'), 'Estado');
    await user.selectOptions(within(filtros).getByLabelText('Valor de Estado'), 'Entregado');
    await user.click(within(filtros).getByRole('button', { name: 'Ver reporte' }));

    expect(await screen.findByText('1 fila en el reporte', {}, ESPERA)).toBeVisible();
    const resumen = screen.getByLabelText('Totales del reporte');
    expect(within(resumen).getByText('20')).toBeVisible();
    const tabla = screen.getByRole('table', { name: 'Filas del reporte de Pedidos' });
    expect(within(tabla).getByText('Carla')).toBeVisible();
    expect(within(tabla).queryByText('Beto')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Descargar Excel (XLSX)' }));

    await waitFor(() => expect(guardados).toHaveLength(1), ESPERA);
    const lineas = (await guardados[0]?.contenido)?.split('\n') ?? [];
    expect(lineas).toHaveLength(3);
    expect(lineas[1]).toContain('Carla');
    expect(lineas[2]?.split('\t')[1]).toBe('20');
  });

  it('limpia los filtros y vuelve a mostrar todo', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    const filtros = await abrirReportes(proyecto.id);
    await user.type(within(filtros).getByLabelText('Buscar en Pedidos'), 'ana');
    await user.click(within(filtros).getByRole('button', { name: 'Ver reporte' }));
    expect(await screen.findByText('1 fila en el reporte', {}, ESPERA)).toBeVisible();

    await user.click(within(filtros).getByRole('button', { name: 'Limpiar filtros' }));

    expect(await screen.findByText('3 filas en el reporte', {}, ESPERA)).toBeVisible();
    expect(within(filtros).getByLabelText('Buscar en Pedidos')).toHaveValue('');
  });

  it('explica cuando ninguna fila cumple los filtros y no deja descargar', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    const filtros = await abrirReportes(proyecto.id);

    await user.type(within(filtros).getByLabelText('Buscar en Pedidos'), 'zzz');
    await user.click(within(filtros).getByRole('button', { name: 'Ver reporte' }));

    expect(await screen.findByText('No hay filas para este reporte', {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Descargar Excel (XLSX)' })).toBeDisabled();
  });

  it('avisa si la fecha desde es posterior a la fecha hasta sin pedir el reporte', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    const filtros = await abrirReportes(proyecto.id);
    await screen.findByText('3 filas en el reporte', {}, ESPERA);

    await user.type(within(filtros).getByLabelText('Cargado desde'), '2026-10-10');
    await user.type(within(filtros).getByLabelText('Cargado hasta'), '2026-10-01');
    await user.click(within(filtros).getByRole('button', { name: 'Ver reporte' }));

    expect(
      await within(filtros).findByText('La fecha desde no puede ser posterior a la fecha hasta.'),
    ).toBeVisible();
    expect(screen.getByText('3 filas en el reporte')).toBeVisible();
  });

  it('avisa que se muestran las primeras filas cuando hay más de 50', async () => {
    const { proyecto, fila } = await setupPedidos();
    for (let n = 0; n < 57; n++)
      fila(`Cliente ${n}`, 1, 'Pendiente', false, '2026-10-12T10:00:00Z');
    await abrirReportes(proyecto.id);

    expect(await screen.findByText('60 filas en el reporte', {}, ESPERA)).toBeVisible();
    expect(
      screen.getByText(
        'Se muestran las primeras 50 filas de 60. Los totales y el archivo incluyen todas.',
      ),
    ).toBeVisible();
  });

  it('muestra el error del servidor si no se puede descargar', async () => {
    const { proyecto } = await setupPedidos();
    server.use(
      http.get(/\/bloques\/[^/]+\/reporte\/descarga/, ({ request }) =>
        errorResponse(
          400,
          'El reporte tiene más de 10000 filas: acotá los filtros para poder descargarlo',
          new URL(request.url).pathname,
        ),
      ),
    );
    const user = userEvent.setup();
    await abrirReportes(proyecto.id);
    await screen.findByText('3 filas en el reporte', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Descargar Excel (XLSX)' }));

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent('acotá los filtros');
    expect(guardados).toHaveLength(0);
  });

  it('cambia de tabla y es accesible', async () => {
    const { proyecto } = await setupPedidos();
    const clientes = await crearBloque({ proyectoId: proyecto.id, nombre: 'Clientes' });
    const nombre = await crearCampo({
      bloqueId: clientes.id,
      datos: columna({ nombre: 'Nombre' }),
    });
    agregarRegistro(clientes.id, { [nombre.id]: 'Beto' });
    const user = userEvent.setup();
    await abrirReportes(proyecto.id);
    await screen.findByText('3 filas en el reporte', {}, ESPERA);
    await sinViolaciones(screen.getByRole('form', { name: 'Filtros del reporte' }));

    await user.selectOptions(screen.getByLabelText('Tabla del reporte'), 'Clientes');

    expect(await screen.findByText('1 fila en el reporte', {}, ESPERA)).toBeVisible();
    expect(screen.queryByLabelText('Totales del reporte')).not.toBeInTheDocument();
  });

  it('el selector de modo del sistema lleva a Reportes', async () => {
    const { proyecto } = await setupPedidos();
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync('Mi negocio');

    await user.click(
      within(screen.getByRole('navigation', { name: 'Modo del sistema' })).getByRole('link', {
        name: 'Reportes',
      }),
    );

    expect(await screen.findByRole('form', { name: 'Filtros del reporte' }, ESPERA)).toBeVisible();
    expect(
      within(screen.getByRole('navigation', { name: 'Ruta de navegación' })).getByText('Reportes'),
    ).toBeVisible();
  });
});

describe('nombreDeArchivo', () => {
  it('lee el nombre del encabezado de descarga', () => {
    expect(nombreDeArchivo('attachment; filename="reporte-pedidos-20261013.xlsx"')).toBe(
      'reporte-pedidos-20261013.xlsx',
    );
    expect(nombreDeArchivo("attachment; filename*=UTF-8''reporte%20%C3%B1.xlsx")).toBe(
      'reporte ñ.xlsx',
    );
    expect(nombreDeArchivo('attachment; filename=simple.xlsx')).toBe('simple.xlsx');
    expect(nombreDeArchivo(null)).toBe('descarga');
    expect(nombreDeArchivo('inline')).toBe('descarga');
  });
});
