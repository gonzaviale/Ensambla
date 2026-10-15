import type { ComponentType } from 'react';
import {
  ArrowsLeftRightIcon,
  FileTextIcon,
  PackageIcon,
  ScalesIcon,
  SealWarningIcon,
  TagIcon,
  WarningIcon,
} from '@phosphor-icons/react';
import type { BadgeTone } from '@/components/ui';
import type { TipoAlerta } from '@/types/red';

export type GrupoAviso = 'corregir' | 'comprar';

type IconoAviso = ComponentType<{ size?: number; weight?: 'bold' | 'fill' | 'regular' }>;

type MetaAviso = {
  etiqueta: string;
  /** Qué explica el tipo de aviso en el panel de ajustes. */
  descripcion: string;
  grupo: GrupoAviso;
  Icono: IconoAviso;
  tono: BadgeTone;
};

// Qué es cada tipo de aviso para el comercio: una etiqueta corta, el grupo en el que se lo muestra y
// un ícono (nunca sólo color).
export const META_AVISO: Record<TipoAlerta, MetaAviso> = {
  'precio-en-riesgo': {
    etiqueta: 'Precio en riesgo',
    descripcion: 'Un costo subió y estás ganando muy poco (o perdiendo).',
    grupo: 'corregir',
    Icono: WarningIcon,
    tono: 'bad',
  },
  'cambio-raro': {
    etiqueta: 'Cambio raro',
    descripcion: 'Un precio de la lista cambió de golpe, puede ser un error.',
    grupo: 'corregir',
    Icono: SealWarningIcon,
    tono: 'warn',
  },
  'lista-nueva': {
    etiqueta: 'Lista nueva',
    descripcion: 'Tu proveedor mandó una lista y cambió algo de lo que vendés.',
    grupo: 'corregir',
    Icono: FileTextIcon,
    tono: 'accent',
  },
  'stock-por-agotarse': {
    etiqueta: 'Se termina',
    descripcion: 'Al ritmo de ventas, un producto se termina antes de tu próxima compra.',
    grupo: 'comprar',
    Icono: PackageIcon,
    tono: 'warn',
  },
  'oferta-que-sirve': {
    etiqueta: 'Oferta que sirve',
    descripcion: 'Un proveedor publicó una oferta de algo que vendés.',
    grupo: 'comprar',
    Icono: TagIcon,
    tono: 'ok',
  },
  'compras-caro': {
    etiqueta: 'Comprás caro',
    descripcion: 'Pagás bastante más que otros comercios de tu zona.',
    grupo: 'comprar',
    Icono: ScalesIcon,
    tono: 'warn',
  },
  'mejor-proveedor': {
    etiqueta: 'Mejor proveedor',
    descripcion: 'Otro proveedor tiene el mismo producto a menor costo.',
    grupo: 'comprar',
    Icono: ArrowsLeftRightIcon,
    tono: 'ok',
  },
};

export const ORDEN_TIPOS: TipoAlerta[] = [
  'precio-en-riesgo',
  'cambio-raro',
  'lista-nueva',
  'stock-por-agotarse',
  'oferta-que-sirve',
  'compras-caro',
  'mejor-proveedor',
];

export const TITULO_GRUPO: Record<GrupoAviso, string> = {
  corregir: 'Para corregir ya',
  comprar: 'Para comprar mejor',
};

/** Aviso corto que muestra la pantalla (Toast) y qué botón extra ofrece. */
export type MensajeHoy = {
  mensaje: string;
  tipo: 'exito' | 'error';
  accion?: { etiqueta: string; onClick: () => void };
};
