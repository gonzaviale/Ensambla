import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { plantillasMock } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

function elemento<T>(items: readonly T[], indice = 0): T {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento de prueba ${indice}`);
  return item;
}

type Producto = { nombre: string; stock: number; rubro: string };

const PRODUCTOS: Producto[] = [
  { nombre: 'Harina 000', stock: 1500, rubro: 'Insumos' },
  { nombre: 'Azúcar', stock: 20, rubro: 'Insumos' },
  { nombre: 'Torta', stock: 300, rubro: 'Terminados' },
];

async function abrirUsar(productos: Producto[] = PRODUCTOS) {
  setupUsuarioConSesion();
  const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
  const bloque = elemento(proyecto.bloques);
  const [nombre, stock, rubro] = [0, 1, 2].map((i) => elemento(bloque.campos, i).id);
  productos.forEach((producto, n) => {
    agregarRegistro(
      bloque.id,
      {
        [String(nombre)]: producto.nombre,
        [String(stock)]: producto.stock,
        [String(rubro)]: producto.rubro,
      },
      new Date(Date.UTC(2026, 9, 10, 12, n)).toISOString(),
    );
  });
  renderApp(`/proyectos/${proyecto.id}/usar`);
  await verifyHeadingPresentAsync(proyecto.nombre);
  return { bloqueId: bloque.id };
}

const filasDeDatos = () => screen.getAllByRole('row').slice(1);
const primeraCelda = (fila: HTMLElement) => elemento(within(fila).getAllByRole('cell')).textContent;

describe('H-06: tabla de registros', () => {
  it('muestra una columna por campo del bloque y formatea los datos', async () => {
    await abrirUsar();

    for (const nombre of ['Nombre', 'Stock', 'Rubro', 'Cargado']) {
      expect(await screen.findByRole('columnheader', { name: nombre }, ESPERA)).toBeInTheDocument();
    }
    expect(screen.getByText('Harina 000')).toBeInTheDocument();
    expect(screen.getByText('1.500')).toBeInTheDocument();
    expect(screen.getByText('3 registros')).toBeInTheDocument();
  });

  it('por defecto muestra primero lo último que se cargó', async () => {
    await abrirUsar();

    await screen.findByText('Torta', {}, ESPERA);

    expect(filasDeDatos().map(primeraCelda)).toEqual(['Torta', 'Azúcar', 'Harina 000']);
  });

  it('la búsqueda filtra los registros y avisa cuando no hay coincidencias', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);

    await user.type(screen.getByLabelText('Buscar en Productos'), 'harina');

    await waitFor(() => expect(screen.queryByText('Azúcar')).not.toBeInTheDocument(), ESPERA);
    expect(screen.getByText('Harina 000')).toBeInTheDocument();
    expect(screen.getByText('1 registro')).toBeInTheDocument();

    await user.clear(screen.getByLabelText('Buscar en Productos'));
    await user.type(screen.getByLabelText('Buscar en Productos'), 'zzz');

    expect(await screen.findByText('No hay resultados', {}, ESPERA)).toBeInTheDocument();
  });

  it('ordena por una columna y invierte el orden al segundo click', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Stock' }));

    await waitFor(
      () =>
        expect(screen.getByRole('columnheader', { name: 'Stock' })).toHaveAttribute(
          'aria-sort',
          'ascending',
        ),
      ESPERA,
    );
    await waitFor(
      () => expect(filasDeDatos().map(primeraCelda)).toEqual(['Azúcar', 'Torta', 'Harina 000']),
      ESPERA,
    );

    await user.click(screen.getByRole('button', { name: 'Stock' }));

    await waitFor(
      () => expect(filasDeDatos().map(primeraCelda)).toEqual(['Harina 000', 'Torta', 'Azúcar']),
      ESPERA,
    );
    expect(screen.getByRole('columnheader', { name: 'Stock' })).toHaveAttribute(
      'aria-sort',
      'descending',
    );
  });

  it('pagina cuando hay más registros que los de una página', async () => {
    const muchos = Array.from({ length: 45 }, (_, n) => ({
      nombre: `Producto ${String(n + 1).padStart(2, '0')}`,
      stock: n,
      rubro: 'Insumos',
    }));
    await abrirUsar(muchos);
    const user = userEvent.setup();

    expect(await screen.findByText('Página 1 de 3', {}, ESPERA)).toBeInTheDocument();
    expect(filasDeDatos()).toHaveLength(20);
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('Página 2 de 3', {}, ESPERA)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled();
  });

  it('indica que todavía no hay datos cuando el bloque está vacío', async () => {
    await abrirUsar([]);

    expect(await screen.findByText('Todavía no cargaste nada', {}, ESPERA)).toBeInTheDocument();
  });

  it('muestra el error y permite reintentar cuando falla la carga', async () => {
    const { bloqueId } = await abrirUsar();
    await screen.findByText('Azúcar', {}, ESPERA);
    setupServerError('get', `/bloques/${bloqueId}/registros`, 500, 'No pudimos leer los registros');
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('Buscar en Productos'), 'a');

    expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'No pudimos leer los registros',
    );
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  it('avisa cuando el proyecto todavía no tiene bloques', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Mi taller' });
    renderApp(`/proyectos/${proyecto.id}/usar`);

    expect(
      await screen.findByRole('heading', { name: 'Este proyecto no tiene bloques' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir al constructor' })).toBeInTheDocument();
  });
});

describe('H-06: ficha del registro', () => {
  it('muestra todos los datos del registro', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Harina 000', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Ver ficha de Harina 000' }));

    const ficha = screen.getByRole('complementary', { name: 'Ficha del registro' });
    expect(within(ficha).getByRole('heading', { name: 'Ficha de Productos' })).toBeInTheDocument();
    expect(within(ficha).getByText('Nombre')).toBeInTheDocument();
    expect(within(ficha).getByText('Harina 000')).toBeInTheDocument();
    expect(within(ficha).getByText('1.500')).toBeInTheDocument();
    expect(within(ficha).getByText('Insumos')).toBeInTheDocument();
    expect(within(ficha).getByText(/Cargado:/)).toBeInTheDocument();
  });

  it('se cierra sin tocar los datos', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));

    await user.click(screen.getByRole('button', { name: 'Cerrar ficha' }));

    expect(
      screen.queryByRole('complementary', { name: 'Ficha del registro' }),
    ).not.toBeInTheDocument();
    expect(screen.getByText('3 registros')).toBeInTheDocument();
  });

  it('pide confirmación para eliminar y actualiza la tabla', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));

    await user.click(screen.getByRole('button', { name: 'Eliminar registro' }));
    expect(screen.getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
    expect(screen.getByText('3 registros')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar registro' }));

    await waitFor(() => expect(screen.getByText('2 registros')).toBeInTheDocument(), ESPERA);
    expect(screen.queryByText('Azúcar')).not.toBeInTheDocument();
    expect(
      screen.queryByRole('complementary', { name: 'Ficha del registro' }),
    ).not.toBeInTheDocument();
  });

  it('conserva el registro si se cancela la confirmación', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar registro' }));

    await user.click(screen.getByRole('button', { name: 'Conservar registro' }));

    expect(screen.queryByText(/Esta acción no se puede deshacer/)).not.toBeInTheDocument();
    expect(screen.getByText('3 registros')).toBeInTheDocument();
  });
});
