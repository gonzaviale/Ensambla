import { useMemo } from 'react';
import { queryOptions, useQueries } from '@tanstack/react-query';
import { obtenerReceta } from '@/api/receta';
import { faltan, RESPUESTAS } from '@/lib/respuestas/definiciones';
import {
  periodosDelMes,
  respuestaCostoPlato,
  respuestaDeuda,
  respuestaMargen,
  respuestaPedidos,
  respuestaPerdida,
  respuestaStock,
  respuestaTeQuedo,
  respuestaTotal,
  type FilaProducto,
} from '@/lib/respuestas/calculos';
import type { RespuestaId, RespuestaVista, ResultadoRespuesta } from '@/types/negocio';
import type { Reporte } from '@/types/reporte';
import type { Registro } from '@/types/registro';
import { registrosQuery } from '../registros/queries';
import { reporteQuery } from '../reportes/queries';
import { usePlanActual } from './usePlan';
import { useMiNegocio } from './useMiNegocio';

// Respuestas de «Mi negocio»: se calculan con lo que ya tiene el motor (reportes con totales y filas).
// Una respuesta sin las piezas necesarias queda «bloqueada» (con qué sumar), nunca falla.

const FILAS = 100;
const vacio: Reporte = {
  filas: [],
  totales: { cantidad: 0, sumas: {} },
  truncado: false,
  maximoFilas: 0,
};

const numero = (registro: Registro, campoId: string | undefined) => {
  const valor = campoId ? registro.valores[campoId] : null;
  return typeof valor === 'number' ? valor : null;
};
const texto = (registro: Registro, campoId: string | undefined) => {
  const valor = campoId ? registro.valores[campoId] : null;
  return typeof valor === 'string' ? valor : '';
};

