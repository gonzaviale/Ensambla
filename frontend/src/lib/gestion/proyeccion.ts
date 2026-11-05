import { normalize } from '@/lib/sistema/core';
import { pesos } from './formato';
import {
  cuentaDelMes,
  GASTOS_FIJOS_MENSUALES,
  ventasDelPeriodo,
  type Cuenta,
  type DatosDelNegocio,
  type Producto,
} from './resultado';

// «¿Me conviene?»: proyecta una decisión sobre la cuenta del último mes. Cambia lo vendido, su costo o los gastos
// fijos y vuelve a hacer la misma cuenta de la ganancia real, así antes y después se comparan con la misma regla.

export type Escenario =
  | { tipo: 'precio'; productoId: string; cambio: number }
  | { tipo: 'producto'; nombre: string; costo: number; precio: number; unidadesPorDia: number }
  | { tipo: 'gastoFijo'; concepto: string; mensual: number; ventasExtraPorDia: number };

export type Veredicto = 'conviene' | 'no-conviene' | 'parecido';

export type Proyeccion = {
  antes: Cuenta;
  despues: Cuenta;
  diferencia: number;
  veredicto: Veredicto;
  /** Lo que Sami explica, en frases cortas. */
  porQue: string[];
  /** Ganancia acumulada en los próximos 90 días, de a una semana. */
  serie: { dia: number; antes: number; despues: number }[];
};

/** Cuánto cae la venta por cada 1 % que sube el precio, según el tipo de producto (comercios parecidos de la zona). */
export function sensibilidadDe(nombre: string) {
  const texto = normalize(nombre);
  const basicos = [
    'yerba',
    'aceite',
    'harina',
    'azucar',
    'fideos',
    'arroz',
    'huevos',
    'pan',
    'leche',
    'sal',
  ];
  const bebidas = ['coca', 'sprite', 'cerveza', 'vino', 'fernet'];
  if (basicos.some((palabra) => texto.includes(palabra))) return 0.6;
  if (bebidas.some((palabra) => texto.includes(palabra))) return 1.3;
  return 0.9;
}

type Totales = { ventas: number; costo: number; unidadesPorProducto: Map<string, number> };

function totalesDelMes(datos: DatosDelNegocio, hoy: string): Totales {
  const porId = new Map(datos.productos.map((producto) => [producto.id, producto]));
  const unidadesPorProducto = new Map<string, number>();
  let ventas = 0;
  let costo = 0;
  for (const venta of ventasDelPeriodo(datos.ventas, hoy, 30)) {
    ventas += venta.precio * venta.cantidad;
    costo += (porId.get(venta.productoId)?.costo ?? 0) * venta.cantidad;
    unidadesPorProducto.set(
      venta.productoId,
      (unidadesPorProducto.get(venta.productoId) ?? 0) + venta.cantidad,
    );
  }
  return { ventas, costo, unidadesPorProducto };
}

