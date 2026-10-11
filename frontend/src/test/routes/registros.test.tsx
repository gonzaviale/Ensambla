import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { crearProyecto, crearProyectoDesdePlantilla } from '@/api/proyectos';
import { estado, plantillasMock } from '@/mocks/db/estado';
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
  // «Cargar» es la vista por defecto: acá se mira la tabla de filas.
  await userEvent.setup().click(await screen.findByRole('tab', { name: 'Ver filas' }));
  return { bloqueId: bloque.id };
}

const filasDeDatos = () =>
  screen
    .getAllByRole('row')
    .slice(1)
    .filter((fila) => !fila.closest('tfoot'));
const primeraCelda = (fila: HTMLElement) => elemento(within(fila).getAllByRole('cell')).textContent;

describe('H-06: tabla de registros', () => {
  it('muestra una columna por campo del bloque y formatea los datos', async () => {
    await abrirUsar();

    for (const nombre of ['Nombre', 'Stock', 'Rubro', 'Cargado']) {
      expect(await screen.findByRole('columnheader', { name: nombre }, ESPERA)).toBeInTheDocument();
    }
    expect(screen.getByText('Harina 000')).toBeInTheDocument();
    expect(screen.getByText('1.500')).toBeInTheDocument();
    expect(screen.getByText('3 filas')).toBeInTheDocument();
  });

  it('muestra al pie la cantidad de filas y la suma de las columnas numéricas', async () => {
    await abrirUsar();

    const pie = await screen.findByRole('row', { name: /Totales de 3 filas/ }, ESPERA);

    expect(within(pie).getByText('Totales de 3 filas')).toBeInTheDocument();
    expect(within(pie).getByText('1.820')).toBeInTheDocument();
  });

  it('los totales respetan la búsqueda y no sólo la página', async () => {
    const muchos = Array.from({ length: 45 }, (_, n) => ({
      nombre: n < 5 ? `Harina ${n}` : `Producto ${n}`,
      stock: n + 1,
      rubro: 'Insumos',
    }));
    await abrirUsar(muchos);
    const user = userEvent.setup();

    const pie = await screen.findByRole('row', { name: /Totales de 45 filas/ }, ESPERA);
    // 1 + 2 + … + 45 = 1.035, aunque la página muestre sólo 20 filas.
    expect(within(pie).getByText('1.035')).toBeInTheDocument();
    expect(filasDeDatos()).toHaveLength(20);

    await user.type(screen.getByLabelText('Buscar en Productos'), 'harina');

    const filtrado = await screen.findByRole('row', { name: /Totales de 5 filas/ }, ESPERA);
    // 1 + 2 + 3 + 4 + 5
    expect(within(filtrado).getByText('15')).toBeInTheDocument();
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
    expect(screen.getByText('1 fila')).toBeInTheDocument();

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

  it('sin registros igual muestra las columnas y ofrece cargar el primero', async () => {
    await abrirUsar([]);
    const user = userEvent.setup();

    expect(
      await screen.findByText('Todavía no cargaste nada en Productos.', {}, ESPERA),
    ).toBeInTheDocument();
    for (const nombre of ['Nombre', 'Stock', 'Rubro', 'Cargado']) {
      expect(screen.getByRole('columnheader', { name: nombre })).toBeInTheDocument();
    }

    await user.click(screen.getByRole('button', { name: 'Cargar la primera fila' }));

    expect(screen.getByRole('tab', { name: 'Cargar' })).toHaveAttribute('aria-selected', 'true');
    await waitFor(() => expect(screen.getByLabelText('Nombre')).toHaveFocus());
  });

  it('avisa si el bloque no tiene datos y ofrece agregarlos en Construir', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
    const bloque = elemento(proyecto.bloques);
    const item = estado.proyectos.get(proyecto.id);
    if (!item) throw new Error('Falta el proyecto simulado');
    item.proyecto = { ...proyecto, bloques: [{ ...bloque, campos: [] }] };
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await userEvent.setup().click(await screen.findByRole('tab', { name: 'Ver filas' }));

    await verifyHeadingPresentAsync('Productos todavía no tiene columnas');
    expect(screen.getByRole('link', { name: 'Agregar columnas en Armar' })).toHaveAttribute(
      'href',
      `/proyectos/${proyecto.id}/construir?bloque=${bloque.id}`,
    );
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('con pocos bloques los muestra como botones y cambia de bloque', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
    const original = elemento(proyecto.bloques);
    const item = estado.proyectos.get(proyecto.id);
    if (!item) throw new Error('Falta el proyecto simulado');
    item.proyecto = {
      ...proyecto,
      bloques: [
        original,
        {
          id: crypto.randomUUID(),
          nombre: 'Clientes',
          posicion: 1,
          campos: [{ ...elemento(original.campos), id: crypto.randomUUID(), nombre: 'Teléfono' }],
        },
      ],
    };
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync(proyecto.nombre);
    await user.click(await screen.findByRole('tab', { name: 'Ver filas' }));

    const grupo = screen.getByRole('group', { name: 'Tabla' });
    expect(within(grupo).getByRole('radio', { name: 'Productos' })).toBeChecked();
    await user.click(within(grupo).getByRole('radio', { name: 'Clientes' }));

    expect(within(grupo).getByRole('radio', { name: 'Clientes' })).toBeChecked();
    expect(
      await screen.findByRole('columnheader', { name: 'Teléfono' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Buscar en Clientes')).toBeInTheDocument();
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

  it('avisa cuando el sistema todavía no tiene tablas', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyecto({ nombre: 'Mi taller' });
    renderApp(`/proyectos/${proyecto.id}/usar`);

    expect(
      await screen.findByRole('heading', { name: 'Este sistema no tiene tablas' }, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Armar mis tablas' })).toBeInTheDocument();
  });
});

describe('H-06: ficha del registro', () => {
  it('muestra todos los datos del registro', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Harina 000', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Ver ficha de Harina 000' }));

    const ficha = screen.getByRole('complementary', { name: 'Ficha de la fila' });
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
      screen.queryByRole('complementary', { name: 'Ficha de la fila' }),
    ).not.toBeInTheDocument();
    expect(screen.getByText('3 filas')).toBeInTheDocument();
  });

  it('pide confirmación para eliminar y actualiza la tabla', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));

    await user.click(screen.getByRole('button', { name: 'Eliminar fila' }));
    expect(screen.getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
    expect(screen.getByText('3 filas')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar fila' }));

    await waitFor(() => expect(screen.getByText('2 filas')).toBeInTheDocument(), ESPERA);
    expect(screen.queryByText('Azúcar')).not.toBeInTheDocument();
    expect(
      screen.queryByRole('complementary', { name: 'Ficha de la fila' }),
    ).not.toBeInTheDocument();
  });

  it('conserva el registro si se cancela la confirmación', async () => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText('Azúcar', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));
    await user.click(screen.getByRole('button', { name: 'Eliminar fila' }));

    await user.click(screen.getByRole('button', { name: 'Conservar fila' }));

    expect(screen.queryByText(/Esta acción no se puede deshacer/)).not.toBeInTheDocument();
    expect(screen.getByText('3 filas')).toBeInTheDocument();
  });
});

