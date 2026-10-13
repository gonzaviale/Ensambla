import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearBloque } from '@/api/bloques';
import { crearCampo, listarCampos } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { estado } from '@/mocks/db/estado';
import { agregarRegistro } from '@/mocks/db/registros';
import { errorResponse } from '@/mocks/errores';
import { server } from '@/mocks/node';
import type { GuardarCampo } from '@/types/campo';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const dato = (datos: Partial<GuardarCampo> & Pick<GuardarCampo, 'nombre'>): GuardarCampo => ({
  tipo: 'TEXTO',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: null,
  formula: null,
  ...datos,
});

/* setup */

async function setupProyecto() {
  setupUsuarioConSesion();
  return crearProyecto({ nombre: 'Mi almacén' });
}

async function setupProyectoConBloques() {
  const proyecto = await setupProyecto();
  const productos = await crearBloque({ proyectoId: proyecto.id, nombre: 'Productos' });
  const clientes = await crearBloque({ proyectoId: proyecto.id, nombre: 'Clientes' });
  await crearCampo({
    bloqueId: productos.id,
    datos: dato({ nombre: 'Nombre', obligatorio: true }),
  });
  await crearCampo({
    bloqueId: productos.id,
    datos: dato({ nombre: 'Stock', tipo: 'NUMERO', valorInicial: '0' }),
  });
  await crearCampo({ bloqueId: clientes.id, datos: dato({ nombre: 'Teléfono' }) });
  return { proyecto, productos, clientes };
}

async function abrirConstructor(proyectoId: string, bloqueId?: string) {
  const router = renderApp(
    `/proyectos/${proyectoId}/construir${bloqueId ? `?bloque=${bloqueId}` : ''}`,
  );
  await verifyHeadingPresentAsync('Tus tablas');
  return router;
}

/* consultas */

const listaDeBloques = () => screen.getByRole('navigation', { name: 'Tablas del sistema' });
const botonDeBloque = (nombre: string) =>
  within(listaDeBloques()).getByRole('button', { name: nombre });
const detalle = (nombre: string) => screen.getByRole('region', { name: nombre });
const datosDelBloque = () => screen.findByRole('list', { name: 'Columnas de la tabla' }, ESPERA);
// «Stock» también es una sección del sistema (en la navegación): sólo cuenta el texto de la pantalla.
const textoFueraDeLaNavegacion = (texto: string) =>
  screen.queryAllByText(texto).filter((elemento) => !elemento.closest('nav'));
const formulario = (nombre: string | RegExp) => screen.getByRole('form', { name: nombre });

async function abrirNuevoDato(user: UserEvent) {
  await user.click(await screen.findByRole('button', { name: 'Agregar columna' }, ESPERA));
  return formulario('Nueva columna');
}

async function guardarDato(user: UserEvent, form: HTMLElement) {
  await user.click(within(form).getByRole('button', { name: 'Guardar columna' }));
}

