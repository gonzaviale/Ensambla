import type { CierreCaja, CobroDeudaCaja, ConsultaCaja, VentaCaja } from '@/types/caja';

export function fixtureCaja() {
  const venta: VentaCaja = {
    id: '00000000-0000-0000-0000-000000000101',
    lineas: [
      {
        productoId: null,
        nombre: 'Servicio',
        modo: 'UNIDAD',
        cantidad: 1,
        precio: 2500,
        importe: 2500,
      },
    ],
    medio: 'TRANSFERENCIA',
    total: 2500,
    recibido: 500,
    vuelto: 0,
    creadoEn: '2026-10-16T12:00:00-03:00',
    clienteId: '00000000-0000-0000-0000-000000000105',
    clienteNombre: 'Ana de prueba',
    pagosIniciales: [{ medio: 'TRANSFERENCIA', importe: 500 }],
    pagos: [{ medio: 'TRANSFERENCIA', importe: 500 }],
    cobrado: 500,
    saldo: 2000,
    estado: 'PARCIAL',
  };
  const cobros: CobroDeudaCaja[] = [];
  const resumen = {
    ventas: [venta],
    cobros,
    total: 2500,
    efectivo: 0,
    transferencia: 500,
    tarjeta: 0,
    desde: venta.creadoEn,
    revision: 'fixture-caja',
  };
  const consulta: ConsultaCaja = {
    fecha: '2026-11-15',
    dia: resumen,
    abierta: resumen,
    cierres: [],
    pagina: 0,
    hayMas: false,
  };
  const cierre: CierreCaja = {
    id: '00000000-0000-0000-0000-000000000110',
    resumen,
    fondoInicial: 0,
    entradas: 0,
    retiros: 0,
    esperado: 0,
    contado: 0,
    diferencia: 0,
    nota: '',
    cerradoEn: '2026-11-15T18:00:00-03:00',
  };
  return { venta, ventas: [venta], cobros, consulta, cierre };
}