describe('H-06: edición desde la ficha', () => {
  const abrirFicha = async (nombre: string) => {
    await abrirUsar();
    const user = userEvent.setup();
    await screen.findByText(nombre, {}, ESPERA);
    await user.click(screen.getByRole('button', { name: `Ver ficha de ${nombre}` }));
    return user;
  };
  const ficha = () => screen.getByRole('complementary', { name: 'Ficha de la fila' });
  const registroDe = (nombre: string) => {
    const registro = [...estado.registros.values()].find((item) =>
      Object.values(item.valores).includes(nombre),
    );
    if (!registro) throw new Error(`Falta el registro ${nombre}`);
    return registro;
  };

  it('edita la fila, muestra los datos nuevos y actualiza la tabla', async () => {
    const user = await abrirFicha('Azúcar');

    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));
    expect(within(ficha()).getByLabelText('Nombre')).toHaveValue('Azúcar');
    expect(within(ficha()).getByLabelText('Stock')).toHaveValue('20');
    await user.clear(within(ficha()).getByLabelText('Stock'));
    await user.type(within(ficha()).getByLabelText('Stock'), '35');
    await user.click(within(ficha()).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(ficha()).findByText('Cambios guardados.', {}, ESPERA)).toBeInTheDocument();
    expect(within(ficha()).getByText('35')).toBeInTheDocument();
    expect(within(ficha()).queryByLabelText('Stock')).not.toBeInTheDocument();
    await waitFor(
      () => expect(within(screen.getByRole('table')).getByText('35')).toBeInTheDocument(),
      ESPERA,
    );
    expect(within(screen.getByRole('table')).queryByText('20')).not.toBeInTheDocument();
    expect(within(ficha()).getByRole('button', { name: 'Editar fila' })).toHaveFocus();
  });

  it('borra un dato opcional al vaciarlo', async () => {
    const user = await abrirFicha('Azúcar');
    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));

    await user.selectOptions(within(ficha()).getByLabelText('Rubro'), '');
    await user.click(within(ficha()).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(ficha()).findByText('Cambios guardados.', {}, ESPERA)).toBeInTheDocument();
    expect(within(ficha()).queryByText('Insumos')).not.toBeInTheDocument();
    expect(Object.values(registroDe('Azúcar').valores)).toEqual(['Azúcar', 20]);
  });

  it('no guarda si falta un dato obligatorio y marca el campo', async () => {
    const user = await abrirFicha('Azúcar');
    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));

    await user.clear(within(ficha()).getByLabelText('Nombre'));
    await user.click(within(ficha()).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(ficha()).findByText('Este dato es obligatorio')).toBeInTheDocument();
    expect(within(ficha()).getByLabelText('Nombre')).toHaveFocus();
    expect(Object.values(registroDe('Azúcar').valores)).toContain('Azúcar');
  });

  it('muestra el error del servidor y conserva lo escrito para reintentar', async () => {
    const user = await abrirFicha('Azúcar');
    setupServerError('patch', '/registros/:id', 500, 'No pudimos guardar los cambios');
    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));

    await user.clear(within(ficha()).getByLabelText('Stock'));
    await user.type(within(ficha()).getByLabelText('Stock'), '35');
    await user.click(within(ficha()).getByRole('button', { name: 'Guardar cambios' }));

    expect(
      await screen.findByText('No pudimos guardar los cambios', {}, ESPERA),
    ).toBeInTheDocument();
    expect(within(ficha()).getByLabelText('Stock')).toHaveValue('35');
    expect(within(ficha()).getByRole('button', { name: 'Guardar cambios' })).toBeEnabled();
    expect(Object.values(registroDe('Azúcar').valores)).toContain(20);
  });

  it('cancela la edición sin tocar los datos', async () => {
    const user = await abrirFicha('Azúcar');
    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));
    await user.clear(within(ficha()).getByLabelText('Nombre'));
    await user.type(within(ficha()).getByLabelText('Nombre'), 'Otra cosa');

    await user.click(within(ficha()).getByRole('button', { name: 'Cancelar' }));

    expect(within(ficha()).getByText('Azúcar')).toBeInTheDocument();
    expect(within(ficha()).queryByText('Otra cosa')).not.toBeInTheDocument();
    expect(within(ficha()).queryByText('Cambios guardados.')).not.toBeInTheDocument();
    expect(within(ficha()).getByRole('button', { name: 'Editar fila' })).toHaveFocus();
  });

  it('al elegir otra fila sale de la edición', async () => {
    const user = await abrirFicha('Azúcar');
    await user.click(within(ficha()).getByRole('button', { name: 'Editar fila' }));

    await user.click(screen.getByRole('button', { name: 'Ver ficha de Torta' }));

    expect(within(ficha()).getByText('Torta')).toBeInTheDocument();
    expect(
      within(ficha()).queryByRole('button', { name: 'Guardar cambios' }),
    ).not.toBeInTheDocument();
  });
});