describe('ENS-47 · T-019: layout del constructor', () => {
  it('muestra la carga y después el estado vacío con la explicación de qué es una tabla', async () => {
    const proyecto = await setupProyecto();
    renderApp(`/proyectos/${proyecto.id}/construir`);

    expect(await screen.findByText('Cargando las tablas…', {}, ESPERA)).toBeInTheDocument();
    await verifyHeadingPresentAsync('Todavía no hay tablas en este sistema');
    expect(screen.getByRole('button', { name: 'Agregar tabla' })).toBeInTheDocument();
  });

  it('abre el primer bloque y navega entre bloques mostrando sus datos', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    const router = await abrirConstructor(proyecto.id);

    await verifyHeadingPresentAsync('Productos');
    expect(within(await datosDelBloque()).getByText('Stock')).toBeInTheDocument();
    expect(botonDeBloque('Productos')).toHaveAttribute('aria-current', 'true');

    await user.click(botonDeBloque('Clientes'));

    const clientes = await screen.findByRole('heading', { level: 2, name: 'Clientes' }, ESPERA);
    expect(clientes).toHaveFocus();
    expect(botonDeBloque('Clientes')).toHaveAttribute('aria-current', 'true');
    expect(botonDeBloque('Productos')).not.toHaveAttribute('aria-current');
    expect(await screen.findByText('Teléfono', {}, ESPERA)).toBeInTheDocument();
    expect(textoFueraDeLaNavegacion('Stock')).toHaveLength(0);
    expect(router.state.location.search).toMatchObject({ bloque: expect.any(String) });
  });

  it('recuerda el bloque abierto en la dirección y vuelve a la lista desde el detalle', async () => {
    const { proyecto, clientes } = await setupProyectoConBloques();
    const user = userEvent.setup();
    const router = await abrirConstructor(proyecto.id, clientes.id);

    await verifyHeadingPresentAsync('Clientes');
    await user.click(screen.getByRole('button', { name: 'Ver todas las tablas' }));

    await waitFor(() => expect(router.state.location.search).toEqual({}), ESPERA);
    await waitFor(() => expect(botonDeBloque('Clientes')).toHaveFocus(), ESPERA);
  });

  it('muestra el error y permite reintentar si no se pueden cargar los bloques', async () => {
    const proyecto = await setupProyecto();
    const path = `/proyectos/${proyecto.id}/bloques`;
    server.use(
      http.get(`/api${path}`, () => errorResponse(500, 'El servidor no responde', `/api${path}`), {
        once: true,
      }),
    );
    const user = userEvent.setup();
    renderApp(`/proyectos/${proyecto.id}/construir`);

    await verifyHeadingPresentAsync('No pudimos cargar las tablas');
    expect(screen.getByRole('alert')).toHaveTextContent('El servidor no responde');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));

    await verifyHeadingPresentAsync('Todavía no hay tablas en este sistema');
  });
});

describe('ENS-48 · T-020: agregar, renombrar y eliminar bloques', () => {
  it('agrega el primer bloque y lo abre', async () => {
    const proyecto = await setupProyecto();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    await user.type(screen.getByLabelText('Nombre de la tabla nueva'), 'Pedidos');
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));

    await verifyHeadingPresentAsync('Pedidos');
    expect(botonDeBloque('Pedidos')).toHaveAttribute('aria-current', 'true');
    expect(
      await screen.findByText(/Esta tabla todavía no tiene columnas/, {}, ESPERA),
    ).toBeVisible();
  });

  it('pide un nombre antes de agregar el bloque', async () => {
    const proyecto = await setupProyecto();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));

    const input = screen.getByLabelText('Nombre de la tabla nueva');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription(/Escribí un nombre para la tabla/);
  });

  it('muestra junto al nombre el 409 del servidor cuando el bloque ya existe', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    await user.type(screen.getByLabelText('Nombre de la tabla nueva'), 'productos');
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));

    const input = screen.getByLabelText('Nombre de la tabla nueva');
    await waitFor(
      () =>
        expect(input).toHaveAccessibleDescription(
          /Ya hay un bloque llamado "productos" en este proyecto/,
        ),
      ESPERA,
    );
    expect(input).toHaveValue('productos');
  });

  it('muestra junto al nombre el error 400 que devuelve el servidor', async () => {
    const proyecto = await setupProyecto();
    setupServerError(
      'post',
      `/proyectos/${proyecto.id}/bloques`,
      400,
      'Error de validación en los datos enviados',
      { nombre: 'El nombre no puede tener sólo símbolos' },
    );
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    await user.type(screen.getByLabelText('Nombre de la tabla nueva'), '***');
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));

    await waitFor(
      () =>
        expect(screen.getByLabelText('Nombre de la tabla nueva')).toHaveAccessibleDescription(
          /El nombre no puede tener sólo símbolos/,
        ),
      ESPERA,
    );
  });

  it('renombra el bloque y actualiza la lista', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await verifyHeadingPresentAsync('Productos');

    await user.click(screen.getByRole('button', { name: 'Cambiar nombre' }));
    const input = screen.getByLabelText('Nuevo nombre de la tabla');
    expect(input).toHaveValue('Productos');
    await user.clear(input);
    await user.type(input, 'Mercadería');
    await user.click(screen.getByRole('button', { name: 'Guardar nombre' }));

    await verifyHeadingPresentAsync('Mercadería');
    await waitFor(() => expect(botonDeBloque('Mercadería')).toBeInTheDocument(), ESPERA);
    expect(within(listaDeBloques()).queryByText('Productos')).not.toBeInTheDocument();
  });

  it('no renombra con un nombre que ya usa otro bloque', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await verifyHeadingPresentAsync('Productos');

    await user.click(screen.getByRole('button', { name: 'Cambiar nombre' }));
    await user.clear(screen.getByLabelText('Nuevo nombre de la tabla'));
    await user.type(screen.getByLabelText('Nuevo nombre de la tabla'), 'Clientes');
    await user.click(screen.getByRole('button', { name: 'Guardar nombre' }));

    await waitFor(
      () =>
        expect(screen.getByLabelText('Nuevo nombre de la tabla')).toHaveAccessibleDescription(
          /Ya hay un bloque llamado "Clientes"/,
        ),
      ESPERA,
    );
  });

  it('pide confirmación, avisa qué se borra y elimina el bloque con sus registros', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    agregarRegistro(productos.id, {});
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await verifyHeadingPresentAsync('Productos');
    await datosDelBloque();

    await user.click(screen.getByRole('button', { name: 'Eliminar tabla' }));
    const aviso = screen.getByRole('alertdialog', { name: '¿Eliminar la tabla «Productos»?' });
    expect(within(aviso).getByRole('button', { name: 'No, conservarla' })).toHaveFocus();
    expect(aviso).toHaveTextContent('Se van a borrar sus 2 columnas y la fila que cargaste.');
    expect(aviso).toHaveTextContent('No se puede deshacer');

    await user.click(within(aviso).getByRole('button', { name: 'No, conservarla' }));
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(botonDeBloque('Productos')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Eliminar tabla' }));
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar tabla' }));

    await waitFor(
      () => expect(within(listaDeBloques()).queryByText('Productos')).not.toBeInTheDocument(),
      ESPERA,
    );
    await verifyHeadingPresentAsync('Clientes');
    await waitFor(
      () => expect(screen.getByRole('heading', { name: 'Tus tablas' })).toHaveFocus(),
      ESPERA,
    );
    expect([...estado.registros.values()]).toHaveLength(0);
  });

  it('conserva la estructura al volver a abrir el proyecto', async () => {
    const proyecto = await setupProyecto();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    await user.type(screen.getByLabelText('Nombre de la tabla nueva'), 'Ventas');
    await user.click(screen.getByRole('button', { name: 'Agregar tabla' }));
    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Monto');
    await user.click(within(form).getByRole('radio', { name: 'Número' }));
    await guardarDato(user, form);
    await waitFor(() => expect(screen.queryByRole('form')).not.toBeInTheDocument(), ESPERA);

    cleanup();
    await abrirConstructor(proyecto.id);

    await verifyHeadingPresentAsync('Ventas');
    expect(within(await datosDelBloque()).getByText('Monto')).toBeInTheDocument();
    expect(screen.getByText('Número')).toBeInTheDocument();
  });
});

