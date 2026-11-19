import type { Valor } from '../tipos';

export type TipoDeAviso =
  | 'stock'
  | 'precio_zona'
  | 'estancado'
  | 'muy_vendido'
  | 'combo'
  | 'consumo_zona'
  | 'revisar_precio';

export type GrupoDeAviso = 'stock' | 'ventas' | 'zona';

export const GRUPOS: Record<GrupoDeAviso, string> = {
  stock: 'Tu stock',
  ventas: 'Precios y ventas',
  zona: 'Tu zona',
};

export const GRUPO_DE: Record<TipoDeAviso, GrupoDeAviso> = {
  stock: 'stock',
  precio_zona: 'ventas',
  estancado: 'ventas',
  muy_vendido: 'ventas',
  combo: 'ventas',
  consumo_zona: 'zona',
  revisar_precio: 'ventas',
};

export const ORDEN: TipoDeAviso[] = [
  'stock',
  'estancado',
  'revisar_precio',
  'precio_zona',
  'muy_vendido',
  'combo',
  'consumo_zona',
];

export type AccionDeAviso =
  /** Cambia valores de un registro (un precio, por ejemplo). */
  | {
      tipo: 'aplicar';
      texto: string;
      sistemaId: string;
      bloqueId: string;
      filaId: string;
      valores: Record<string, Valor>;
      confirmacion: string;
    }
  /** Suma un registro nuevo a un bloque (un combo, un producto de la zona). */
  | {
      tipo: 'crear';
      texto: string;
      sistemaId: string;
      bloqueId: string;
      valores: Record<string, Valor>;
      confirmacion: string;
    }
  /** Abre el formulario de un registro nuevo (por ejemplo, una entrada de mercadería). */
  | { tipo: 'registrar'; texto: string; sistemaId: string; bloqueId: string; avisoId?: string }
  | { tipo: 'abrir'; texto: string; sistemaId: string; bloqueId: string; filaId?: string }
  /** Lleva a «Mi zona», filtrando por un producto. */
  | { tipo: 'zona'; texto: string; producto?: string }
  /** Le pide al servidor que ejecute la acción del aviso y lo cierre, todo en una sola operación. */
  | {
      tipo: 'servidor';
      texto: string;
      avisoId: string;
      confirmacion: string;
      /** Dónde está el registro que cambia, para poder mostrarlo. */
      sistemaId?: string;
      bloqueId?: string;
      filaId?: string;
    };

export type Aviso = {
  /** Depende del estado de los datos: si lo que lo disparó cambia, el id cambia y el aviso desaparece. */
  id: string;
  tipo: TipoDeAviso;
  grupo: GrupoDeAviso;
  /** 3 urgente, 2 para hoy, 1 para mirar. */
  prioridad: 1 | 2 | 3;
  sistemaId: string;
  sistema: string;
  titulo: string;
  detalle: string;
  porque: string[];
  acciones: AccionDeAviso[];
  /** Id del aviso en el servidor; falta en los que Sami calcula en el navegador. */
  servidorId?: string;
  /** El registro (o la tabla) del que habla el aviso, para llevar ahí con «Ver». */
  objeto?: { sistemaId: string; bloqueId: string; filaId?: string };
  /** Los números de origen del aviso del servidor (cantidad, mínimo, precio, sugerido…). */
  datos?: Record<string, unknown>;
  /** Cuándo lo detectó el motor del servidor (ISO). */
  creadoEn?: string;
};

/** Un aviso recién calculado: el grupo se le pone al sumarlo, según su tipo. */
export type AvisoSinGrupo = Omit<Aviso, 'grupo'>;
