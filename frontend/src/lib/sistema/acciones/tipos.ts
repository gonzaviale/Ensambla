export type AccionDeSami = { herramienta: string; argumentos: Record<string, unknown> };

/**
 * El sistema resumido que se manda en cada turno: sin filas de ejemplo, para gastar pocos tokens. De cada pieza van
 * los registros reales, también los ya guardados (con su id), así Sami los puede corregir sin cargarlos de nuevo.
 */
export type EstadoCompacto = {
  nombre: string;
  distribucion: string;
  piezas: PiezaCompacta[];
};

export type PiezaCompacta = {
  id: string;
  tipo: string;
  nombre: string;
  seccion: string;
  campos: { etiqueta: string; tipo: string }[];
  registros: { id: string; datos: DatosParaSami }[];
  /** Si otra pieza se vincula con esta: los nombres que ya tiene, para que Sami use el mismo y no invente otro. */
  nombres?: string[];
};

export type DatosParaSami = Record<string, string | number | boolean>;

/**
 * Un nombre que Sami usó en un vínculo (el Cliente o el Producto de una venta) y que la pieza destino no tiene. No se
 * crea solo: la persona confirma si lo suma. Mientras tanto, el vínculo de esas filas queda vacío.
 */
export type VinculoPendiente = {
  destinoId: string;
  nombre: string;
  filas: FilaPendiente[];
};

export type FilaPendiente = { bloqueId: string; filaId: string; campoKey: string };

export type ResultadoDeAcciones = {
  /** Lo que se hizo, en palabras. */
  aplicadas: string[];
  /** Lo que se pidió y no se pudo hacer, y por qué. */
  omitidas: string[];
  /** Los nombres de vínculos que no se encontraron y esperan confirmación. */
  pendientes: VinculoPendiente[];
  /** Los productos a los que no les alcanzó el stock para las ventas que cargó Sami: quedaron en 0. */
  sinStock: SinStock[];
};

/** Una venta (o salida) que pedía más de lo que había: se descontó hasta 0. */
export type SinStock = { producto: string; pedido: number; habia: number };