describe('ENS-49 · T-021: editor de datos (campos)', () => {
  it('crea los cinco tipos de dato con obligatorio, valor inicial y opciones', async () => {
    const proyecto = await setupProyecto();
    const bloque = await crearBloque({ proyectoId: proyecto.id, nombre: 'Pedidos' });
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id, bloque.id);

    let form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Cliente');
    await user.type(within(form).getByLabelText('Valor inicial (opcional)'), 'Consumidor final');
    await user.click(within(form).getByRole('checkbox', { name: 'Es obligatorio' }));
    await guardarDato(user, form);

    form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Precio');
    await user.click(within(form).getByRole('radio', { name: 'Número' }));
    await user.type(within(form).getByLabelText('Valor inicial (opcional)'), '12,5');
    await guardarDato(user, form);

    form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Entrega');
    await user.click(within(form).getByRole('radio', { name: 'Fecha' }));
    await user.type(within(form).getByLabelText('Valor inicial (opcional)'), '2026-10-20');
    await guardarDato(user, form);

    form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Tamaño');
    await user.click(within(form).getByRole('radio', { name: 'Lista de opciones' }));
    await user.type(within(form).getByLabelText('Opción 1'), 'Chico');
    await user.type(within(form).getByLabelText('Opción 2'), 'Mediano');
    await user.click(within(form).getByRole('button', { name: 'Agregar otra opción' }));
    await user.type(within(form).getByLabelText('Opción 3'), 'Grande');
    await user.selectOptions(within(form).getByLabelText('Valor inicial (opcional)'), 'Mediano');
    await guardarDato(user, form);

    form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Pagado');
    await user.click(within(form).getByRole('radio', { name: 'Sí o no' }));
    const valor = within(form).getByRole('group', { name: 'Valor inicial (opcional)' });
    await user.click(within(valor).getByRole('radio', { name: 'No' }));
    await guardarDato(user, form);
    await waitFor(() => expect(screen.queryByRole('form')).not.toBeInTheDocument(), ESPERA);

    const lista = await datosDelBloque();
    expect(within(lista).getAllByRole('listitem')).toHaveLength(5);
    expect(within(lista).getByText('Obligatorio')).toBeInTheDocument();
    expect(within(lista).getByText('Lista de opciones: Chico, Mediano, Grande')).toBeVisible();
    expect(within(lista).getByText('Valor inicial: 12,5')).toBeVisible();
    expect(within(lista).getByText('Valor inicial: 20/10/2026')).toBeVisible();
    expect(within(lista).getByText('Valor inicial: No')).toBeVisible();

    const guardados = await listarCampos(bloque.id);
    expect(
      guardados.map(({ nombre, tipo, obligatorio, valorInicial, opciones }) => ({
        nombre,
        tipo,
        obligatorio,
        valorInicial,
        opciones,
      })),
    ).toEqual([
      {
        nombre: 'Cliente',
        tipo: 'TEXTO',
        obligatorio: true,
        valorInicial: 'Consumidor final',
        opciones: [],
      },
      { nombre: 'Precio', tipo: 'NUMERO', obligatorio: false, valorInicial: '12.5', opciones: [] },
      {
        nombre: 'Entrega',
        tipo: 'FECHA',
        obligatorio: false,
        valorInicial: '2026-10-20',
        opciones: [],
      },
      {
        nombre: 'Tamaño',
        tipo: 'OPCION',
        obligatorio: false,
        valorInicial: 'Mediano',
        opciones: ['Chico', 'Mediano', 'Grande'],
      },
      { nombre: 'Pagado', tipo: 'SI_NO', obligatorio: false, valorInicial: 'false', opciones: [] },
    ]);
  });

  it('muestra las opciones sólo cuando el tipo es lista de opciones', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    const form = await abrirNuevoDato(user);
    expect(within(form).getByRole('radio', { name: 'Texto' })).toBeChecked();
    expect(within(form).queryByRole('group', { name: 'Opciones de la lista' })).toBeNull();

    await user.click(within(form).getByRole('radio', { name: 'Lista de opciones' }));
    expect(within(form).getByRole('group', { name: 'Opciones de la lista' })).toBeVisible();

    await user.click(within(form).getByRole('radio', { name: 'Fecha' }));
    expect(within(form).queryByRole('group', { name: 'Opciones de la lista' })).toBeNull();
  });

  it('valida en pantalla nombre, opciones y valor inicial antes de enviar', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    const form = await abrirNuevoDato(user);
    await user.click(within(form).getByRole('radio', { name: 'Lista de opciones' }));
    await guardarDato(user, form);
    const nombre = within(form).getByLabelText('Nombre de la columna');
    expect(nombre).toHaveAccessibleDescription(/Escribí un nombre para la columna/);
    await waitFor(() => expect(nombre).toHaveFocus());
    expect(within(form).getByRole('group', { name: 'Opciones de la lista' })).toHaveTextContent(
      'Agregá al menos una opción.',
    );

    await user.type(nombre, 'Talle');
    await user.type(within(form).getByLabelText('Opción 1'), 'S');
    await user.type(within(form).getByLabelText('Opción 2'), 's');
    await guardarDato(user, form);
    expect(within(form).getByRole('group', { name: 'Opciones de la lista' })).toHaveTextContent(
      'La opción «s» está repetida.',
    );

    await user.click(within(form).getByRole('radio', { name: 'Número' }));
    await user.type(within(form).getByLabelText('Valor inicial (opcional)'), 'doce');
    await guardarDato(user, form);
    expect(within(form).getByLabelText('Valor inicial (opcional)')).toHaveAccessibleDescription(
      /Escribí un número/,
    );
  });

  it('muestra junto a cada input los errores 400 del servidor', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    setupServerError(
      'post',
      `/bloques/${productos.id}/campos`,
      400,
      'Error de validación en los datos enviados',
      {
        opciones: 'Las opciones no pueden repetirse',
        valorInicial: 'El valor inicial no es válido para el tipo de campo',
      },
    );
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Rubro');
    await user.click(within(form).getByRole('radio', { name: 'Lista de opciones' }));
    await user.type(within(form).getByLabelText('Opción 1'), 'Insumos');
    await user.selectOptions(within(form).getByLabelText('Valor inicial (opcional)'), 'Insumos');
    await guardarDato(user, form);

    await waitFor(
      () =>
        expect(within(form).getByRole('group', { name: 'Opciones de la lista' })).toHaveTextContent(
          'Las opciones no pueden repetirse',
        ),
      ESPERA,
    );
    expect(within(form).getByLabelText('Valor inicial (opcional)')).toHaveAccessibleDescription(
      /El valor inicial no es válido para el tipo de campo/,
    );
    expect(within(form).getByLabelText('Nombre de la columna')).toHaveValue('Rubro');
  });

  it('muestra junto al nombre el 409 cuando el dato ya existe en el bloque', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'stock');
    await guardarDato(user, form);

    await waitFor(
      () =>
        expect(within(form).getByLabelText('Nombre de la columna')).toHaveAccessibleDescription(
          /Ya hay un campo llamado "stock" en este bloque/,
        ),
      ESPERA,
    );
  });

  it('muestra como aviso un error del servidor que no corresponde a un input', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    setupServerError('post', `/bloques/${productos.id}/campos`, 404, 'El bloque no existe');
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Color');
    await guardarDato(user, form);

    expect(await within(form).findByRole('alert', {}, ESPERA)).toHaveTextContent(
      'El bloque no existe',
    );
  });

  it('edita un dato existente con sus valores cargados', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await datosDelBloque();

    await user.click(screen.getByRole('button', { name: 'Cambiar Stock' }));
    const form = formulario('Cambiar la columna «Stock»');
    expect(within(form).getByLabelText('Nombre de la columna')).toHaveValue('Stock');
    expect(within(form).getByRole('radio', { name: 'Número' })).toBeChecked();
    expect(within(form).getByLabelText('Valor inicial (opcional)')).toHaveValue('0');

    await user.clear(within(form).getByLabelText('Nombre de la columna'));
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Cantidad');
    await user.clear(within(form).getByLabelText('Valor inicial (opcional)'));
    await user.type(within(form).getByLabelText('Valor inicial (opcional)'), '3,5');
    await user.click(within(form).getByRole('checkbox', { name: 'Es obligatorio' }));
    await guardarDato(user, form);

    await waitFor(() => expect(screen.queryByRole('form')).not.toBeInTheDocument(), ESPERA);
    expect(await screen.findByText('Cantidad', {}, ESPERA)).toBeInTheDocument();
    expect(textoFueraDeLaNavegacion('Stock')).toHaveLength(0);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Cambiar Cantidad' })).toHaveFocus(),
    );
    const [, editado] = await listarCampos(productos.id);
    expect(editado).toMatchObject({ nombre: 'Cantidad', valorInicial: '3.5', obligatorio: true });
  });

  it('pide confirmación y elimina un dato', async () => {
    const { proyecto } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id);
    await datosDelBloque();

    await user.click(screen.getByRole('button', { name: 'Eliminar Stock' }));
    const aviso = screen.getByRole('alertdialog', { name: '¿Eliminar la columna «Stock»?' });
    expect(aviso).toHaveTextContent('No se puede deshacer');
    await user.click(within(aviso).getByRole('button', { name: 'No, conservarla' }));
    expect(textoFueraDeLaNavegacion('Stock')).toHaveLength(1);

    await user.click(screen.getByRole('button', { name: 'Eliminar Stock' }));
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar columna' }));

    await waitFor(() => expect(textoFueraDeLaNavegacion('Stock')).toHaveLength(0), ESPERA);
    expect(within(detalle('Productos')).getByText('Nombre')).toBeInTheDocument();
  });
});

