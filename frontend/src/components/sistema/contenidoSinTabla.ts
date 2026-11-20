import type { ComponentType } from 'react';
import type { TipoBloque } from '@/lib/sistema/tipos';
import { ContenidoDeNota, ContenidoDeResumen } from './ContenidosSinTabla';
import type { ContenidoDeBloqueProps } from './tiposDeContenido';

// Los bloques que no llevan tabla de registros: cada tipo sabe qué mostrar y el resto es una tabla.
export const CONTENIDOS_SIN_TABLA: Partial<
  Record<TipoBloque, ComponentType<ContenidoDeBloqueProps>>
> = {
  summary: ContenidoDeResumen,
  note: ContenidoDeNota,
};
