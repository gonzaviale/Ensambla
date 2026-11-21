import { screen, waitFor } from '@testing-library/react';
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

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let clienteId = '';
let solicitudes: SolicitudCobro[] = [];

describe('ventas con saldo pendiente', () => {
  it('no deja fiar sin cliente', async () => {
    await dadoUnCobroPorDiezMilPesos();

    await cuandoSeElijeFiada();

    entoncesNoSePuedeGuardarElFiado();
  });

  it('envía una venta fiada sin cobros', async () => {
    await dadoUnCobroPorDiezMilPesos();
    await cuandoSeElijeFiada();

    await cuandoSeEligeAAnaYSeGuardaElFiado();

    await entoncesSeEnvioUnaVentaFiadaDeAna();
  });

  it('registra la entrega parcial con su saldo y medios mixtos', async () => {
    await dadoUnCobroPorDiezMilPesos();

    await cuandoSePagaAMediasConEfectivoYTransferencia();

    await entoncesSeEnvioLaEntregaDividida();
  });

  it('rechaza un importe parcial que alcanza o supera el total', async () => {
    await dadoUnCobroPorDiezMilPesos();

    await cuandoSePagaAhoraMasQueElTotal();

    entoncesNoSePuedeGuardarLaVentaNiSeEnvioNada();
  });

  it('permite crear al cliente durante el cobro y espera a que se guarde', async () => {
    await dadoUnCobroPorDiezMilPesos();
    await cuandoSeElijeFiada();

    await cuandoSeCreaAPedroDuranteElCobro();

    await entoncesSeEnvioElFiadoDelClienteNuevo();
  });
});

async function dadoUnCobroPorDiezMilPesos() {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  const clientes = allBlocks(sistema).find((b) => b.type === 'clients');
  if (!inventario || !clientes) throw new Error('Falta la estructura de prueba');
  clienteId = `e${crypto.randomUUID()}`;
  clientes.rows = [{ id: clienteId, name: 'Ana López' }];
  inventario.rows = [
    { id: crypto.randomUUID(), name: 'Producto', price: 10000, quantity: 10, favorite: true },
  ];
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  solicitudes = [];
  server.use(http.post('/api/caja/ventas', responderCobro));
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  user = userEvent.setup();
  await user.click(screen.getByRole('tab', { name: 'Favoritos' }));
  await user.click(screen.getByRole('button', { name: /Producto.*10.000/ }));
  await user.click(screen.getByRole('button', { name: /Cobrar.*10.000/ }));
}

async function responderCobro({ request }: { request: Request }) {
  const body = (await request.json()) as SolicitudCobro;
  solicitudes.push(body);
  const cobrado = body.pagos?.reduce((sum, p) => sum + p.importe, 0) ?? 10000;
  return HttpResponse.json({
    id: body.id,
    lineas: [],
    medio: body.medio,
    total: 10000,
    recibido: cobrado,
    vuelto: 0,
    creadoEn: new Date().toISOString(),
    pagos: body.pagos ?? [],
    clienteId: body.clienteId,
    clienteNombre: 'Ana López',
    cobrado,
    saldo: 10000 - cobrado,
    estado: cobrado === 0 ? 'PENDIENTE' : cobrado === 10000 ? 'PAGADA' : 'PARCIAL',
  });
}

async function cuandoSeElijeFiada() {
  await user.click(screen.getByRole('button', { name: 'Fiada' }));
}

async function cuandoSeEligeAAna() {
  await user.click(screen.getByRole('button', { name: 'Elegir quién debe' }));
  await user.click(screen.getByRole('button', { name: 'Ana López' }));
}

async function cuandoSeEligeAAnaYSeGuardaElFiado() {
  await cuandoSeEligeAAna();
  await user.click(screen.getByRole('button', { name: 'Guardar fiado' }));
}

async function cuandoSePagaAMediasConEfectivoYTransferencia() {
  await user.click(screen.getByRole('button', { name: 'A medias' }));
  await user.type(screen.getByLabelText('Paga ahora ($)'), '4000');
  await cuandoSeEligeAAna();
  await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
  await user.type(screen.getByLabelText('Importe en efectivo ($)'), '1500');
  await user.click(screen.getByRole('button', { name: 'Guardar venta y pago' }));
}

async function cuandoSePagaAhoraMasQueElTotal() {
  await user.click(screen.getByRole('button', { name: 'A medias' }));
  await cuandoSeEligeAAna();
  await user.type(screen.getByLabelText('Paga ahora ($)'), '10001');
}

async function cuandoSeCreaAPedroDuranteElCobro() {
  await user.click(screen.getByRole('button', { name: 'Elegir quién debe' }));
  await user.click(screen.getByRole('button', { name: 'Crear cliente' }));
  await user.type(screen.getByLabelText(/^Nombre/), 'Pedro');
  await user.click(screen.getByRole('button', { name: 'Guardar cliente' }));
  await waitFor(
    () => expect(screen.getByRole('button', { name: 'Guardar fiado' })).toBeEnabled(),
    ESPERA,
  );
  await user.click(screen.getByRole('button', { name: 'Guardar fiado' }));
}

function entoncesNoSePuedeGuardarElFiado() {
  expect(screen.getByRole('button', { name: 'Guardar fiado' })).toBeDisabled();
}

async function entoncesSeEnvioUnaVentaFiadaDeAna() {
  await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
  expect(solicitudes).toHaveLength(1);
  expect(solicitudes[0]).toMatchObject({
    clienteId: clienteId.slice(1),
    pagos: [],
    recibido: null,
  });
  expect(screen.getByText(/saldo pendiente quedaron guardados/)).toBeVisible();
}

async function entoncesSeEnvioLaEntregaDividida() {
  await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
  expect(solicitudes[0]?.pagos).toEqual([
    { medio: 'EFECTIVO', importe: 1500 },
    { medio: 'TRANSFERENCIA', importe: 2500 },
  ]);
  expect(screen.getByText(/Pendiente:/)).toHaveTextContent('6.000');
}

function entoncesNoSePuedeGuardarLaVentaNiSeEnvioNada() {
  expect(screen.getByRole('button', { name: 'Guardar venta y pago' })).toBeDisabled();
  expect(solicitudes).toHaveLength(0);
}

async function entoncesSeEnvioElFiadoDelClienteNuevo() {
  await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
  expect(solicitudes[0]?.clienteId).toBeTruthy();
  expect(solicitudes[0]?.pagos).toEqual([]);
}