describe('ENS-158: columna que se conecta con otra tabla', () => {
  it('crea una conexión eligiendo la tabla, sin valor inicial', async () => {
    const { proyecto, productos, clientes } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id, productos.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Proveedor');
    await user.click(within(form).getByRole('radio', { name: 'Conexión con otra tabla' }));
    expect(within(form).queryByLabelText('Valor inicial (opcional)')).not.toBeInTheDocument();
    const tabla = await within(form).findByLabelText('¿Con qué tabla se conecta?');
    await waitFor(() => expect(tabla).toBeEnabled());
    expect(
      within(tabla).getByRole('option', { name: 'Productos (esta misma tabla)' }),
    ).toBeInTheDocument();
    await user.selectOptions(tabla, 'Clientes');
    await guardarDato(user, form);
    await waitFor(() => expect(screen.queryByRole('form')).not.toBeInTheDocument(), ESPERA);

    const lista = await datosDelBloque();
    expect(await within(lista).findByText('Se conecta con «Clientes»', {}, ESPERA)).toBeVisible();
    expect(within(lista).getByText('Conexión con otra tabla')).toBeVisible();
    const guardados = await listarCampos(productos.id);
    expect(guardados.at(-1)).toMatchObject({
      nombre: 'Proveedor',
      tipo: 'RELACION',
      bloqueRelacionadoId: clientes.id,
      formula: null,
      valorInicial: null,
      opciones: [],
    });
  });

  it('pide la tabla antes de enviar y la vacía al cambiar de tipo', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id, productos.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Proveedor');
    await user.click(within(form).getByRole('radio', { name: 'Conexión con otra tabla' }));
    await guardarDato(user, form);
    const tabla = within(form).getByLabelText('¿Con qué tabla se conecta?');
    expect(tabla).toHaveAccessibleDescription(/Elegí la tabla con la que se conecta/);
    expect(screen.getByRole('form', { name: 'Nueva columna' })).toBeInTheDocument();
    expect(await listarCampos(productos.id)).toHaveLength(2);

    await waitFor(() => expect(tabla).toBeEnabled());
    await user.selectOptions(tabla, 'Clientes');
    await user.click(within(form).getByRole('radio', { name: 'Texto' }));
    await user.click(within(form).getByRole('radio', { name: 'Conexión con otra tabla' }));
    expect(within(form).getByLabelText('¿Con qué tabla se conecta?')).toHaveValue('');
  });

  it('muestra junto a la tabla el error 400 del servidor', async () => {
    const { proyecto, productos } = await setupProyectoConBloques();
    server.use(
      http.post('/api/bloques/:bloqueId/campos', ({ request }) =>
        errorResponse(
          400,
          'Error de validación en los datos enviados',
          new URL(request.url).pathname,
          { bloqueRelacionadoId: 'Elegí una tabla de este sistema' },
        ),
      ),
    );
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id, productos.id);

    const form = await abrirNuevoDato(user);
    await user.type(within(form).getByLabelText('Nombre de la columna'), 'Proveedor');
    await user.click(within(form).getByRole('radio', { name: 'Conexión con otra tabla' }));
    const tabla = within(form).getByLabelText('¿Con qué tabla se conecta?');
    await waitFor(() => expect(tabla).toBeEnabled());
    await user.selectOptions(tabla, 'Clientes');
    await guardarDato(user, form);
    await waitFor(
      () => expect(tabla).toHaveAccessibleDescription(/Elegí una tabla de este sistema/),
      ESPERA,
    );
  });

  it('no elimina una tabla que usa otra y explica dónde se usa', async () => {
    const { proyecto, productos, clientes } = await setupProyectoConBloques();
    await crearCampo({
      bloqueId: productos.id,
      datos: dato({ nombre: 'Proveedor', tipo: 'RELACION', bloqueRelacionadoId: clientes.id }),
    });
    const user = userEvent.setup();
    await abrirConstructor(proyecto.id, clientes.id);
    await verifyHeadingPresentAsync('Clientes');
    await datosDelBloque();

    await user.click(screen.getByRole('button', { name: 'Eliminar tabla' }));
    await user.click(screen.getByRole('button', { name: 'Sí, eliminar tabla' }));
    const aviso = screen.getByRole('alertdialog', { name: '¿Eliminar la tabla «Clientes»?' });
    expect(
      await within(aviso).findByText(
        'La tabla «Clientes» se usa en la columna «Proveedor» de «Productos»',
        {},
        ESPERA,
      ),
    ).toBeVisible();
    expect(botonDeBloque('Clientes')).toBeInTheDocument();
  });
});
