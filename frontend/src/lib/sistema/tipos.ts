import type { NombreDeIcono } from '@/components/sistema/iconos';

export type Color = 'orange' | 'coral' | 'yellow' | 'blue' | 'petrol';

export type TipoCampo =
  | 'text'
  | 'textarea'
  | 'number'
  | 'currency'
  | 'date'
  | 'select'
  | 'email'
  | 'tel'
  | 'checkbox'
  | 'relation'
  | 'time';

export type TipoBloque =
  | 'inventory'
  | 'orders'
  | 'clients'
  | 'sales'
  | 'expenses'
  | 'summary'
  | 'form'
  | 'note'
  | 'entries'
  | 'exits';

export type Ancho = 'third' | 'half' | 'wide' | 'full';

export type Distribucion = 'mosaic' | 'single' | 'two' | 'three';

export type ClavePlantilla = 'warehouse' | 'business' | 'shop';

export type Valor = string | number | boolean | null | undefined;

// Una fila guarda los valores por la clave de cada campo. `__example` marca lo que viene de ejemplo y
// `__source` lo que llegó de una foto.
export type Fila = {
  id: string;
  __example?: boolean;
  __source?: string;
  [clave: string]: Valor;
};

export type Campo = {
  key: string;
  label: string;
  type: TipoCampo;
  required: boolean;
  options?: string[];
  min?: number;
  /** Id del bloque al que apunta un campo de tipo vínculo. */
  target?: string;
};

export type Bloque = {
  id: string;
  type: TipoBloque;
  name: string;
  description: string;
  color: Color;
  width: Ancho;
  fields: Campo[];
  rows: Fila[];
  content: string;
  /** Empieza una fila nueva: el bloque que va al lado se une a él. */
  startRow?: boolean;
  /** +1 si suma stock (entradas), -1 si lo descuenta (salidas y ventas). */
  stockDirection?: 1 | -1;
};

export type Seccion = { id: string; name: string; blocks: Bloque[] };

export type OpcionesPlantilla = { movements: boolean; expenses: boolean };

export type Sistema = {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  sections: Seccion[];
  template?: ClavePlantilla;
  options?: OpcionesPlantilla;
  layout?: Distribucion;
  /** El almacén de ejemplo que arma el panel de avisos. */
  demoAlmacen?: boolean;
};

export type ErroresFila = Record<string, string>;

export type DefinicionDeBloque = {
  name: string;
  singular: string;
  icon: NombreDeIcono;
  color: Color;
  description: string;
  purpose: string;
  fields: Campo[];
  examples: Record<string, Valor>[];
};

export type DefinicionDePlantilla = {
  name: string;
  subtitle: string;
  description: string;
  icon: NombreDeIcono;
  color: Color;
  types: TipoBloque[];
  tags: string[];
  defaultName: string;
};