export function proyectar(datos: DatosDelNegocio, hoy: string, escenario: Escenario): Proyeccion {
  const base = totalesDelMes(datos, hoy);
  const antes = cuentaDelMes(base.ventas, base.costo, GASTOS_FIJOS_MENSUALES);
  let despues = antes;
  const porQue: string[] = [];

  if (escenario.tipo === 'precio') {
    const producto = datos.productos.find((item) => item.id === escenario.productoId);
    const unidades = producto ? (base.unidadesPorProducto.get(producto.id) ?? 0) : 0;
    if (producto && unidades > 0) {
      const sensibilidad = sensibilidadDe(producto.nombre);
      const unidadesNuevas = Math.max(0, unidades * (1 - sensibilidad * escenario.cambio));
      const precioNuevo = producto.precio * (1 + escenario.cambio);
      const ventas = base.ventas - unidades * producto.precio + unidadesNuevas * precioNuevo;
      const costo = base.costo - unidades * producto.costo + unidadesNuevas * producto.costo;
      despues = cuentaDelMes(ventas, costo, GASTOS_FIJOS_MENSUALES);
      const semana = (valor: number) => Math.round((valor / 30) * 7);
      porQue.push(
        `Hoy vendés unas ${semana(unidades)} por semana a ${pesos(producto.precio)}. A ${pesos(precioNuevo)} calculo unas ${semana(unidadesNuevas)}.`,
        `Todavía no cambiaste este precio, así que uso lo que pasa en comercios parecidos de tu zona: por cada 10 % que sube, la venta ${escenario.cambio >= 0 ? 'cae' : 'sube'} cerca de un ${Math.round(sensibilidad * 10)} %.`,
      );
    } else {
      porQue.push(
        'Ese producto no se vendió en el último mes, así que cambiarle el precio no mueve la cuenta.',
      );
    }
  }

  if (escenario.tipo === 'producto') {
    const mensuales = escenario.unidadesPorDia * 30;
    despues = cuentaDelMes(
      base.ventas + mensuales * escenario.precio,
      base.costo + mensuales * escenario.costo,
      GASTOS_FIJOS_MENSUALES,
    );
    const primeraCompra = escenario.costo * escenario.unidadesPorDia * 7;
    const dejaPorDia = (despues.ganancia - antes.ganancia) / 30;
    porQue.push(
      `Cada ${escenario.nombre || 'unidad'} te deja ${pesos(escenario.precio - escenario.costo)} antes de comisiones y pérdidas.`,
      `La primera compra (una semana) es de ${pesos(primeraCompra)}${dejaPorDia > 0 ? ` y la recuperás en ${Math.ceil(primeraCompra / dejaPorDia)} días` : ''}.`,
    );
  }

  if (escenario.tipo === 'gastoFijo') {
    const margenDelMes = antes.ventas > 0 ? antes.costoDeLoVendido / antes.ventas : 0;
    const extra = escenario.ventasExtraPorDia * 30;
    despues = cuentaDelMes(
      base.ventas + extra,
      base.costo + extra * margenDelMes,
      GASTOS_FIJOS_MENSUALES + escenario.mensual,
    );
    porQue.push(
      `Tus gastos fijos pasan de ${pesos(antes.gastosFijos)} a ${pesos(despues.gastosFijos)} por mes.`,
      antes.contribucion > 0
        ? `De cada $100 que vendés te quedan ${pesos(antes.contribucion * 100)} para los fijos: para que se pague solo tendrías que vender ${pesos(escenario.mensual / antes.contribucion / 30)} más por día.`
        : 'Hoy lo que vendés no deja margen para pagar un gasto más.',
    );
  }

  const diferencia = despues.ganancia - antes.ganancia;
  const umbral = Math.max(1000, Math.abs(antes.ganancia) * 0.02);
  const veredicto: Veredicto =
    diferencia > umbral ? 'conviene' : diferencia < -umbral ? 'no-conviene' : 'parecido';
  porQue.push(
    veredicto === 'parecido'
      ? 'La ganancia queda casi igual: decidí por otra cosa (tiempo, clientes, espacio).'
      : `Tu ganancia de verdad ${diferencia > 0 ? 'sube' : 'baja'} ${pesos(Math.abs(diferencia))} por mes.`,
  );

  const serie = Array.from({ length: 14 }, (_, semana) => {
    const dia = semana * 7;
    return { dia, antes: (antes.ganancia / 30) * dia, despues: (despues.ganancia / 30) * dia };
  });

  return { antes, despues, diferencia, veredicto, porQue, serie };
}

/** «350 mil», «350.000», «$60000» o «10%» como número; `null` si no hay. */
function numeroEn(texto: string): number | null {
  const encontrado = texto.match(/(\d+(?:[.,]\d+)*)\s*(mil|k)?/);
  if (!encontrado?.[1]) return null;
  const crudo = encontrado[1];
  // «2,5» es decimal; «350.000» o «1,200» son miles.
  const valor = /^\d+[.,]\d{3}(?:[.,]\d{3})*$/.test(crudo)
    ? Number(crudo.replace(/[.,]/g, ''))
    : Number(crudo.replace(',', '.'));
  return encontrado[2] ? valor * 1000 : valor;
}

/** El producto que nombra el texto, por la primera palabra de su nombre («coca», «yerba»). */
function productoNombrado(texto: string, productos: Producto[]) {
  return productos.find((producto) => {
    const primera = normalize(producto.nombre).split(/[\s-]+/)[0] ?? '';
    return primera.length > 2 && texto.includes(primera);
  });
}

/**
 * Lo que entiende Sami de una pregunta escrita. En el mockup es por palabras; en el producto lo haría la IA
 * con una herramienta por tipo de escenario, como el taller.
 */
export function interpretarPregunta(pregunta: string, productos: Producto[]): Escenario | null {
  const texto = normalize(pregunta);
  const producto = productoNombrado(texto, productos);
  const numero = numeroEn(texto);

  if (/emplead|ayudante|sueldo|alquiler|contrat|tomo a|tomar a|gasto fijo/.test(texto)) {
    const concepto = /alquiler/.test(texto) ? 'Más alquiler' : 'Un ayudante';
    return { tipo: 'gastoFijo', concepto, mensual: numero ?? 60000, ventasExtraPorDia: 0 };
  }
  if (producto && /%|precio|subo|subir|bajo|bajar|aument|descuento|cobro/.test(texto)) {
    const porcentaje = (numero ?? 10) / 100;
    const baja = /bajo|bajar|descuento|rebaj/.test(texto);
    return { tipo: 'precio', productoId: producto.id, cambio: baja ? -porcentaje : porcentaje };
  }
  if (/vender|sumar|agregar|traer|empiezo a|empezar a/.test(texto)) {
    const nombre =
      pregunta.match(
        /(?:vender|sumar|agregar|traer|empiezo a vender|empezar a vender)\s+(.+?)[?.!]*$/i,
      )?.[1] ?? 'Producto nuevo';
    return { tipo: 'producto', nombre, costo: 1000, precio: 1500, unidadesPorDia: 3 };
  }
  return null;
}
