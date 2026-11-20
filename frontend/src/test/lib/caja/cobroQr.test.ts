import { describe, expect, it } from 'vitest';
import { centavosPorTransferencia, horaDelVencimiento } from '@/lib/caja/cobroQr';
import type { PagoCaja } from '@/types/caja';

let pagos: PagoCaja[];
let centavos: number;
let hora: string;

describe('cobro con QR de la caja', () => {
  it('suma sólo lo que se paga por transferencia', () => {
    dadosUnPagoEnEfectivoYOtroPorTransferencia();

    cuandoSeCalculaLoQueSeTransfiere();

    entoncesSeTransfieren(250000);
  });

  it('sin transferencias no hay nada que transferir', () => {
    dadoUnPagoSoloEnEfectivo();

    cuandoSeCalculaLoQueSeTransfiere();

    entoncesSeTransfieren(0);
  });

  it('una venta fiada, sin pagos, no tiene nada que transferir', () => {
    dadosCeroPagos();

    cuandoSeCalculaLoQueSeTransfiere();

    entoncesSeTransfieren(0);
  });

  it('redondea los centavos sin errores de coma flotante', () => {
    dadoUnPagoPorTransferenciaDeDiezConCincuentaYOtroDeVeinteCentavos();

    cuandoSeCalculaLoQueSeTransfiere();

    entoncesSeTransfieren(1070);
  });

  it('muestra la hora de vencimiento en hora argentina', () => {
    cuandoSePideLaHoraDelVencimiento('2026-11-19T18:45:00Z');

    expect(hora).toBe('15:45');
  });
});

function dadosUnPagoEnEfectivoYOtroPorTransferencia() {
  pagos = [
    { medio: 'EFECTIVO', importe: 1000 },
    { medio: 'TRANSFERENCIA', importe: 2500 },
  ];
}

function dadoUnPagoSoloEnEfectivo() {
  pagos = [{ medio: 'EFECTIVO', importe: 1000 }];
}

function dadosCeroPagos() {
  pagos = [];
}

function dadoUnPagoPorTransferenciaDeDiezConCincuentaYOtroDeVeinteCentavos() {
  pagos = [
    { medio: 'TRANSFERENCIA', importe: 10.5 },
    { medio: 'TRANSFERENCIA', importe: 0.2 },
  ];
}

function cuandoSeCalculaLoQueSeTransfiere() {
  centavos = centavosPorTransferencia(pagos);
}

function cuandoSePideLaHoraDelVencimiento(venceEn: string) {
  hora = horaDelVencimiento(venceEn);
}

function entoncesSeTransfieren(esperado: number) {
  expect(centavos).toBe(esperado);
}
