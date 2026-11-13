import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';
import type { SolicitudCobro } from '@/types/caja';

async function abrir() {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta inventario');
  inventario.rows = [
    {
      id: crypto.randomUUID(),
      name: 'Coca-Cola',
      barcode: '7790895000997',
      quantity: 10,
      price: 2700,
      favorite: true,
    },
    {
      id: crypto.randomUUID(),
      name: 'Pan francés',
      quantity: 2,
      price: 4000,
      saleMode: 'peso',
      favorite: true,
    },
  ];
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  return { user: userEvent.setup(), sistema, inventario };
}

function comprobante(request: SolicitudCobro) {
  return {
    id: request.id,
    lineas: request.lineas.map((l) => ({
      ...l,
      nombre: 'Producto',
      importe: l.cantidad * l.precio,
    })),
    medio: request.medio,
    total: request.lineas.reduce((sum, l) => sum + l.cantidad * l.precio, 0),
    recibido: request.recibido ?? 2700,
    vuelto: 0,
    creadoEn: new Date().toISOString(),
  };
}

describe('caja integrada con el cliente HTTP', () => {
  it('protege un artículo sin guardar con un diálogo propio y conserva los datos al seguir editando', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Agregar artículo sin código' }));
    await user.type(screen.getByLabelText('Nombre del artículo'), 'Yerba de prueba');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(
      await screen.findByRole('alertdialog', { name: 'Hay cambios sin guardar' }),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect(screen.getByLabelText('Nombre del artículo')).toHaveValue('Yerba de prueba');
    await user.keyboard('{Escape}');
    await user.click(await screen.findByRole('button', { name: 'Descartar y continuar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
  it('ubica Caja entre Inicio y Mi Negocio y suma unidades con retiro y deshacer', async () => {
    const { user } = await abrir();
    const nav = screen.getByRole('navigation', { name: 'Principal' });
    expect(
      within(nav)
        .getAllByRole('button')
        .slice(0, 3)
        .map((b) => b.textContent),
    ).toEqual(['Inicio', 'Caja', 'Mi Negocio']);
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('2');
    await user.click(screen.getByRole('button', { name: 'Restar una unidad de Coca-Cola' }));
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('1');
    await user.click(screen.getByRole('button', { name: 'Quitar Coca-Cola' }));
    expect(screen.queryByLabelText('Cantidad de Coca-Cola')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Deshacer último cambio' }));
    expect(screen.getByLabelText('Cantidad de Coca-Cola')).toHaveValue('1');
  });

  it('toma gramos para un favorito por kilo y permite corregir el peso final', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Pan francés.*4.000/ }));
    const dialogo = screen.getByRole('dialog');
    await user.clear(within(dialogo).getByLabelText('Gramos'));
    await user.type(within(dialogo).getByLabelText('Gramos'), '500');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar a la compra' }));
    const gramos = screen.getByLabelText('Cantidad de Pan francés en gramos');
    expect(gramos).toHaveValue('500');
    await user.clear(gramos);
    await user.type(gramos, '550');
    await user.tab();
    expect(screen.getByRole('button', { name: /Cobrar.*2.200/ })).toBeEnabled();
  });

  it('un código desconocido permite un alta rápida y continuar la venta', async () => {
    const { user } = await abrir();
    await user.type(screen.getByLabelText('Código de barras'), '77903754{Enter}');
    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText(/Artículo no reconocido/)).toBeVisible();
    await user.type(within(dialogo).getByLabelText('Nombre del artículo'), 'Alfajor');
    await user.type(within(dialogo).getByLabelText('Precio por unidad ($)'), '850');
    await user.click(within(dialogo).getByRole('button', { name: 'Agregar a la compra' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByLabelText('Cantidad de Alfajor')).toHaveValue('1');
    expect(screen.getByLabelText('Código de barras')).toHaveValue('');
  });

  it('usa la API de cobro y muestra éxito después de recibir confirmación', async () => {
    const solicitudes: SolicitudCobro[] = [];
    server.use(
      http.post('/api/caja/ventas', async ({ request }) => {
        const body = (await request.json()) as SolicitudCobro;
        solicitudes.push(body);
        return HttpResponse.json(comprobante(body));
      }),
    );
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
    await screen.findByRole('heading', { name: 'Venta registrada' }, ESPERA);
    expect(solicitudes).toHaveLength(1);
    expect(solicitudes[0]?.lineas[0]).toMatchObject({ cantidad: 1, precio: 2700, modo: 'UNIDAD' });
  });

  it('un resultado incierto se verifica con el mismo id sin crear otro cobro', async () => {
    const solicitudes: SolicitudCobro[] = [];
    server.use(
      http.post('/api/caja/ventas', async ({ request }) => {
        const body = (await request.json()) as SolicitudCobro;
        solicitudes.push(body);
        return solicitudes.length === 1
          ? HttpResponse.error()
          : HttpResponse.json(comprobante(body));
      }),
    );
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
    await user.click(
      await screen.findByRole('button', { name: 'Verificar misma operación' }, ESPERA),
    );
    await screen.findByRole('heading', { name: 'Venta registrada' }, ESPERA);
    expect(solicitudes).toHaveLength(2);
    expect(solicitudes[1]?.id).toBe(solicitudes[0]?.id);
  });
});
