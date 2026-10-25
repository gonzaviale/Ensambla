import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { findBlock, makeSystem, makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

const abrirEditor = async (
  sistema: Sistema = makeTemplate('shop', {}, true),
  modo: 'construir' | 'usar' = 'construir',
) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}?modo=${modo}`);
  await screen.findByRole('heading', { level: 1, name: new RegExp(sistema.name) }, ESPERA);
  return { router, user: userEvent.setup(), sistema, usuario };
};

const activo = () => {
  const sistema = seleccionarSistemaActivo(useSistemaStore.getState());
  if (!sistema) throw new Error('No hay un sistema activo');
  return sistema;
};

const bloquePorNombre = (nombre: string) => {
  const nodo = [...document.querySelectorAll<HTMLElement>('.block')].find(
    (bloque) => bloque.getAttribute('aria-label') === nombre,
  );
  if (!nodo) throw new Error(`Falta el bloque ${nombre}`);
  return nodo;
};

const tipoDeBloque = (tipo: string) => {
  const bloque = activo()
    .sections.flatMap((seccion) => seccion.blocks)
    .find((item) => item.type === tipo);
  if (!bloque) throw new Error(`Falta el bloque de tipo ${tipo}`);
  return bloque;
};

describe('el editor: construir y usar', () => {
  it('abre el sistema de la dirección con la biblioteca, el lienzo y el inspector', async () => {
    await abrirEditor();

    expect(screen.getByRole('button', { name: /Construir/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByText('Tus piezas')).toBeVisible();
    expect(screen.getByText('Elegí un bloque.')).toBeVisible();
    expect(document.querySelectorAll('.library-item')).toHaveLength(10);
    expect(document.querySelectorAll('.block')).toHaveLength(3);
    expect(screen.getByText(/Estás viendo datos de ejemplo/)).toBeVisible();
  });

  it('en modo usar muestra el cartel de «listo» y Personalizar pasa a Construir', async () => {
    const { user } = await abrirEditor(makeTemplate('shop', {}, true), 'usar');
    useSistemaStore.setState({ listo: true });

    expect(await screen.findByText('Tu sistema está listo. Ya podés usarlo.')).toBeVisible();
    expect(screen.queryByText('Tus piezas')).toBeNull();
    await clickButton(user, /Personalizar/);

    expect(await screen.findByText('Tus piezas')).toBeVisible();
    expect(screen.queryByText('Tu sistema está listo. Ya podés usarlo.')).toBeNull();
    expect(useSistemaStore.getState().modo).toBe('build');
  });

  it('un sistema que no existe lleva al inicio', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/sistemas/no-existe?modo=usar');

    await verifyPathAsync(router, '/inicio');
  });

  it('el nombre del sistema se cambia desde el título', async () => {
    const { user } = await abrirEditor();

    await user.click(
      screen.getByRole('heading', { level: 1 }).querySelector('button') as HTMLElement,
    );
    const campo = await screen.findByLabelText('Nombre');
    await user.clear(campo);
    await user.type(campo, 'Almacén Don Luis');
    await clickButton(user, 'Guardar nombre');

    expect(
      await screen.findByRole('heading', { level: 1, name: /Almacén Don Luis/ }),
    ).toBeVisible();
    expect(activo().name).toBe('Almacén Don Luis');
  });

  it('el guardado llega al servidor y la barra lo cuenta', async () => {
    const { user, usuario, sistema } = await abrirEditor();

    await user.click(
      within(bloquePorNombre('Inventario')).getByRole('button', { name: 'Duplicar bloque' }),
    );

    expect(await screen.findByText('Guardando…')).toBeVisible();
    await waitFor(
      () => expect(estado.sistemas.get(usuario.email)?.[0]?.sections[0]?.blocks).toHaveLength(4),
      ESPERA,
    );
    await screen.findByText('Guardado', {}, ESPERA);
    expect(sistema.sections[0]?.blocks).toHaveLength(3);
  });
});

describe('bloques', () => {
  it('suma una pieza de la biblioteca, la elige y se puede deshacer', async () => {
    const { user } = await abrirEditor();

    await user.click(screen.getByRole('button', { name: 'Agregar Gastos' }));

    await verifyToastAsync('Gastos agregado');
    expect(activo().sections[0]?.blocks.at(-1)?.type).toBe('expenses');
    expect(screen.getByLabelText('Personalizar Gastos')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Deshacer el último cambio' }));
    expect(activo().sections[0]?.blocks).toHaveLength(3);
    await verifyToastAsync('Cambio deshecho.');
  });

  it('Enter en una pieza de la biblioteca la agrega y Ctrl+Z deshace', async () => {
    const { user } = await abrirEditor();

    screen.getByRole('button', { name: 'Agregar Nota' }).focus();
    await user.keyboard('{Enter}');
    expect(activo().sections[0]?.blocks.at(-1)?.type).toBe('note');

    await user.keyboard('{Control>}z{/Control}');
    expect(activo().sections[0]?.blocks).toHaveLength(3);
  });

  it('busca en la biblioteca', async () => {
    const { user } = await abrirEditor();

    await user.type(screen.getByLabelText('Buscar bloques'), 'fiado');

    const visibles = [...document.querySelectorAll('.library-item')].filter(
      (nodo) => !(nodo as HTMLElement).hidden,
    );
    expect(visibles.length).toBeGreaterThan(0);
    expect(visibles.length).toBeLessThan(10);
  });

  it('mueve, duplica y quita un bloque con confirmación', async () => {
    const { user } = await abrirEditor();
    const nombres = () => activo().sections[0]?.blocks.map((bloque) => bloque.name);
    const [primero, segundo] = nombres() ?? [];

    await user.click(
      within(bloquePorNombre(segundo ?? '')).getByRole('button', { name: 'Mover arriba' }),
    );
    expect(nombres()?.slice(0, 2)).toEqual([segundo, primero]);

    await user.click(
      within(bloquePorNombre('Clientes')).getByRole('button', { name: 'Duplicar bloque' }),
    );
    expect(nombres()).toContain('Clientes (copia)');

    await user.click(
      within(bloquePorNombre('Clientes (copia)')).getByRole('button', { name: 'Quitar bloque' }),
    );
    const confirmacion = await screen.findByRole('dialog');
    expect(within(confirmacion).getByText('¿Quitar Clientes (copia)?')).toBeVisible();
    await user.click(within(confirmacion).getByRole('button', { name: 'Quitar bloque' }));
    expect(nombres()).not.toContain('Clientes (copia)');
  });

  it('el inspector cambia nombre, color, ancho y sección', async () => {
    const { user } = await abrirEditor();
    await user.click(
      within(bloquePorNombre('Clientes')).getByRole('button', { name: 'Personalizar bloque' }),
    );
    const inspector = screen.getByLabelText('Personalizar Clientes');

    const nombre = within(inspector).getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Los de siempre');
    await user.tab();
    expect(findBlock(activo(), useSistemaStore.getState().selectedId ?? '')?.name).toBe(
      'Los de siempre',
    );

    await user.click(within(inspector).getByRole('button', { name: 'Coral' }));
    expect(tipoDeBloque('clients').color).toBe('coral');

    await user.selectOptions(within(inspector).getByLabelText('Ancho en la pantalla'), 'third');
    expect(tipoDeBloque('clients').width).toBe('third');
  });

  it('un nombre vacío en el inspector no se guarda', async () => {
    const { user } = await abrirEditor();
    await user.click(
      within(bloquePorNombre('Clientes')).getByRole('button', { name: 'Personalizar bloque' }),
    );

    const nombre = within(screen.getByLabelText('Personalizar Clientes')).getByLabelText('Nombre');
    await user.clear(nombre);
    await user.tab();

    expect(tipoDeBloque('clients').name).toBe('Clientes');
  });

  it('los botones de ancho del bloque lo cambian', async () => {
    const { user } = await abrirEditor();

    await user.click(
      within(bloquePorNombre('Ventas')).getByRole('button', { name: 'Fila completa' }),
    );

    expect(tipoDeBloque('sales').width).toBe('full');
  });

  it('la distribución se aplica a la sección', async () => {
    const { user } = await abrirEditor();

    await clickButton(user, /1 columna/);

    expect(activo().sections[0]?.blocks.every((bloque) => bloque.width === 'full')).toBe(true);
    expect(activo().layout).toBe('single');
  });

  it('soltar una pieza de la biblioteca en el lienzo la agrega antes del bloque de destino', async () => {
    await abrirEditor();
    const pieza = screen.getByRole('button', { name: 'Agregar Gastos' });
    const destino = bloquePorNombre('Ventas');
    const dataTransfer = { setData: () => undefined, effectAllowed: '' };

    fireEvent.dragStart(pieza, { dataTransfer });
    fireEvent.dragOver(destino, { dataTransfer });
    expect(destino).toHaveClass('drag-over');
    fireEvent.drop(destino, { dataTransfer });

    const tipos = activo().sections[0]?.blocks.map((bloque) => bloque.type);
    expect(tipos?.indexOf('expenses')).toBe((tipos?.indexOf('sales') ?? 0) - 1);
  });

  it('soltar un bloque al lado de otro hace que compartan la fila', async () => {
    await abrirEditor();
    const tirador = within(bloquePorNombre('Clientes')).getByRole('button', {
      name: 'Arrastrar bloque',
    });
    const lado = bloquePorNombre('Ventas').querySelector<HTMLElement>('[data-drop-side="left"]');
    if (!lado) throw new Error('Falta la zona de soltar');
    const dataTransfer = { setData: () => undefined, effectAllowed: '' };

    fireEvent.dragStart(tirador, { dataTransfer });
    fireEvent.dragOver(lado, { dataTransfer });
    fireEvent.drop(lado, { dataTransfer });

    expect(tipoDeBloque('clients').width).toBe('half');
    expect(tipoDeBloque('sales').width).toBe('half');
    await verifyToastAsync('Las dos piezas comparten la fila.');
  });
});

describe('registros', () => {
  it('registra un producto, lo edita y lo elimina', async () => {
    const { user } = await abrirEditor();
    const inventario = bloquePorNombre('Inventario');

    await user.click(within(inventario).getByRole('button', { name: /Registrar/ }));
    const modal = await screen.findByRole('dialog');
    await user.type(within(modal).getByLabelText(/Producto/), 'Alfajor Jorgito');
    await user.type(within(modal).getByLabelText(/Cantidad/), '12');
    await user.type(within(modal).getByLabelText('Precio'), '900');
    await user.click(within(modal).getByRole('button', { name: 'Guardar' }));

    expect(await within(inventario).findByText('Alfajor Jorgito')).toBeVisible();
    await verifyToastAsync('Registro guardado.');

    const fila = within(inventario).getByText('Alfajor Jorgito').closest('tr');
    if (!fila) throw new Error('Falta la fila');
    await user.click(within(fila).getByRole('button', { name: 'Editar registro' }));
    const edicion = await screen.findByRole('dialog');
    const cantidad = within(edicion).getByLabelText(/Cantidad/);
    await user.clear(cantidad);
    await user.type(cantidad, '5');
    await user.click(within(edicion).getByRole('button', { name: 'Guardar' }));
    await verifyToastAsync('Registro actualizado.');
    expect(
      tipoDeBloque('inventory').rows.find((fila) => fila.name === 'Alfajor Jorgito')?.quantity,
    ).toBe(5);

    await user.click(
      within(
        within(inventario).getByText('Alfajor Jorgito').closest('tr') as HTMLElement,
      ).getByRole('button', { name: 'Editar registro' }),
    );
    await user.click(await screen.findByRole('button', { name: /Eliminar/ }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
    );

    await waitFor(() => expect(within(inventario).queryByText('Alfajor Jorgito')).toBeNull());
  });

  it('muestra los errores junto a cada campo y no guarda', async () => {
    const { user } = await abrirEditor();

    await user.click(
      within(bloquePorNombre('Inventario')).getByRole('button', { name: /Registrar/ }),
    );
    const modal = await screen.findByRole('dialog');
    await user.click(within(modal).getByRole('button', { name: 'Guardar' }));

    expect(within(modal).getAllByText('Completá este campo.').length).toBeGreaterThan(0);
    expect(modal.querySelector('.field.invalid')).not.toBeNull();
    expect(tipoDeBloque('inventory').rows).toHaveLength(3);
  });

  it('una venta descuenta el stock del producto', async () => {
    const { user } = await abrirEditor();
    const antes = Number(tipoDeBloque('inventory').rows[0]?.quantity);

    await user.click(within(bloquePorNombre('Ventas')).getByRole('button', { name: /Registrar/ }));
    const modal = await screen.findByRole('dialog');
    await user.selectOptions(
      within(modal).getByLabelText(/Producto/),
      tipoDeBloque('inventory').rows[0]?.id ?? '',
    );
    await user.type(within(modal).getByLabelText(/Cantidad/), '2');
    await user.click(within(modal).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(Number(tipoDeBloque('inventory').rows[0]?.quantity)).toBe(antes - 2),
    );
  });

  it('busca dentro de un bloque', async () => {
    const { user } = await abrirEditor();
    const inventario = bloquePorNombre('Inventario');

    await user.type(within(inventario).getByLabelText('Buscar en Inventario'), 'coca');

    const visibles = [...inventario.querySelectorAll<HTMLElement>('tbody tr')].filter(
      (fila) => !fila.hidden,
    );
    expect(visibles).toHaveLength(1);
    expect(visibles[0]).toHaveTextContent('Coca-Cola');
  });

  it('un bloque Formulario en modo usar guarda desde el mismo bloque', async () => {
    const sistema = makeSystem('Con formulario', ['form']);
    const { user } = await abrirEditor(sistema, 'usar');
    const bloque = bloquePorNombre('Formulario');

    await user.type(within(bloque).getByLabelText(/Nombre/), 'Dato suelto');
    await user.click(within(bloque).getByRole('button', { name: 'Guardar registro' }));

    await waitFor(() => expect(activo().sections[0]?.blocks[0]?.rows).toHaveLength(1));
  });

  it('quitar los ejemplos conserva lo que la persona cargó', async () => {
    const { user } = await abrirEditor();

    await clickButton(user, 'Quitar ejemplos');
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Quitar ejemplos' }),
    );

    await waitFor(() => expect(screen.queryByText(/Estás viendo datos de ejemplo/)).toBeNull());
    expect(tipoDeBloque('inventory').rows).toHaveLength(0);
  });
});

describe('campos y secciones', () => {
  it('agrega un campo de opciones y lo quita', async () => {
    const { user } = await abrirEditor();
    await user.click(
      within(bloquePorNombre('Clientes')).getByRole('button', { name: 'Personalizar bloque' }),
    );

    await clickButton(user, /Agregar campo/);
    const modal = await screen.findByRole('dialog');
    await user.type(within(modal).getByLabelText('Nombre del campo'), 'Zona');
    await user.selectOptions(within(modal).getByLabelText('Tipo de dato'), 'select');
    await user.type(within(modal).getByLabelText(/Opciones/), 'Norte, Sur, Norte');
    await user.click(within(modal).getByRole('button', { name: 'Guardar campo' }));

    const campo = tipoDeBloque('clients').fields.find((item) => item.label === 'Zona');
    expect(campo?.options).toEqual(['Norte', 'Sur']);

    await user.click(
      within(screen.getByLabelText('Personalizar Clientes')).getByRole('button', { name: /Zona/ }),
    );
    await user.click(await screen.findByRole('button', { name: /Quitar campo/ }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Quitar campo' }),
    );

    expect(tipoDeBloque('clients').fields.some((item) => item.label === 'Zona')).toBe(false);
  });

  it('pide un nombre y no deja repetirlo', async () => {
    const { user } = await abrirEditor();
    await user.click(
      within(bloquePorNombre('Clientes')).getByRole('button', { name: 'Personalizar bloque' }),
    );
    await clickButton(user, /Agregar campo/);
    const modal = await screen.findByRole('dialog');

    await user.click(within(modal).getByRole('button', { name: 'Guardar campo' }));
    expect(within(modal).getByText('Elegí un nombre para el campo.')).toBeVisible();

    await user.type(
      within(modal).getByLabelText('Nombre del campo'),
      tipoDeBloque('clients').fields[0]?.label ?? '',
    );
    await user.click(within(modal).getByRole('button', { name: 'Guardar campo' }));
    expect(within(modal).getByText('Ya hay un campo con ese nombre.')).toBeVisible();
  });

  it('agrega una sección con piezas, la renombra, la mueve y la quita', async () => {
    const { user } = await abrirEditor();

    await user.click(screen.getByRole('button', { name: 'Agregar una sección' }));
    const nueva = await screen.findByRole('dialog');
    await user.type(within(nueva).getByLabelText('Nombre de la sección'), 'Cuaderno');
    await user.selectOptions(within(nueva).getByLabelText('¿Con qué querés empezar?'), 'sales');
    await user.click(within(nueva).getByRole('button', { name: /Agregar sección/ }));

    expect(activo().sections.map((seccion) => seccion.name)).toEqual(['Vista general', 'Cuaderno']);
    expect(activo().sections[1]?.blocks.map((bloque) => bloque.type)).toEqual(['sales', 'clients']);
    expect(await screen.findByRole('button', { name: 'Cuaderno', current: 'page' })).toBeVisible();

    await clickButton(user, /Secciones/);
    const gestor = await screen.findByRole('dialog');
    await user.click(
      within(gestor).getAllByRole('button', { name: 'Mover sección arriba' })[1] as HTMLElement,
    );
    expect(activo().sections.map((seccion) => seccion.name)).toEqual(['Cuaderno', 'Vista general']);

    await user.click(
      within(gestor).getAllByRole('button', {
        name: 'Quitar sección y conservar bloques',
      })[0] as HTMLElement,
    );
    expect(activo().sections.map((seccion) => seccion.name)).toEqual(['Vista general']);
    expect(activo().sections[0]?.blocks).toHaveLength(5);
  });

  it('el selector de bloques ofrece todo el catálogo', async () => {
    const { user } = await abrirEditor();

    await clickButton(user, /Agregar otro bloque/);
    const modal = await screen.findByRole('dialog');
    expect(modal.querySelectorAll('.need-card')).toHaveLength(10);
    await user.click(within(modal).getByRole('button', { name: /Gastos/ }));

    expect(activo().sections[0]?.blocks.at(-1)?.type).toBe('expenses');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('un conjunto listo se agrega en una sección nueva', async () => {
    const { user } = await abrirEditor();

    await clickButton(user, 'Conjuntos listos');
    await user.click(screen.getByRole('button', { name: /Depósito/ }));

    expect(activo().sections).toHaveLength(2);
    expect(activo().sections[1]?.name).toBe('Depósito');
  });
});

describe('traer datos de una foto', () => {
  const foto = () => new File(['pixeles'], 'cuaderno.jpg', { type: 'image/jpeg' });

  it('desde el editor lleva la foto a Sami, que la mira y pregunta qué ve mal', async () => {
    const { user } = await abrirEditor(makeTemplate('shop', {}, false));

    await clickButton(user, /Traer datos de una foto/);
    const entrada = document.querySelector<HTMLInputElement>('#photo-file');
    if (!entrada) throw new Error('Falta el selector de archivos');
    await user.upload(entrada, foto());

    await screen.findByRole('heading', { name: 'Lo ajustamos juntos.' }, ESPERA);
    expect(await screen.findByText(/Miré tu foto/, undefined, ESPERA)).toBeVisible();
    expect(screen.getByText(/Te paso una foto de cómo anoto hoy/)).toBeVisible();
    expect(await screen.findByText('¿Está bien lo que leí?', undefined, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Es de otro cuaderno' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Quiero sumar otra tabla' })).toBeVisible();
  });

  it('rechaza un archivo que no es una imagen', async () => {
    await abrirEditor();
    const user = userEvent.setup({ applyAccept: false });

    await clickButton(user, /Traer datos de una foto/);
    const entrada = document.querySelector<HTMLInputElement>('#photo-file');
    if (!entrada) throw new Error('Falta el selector de archivos');
    await user.upload(entrada, new File(['hola'], 'nota.txt', { type: 'text/plain' }));

    expect(await screen.findByText('Elegí una imagen JPG, PNG o WEBP.')).toBeVisible();
  });

  it('desde el inicio, sin sistema, también va a Sami', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    await screen.findByRole('heading', { name: /En qué puedo/ }, ESPERA);
    const user = userEvent.setup();

    await user.click(screen.getAllByRole('button', { name: /Mostrarme cómo/ })[1] as HTMLElement);
    const entrada = document.querySelector<HTMLInputElement>('#photo-file');
    if (!entrada) throw new Error('Falta el selector de archivos');
    await user.upload(entrada, foto());

    await verifyPathAsync(router, '/taller');
    expect(await screen.findByText(/Miré tu foto/, undefined, ESPERA)).toBeVisible();
  });
});
