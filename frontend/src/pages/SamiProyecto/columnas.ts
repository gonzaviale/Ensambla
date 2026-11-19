import type { ColumnaSugerida } from '@/types/samiProyecto';

const NOMBRE_DE_TIPO: Record<string, string> = {
  TEXTO: 'texto',
  NUMERO: 'número',
  FECHA: 'fecha',
  OPCION: 'opción',
  SI_NO: 'sí/no',
};

export const tipoEnPalabras = (tipo: string) => NOMBRE_DE_TIPO[tipo] ?? tipo.toLowerCase();

export const detalleDeColumna = (columna: ColumnaSugerida) =>
  columna.tipo === 'OPCION' && columna.opciones.length
    ? `${tipoEnPalabras(columna.tipo)}: ${columna.opciones.join(', ')}`
    : tipoEnPalabras(columna.tipo);
