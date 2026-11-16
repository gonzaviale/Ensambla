import type { CobroDeudaCaja, CuentaClienteCaja, VentaCaja } from '@/types/caja';

export function cuentasDeCaja(ventas: VentaCaja[], cobros: CobroDeudaCaja[]): CuentaClienteCaja[] {
  const clientes = [
    ...new Set(ventas.flatMap((venta) => (venta.clienteId ? [venta.clienteId] : []))),
  ];
  return clientes.map((clienteId) => cuentaDeCliente(clienteId, ventas, cobros));
}

function cuentaDeCliente(
  clienteId: string,
  todas: VentaCaja[],
  cobros: CobroDeudaCaja[],
): CuentaClienteCaja {
  const ventas = todas.filter((venta) => venta.clienteId === clienteId);
  const pendientes = ventas.filter((venta) => (venta.saldo ?? 0) > 0);
  const fechas = pendientes.map((venta) => venta.creadoEn).sort();
  return {
    clienteId,
    clienteNombre: ventas.at(-1)?.clienteNombre ?? 'Cliente',
    saldo: pendientes.reduce((suma, venta) => suma + (venta.saldo ?? 0), 0),
    cantidadPendientes: pendientes.length,
    pendienteDesde: fechas[0] ?? null,
    ventas,
    cobros: cobros.filter((cobro) => cobro.clienteId === clienteId),
  };
}
