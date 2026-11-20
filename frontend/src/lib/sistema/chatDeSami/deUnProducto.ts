import { pesos } from '../alertas';
import { normalize } from '../core';
import {
  destinoDe,
  esDeLosUltimos30Dias,
  numero,
  plural,
  tiene,
  ventasDe,
  type ContextoDeChat,
  type ItemDeChat,
  type Producto,
  type Respuesta,
} from './datos';

/** Lo que el chat sabe de un producto para contestar sobre él. */
type DatosDelProducto = {
  producto: Producto;
  cantidad: number;
  minimo: number;
  precio: number;
  costo: number;
  vendidas: number;
  conAviso: boolean;
};

type TemaDelProducto = { palabras: string[]; texto: (datos: DatosDelProducto) => string };

const SUGERENCIAS = ['¿Qué me falta?', '¿Qué avisos tengo?'];

const decimal = (valor: number) => valor.toFixed(1).replace('.', ',');

function textoDelPrecio({ producto, precio, costo }: DatosDelProducto) {
  const teCuesta = costo ? ` y te cuesta ${pesos(costo)}` : '';
  return `${producto.nombre} está a ${pesos(precio)}${teCuesta}.${margenDe(precio, costo)}`;
}

const margenDe = (precio: number, costo: number) =>
  costo > 0 && precio > 0
    ? ` Te deja ${pesos(precio - costo)} por unidad (${Math.round(((precio - costo) / precio) * 100)} %).`
    : '';

const textoDeLasVentas = ({ producto, vendidas }: DatosDelProducto) =>
  vendidas
    ? `De ${producto.nombre} vendiste ${plural(vendidas, 'unidad', 'unidades')} en los últimos 30 días (${decimal(vendidas / 30)} por día).`
    : `De ${producto.nombre} no anotaste ventas en los últimos 30 días.`;

function textoDelStock(datos: DatosDelProducto) {
  const { producto, cantidad, minimo, conAviso } = datos;
  const estado = cantidad <= 0 ? 'No te queda nada' : `Te quedan ${cantidad}`;
  const tuMinimo = minimo ? ` (tu mínimo es ${minimo})` : '';
  const aviso = conAviso ? ' Tiene un aviso para mirar.' : '';
  return `${producto.nombre}: ${estado}${tuMinimo}.${loQueAlcanza(datos)}${aviso}`;
}

function loQueAlcanza({ cantidad, vendidas }: DatosDelProducto) {
  const diario = vendidas / 30;
  if (diario <= 0 || cantidad <= 0) return '';
  const dias = cantidad / diario;
  return ` Con lo que vendés, te alcanza para ${dias < 1 ? 'menos de 1 día' : `unos ${Math.floor(dias)} días`}.`;
}

/** En orden: el primero que reconoce la pregunta contesta; si ninguno, se cuenta el stock. */
const TEMAS: TemaDelProducto[] = [
  { palabras: ['precio', 'cuesta', 'sale', 'costo', 'margen', 'ganancia'], texto: textoDelPrecio },
  { palabras: ['vend', 'venta', 'rotacion', 'salida'], texto: textoDeLasVentas },
];

export function deUnProducto(
  pregunta: string,
  producto: Producto,
  contexto: ContextoDeChat,
): Respuesta {
  const texto = normalize(pregunta);
  const datos = datosDelProducto(producto, contexto);
  const tema = TEMAS.find(({ palabras }) => tiene(texto, palabras));
  const contestar = tema?.texto ?? textoDelStock;
  return {
    texto: contestar(datos),
    items: [itemDelProducto(datos, contexto)],
    sugerencias: SUGERENCIAS,
  };
}

function datosDelProducto(producto: Producto, contexto: ContextoDeChat): DatosDelProducto {
  const { fila } = producto;
  return {
    producto,
    cantidad: numero(fila.quantity),
    minimo: numero(fila.minimum),
    precio: numero(fila.price),
    costo: numero(fila.cost),
    vendidas: vendidasEn30Dias(producto, contexto),
    conAviso: contexto.avisos.some((aviso) => aviso.objeto?.filaId === fila.id),
  };
}

const vendidasEn30Dias = (producto: Producto, { sistema, hoy }: ContextoDeChat) =>
  ventasDe(sistema, hoy)
    .filter((venta) => venta.productoId === producto.fila.id && esDeLosUltimos30Dias(venta))
    .reduce((acumulado, venta) => acumulado + venta.unidades, 0);

const itemDelProducto = (datos: DatosDelProducto, contexto: ContextoDeChat): ItemDeChat => ({
  id: datos.producto.fila.id,
  titulo: datos.producto.nombre,
  nivel: nivelDelStock(datos),
  destino: destinoDe(datos.producto, contexto.sistema),
});

function nivelDelStock({ cantidad, minimo }: DatosDelProducto): ItemDeChat['nivel'] {
  if (cantidad <= 0) return 3;
  return cantidad <= minimo ? 2 : undefined;
}
