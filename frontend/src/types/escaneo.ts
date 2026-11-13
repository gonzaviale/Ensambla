import type { Valor } from '@/lib/sistema/tipos';

export type FuenteCatalogo = 'sepa' | 'openfoodfacts';
export type CamaraDelLector = { id: string; nombre: string };
export type ProductoDeCatalogo = {
  codigo: string;
  nombre: string;
  marca: string;
  presentacion: string;
  imagen?: string;
};
export type CoincidenciaCatalogo = {
  fuente: FuenteCatalogo;
  estado: 'encontrado' | 'no_encontrado' | 'no_disponible';
  producto?: ProductoDeCatalogo;
};
export type ResultadoCatalogos = { codigo: string; fuentes: CoincidenciaCatalogo[] };
export type ProductoSugerido = {
  nombre: string;
  fuenteDescripcion?: FuenteCatalogo;
  imagen?: string;
  fuenteImagen?: FuenteCatalogo;
};

export type BorradorProductoInventario = {
  productoId: string | null;
  codigo: string;
  valores: Record<string, Valor>;
  originales: Record<string, Valor>;
};
