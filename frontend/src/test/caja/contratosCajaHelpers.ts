import { expect } from 'vitest';
import { fixtureCaja } from '@/mocks/fixtures/caja';
import { crearHandlersCaja } from '@/mocks/handlers/caja';
import { server } from '@/mocks/node';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import { ApiError } from '@/api/apiError';
import {
  listarCuentasCaja,
  cobrarVenta,
  cerrarCaja,
  consultarCaja,
  obtenerCierreCaja,
} from '@/api/caja';

const proyecto = '00000000-0000-0000-0000-000000000102';
const inventario = '00000000-0000-0000-0000-000000000103';

export function givenContratosCajaSimulados(rechazar = false) {
  setupUsuarioConSesion();
  const fixture = fixtureCaja();
  server.use(
    ...crearHandlersCaja({
      ventas: () => fixture.ventas,
      cobros: () => fixture.cobros,
      cobrar: () => fixture.venta,
      consultar: () => fixture.consulta,
      obtenerCierre: () => fixture.cierre,
      cerrar: () => {
        if (rechazar)
          throw new ApiError(400, 'Explicá la diferencia entre el efectivo esperado y el contado');
        return fixture.cierre;
      },
    }),
  );
}
export function whenSeConsultaCuentaSimulada() {
  return listarCuentasCaja(proyecto, inventario);
}
export function whenSeCobraImporteLibreSimulado() {
  return cobrarVenta({
    id: crypto.randomUUID(),
    proyectoId: proyecto,
    inventarioId: inventario,
    ventasId: 'ventas',
    medio: 'TRANSFERENCIA',
    recibido: null,
    clienteId: 'cliente',
    pagos: [{ medio: 'TRANSFERENCIA', importe: 500 }],
    lineas: [
      { productoId: null, descripcion: 'Servicio', cantidad: 1, precio: 2500, modo: 'UNIDAD' },
    ],
  });
}
export function whenSeCierraCajaSimulada() {
  return cerrarCaja({
    id: crypto.randomUUID(),
    proyectoId: proyecto,
    inventarioId: inventario,
    revision: 'vigente',
    fondoInicial: 0,
    entradas: 0,
    retiros: 0,
    contado: 0,
    nota: '',
  });
}
export async function whenSeConsultaCierreSimulado() {
  const consulta = await consultarCaja(proyecto, inventario, '2026-11-15', 0);
  const cierre = await obtenerCierreCaja('cierre');
  return { consulta, cierre };
}
export function thenSeRecibenSaldosAgrupados(
  cuentas: Awaited<ReturnType<typeof listarCuentasCaja>>,
) {
  expect(cuentas.cuentas[0]).toMatchObject({
    saldo: 2000,
    cantidadPendientes: 1,
    pendienteDesde: '2026-10-16T12:00:00-03:00',
  });
}
export function thenSeRecibeImporteLibre(venta: Awaited<ReturnType<typeof cobrarVenta>>) {
  expect(venta.lineas[0]).toMatchObject({ productoId: null, nombre: 'Servicio' });
  expect(venta.saldo).toBe(2000);
}
export function thenSeRecibeCierreConCero(
  datos: Awaited<ReturnType<typeof whenSeConsultaCierreSimulado>>,
) {
  expect(datos.consulta.abierta.transferencia).toBe(500);
  expect(datos.cierre.contado).toBe(0);
  expect(datos.cierre.diferencia).toBe(0);
}
export async function thenSeExplicaRechazoDeCierre() {
  await expect(whenSeCierraCajaSimulada()).rejects.toMatchObject({
    status: 400,
    message: 'Explicá la diferencia entre el efectivo esperado y el contado',
  });
}
