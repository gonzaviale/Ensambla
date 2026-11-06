import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { claveDemoProveedores } from '@/hooks/proveedores/useProveedoresDemo';
import { demoProveedoresSchema } from '@/types/proveedores';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

async function abrir() {
  const usuario = setupUsuarioConSesion();
  renderApp('/proveedores');
  await screen.findByRole('heading', { name: 'Proveedores', level: 1 }, ESPERA);
  return { user: userEvent.setup(), usuario };
}

describe('proveedores: demo funcional', () => {
  it('permite quitar un agotado, agregar stock suficiente y editar la cantidad del mensaje', async () => {
    const { user } = await abrir();
    await user.click(
      screen.getByRole('button', { name: 'Quitar Aceite Natura 900 ml del pedido' }),
    );
    expect(
      (screen.getByLabelText('Mensaje para WhatsApp') as HTMLTextAreaElement).value,
    ).not.toContain('Aceite Natura');
    await user.selectOptions(
      screen.getByLabelText('Agregar otro producto'),
      screen.getByRole('option', { name: /Coca-Cola/ }),
    );
    await user.click(screen.getByRole('button', { name: 'Agregar' }));
    const cantidad = screen.getByLabelText('Cantidad a pedir de Coca-Cola 2,25 L');
    await user.clear(cantidad);
    await user.type(cantidad, '24');
    expect((screen.getByLabelText('Mensaje para WhatsApp') as HTMLTextAreaElement).value).toContain(
      '24 × Coca-Cola',
    );
    expect(screen.getByRole('button', { name: /Guardar PDF/ })).toBeEnabled();
  });

  it('exige revisión y registra la compra en stock, historial y gastos de la misma cuenta', async () => {
    const { user, usuario } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Compras e historial' }));
    await user.click(screen.getByRole('button', { name: 'Probar sin archivo' }));
    const confirmar = screen.getByRole('button', { name: 'Confirmar e ingresar al stock' });
    expect(confirmar).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: /Revisé productos/ }));
    await user.click(confirmar);
    expect(screen.getByText(/Compra confirmada: stock/)).toBeVisible();
    expect(screen.getByText('DEMO-3')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Stock de prueba' }));
    const tabla = screen.getByRole('table', { name: 'Inventario de demostración' });
    const fila = within(tabla).getByRole('row', { name: /Yerba Playadito/ });
    expect(within(fila).getByRole('cell', { name: '16' })).toBeVisible();
    const guardados = demoProveedoresSchema.parse(
      JSON.parse(localStorage.getItem(claveDemoProveedores(usuario.id)) ?? '{}'),
    );
    expect(guardados.compras).toHaveLength(3);
    expect(guardados.gastos.filter((g) => g.compraId === guardados.compras[2]?.id)).toHaveLength(1);
    await user.click(screen.getByRole('button', { name: 'Costos y gastos' }));
    expect(screen.getByText('+12,0 %')).toBeVisible();
    expect(screen.getByText('Compra DEMO-3')).toBeVisible();
  });

  it('bloquea la recepción cuando el total difiere de los renglones', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Compras e historial' }));
    await user.click(screen.getByRole('button', { name: 'Probar sin archivo' }));
    await user.clear(screen.getByLabelText('Total del comprobante ($)'));
    await user.type(screen.getByLabelText('Total del comprobante ($)'), '1');
    await user.click(screen.getByRole('checkbox', { name: /Revisé productos/ }));
    expect(screen.getByRole('alert')).toHaveTextContent('El total no coincide');
    expect(screen.getByRole('button', { name: 'Confirmar e ingresar al stock' })).toBeDisabled();
  });

  it('agrega un gasto general al período y lo conserva con las categorías existentes', async () => {
    const { user, usuario } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Costos y gastos' }));
    await user.type(screen.getByLabelText('Concepto del gasto'), 'Gas del local');
    await user.type(screen.getByLabelText('Importe del gasto ($)'), '1500');
    await user.click(screen.getByRole('button', { name: 'Agregar gasto de prueba' }));
    expect(screen.getByText('Gas del local')).toBeVisible();
    const datos = demoProveedoresSchema.parse(
      JSON.parse(localStorage.getItem(claveDemoProveedores(usuario.id)) ?? '{}'),
    );
    expect(datos.gastos.at(-1)).toMatchObject({
      name: 'Gas del local',
      category: 'Servicios',
      total: 1500,
    });
    expect(localStorage.getItem(claveDemoProveedores('otra-cuenta'))).toBeNull();
  });
});
