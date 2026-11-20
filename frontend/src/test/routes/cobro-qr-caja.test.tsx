import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { prepararCaja } from '@/lib/caja';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import {
  cuandoSeCancelaElCobroConQr,
  cuandoSeCobraConQr,
  cuandoSeCopiaElAlias,
  cuandoYaLeTransfirieron,
  dadoUnNegocioQueCobraEnSuAlias,
  dadoUnServidorQueGeneraYCancelaCobrosQr,
  dadoUnServidorQueNoPuedeGenerarCobrosQr,
  elIdDelCobroQrGenerado,
  entoncesElQrMuestraElError,
  entoncesNoSeCancelaNingunCobroQr,
  entoncesNoSeOfreceCobrarConQr,
  entoncesSeAvisaQueEnsamblaNoVerificoElPago,
  entoncesSeAvisaQueNoSePudoGenerarElQr,
  entoncesSeCanceloElCobroQrGenerado,
  entoncesSeCopioElAlias,
  entoncesSeGeneroUnSoloCobroQr,
  entoncesSeIndicaHastaCuandoVale,
  entoncesSeMuestraElQrPor,
  entoncesSeMuestranElAliasYElTitular,
  entoncesSeOfreceCobrarConQr,
  entoncesSePidioElQrPorImporte,
  entoncesSePideCargarElAlias,
} from '@/test/caja/cobroQrHelpers';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';
import type { SolicitudCobro } from '@/types/caja';

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let solicitudes: SolicitudCobro[] = [];

describe('cobrar una venta con QR en la caja', () => {
  it('con efectivo no ofrece cobrar con QR', async () => {
    await dadoUnCobroPorDiezMilPesosEnLaCaja();

    entoncesNoSeOfreceCobrarConQr();
  });

  it('al elegir transferencia ofrece cobrar con QR', async () => {
    await dadoUnCobroPorDiezMilPesosEnLaCaja();

    await cuandoSeElijeTransferencia();

    entoncesSeOfreceCobrarConQr();
  });

  it('muestra el QR grande con el monto a transferir', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestraElQrPor(/10\.000,00/);
  });

  it('muestra el alias y el titular para que el cliente los vea', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestranElAliasYElTitular();
  });

  it('avisa que Ensambla no verificó el pago', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeAvisaQueEnsamblaNoVerificoElPago();
  });

  it('indica hasta qué hora vale el QR', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeIndicaHastaCuandoVale();
  });

  it('el alias se puede copiar', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    await cuandoSeCobraConQr(user);
    await entoncesSeMuestranElAliasYElTitular();

    await cuandoSeCopiaElAlias(user);

    await entoncesSeCopioElAlias();
  });

  it('pide el QR por todo el total cuando la venta se paga por transferencia', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestraElQrPor(/10\.000,00/);
    entoncesSePidioElQrPorImporte(10000);
  });

  it('en una venta a medias el QR es sólo por lo que paga ahora', async () => {
    await dadoUnCobroPorDiezMilPesosEnLaCaja();
    await cuandoSePagaAMediasCuatroMilPorTransferencia();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestraElQrPor(/4\.000,00/);
    entoncesSePidioElQrPorImporte(4000);
  });

  it('en un pago dividido el QR es sólo por la parte en transferencia', async () => {
    await dadoUnCobroPorDiezMilPesosEnLaCaja();
    await cuandoSePagaSeisMilEnEfectivoYElRestoPorTransferencia();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestraElQrPor(/4\.000,00/);
    entoncesSePidioElQrPorImporte(4000);
  });

  it('Ya me transfirieron guarda la venta con el cobro con QR', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    await cuandoSeCobraConQr(user);
    await entoncesSeMuestraElQrPor(/10\.000,00/);

    await cuandoYaLeTransfirieron(user);

    await entoncesSeGuardoLaVentaConElCobroQrGenerado();
  });

  it('cancelar el cobro con QR avisa al servidor y no guarda la venta', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    await cuandoSeCobraConQr(user);
    await entoncesSeMuestraElQrPor(/10\.000,00/);

    await cuandoSeCancelaElCobroConQr(user);

    await entoncesSeCanceloElCobroQrGenerado();
    entoncesNoSeGuardoNingunaVenta();
  });

  it('después de cancelar se puede volver a cobrar con QR', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    await cuandoSeCobraConQr(user);
    await entoncesSeMuestraElQrPor(/10\.000,00/);
    await cuandoSeCancelaElCobroConQr(user);

    await entoncesSeCanceloElCobroQrGenerado();

    entoncesSeOfreceCobrarConQr();
  });

  it('abrir el QR genera un solo cobro aunque la pantalla se vuelva a dibujar', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeMuestraElQrPor(/10\.000,00/);
    entoncesSeGeneroUnSoloCobroQr();
  });

  it('sin alias cargado explica cómo cargarlo y no genera ningún cobro', async () => {
    await dadaUnaVentaPorTransferenciaSinAliasCargado();

    await cuandoSeCobraConQr(user);

    await entoncesSePideCargarElAlias();
    entoncesNoSeCancelaNingunCobroQr();
  });

  it('si no se puede generar el QR avisa y deja volver', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    dadoUnServidorQueNoPuedeGenerarCobrosQr();

    await cuandoSeCobraConQr(user);

    await entoncesSeAvisaQueNoSePudoGenerarElQr();
  });

  it('si la venta se rechaza muestra el motivo en la pantalla del QR', async () => {
    await dadaUnaVentaPorTransferenciaListaParaCobrarConQr();
    await cuandoSeCobraConQr(user);
    await entoncesSeMuestraElQrPor(/10\.000,00/);
    setupServerError('post', '/caja/ventas', 400, 'El cobro con QR ya está cobrado');

    await cuandoYaLeTransfirieron(user);

    await entoncesElQrMuestraElError('El cobro con QR ya está cobrado');
  });
});

