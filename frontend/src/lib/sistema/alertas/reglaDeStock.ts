import { pesos } from './medidas';
import { datosDelSistema, verStock, type ProductoAnalizado } from './producto';
import type { AccionDeAviso, AvisoSinGrupo } from './tipos';

const DIAS_A_CUBRIR = 14;
const PEDIDO_SIN_MINIMO = 10;

const estaAgotado = ({ cantidad }: ProductoAnalizado) => cantidad <= 0;

const faltaStock = (producto: ProductoAnalizado) =>
  estaAgotado(producto) || (producto.minimo > 0 && producto.cantidad <= producto.minimo);

const ventaDiaria = ({ rotacion }: ProductoAnalizado) => rotacion.u30 / 30;

/** 1. Stock bajo o agotado. */
export function avisoDeStock(producto: ProductoAnalizado): AvisoSinGrupo | undefined {
  if (!faltaStock(producto)) return undefined;
  const { sistema, inventario, fila } = producto;
  const pedir = cuantoPedir(producto);
  return {
    ...datosDelSistema(producto),
    id: `stock:${fila.id}:${producto.cantidad}`,
    objeto: { sistemaId: sistema.id, bloqueId: inventario.id, filaId: fila.id },
    tipo: 'stock',
    prioridad: estaAgotado(producto) ? 3 : 2,
    titulo: tituloDeStock(producto),
    detalle: detalleDeStock(producto, pedir),
    porque: motivosDeStock(producto, pedir),
    acciones: [accionParaReponer(producto)],
  };
}

function cuantoPedir(producto: ProductoAnalizado) {
  const diario = ventaDiaria(producto);
  const objetivo = diario > 0 ? Math.ceil(diario * DIAS_A_CUBRIR) : pedidoSinVentas(producto);
  return Math.max(objetivo - producto.cantidad, 1);
}

const pedidoSinVentas = ({ minimo }: ProductoAnalizado) =>
  minimo > 0 ? minimo * 2 : PEDIDO_SIN_MINIMO;

const tituloDeStock = (producto: ProductoAnalizado) =>
  estaAgotado(producto)
    ? `Te quedaste sin ${producto.nombre}`
    : `${producto.nombre} está por debajo del mínimo`;

function detalleDeStock(producto: ProductoAnalizado, pedir: number) {
  const quedan = estaAgotado(producto) ? 'No te quedan unidades' : `Te quedan ${producto.cantidad}`;
  const minimo = producto.minimo ? ` y tu mínimo es ${producto.minimo}` : '';
  return `${quedan}${minimo}. Te conviene pedir ${pedir} para no quedarte sin stock.`;
}

function motivosDeStock(producto: ProductoAnalizado, pedir: number): string[] {
  const motivos =
    producto.rotacion.u30 > 0
      ? motivosConVentas(producto, pedir)
      : motivosSinVentas(producto, pedir);
  const { delaZona } = producto;
  if (!delaZona) return motivos;
  return [
    ...motivos,
    `En tu zona lo venden ${delaZona.ventas.comercios} comercios, a unos ${pesos(delaZona.precio.promedio)}.`,
  ];
}

const motivosConVentas = (producto: ProductoAnalizado, pedir: number) => [
  estaAgotado(producto)
    ? `Vendiste ${producto.rotacion.u30} en los últimos 30 días.`
    : `Te alcanza para ${loQueAlcanza(producto.cantidad / ventaDiaria(producto))}.`,
  `Te conviene pedir ${pedir} para cubrir las próximas 2 semanas.`,
];

const loQueAlcanza = (dias: number) => (dias < 1 ? 'menos de 1 día' : `${Math.floor(dias)} días`);

const motivosSinVentas = ({ minimo, cantidad }: ProductoAnalizado, pedir: number) => [
  minimo > 0
    ? `Definiste un mínimo de ${minimo} y quedan ${cantidad}.`
    : 'No tiene un mínimo definido: te avisamos porque se agotó.',
  `Te conviene pedir ${pedir} para no quedarte sin stock.`,
];

const accionParaReponer = (producto: ProductoAnalizado): AccionDeAviso =>
  producto.entradas
    ? {
        tipo: 'registrar',
        texto: 'Cargar entrada',
        sistemaId: producto.sistema.id,
        bloqueId: producto.entradas.id,
      }
    : verStock(producto);
