import type { Aviso } from '../alertas';
import { allBlocks } from '../core';
import { destinoDeAviso, type DestinoDeAviso } from '../destinoDeAviso';
import type { Bloque, Fila, Sistema } from '../tipos';

/** Una línea de una respuesta: un producto o un aviso, con a dónde llevar si se toca «Ver». */
export type ItemDeChat = {
  id: string;
  titulo: string;
  detalle?: string;
  /** 3 urgente, 2 para hoy, 1 para mirar: sólo marca el punto de color. */
  nivel?: 1 | 2 | 3;
  aviso?: Aviso;
  destino?: DestinoDeAviso;
};

export type Respuesta = {
  texto: string;
  items?: ItemDeChat[];
  /** Ofrece abrir la pantalla con todos los avisos. */
  verTodos?: number;
  sugerencias?: string[];
};

export type ContextoDeChat = { sistema: Sistema; avisos: Aviso[]; hoy: string };

export type Producto = { bloque: Bloque; fila: Fila; nombre: string };

export type Venta = {
  productoId: string;
  unidades: number;
  total: number;
  dias: number;
  fiado: boolean;
};

export const SUGERENCIAS_INICIALES = ['¿Qué avisos tengo?', '¿Qué me falta?', '¿Cuánto vendí?'];

const DIA_MS = 86_400_000;

export const numero = (valor: unknown) => Number(valor ?? 0) || 0;

const diasEntre = (hoy: string, fecha: string) =>
  Math.floor((Date.parse(`${hoy}T12:00:00`) - Date.parse(`${fecha}T12:00:00`)) / DIA_MS);

export const plural = (cantidad: number, uno: string, varios: string) =>
  `${cantidad} ${cantidad === 1 ? uno : varios}`;

export const tiene = (texto: string, palabras: string[]) =>
  palabras.some((palabra) => texto.includes(palabra));

const bloquesDelTipo = (sistema: Sistema, tipo: string) =>
  allBlocks(sistema).filter((bloque) => bloque.type === tipo);

export const productosDe = (sistema: Sistema): Producto[] =>
  bloquesDelTipo(sistema, 'inventory').flatMap((bloque) =>
    bloque.rows.flatMap((fila) => {
      const nombre = String(fila.name ?? '').trim();
      return nombre ? [{ bloque, fila, nombre }] : [];
    }),
  );

export const ventasDe = (sistema: Sistema, hoy: string): Venta[] =>
  bloquesDelTipo(sistema, 'sales').flatMap((bloque) =>
    bloque.rows.filter((fila) => fila.date && fila.product).map((fila) => ventaDe(fila, hoy)),
  );

function ventaDe(fila: Fila, hoy: string): Venta {
  const unidades = numero(fila.quantity);
  return {
    productoId: String(fila.product),
    unidades,
    total: unidades * numero(fila.price),
    dias: diasEntre(hoy, String(fila.date)),
    fiado: fila.credit === true,
  };
}

export const esDeLosUltimos30Dias = (venta: Venta) => venta.dias >= 0 && venta.dias < 30;

export const destinoDe = (producto: Producto, sistema: Sistema): DestinoDeAviso => ({
  sistemaId: sistema.id,
  bloqueId: producto.bloque.id,
  filaId: producto.fila.id,
});

export const itemDeAviso = (aviso: Aviso): ItemDeChat => ({
  id: aviso.id,
  titulo: aviso.titulo,
  detalle: aviso.detalle,
  nivel: aviso.prioridad,
  aviso,
  destino: destinoDeAviso(aviso) ?? undefined,
});