async function abrirElCobroPorDiezMilPesos(conAlias: boolean) {
  const usuario = setupUsuarioConSesion();
  if (conAlias) dadoUnNegocioQueCobraEnSuAlias(usuario.email);
  dadoUnServidorQueGeneraYCancelaCobrosQr();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  const clientes = allBlocks(sistema).find((b) => b.type === 'clients');
  if (!inventario || !clientes) throw new Error('Falta la estructura de prueba');
  clientes.rows = [{ id: `e${crypto.randomUUID()}`, name: 'Ana López' }];
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
    cobrado,
    saldo: 10000 - cobrado,
    estado: cobrado === 10000 ? 'PAGADA' : 'PARCIAL',
  });
}

// --- dado ---

const dadoUnCobroPorDiezMilPesosEnLaCaja = () => abrirElCobroPorDiezMilPesos(true);

async function dadaUnaVentaPorTransferenciaListaParaCobrarConQr() {
  await abrirElCobroPorDiezMilPesos(true);
  await cuandoSeElijeTransferencia();
}

async function dadaUnaVentaPorTransferenciaSinAliasCargado() {
  await abrirElCobroPorDiezMilPesos(false);
  await cuandoSeElijeTransferencia();
}

// --- cuando ---

const cuandoSeElijeTransferencia = () =>
  user.click(screen.getByRole('button', { name: 'Transferencia' }));

async function cuandoSeEligeAAna() {
  await user.click(screen.getByRole('button', { name: 'Elegir quién debe' }));
  await user.click(screen.getByRole('button', { name: 'Ana López' }));
}

async function cuandoSePagaAMediasCuatroMilPorTransferencia() {
  await user.click(screen.getByRole('button', { name: 'A medias' }));
  await user.type(screen.getByLabelText('Paga ahora ($)'), '4000');
  await cuandoSeEligeAAna();
  await cuandoSeElijeTransferencia();
}

async function cuandoSePagaSeisMilEnEfectivoYElRestoPorTransferencia() {
  await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
  await user.type(screen.getByLabelText('Importe en efectivo ($)'), '6000');
}

// --- entonces ---

async function entoncesSeGuardoLaVentaConElCobroQrGenerado() {
  await waitFor(() => expect(solicitudes).toHaveLength(1), ESPERA);
  expect(solicitudes[0]?.cobroQrId).toBe(elIdDelCobroQrGenerado());
  expect(solicitudes[0]?.medio).toBe('TRANSFERENCIA');
}

function entoncesNoSeGuardoNingunaVenta() {
  expect(solicitudes).toHaveLength(0);
}
