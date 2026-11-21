import { aDiez, fechaCorta, pesos, porcentaje } from './medidas';
import {
  aplicarPrecio,
  datosDelSistema,
  referenciaDeZona,
  tienePrecioYZona,
  verStock,
  type ProductoAnalizado,
  type ProductoConZona,
} from './producto';
import type { AvisoSinGrupo } from './tipos';

type ReglaDePrecio = (producto: ProductoConZona) => AvisoSinGrupo | undefined;

const promedioDeZona = ({ delaZona }: ProductoConZona) => delaZona.precio.promedio;

const esMuyVendido = (producto: ProductoConZona) =>
  producto.rotacion.u30 >= 15 &&
  producto.rotacion.u30 >= producto.tipica * 1.5 &&
  producto.venta < promedioDeZona(producto) * 0.95;

const estaEstancado = (producto: ProductoConZona) =>
  producto.hayVentas &&
  producto.cantidad > 0 &&
  producto.rotacion.u30 === 0 &&
  producto.venta > promedioDeZona(producto) * 1.05;

const esMasBaratoQueLaZona = (producto: ProductoConZona) =>
  !esMuyVendido(producto) && producto.venta < promedioDeZona(producto) * 0.92;

const esMasCaroQueLaZona = (producto: ProductoConZona) =>
  !estaEstancado(producto) && producto.venta > promedioDeZona(producto) * 1.12;

/** 2. Mucha rotación con un precio por debajo del promedio de la zona. */
const avisoDeMuyVendido: ReglaDePrecio = (producto) => {
  if (!esMuyVendido(producto)) return undefined;
  const { fila, nombre, venta, rotacion } = producto;
  const promedio = promedioDeZona(producto);
  const sugerido = Math.max(aDiez(promedio * 0.98), aDiez(venta) + 10);
  return {
    ...datosDelSistema(producto),
    id: `muy_vendido:${fila.id}:${venta}`,
    tipo: 'muy_vendido',
    prioridad: 1,
    titulo: `${nombre} se vende mucho y lo tenés por debajo de tu zona`,
    detalle: `Vendiste ${rotacion.u30} en 30 días a ${pesos(venta)}; el promedio de tu zona es ${pesos(promedio)}.`,
    porque: [
      referenciaDeZona(producto),
      `Con las mismas ventas, subirlo a ${pesos(sugerido)} te deja ${pesos((sugerido - venta) * rotacion.u30)} más por mes.`,
    ],
    acciones: [aplicarPrecio(producto, `Subir a ${pesos(sugerido)}`, sugerido), verStock(producto)],
  };
};

/** 3. Caro para la zona y sin ventas en 30 días. */
const avisoDeEstancado: ReglaDePrecio = (producto) => {
  if (!estaEstancado(producto)) return undefined;
  const { fila, nombre, venta } = producto;
  const promedio = promedioDeZona(producto);
  const sugerido = aDiez(promedio);
  return {
    ...datosDelSistema(producto),
    id: `estancado:${fila.id}:${venta}`,
    tipo: 'estancado',
    prioridad: 2,
    titulo: `${nombre} está caro y no se mueve`,
    detalle: `Lo tenés a ${pesos(venta)}, ${porcentaje(venta / promedio - 1)} más que el promedio de tu zona, y no lo vendiste en 30 días.`,
    porque: [
      referenciaDeZona(producto),
      ultimaVentaYStock(producto),
      `Rebajarlo a ${pesos(sugerido)} lo deja en el promedio de tu zona.`,
    ],
    acciones: [
      aplicarPrecio(producto, `Rebajar a ${pesos(sugerido)}`, sugerido),
      verStock(producto),
    ],
  };
};

const ultimaVentaYStock = ({ rotacion, cantidad }: ProductoConZona) =>
  rotacion.ultima
    ? `La última venta que anotaste fue el ${fechaCorta(rotacion.ultima)}. Tenés ${cantidad} en stock.`
    : `Todavía no anotaste ventas de este producto. Tenés ${cantidad} en stock.`;

/** 4a. Más barato que la zona: puede estar perdiendo margen. */
const avisoDePrecioBajo: ReglaDePrecio = (producto) => {
  if (!esMasBaratoQueLaZona(producto)) return undefined;
  const { fila, nombre, venta, costo, delaZona } = producto;
  const promedio = promedioDeZona(producto);
  const sugerido = Math.max(
    aDiez(Math.min(promedio * 0.97, delaZona.precio.maximo)),
    aDiez(venta) + 10,
  );
  const alCosto = costo > 0 && venta <= costo * 1.1;
  return {
    ...datosDelSistema(producto),
    id: `precio_zona:${fila.id}:${venta}`,
    tipo: 'precio_zona',
    prioridad: alCosto ? 2 : 1,
    titulo: alCosto ? `Vendés ${nombre} casi al costo` : `${nombre} está más barato que en tu zona`,
    detalle: `Lo tenés a ${pesos(venta)} y el promedio de tu zona es ${pesos(promedio)}. Puede que estés ganando menos de lo que podrías.`,
    porque: [referenciaDeZona(producto), margenConElSugerido(producto, sugerido)],
    acciones: [aplicarPrecio(producto, `Subir a ${pesos(sugerido)}`, sugerido), verStock(producto)],
  };
};

const margenConElSugerido = ({ venta, costo }: ProductoConZona, sugerido: number) =>
  costo > 0
    ? `Hoy ganás ${pesos(venta - costo)} por unidad; a ${pesos(sugerido)} ganarías ${pesos(sugerido - costo)}.`
    : `A ${pesos(sugerido)} ganarías ${pesos(sugerido - venta)} más por unidad.`;

/** 4b. Más caro que la zona: bajarlo vende más. */
const avisoDePrecioAlto: ReglaDePrecio = (producto) => {
  if (!esMasCaroQueLaZona(producto)) return undefined;
  const { fila, nombre, venta } = producto;
  const promedio = promedioDeZona(producto);
  const sugerido = aDiez(promedio * 1.03);
  return {
    ...datosDelSistema(producto),
    id: `precio_zona:${fila.id}:${venta}`,
    tipo: 'precio_zona',
    prioridad: 1,
    titulo: `${nombre} está más caro que en tu zona`,
    detalle: `Lo tenés a ${pesos(venta)} y el promedio de tu zona es ${pesos(promedio)}. Bajarlo puede ayudarte a vender más.`,
    porque: [
      referenciaDeZona(producto),
      `${ventasAnotadas(producto)} A ${pesos(sugerido)} quedás apenas por encima del promedio.`,
    ],
    acciones: [aplicarPrecio(producto, `Bajar a ${pesos(sugerido)}`, sugerido), verStock(producto)],
  };
};

const ventasAnotadas = ({ hayVentas, rotacion }: ProductoConZona) =>
  hayVentas ? `Vendiste ${rotacion.u30} en 30 días.` : 'Todavía no hay ventas anotadas.';

const REGLAS_DE_PRECIO: ReglaDePrecio[] = [
  avisoDeMuyVendido,
  avisoDeEstancado,
  avisoDePrecioBajo,
  avisoDePrecioAlto,
];

/** Los avisos de precio frente a la zona: sólo para un producto con precio que también se vende en la zona. */
export const avisosDePrecio = (producto: ProductoAnalizado): AvisoSinGrupo[] =>
  tienePrecioYZona(producto) ? REGLAS_DE_PRECIO.flatMap((regla) => regla(producto) ?? []) : [];
