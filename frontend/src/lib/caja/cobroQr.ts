import type { PagoCaja } from '@/types/caja';

const HORA_ARGENTINA = new Intl.DateTimeFormat('es-AR', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: 'America/Argentina/Buenos_Aires',
});

const centavosDe = (pago: PagoCaja) => Math.round(pago.importe * 100);

/** Lo que el cliente tiene que transferir: la suma de los pagos por transferencia, en centavos. */
export const centavosPorTransferencia = (pagos: PagoCaja[]): number =>
  pagos
    .filter((pago) => pago.medio === 'TRANSFERENCIA')
    .reduce((suma, pago) => suma + centavosDe(pago), 0);

export const horaDelVencimiento = (venceEn: string): string =>
  HORA_ARGENTINA.format(new Date(venceEn));
