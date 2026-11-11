import { formatoPesos, ultimoValor, type Objetivo } from './objetivos';
import { posicionDelPrecio } from './sistema/zona';
import type { PrecioZonaObjetivo, ProductoParaPaso } from '@/types/objetivos';

// Los pasos concretos de un objetivo: cosas para hacer con los productos del negocio, dichas como las diría un
// vecino («subile $200 a la yerba»), no como un porcentaje. Salen de lo que la persona tiene cargado.
// Los pasos de precio se pueden ajustar: cada monto tiene su proyección, y subir demasiado hace vender menos.

/** Qué tanto dejan de comprar los clientes cuando sube el precio. */
export type Sensibilidad = 'poca' | 'media' | 'mucha';

export const SENSIBILIDADES: { clave: Sensibilidad; etiqueta: string; elasticidad: number }[] = [
  { clave: 'poca', etiqueta: 'Casi no lo notan', elasticidad: 1 },
  { clave: 'media', etiqueta: 'Algunos compran menos', elasticidad: 2 },
  { clave: 'mucha', etiqueta: 'Compran bastante menos', elasticidad: 3 },
];

const elasticidadDe = (sensibilidad: Sensibilidad) =>
  SENSIBILIDADES.find((item) => item.clave === sensibilidad)?.elasticidad ?? 2;

/** Cuánto se puede mover un paso y dónde conviene empezar. */
export type Ajuste = {
  /** «Subile» o «Bajale»: cómo se nombra el monto. */
  verbo: 'Subile' | 'Bajale';
  desde: number;
  hasta: number;
  paso: number;
  sugerido: (sensibilidad: Sensibilidad) => number;
};

/** Lo que dice un paso con un monto elegido. */
export type Resultado = {
  titulo: string;
  detalle: string;
  efecto: string;
  /** Cuánto suma (o resta) por mes; sin valor cuando el paso libera plata parada en vez de sumar ganancia. */
  sumaPorMes?: number;
  /** Plata que hoy está guardada en mercadería y el paso ayuda a recuperar. */
  liberaPlata?: number;
  /** El precio al que quedaría, para compararlo con la zona. */
  precioNuevo: number;
};

export type Paso = {
  clave: string;
  producto: string;
  precioZona: PrecioZonaObjetivo | null;
  /** Sin ajuste, el paso es fijo. */
  ajuste?: Ajuste;
  calcular: (valor: number, sensibilidad: Sensibilidad) => Resultado;
  /** Cuánto ganarías por mes con cada monto, para dibujar la curva; sólo en los pasos que cambian un precio. */
  curva?: (valor: number, sensibilidad: Sensibilidad) => number;
};

type Producto = Omit<ProductoParaPaso, 'tipo' | 'vendidas30Dias'> & { vendidas: number };

const redondearA = (valor: number, paso: number) => Math.max(paso, Math.round(valor / paso) * paso);
const alPaso = (valor: number, paso: number, desde: number, hasta: number) =>
  Math.min(hasta, Math.max(desde, Math.round(valor / paso) * paso));
const pasoDePrecio = (precio: number) => (precio >= 1500 ? 50 : 10);

const gananciaPorUnidad = (producto: Producto) => producto.precio - producto.costo;

/**
 * Cuántas unidades se venderían al subir el precio. La demanda baja en línea recta: cuanto más se sube, más gente
 * deja de comprar. Es una estimación de demostración, no una promesa.
 */
export function ventasConAumento(
  producto: Pick<Producto, 'precio' | 'vendidas'>,
  aumento: number,
  sensibilidad: Sensibilidad,
) {
  const caida = (elasticidadDe(sensibilidad) * aumento) / producto.precio;
  return Math.max(0, producto.vendidas * (1 - caida));
}

/** Cuánto más (o menos) se gana por mes con este aumento, ya contando lo que se deja de vender. */
export function gananciaExtraPorAumento(
  producto: Producto,
  aumento: number,
  sensibilidad: Sensibilidad,
) {
  const conAumento =
    (gananciaPorUnidad(producto) + aumento) * ventasConAumento(producto, aumento, sensibilidad);
  return conAumento - gananciaPorUnidad(producto) * producto.vendidas;
}

