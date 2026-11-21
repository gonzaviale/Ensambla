import type { PagoCaja, SolicitudPagoDeudaCaja, VentaCaja } from '@/types/caja';

export const clavePagoDeudaCaja = (
  usuarioId: string | null,
  sistemaId: string,
  inventarioId: string,
  clienteId: string,
) => `ensambla:cobro-deuda:${usuarioId}:${sistemaId}:${inventarioId}:${clienteId}`;

export function hayPagoDeudaPendiente(
  usuarioId: string | null,
  sistemaId: string,
  inventarioId: string,
  clienteId: string,
) {
  return (
    sessionStorage.getItem(clavePagoDeudaCaja(usuarioId, sistemaId, inventarioId, clienteId)) !==
    null
  );
}

export function repartirCobroDeuda(
  ventas: VentaCaja[],
  pagos: PagoCaja[],
): SolicitudPagoDeudaCaja['aplicaciones'] {
  const pendientes = ventas
    .filter((v) => (v.saldo ?? 0) > 0)
    .sort((a, b) => Date.parse(a.creadoEn) - Date.parse(b.creadoEn) || a.id.localeCompare(b.id));
  const medios = pagos.map((p) => ({ medio: p.medio, centavos: Math.round(p.importe * 100) }));
  const total = medios.reduce((suma, p) => suma + p.centavos, 0);
  const saldo = pendientes.reduce((suma, v) => suma + Math.round((v.saldo ?? 0) * 100), 0);
  if (
    !medios.length ||
    medios.some((p) => !Number.isSafeInteger(p.centavos) || p.centavos <= 0) ||
    total > saldo
  )
    throw new Error('El pago debe ser mayor a cero y no superar la deuda.');
  const aplicaciones: SolicitudPagoDeudaCaja['aplicaciones'] = [];
  let medio = 0;
  for (const venta of pendientes) {
    let disponible = Math.round((venta.saldo ?? 0) * 100);
    const partes: PagoCaja[] = [];
    while (disponible > 0 && medio < medios.length) {
      const actual = medios[medio];
      if (!actual) break;
      const importe = Math.min(disponible, actual.centavos);
      partes.push({ medio: actual.medio, importe: importe / 100 });
      disponible -= importe;
      actual.centavos -= importe;
      if (actual.centavos === 0) medio++;
    }
    if (partes.length) aplicaciones.push({ ventaId: venta.id, pagos: partes });
  }
  return aplicaciones;
}
