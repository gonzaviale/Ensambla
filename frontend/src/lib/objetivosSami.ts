import type {
  CuotaObjetivos,
  FuenteDeDatos,
  MiniParaGuardar,
  PropuestaObjetivo,
  TipoMiniObjetivoPropuesto,
  TipoObjetivo,
} from '@/types/objetivos';

// Cómo se dice en palabras lo que Sami entiende de un objetivo: con qué se mide, en qué datos se apoyó y cuántos
// objetivos con IA le quedan a la persona este mes.

/** Cuántos mensajes admite una conversación con Sami (los de la persona); el servidor lo vuelve a controlar. */
export const MAXIMO_DE_MENSAJES = 6;
export const MAXIMO_DEL_MENSAJE = 500;
/** Lo que incluye Premium, para explicar qué se gana al pasar de plan; la cuota exacta la dice el servidor. */
export const LIMITE_PREMIUM_DE_OBJETIVOS = 20;
export const MAXIMO_DEL_TITULO = 80;
export const MAXIMO_DE_LA_JUSTIFICACION = 280;

export const COMO_SE_MIDE: Record<TipoObjetivo, string> = {
  MONETARIO: 'Con una meta en pesos',
  CON_DATOS: 'Lo mide Sami con tus datos',
  SIN_DATOS: 'Me lo contás vos cada semana',
  HIBRIDO: 'Una parte la mide Sami y otra me la contás vos',
};

/** Una etiqueta corta para la tarjeta del objetivo. */
export const ETIQUETA_DE_TIPO: Record<TipoObjetivo, string> = {
  MONETARIO: 'Meta en pesos',
  CON_DATOS: 'Con datos',
  SIN_DATOS: 'Sin datos',
  HIBRIDO: 'Con y sin datos',
};

const NOMBRE_DE_FUENTE: Record<FuenteDeDatos, string> = {
  VENTAS: 'ventas',
  MARGEN: 'margen',
  INVENTARIO: 'inventario',
  GASTOS: 'gastos',
  ZONA: 'zona',
};

/** «Se apoyó en ventas y margen. No hay datos de zona.» */
export function datosUsados(datos: PropuestaObjetivo['datos']): string {
  const lista = (hay: boolean) =>
    datos
      .filter((dato) => dato.hay === hay)
      .map((dato) => NOMBRE_DE_FUENTE[dato.fuente])
      .join(', ');
  const con = lista(true);
  const sin = lista(false);
  return [con && `Se apoyó en: ${con}.`, sin && `Sin datos de: ${sin}.`].filter(Boolean).join(' ');
}

/** «Te quedan 3 de 5 este mes»; con cero, que se usaron todos. */
export function textoDeCuota(cuota: CuotaObjetivos): string {
  return cuota.restantes === 0
    ? `Usaste los ${cuota.limite} objetivos con IA de este mes`
    : `Te quedan ${cuota.restantes} de ${cuota.limite} objetivos con IA este mes`;
}

/** Una acción manual no puede afirmar una cifra: los importes y porcentajes los mide Sami, no se escriben. */
export const traeCifras = (texto: string) => /[$%]/.test(texto);

/** Un mini-objetivo en pantalla: lo que Sami propuso, con el texto que la persona puede retocar. */
export type MiniEditable = {
  /** Sólo sirve para distinguirlo en la lista. */
  clave: string;
  propuesto: TipoMiniObjetivoPropuesto;
  titulo: string;
  justificacion: string;
};

export const editablesDe = (propuesta: PropuestaObjetivo): MiniEditable[] =>
  propuesta.minis.map((propuesto, indice) => ({
    clave: `${indice}-${propuesto.titulo}`,
    propuesto,
    titulo: propuesto.titulo,
    justificacion: propuesto.justificacion,
  }));

/** Qué le falta a un mini manual para poder guardarse; nada si está bien. */
export function problemaDelManual(mini: MiniEditable): string | null {
  if (mini.propuesto.modo !== 'MANUAL') return null;
  const titulo = mini.titulo.trim();
  const justificacion = mini.justificacion.trim();
  if (!titulo || !justificacion) return 'Escribí qué vas a hacer y por qué.';
  if (traeCifras(`${titulo} ${justificacion}`))
    return 'Sin importes ni porcentajes: eso lo mide Sami.';
  return null;
}

/** Lo que se manda al guardar: del medido sólo cuál es; del manual, su texto. */
export function paraGuardar(mini: MiniEditable): MiniParaGuardar | null {
  const { propuesto } = mini;
  if (propuesto.modo === 'MANUAL')
    return { modo: 'MANUAL', titulo: mini.titulo.trim(), justificacion: mini.justificacion.trim() };
  if (!propuesto.tipo) return null;
  return {
    modo: 'MEDIDO',
    tipo: propuesto.tipo,
    registroId: propuesto.registroId,
    referencia: propuesto.referencia,
  };
}

/** El tipo de objetivo que queda con estos mini-objetivos, el mismo criterio que usa el servidor. */
export function tipoQueQueda(minis: MiniEditable[]): TipoObjetivo | null {
  const medidos = minis.some((mini) => mini.propuesto.modo === 'MEDIDO');
  const manuales = minis.some((mini) => mini.propuesto.modo === 'MANUAL');
  if (medidos && manuales) return 'HIBRIDO';
  if (medidos) return 'CON_DATOS';
  return manuales ? 'SIN_DATOS' : null;
}