function pasoDeSubirPrecio(producto: Producto): Paso {
  const paso = pasoDePrecio(producto.precio);
  const hasta = redondearA(producto.precio * 0.25, paso);
  const ganancia = (aumento: number, sensibilidad: Sensibilidad) =>
    gananciaExtraPorAumento(producto, aumento, sensibilidad);
  // El aumento que más deja con lo que se espera que dejen de comprar; si ninguno deja, el más chico.
  const sugerido = (sensibilidad: Sensibilidad) => {
    let mejor = paso;
    for (let aumento = paso; aumento <= hasta; aumento += paso) {
      if (ganancia(aumento, sensibilidad) > ganancia(mejor, sensibilidad)) mejor = aumento;
    }
    return mejor;
  };

  return {
    clave: 'subir-precio',
    producto: producto.nombre,
    precioZona: producto.precioZona,
    ajuste: { verbo: 'Subile', desde: paso, hasta, paso, sugerido },
    curva: ganancia,
    calcular: (aumento, sensibilidad) => {
      const ventas = Math.round(ventasConAumento(producto, aumento, sensibilidad));
      // Se redondea a la decena: es una estimación, no hace falta mostrar hasta el último peso.
      const suma = Math.round(ganancia(aumento, sensibilidad) / 10) * 10;
      const menos = producto.vendidas - ventas;
      return {
        titulo: `Subile ${formatoPesos(aumento)} a ${producto.nombre}`,
        detalle: `Hoy lo vendés a ${formatoPesos(producto.precio)} y te deja ${formatoPesos(gananciaPorUnidad(producto))} por unidad. En el último mes salieron ${producto.vendidas}. A ${formatoPesos(producto.precio + aumento)}, Sami calcula que saldrían unas ${ventas}${menos > 0 ? ` (${menos} menos)` : ''}, pero cada una te deja ${formatoPesos(gananciaPorUnidad(producto) + aumento)}. Si ves que se vende menos, volvé al precio de antes.`,
        efecto:
          suma > 0
            ? `Sumás unos ${formatoPesos(suma)} por mes`
            : suma === 0
              ? 'No ganás ni más ni menos: lo que ganás por unidad lo perdés en ventas'
              : `Ganarías unos ${formatoPesos(-suma)} menos por mes: a ese precio se vende demasiado poco`,
        sumaPorMes: suma,
        precioNuevo: producto.precio + aumento,
      };
    },
  };
}

function pasoDeMoverParado(producto: Producto): Paso {
  const paso = pasoDePrecio(producto.precio);
  const margenTotal = producto.precio - producto.costo;
  const hasta = Math.max(paso, Math.floor((margenTotal - paso) / paso) * paso);
  // Si la zona cobra menos, se baja hasta ese promedio (mientras siga dejando ganancia); si no, un 10 %.
  const promedio = producto.precioZona?.promedio;
  const alPromedio =
    promedio !== undefined && promedio > producto.costo && promedio < producto.precio
      ? producto.precio - promedio
      : producto.precio * 0.1;
  const guardada = producto.costo * producto.stock;

  return {
    clave: 'mover-parado',
    producto: producto.nombre,
    precioZona: producto.precioZona,
    ajuste: {
      verbo: 'Bajale',
      desde: paso,
      hasta,
      paso,
      sugerido: () => alPaso(alPromedio, paso, paso, hasta),
    },
    calcular: (rebaja) => {
      const nuevo = producto.precio - rebaja;
      const gananciaTotal = Math.round((nuevo - producto.costo) * producto.stock);
      return {
        titulo: `Bajale ${formatoPesos(rebaja)} a ${producto.nombre} para que salga`,
        detalle: `No vendiste nada en el último mes y tenés ${producto.stock} guardadas. A ${formatoPesos(nuevo)} ganás ${formatoPesos(nuevo - producto.costo)} por unidad: si salen todas, te quedan ${formatoPesos(gananciaTotal)}. Cuanto más lo bajás, más rápido sale, pero menos ganás en cada una.`,
        efecto: `Liberás hasta ${formatoPesos(guardada)} que hoy están guardados`,
        liberaPlata: guardada,
        precioNuevo: nuevo,
      };
    },
  };
}

