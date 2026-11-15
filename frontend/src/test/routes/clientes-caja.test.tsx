import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/node';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

async function abrir(sinTabla = false) {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta el inventario de prueba');
  prepararCaja(sistema, inventario.id);
  for (const seccion of sistema.sections) {
    if (sinTabla) seccion.blocks = seccion.blocks.filter((b) => b.type !== 'clients');
  }
  estado.sistemas.set(usuario.email, [sistema]);
  server.use(http.get('/api/caja/ventas', () => HttpResponse.json({ ventas: [], cobros: [] })));
  const app = renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Clientes' }));
  return { user, sistema, usuario, app };
}

describe('clientes compartidos de Caja y Mi Negocio', () => {
  it('crea un cliente y lo persiste en la tabla compartida', async () => {
    const { user, usuario } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
    const dialogo = within(screen.getByRole('dialog', { name: 'Crear cliente' }));
    await user.type(dialogo.getByLabelText(/^Nombre/), 'Ana López');
    await user.type(dialogo.getByLabelText(/^Teléfono/), '1122334455');
    await user.click(dialogo.getByRole('button', { name: 'Guardar cliente' }));

    expect(await screen.findByText('Ana López')).toBeVisible();
    await waitFor(() => {
      const filas = allBlocks(estado.sistemas.get(usuario.email)?.[0])
        .filter((b) => b.type === 'clients')
        .flatMap((b) => b.rows);
      expect(filas).toEqual([expect.objectContaining({ name: 'Ana López', phone: '1122334455' })]);
    }, ESPERA);
  });

  it('prepara una tabla de clientes cuando el negocio todavía no tiene una', async () => {
    const { user, usuario } = await abrir(true);
    await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
    await user.type(screen.getByLabelText(/^Nombre/), 'Pedro');
    await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));

    await waitFor(() => {
      expect(
        allBlocks(estado.sistemas.get(usuario.email)?.[0]).filter((b) => b.type === 'clients'),
      ).toEqual([expect.objectContaining({ rows: [expect.objectContaining({ name: 'Pedro' })] })]);
    }, ESPERA);
  });

  it('valida el nombre y protege el formulario al descartar cambios', async () => {
    const { user } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
    expect(screen.getByText('Completá este campo.')).toBeVisible();
    await user.type(screen.getByLabelText(/^Nombre/), 'Ana');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    await user.click(await screen.findByRole('button', { name: 'Seguir editando' }));
    expect(screen.getByLabelText(/^Nombre/)).toHaveValue('Ana');
  });

  it('edita sin duplicar al cliente y permite buscar sin distinguir acentos', async () => {
    const { user, usuario } = await abrir();
    await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
    await user.type(screen.getByLabelText(/^Nombre/), 'Ana López');
    await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
    await user.click(screen.getByRole('button', { name: 'Editar a Ana López' }));
    await user.type(screen.getByLabelText(/^Teléfono/), '1144556677');
    await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
    await user.type(screen.getByLabelText('Buscar cliente'), 'lopez');
    expect(screen.getByText('Ana López')).toBeVisible();
    await waitFor(() => {
      const filas = allBlocks(estado.sistemas.get(usuario.email)?.[0])
        .filter((b) => b.type === 'clients')
        .flatMap((b) => b.rows);
      expect(filas).toHaveLength(1);
      expect(filas[0]?.phone).toBe('1144556677');
    }, ESPERA);
  });
});
