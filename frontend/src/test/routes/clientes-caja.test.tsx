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

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let email = '';

describe('clientes compartidos de Caja y Mi Negocio', () => {
  it('crea un cliente y lo muestra', async () => {
    await dadaLaPantallaDeClientes();

    await cuandoSeCreaElCliente('Ana López', '1122334455');

    expect(await screen.findByText('Ana López')).toBeVisible();
  });

  it('persiste al cliente en la tabla compartida', async () => {
    await dadaLaPantallaDeClientes();

    await cuandoSeCreaElCliente('Ana López', '1122334455');

    await entoncesLaTablaCompartidaTieneUnCliente({ name: 'Ana López', phone: '1122334455' });
  });

  it('prepara una tabla de clientes cuando el negocio todavía no tiene una', async () => {
    await dadaLaPantallaDeClientes({ sinTabla: true });

    await cuandoSeCreaElCliente('Pedro');

    await entoncesLaTablaCompartidaTieneUnCliente({ name: 'Pedro' });
  });

  it('valida el nombre', async () => {
    await dadaLaPantallaDeClientes();

    await cuandoSeIntentaGuardarUnClienteSinNombre();

    entoncesSePideCompletarElDato();
  });

  it('protege el formulario al descartar cambios', async () => {
    await dadaLaPantallaDeClientes();
    await cuandoSeEscribeElNombre('Ana');

    await cuandoSeIntentaCancelarYSeSigueEditando();

    entoncesElNombreSigueEscrito('Ana');
  });

  it('edita al cliente sin duplicarlo', async () => {
    await dadaLaPantallaDeClientes();
    await cuandoSeCreaElCliente('Ana López');

    await cuandoSeEditaElTelefonoDe('Ana López', '1144556677');

    await entoncesLaTablaCompartidaTieneUnCliente({ phone: '1144556677' });
  });

  it('permite buscar sin distinguir acentos', async () => {
    await dadaLaPantallaDeClientes();
    await cuandoSeCreaElCliente('Ana López');

    await cuandoSeBuscaCliente('lopez');

    entoncesSeVeAlCliente('Ana López');
  });
});

async function dadaLaPantallaDeClientes({ sinTabla = false } = {}) {
  const usuario = setupUsuarioConSesion();
  email = usuario.email;
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta la sección de stock de prueba');
  prepararCaja(sistema, inventario.id);
  for (const seccion of sistema.sections) {
    if (sinTabla) seccion.blocks = seccion.blocks.filter((b) => b.type !== 'clients');
  }
  estado.sistemas.set(email, [sistema]);
  server.use(
    http.get('/api/caja/ventas', () => HttpResponse.json({ ventas: [], cobros: [], cuentas: [] })),
  );
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Clientes' }));
}

async function cuandoSeCreaElCliente(nombre: string, telefono?: string) {
  await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
  const dialogo = within(screen.getByRole('dialog', { name: 'Crear cliente' }));
  await user.type(dialogo.getByLabelText(/^Nombre/), nombre);
  if (telefono) await user.type(dialogo.getByLabelText(/^Teléfono/), telefono);
  await user.click(dialogo.getByRole('button', { name: 'Guardar cliente' }));
}

async function cuandoSeIntentaGuardarUnClienteSinNombre() {
  await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
  await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
}

async function cuandoSeEscribeElNombre(nombre: string) {
  await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
  await user.type(screen.getByLabelText(/^Nombre/), nombre);
}

async function cuandoSeIntentaCancelarYSeSigueEditando() {
  await user.click(screen.getByRole('button', { name: 'Cancelar' }));
  await user.click(await screen.findByRole('button', { name: 'Seguir editando' }));
}

async function cuandoSeEditaElTelefonoDe(nombre: string, telefono: string) {
  await user.click(screen.getByRole('button', { name: `Editar a ${nombre}` }));
  await user.type(screen.getByLabelText(/^Teléfono/), telefono);
  await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
}

async function cuandoSeBuscaCliente(texto: string) {
  await user.type(screen.getByLabelText('Buscar cliente'), texto);
}

const clientesGuardados = () =>
  allBlocks(estado.sistemas.get(email)?.[0])
    .filter((b) => b.type === 'clients')
    .flatMap((b) => b.rows);

async function entoncesLaTablaCompartidaTieneUnCliente(datos: Record<string, string>) {
  await waitFor(() => {
    expect(clientesGuardados()).toEqual([expect.objectContaining(datos)]);
  }, ESPERA);
}

function entoncesSePideCompletarElDato() {
  expect(screen.getByText('Completá este dato.')).toBeVisible();
}

function entoncesElNombreSigueEscrito(nombre: string) {
  expect(screen.getByLabelText(/^Nombre/)).toHaveValue(nombre);
}

function entoncesSeVeAlCliente(nombre: string) {
  expect(screen.getByText(nombre)).toBeVisible();
}
