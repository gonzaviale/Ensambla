import type { AvisoEconomico } from '@/types/avisoEconomico';
import type { ResumenInicio } from '@/types/resumenInicio';

/** Una señal del aviso de rentabilidad: lo que cambió en las ventas, en la ganancia o qué productos lo explican. */
export type Diapositiva =
  | { tipo: 'ventas' | 'ganancia'; clave: string; cambio: number }
  | { tipo: 'costos'; clave: string }
  | {
      tipo: 'productos';
      clave: string;
      cambios: AvisoEconomico['cambios'];
      categoria: string;
      titulo: string;
    };

/** «+12 %», «−4 %» o «0 %». */
export const conSigno = (valor: number) =>
  `${valor > 0 ? '+' : valor < 0 ? '−' : ''}${Math.abs(valor)} %`;

/**
 * Parte el aviso económico del negocio en señales cortas para el carrusel del Inicio: cuánto cambiaron las ventas,
 * cuánto la ganancia (o que faltan los costos para calcularla) y los productos que más explican el cambio.
 * La clave incluye el título: si el aviso cambia, lo descartado vuelve a aparecer.
 */
export function diapositivasDelAviso(aviso: AvisoEconomico, negocioId: string): Diapositiva[] {
  const base = `${negocioId}:${aviso.titulo}`;
  return [
    { tipo: 'ventas', clave: `${base}:ventas`, cambio: aviso.variacionVentas },
    aviso.variacionResultadoBruto === null
      ? ({ tipo: 'costos', clave: `${base}:costos` } as const)
      : { tipo: 'ganancia', clave: `${base}:ganancia`, cambio: aviso.variacionResultadoBruto },
    ...(aviso.cambios.length > 0
      ? [
          {
            tipo: 'productos',
            clave: `${base}:productos`,
            cambios: aviso.cambios,
            categoria: aviso.categoria,
            titulo: tituloDeProductos(aviso),
          } as const,
        ]
      : []),
  ];
}

/** Qué son esos productos, dicho simple: los que tienen menos (o más) ganancia, o los que vendiste menos (o más). */
export function tituloDeProductos(aviso: Pick<AvisoEconomico, 'categoria' | 'tendencia'>) {
  const menos = aviso.tendencia === 'EMPEORA';
  return aviso.categoria === 'Ventas'
    ? `Productos que vendiste ${menos ? 'menos' : 'más'}`
    : `Productos con ${menos ? 'menos' : 'más'} ganancia`;
}

const diasConVentas = (resumen: ResumenInicio) =>
  resumen.ventasPorDia.filter((dia) => dia.ingresos > 0).length;

/**
 * El carrusel sólo tiene sentido con algo de verdad para comparar: ventas esta semana y en los 7 días anteriores, en
 * al menos dos días distintos. Con un solo día o una sola venta no se muestra.
 */
export const hayVentasParaComparar = (resumen: ResumenInicio) =>
  resumen.ventas.semana.ingresos > 0 &&
  resumen.ventas.semana.ingresosAnteriores > 0 &&
  diasConVentas(resumen) >= 2;
