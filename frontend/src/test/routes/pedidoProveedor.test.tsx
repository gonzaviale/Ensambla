import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import { telefonoDe } from '@/lib/sistema/telefonosDeProveedores';
import type { Fila } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

const FILAS_CON_FALTANTES: Fila[] = [
  { id: 'f-yerba', name: 'Yerba', quantity: 0, minimum: 10, supplier: 'Molinos' },
  { id: 'f-fideos', name: 'Fideos', quantity: 3, minimum: 10, supplier: 'Molinos' },
  { id: 'f-aceite', name: 'Aceite', quantity: 0 },
  { id: 'f-sal', name: 'Sal', quantity: 50, minimum: 10, supplier: 'Molinos' },
];

const FILAS_CON_STOCK: Fila[] = [{ id: 'f-sal', name: 'Sal', quantity: 50, minimum: 10 }];

const abrirPedido = async (filas: Fila[] = FILAS_CON_FALTANTES) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const sistema = makeSystem('Almacén de Ana', ['inventory']);
  const [inventario] = allBlocks(sistema);
  if (!inventario) throw new Error('El sistema de prueba no tiene inventario');
  inventario.rows = filas;
  estado.sistemas.set(usuario.email, [sistema]);
  const user = userEvent.setup();
  renderApp(`/sistemas/${sistema.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  await user.click(await screen.findByRole('button', { name: /Pedido a proveedor/ }, ESPERA));
  const modal = await screen.findByRole('dialog', {}, ESPERA);
  return { user, modal, sistema };
};

type CuerpoDelPedido = { proveedor: string | null; productos: { productoId: string; cantidad: number }[] };

/** Escucha el pedido del PDF y guarda el cuerpo con el que llegó. */
const escucharPedidos = () => {
  const cuerpos: CuerpoDelPedido[] = [];
  server.use(
    http.post('/api/proyectos/:sistemaId/reposicion/pdf', async ({ request }) => {
      cuerpos.push((await request.json()) as CuerpoDelPedido);
      return new HttpResponse(new TextEncoder().encode('%PDF-1.4'), {
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="pedido-molinos-20261019.pdf"',
        },
      });
    }),
  );
  return cuerpos;
};

describe('el pedido a un proveedor', () => {
  beforeEach(() => {
    guardarArchivo.mockClear();
    window.localStorage.clear();
  });

  it('muestra lo que falta agrupado por proveedor, con la cantidad que se sugiere', async () => {
    const { modal } = await abrirPedido();

    expect(await within(modal).findByRole('tab', { name: 'Molinos (2)' })).toHaveAttribute('aria-selected', 'true');
    expect(within(modal).getByRole('tab', { name: 'Sin proveedor (1)' })).toBeVisible();
    expect(within(modal).getByLabelText('Cantidad de Yerba')).toHaveValue(20);
    expect(within(modal).getByLabelText('Cantidad de Fideos')).toHaveValue(17);
    expect(within(modal).queryByText('Sal')).not.toBeInTheDocument();
    expect(within(modal).getByText('Quedan 3 (mínimo 10)')).toBeVisible();
  });

  it('baja el PDF con las cantidades que se ajustaron y se cierra el modal', async () => {
    const cuerpos = escucharPedidos();
    const { user, modal } = await abrirPedido();
    const cantidad = await within(modal).findByLabelText('Cantidad de Yerba');

    await user.clear(cantidad);
    await user.type(cantidad, '25');
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(guardarArchivo).toHaveBeenCalledTimes(1), ESPERA);
    expect(guardarArchivo.mock.calls[0]?.[0]).toMatchObject({ nombre: 'pedido-molinos-20261019.pdf' });
    expect(cuerpos).toEqual([
      {
        proveedor: 'Molinos',
        productos: [
          { productoId: 'f-yerba', cantidad: 25 },
          { productoId: 'f-fideos', cantidad: 17 },
        ],
      },
    ]);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument(), ESPERA);
  });

  it('no pide el producto que se destilda', async () => {
    const cuerpos = escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.click(await within(modal).findByRole('checkbox', { name: 'Pedir Fideos' }));
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(cuerpos).toHaveLength(1), ESPERA);
    expect(cuerpos[0]?.productos).toEqual([{ productoId: 'f-yerba', cantidad: 20 }]);
  });

  it('sin productos elegidos avisa y no pide nada al servidor', async () => {
    const cuerpos = escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.click(await within(modal).findByRole('checkbox', { name: 'Pedir Yerba' }));
    await user.click(within(modal).getByRole('checkbox', { name: 'Pedir Fideos' }));
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent('Elegí al menos un producto para el pedido.');
    expect(cuerpos).toHaveLength(0);
  });

  it('avisa cuando una cantidad no es válida', async () => {
    const cuerpos = escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.clear(await within(modal).findByLabelText('Cantidad de Yerba'));
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent('Revisá las cantidades');
    expect(cuerpos).toHaveLength(0);
  });

  it('los productos sin proveedor explican cómo agruparlos y se piden sin proveedor', async () => {
    const cuerpos = escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.click(await within(modal).findByRole('tab', { name: 'Sin proveedor (1)' }));
    expect(within(modal).getByText(/columna «Proveedor»/)).toBeVisible();
    expect(within(modal).getByLabelText('Cantidad de Aceite')).toHaveValue(10);
    await user.click(within(modal).getByRole('button', { name: /Descargar PDF/ }));

    await waitFor(() => expect(cuerpos).toHaveLength(1), ESPERA);
    expect(cuerpos[0]).toEqual({ proveedor: null, productos: [{ productoId: 'f-aceite', cantidad: 10 }] });
  });

  it('en la computadora «Enviar por WhatsApp» baja el PDF, abre WhatsApp y recuerda el número', async () => {
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null);
    escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.type(await within(modal).findByLabelText(/WhatsApp de Molinos/), '11 5555-0101');
    await user.click(within(modal).getByRole('button', { name: 'Enviar por WhatsApp' }));

    await waitFor(() => expect(abrir).toHaveBeenCalledTimes(1), ESPERA);
    expect(String(abrir.mock.calls[0]?.[0])).toContain('https://wa.me/1155550101?text=');
    expect(guardarArchivo).toHaveBeenCalledTimes(1);
    expect(telefonoDe('Molinos')).toBe('11 5555-0101');
    abrir.mockRestore();
  });

  it('si no se puede abrir WhatsApp lo dice y el modal sigue abierto', async () => {
    vi.spyOn(window, 'open').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    escucharPedidos();
    const { user, modal } = await abrirPedido();

    await user.click(await within(modal).findByRole('button', { name: 'Enviar por WhatsApp' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent('No pudimos abrir WhatsApp');
    expect(screen.getByRole('dialog')).toBeVisible();
  });

  it('si el servidor no puede armar el PDF lo dice y el modal sigue abierto', async () => {
    setupServerError('post', '/proyectos/:sistemaId/reposicion/pdf', 500, 'Error interno');
    const { user, modal } = await abrirPedido();

    await user.click(await within(modal).findByRole('button', { name: /Descargar PDF/ }));

    expect(await within(modal).findByRole('alert')).toBeVisible();
    expect(guardarArchivo).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeVisible();
  });

  it('cuando no falta nada lo dice', async () => {
    const { user, modal } = await abrirPedido(FILAS_CON_STOCK);

    expect(await within(modal).findByText(/no te falta nada/)).toBeVisible();
    await user.click(within(modal).getByRole('button', { name: 'Listo' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument(), ESPERA);
  });

  it('si no puede ver el stock ofrece reintentar', async () => {
    setupServerError('get', '/proyectos/:sistemaId/reposicion', 500, 'Error interno');
    const { user, modal } = await abrirPedido();

    expect(await within(modal).findByRole('alert')).toHaveTextContent('No pudimos ver tu stock');
    server.resetHandlers();
    await user.click(within(modal).getByRole('button', { name: 'Reintentar' }));

    expect(await within(modal).findByRole('tab', { name: 'Molinos (2)' }, ESPERA)).toBeVisible();
  });

  it('el botón está entre las herramientas del cuaderno y lo cierra con Cerrar', async () => {
    const { user } = await abrirPedido();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument(), ESPERA);
  });
});