function pasoDePonerALaVista(producto: Producto): Paso {
  const suma = gananciaPorUnidad(producto) * 5;
  return {
    clave: 'poner-a-la-vista',
    producto: producto.nombre,
    precioZona: producto.precioZona,
    calcular: () => ({
      titulo: `Poné ${producto.nombre} a la vista`,
      detalle: `Es el que más te deja por cada venta: ${formatoPesos(gananciaPorUnidad(producto))}. Ofrecelo cerca de la caja o junto a lo que se lleva la gente. Con 5 ventas más por mes ya se nota.`,
      efecto: `Sumás unos ${formatoPesos(suma)} por mes`,
      sumaPorMes: suma,
      precioNuevo: producto.precio,
    }),
  };
}

/** Convierte los productos ya elegidos por el servidor en los pasos interactivos del frontend. */
export function pasosConcretos(productos: ProductoParaPaso[]): Paso[] {
  return productos.map(({ vendidas30Dias, ...producto }) => {
    const datos = { ...producto, vendidas: vendidas30Dias };
    if (producto.tipo === 'SUBIR_PRECIO') return pasoDeSubirPrecio(datos);
    if (producto.tipo === 'MOVER_PARADO') return pasoDeMoverParado(datos);
    return pasoDePonerALaVista(datos);
  });
}

/** Los montos elegidos a mano, por paso; los que faltan usan el sugerido. */
export type Montos = Readonly<Record<string, number>>;

export const montoDe = (paso: Paso, montos: Montos, sensibilidad: Sensibilidad) =>
  montos[paso.clave] ?? paso.ajuste?.sugerido(sensibilidad) ?? 0;

export type Escenario = {
  /** Lo que se gana hoy por mes. */
  hoy: number;
  /** Lo que se ganaría haciendo los pasos elegidos. */
  conPasos: number;
  /** Lo que todavía faltaría para la meta (0 si se llega). */
  faltaria: number;
  /** Plata guardada que se recuperaría con los pasos elegidos. */
  liberada: number;
};

/** «Probá un escenario»: qué pasaría con el objetivo si se hacen sólo los pasos elegidos, con los montos elegidos. */
export function escenarioDe(
  objetivo: Objetivo,
  pasos: Paso[],
  elegidos: ReadonlySet<string>,
  montos: Montos = {},
  sensibilidad: Sensibilidad = 'media',
): Escenario {
  const hoy = ultimoValor(objetivo);
  const resultados = pasos
    .filter((paso) => elegidos.has(paso.clave))
    .map((paso) => paso.calcular(montoDe(paso, montos, sensibilidad), sensibilidad));
  const suma = resultados.reduce((total, resultado) => total + (resultado.sumaPorMes ?? 0), 0);
  return {
    hoy,
    conPasos: hoy + suma,
    faltaria: Math.max(0, objetivo.meta - (hoy + suma)),
    liberada: resultados.reduce((total, resultado) => total + (resultado.liberaPlata ?? 0), 0),
  };
}

const LUGAR = {
  bajo: 'más barato que lo que cobra la mayoría',
  justo: 'dentro de lo que cobran los demás',
  alto: 'más caro que lo que cobra la mayoría',
} as const;

/** Cómo queda el precio del paso frente a lo que cobran otros comercios de la zona; nada si hay pocos datos. */
export function comparacionConZona(
  precioNuevo: number,
  precioZona: PrecioZonaObjetivo | null,
): string | null {
  if (!precioZona) return null;
  const { minimo, promedio, maximo } = precioZona;
  return `En tu zona lo cobran entre ${formatoPesos(minimo)} y ${formatoPesos(maximo)} (en promedio ${formatoPesos(promedio)}). A ${formatoPesos(precioNuevo)} quedás ${LUGAR[posicionDelPrecio(precioNuevo, promedio)]}.`;
}