describe('Cargar y ver: pestañas con teclado', () => {
  it('Cargar es la vista por defecto y las flechas pasan a Ver filas', async () => {
    setupUsuarioConSesion();
    const proyecto = await crearProyectoDesdePlantilla(elemento(plantillasMock).id);
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/usar`);
    await verifyHeadingPresentAsync(proyecto.nombre);

    const cargar = await screen.findByRole('tab', { name: 'Cargar' });
    expect(cargar).toHaveAttribute('aria-selected', 'true');
    cargar.focus();
    await user.keyboard('{ArrowRight}');

    const ver = screen.getByRole('tab', { name: 'Ver filas' });
    expect(ver).toHaveAttribute('aria-selected', 'true');
    expect(ver).toHaveFocus();
    expect(await screen.findByLabelText('Buscar en Productos', {}, ESPERA)).toBeInTheDocument();
  });
});

describe('Cargar y ver en el celular', () => {
  beforeEach(() => {
    window.matchMedia = ((consulta: string) => ({
      matches: false,
      media: consulta,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    })) as unknown as typeof window.matchMedia;
  });
  afterEach(() => {
    // @ts-expect-error jsdom no define matchMedia: se deja como estaba.
    delete window.matchMedia;
  });

  it('muestra los totales en un resumen debajo de las tarjetas', async () => {
    await abrirUsar();

    const panel = await screen.findByRole('region', { name: 'Totales' }, ESPERA);

    expect(within(panel).getByText('Filas:').nextSibling).toHaveTextContent('3');
    expect(within(panel).getByText('Stock:').nextSibling).toHaveTextContent('1.820');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('muestra cada fila como tarjeta y abre la ficha en una hoja', async () => {
    await abrirUsar();
    const user = userEvent.setup();

    const lista = await screen.findByRole('list', { name: 'Filas de Productos' }, ESPERA);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(within(lista).getAllByRole('listitem')).toHaveLength(3);
    await user.click(screen.getByRole('button', { name: 'Ver ficha de Azúcar' }));

    const hoja = screen.getByRole('dialog', { name: 'Ficha de Productos' });
    expect(within(hoja).getByText('Azúcar')).toBeInTheDocument();
    await user.click(within(hoja).getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