export function useRespuestas() {
  const negocio = useMiNegocio();
  const plan = usePlanActual();
  const { presentes } = negocio;
  const periodos = useMemo(() => periodosDelMes(new Date()), []);

  const ventas = presentes.get('ventas');
  const gastos = presentes.get('gastos');
  const productos = presentes.get('productos');
  const fiados = presentes.get('fiados');
  const pedidos = presentes.get('pedidos');
  const insumos = presentes.get('insumos');
  const pagado = fiados?.campo('pagado');
  const entregado = pedidos?.campo('entregado');

  // Cada consulta lleva una clave para encontrar su resultado sin depender del orden.
  const consultas = useMemo(() => {
    const lista: { clave: string; opciones: ReturnType<typeof queryOptions> }[] = [];
    const reporte = (clave: string, bloqueId: string, filtro: Parameters<typeof reporteQuery>[1]) =>
      lista.push({ clave, opciones: reporteQuery(bloqueId, filtro) as never });
    if (ventas) {
      reporte('ventas', ventas.bloque.id, periodos.actual);
      reporte('ventasAnt', ventas.bloque.id, periodos.anterior);
    }
    if (gastos) {
      reporte('gastos', gastos.bloque.id, periodos.actual);
      reporte('gastosAnt', gastos.bloque.id, periodos.anterior);
    }
    if (productos) {
      lista.push({
        clave: 'productos',
        opciones: registrosQuery(productos.bloque.id, { tamanio: FILAS }) as never,
      });
    }
    if (fiados && pagado) {
      reporte('fiados', fiados.bloque.id, { campoId: pagado.id, valor: 'false' });
    }
    if (pedidos && entregado) {
      reporte('pedidos', pedidos.bloque.id, { campoId: entregado.id, valor: 'false' });
    }
    return lista;
  }, [ventas, gastos, productos, fiados, pagado, pedidos, entregado, periodos]);

  const resultados = useQueries({
    queries: consultas.map((consulta) => consulta.opciones),
    combine: (res) => ({
      datos: res.map((r) => r.data as unknown),
      cargando: res.some((r) => r.isPending),
    }),
  });
  const dato = <T>(clave: string): T | undefined => {
    const indice = consultas.findIndex((consulta) => consulta.clave === clave);
    return indice >= 0 ? (resultados.datos[indice] as T | undefined) : undefined;
  };

  const paginaProductos = dato<{ content: Registro[] }>('productos');
  const filasProducto: FilaProducto[] = useMemo(
    () =>
      (paginaProductos?.content ?? []).map((registro) => ({
        id: registro.id,
        nombre: texto(registro, productos?.campo('nombre')?.id) || 'Sin nombre',
        costo: numero(registro, productos?.campo('costo')?.id),
        precio: numero(registro, productos?.campo('precio')?.id),
        stock: numero(registro, productos?.campo('stock')?.id),
      })),
    [paginaProductos, productos],
  );

  // Costo por producto según su receta (hasta 8 productos): sólo si hay Insumos.
  const idsReceta = insumos && productos ? filasProducto.slice(0, 8) : [];
  const recetas = useQueries({
    queries: idsReceta.map((fila) =>
      queryOptions({
        queryKey: ['receta', fila.id],
        queryFn: ({ signal }) => obtenerReceta(fila.id, signal),
      }),
    ),
    combine: (res) => res.map((r) => r.data),
  });

  const vistas: RespuestaVista[] = RESPUESTAS.map((definicion) => {
    const faltantes = faltan(definicion.requiere, presentes.keys());
    let resultado: ResultadoRespuesta;
    if (faltantes.length > 0) {
      resultado = { estado: 'bloqueada', faltan: faltantes };
    } else if (!definicion.basica && !plan.incluye('respuestas-completas')) {
      resultado = { estado: 'plan' };
    } else {
      const monto = (clave: string, campoId: string | undefined) => {
        const reporte = dato<Reporte>(clave) ?? vacio;
        return {
          total: campoId ? (reporte.totales.sumas[campoId] ?? 0) : 0,
          cantidad: reporte.totales.cantidad,
        };
      };
      const montoVentas = ventas?.campo('monto')?.id;
      const montoGastos = gastos?.campo('monto')?.id;
      const porId: Record<RespuestaId, () => ResultadoRespuesta> = {
        'te-quedo': () =>
          respuestaTeQuedo(
            monto('ventas', montoVentas),
            monto('gastos', montoGastos),
            monto('ventasAnt', montoVentas),
            monto('gastosAnt', montoGastos),
          ),
        vendiste: () =>
          respuestaTotal(monto('ventas', montoVentas), monto('ventasAnt', montoVentas), {
            esGasto: false,
          }),
        gastaste: () =>
          respuestaTotal(monto('gastos', montoGastos), monto('gastosAnt', montoGastos), {
            esGasto: true,
          }),
        'margen-producto': () => respuestaMargen(filasProducto),
        'producto-perdida': () => respuestaPerdida(filasProducto),
        'que-se-termina': () => respuestaStock(filasProducto),
        'quien-te-debe': () => {
          const reporte = dato<Reporte>('fiados') ?? vacio;
          return respuestaDeuda(
            reporte.filas.map((fila) => ({
              cliente: texto(fila, fiados?.campo('cliente')?.id) || 'Sin nombre',
              monto: numero(fila, fiados?.campo('monto')?.id) ?? 0,
            })),
            reporte.totales.cantidad,
          );
        },
        'pedidos-pendientes': () => {
          const reporte = dato<Reporte>('pedidos') ?? vacio;
          return respuestaPedidos(
            reporte.filas.map((fila) => ({
              cliente: texto(fila, pedidos?.campo('cliente')?.id) || 'Sin nombre',
              monto: numero(fila, pedidos?.campo('monto')?.id) ?? 0,
              sena: numero(fila, pedidos?.campo('sena')?.id) ?? 0,
            })),
          );
        },
        'costo-plato': () =>
          respuestaCostoPlato(
            idsReceta.flatMap((fila, i) => {
              const receta = recetas[i];
              return receta && receta.lineas.length > 0
                ? [{ nombre: fila.nombre, costo: receta.costo, precio: fila.precio }]
                : [];
            }),
          ),
      };
      resultado = porId[definicion.id]();
    }
    return { definicion, resultado };
  });

  return {
    /** Falta la lista de sistemas o algún dato para calcular. */
    cargando: negocio.cargando || resultados.cargando,
    hayNegocio: negocio.hayNegocio,
    sistemaId: negocio.sistemaId,
    piezasPresentes: negocio.piezasPresentes,
    vistas,
  };
}
