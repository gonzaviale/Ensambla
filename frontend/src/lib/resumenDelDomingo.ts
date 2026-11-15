import { sumarSemanas } from '@/lib/objetivosAutogestion';
import type { DecisionObjetivo, MiniObjetivo, SemanaObjetivo } from '@/types/objetivos';

// El resumen del domingo en tres partes: lo que se cumplió, lo que no y lo nuevo que propone Sami. Se arma con lo que
// el servidor ya devuelve del cierre (decisiones, reajustes y mini-objetivos), sin números propios.

export type ResumenDelDomingo = {
  /** Lo que la persona hizo o lo que Sami midió como logrado. */
  cumplido: string[];
  /** Lo que no se hizo del todo, lo que no funcionó y lo que quedó sin contar. */
  noCumplido: { titulo: string; motivo: 'EN_PARTE' | 'NO_HECHO' | 'CAMBIADO' | 'SIN_RESPUESTA' }[];
  /** Lo que Sami propone para la semana que empieza. */
  nuevo: string[];
};

const sinRepetir = (textos: string[]) => [...new Set(textos)];

type Entrada = {
  /** El lunes de la semana que cerró el domingo. */
  semana: SemanaObjetivo;
  decisiones: DecisionObjetivo[];
  /** Los mini-objetivos en curso. */
  minis: MiniObjetivo[];
  /** Los títulos de los manuales que esperan respuesta. */
  sinResponder: string[];
};

export function armarResumenDelDomingo({
  semana,
  decisiones,
  minis,
  sinResponder,
}: Entrada): ResumenDelDomingo {
  const deLaSemana = decisiones.filter((decision) => decision.semanaInicio === semana.semana);
  const dichas = (tipo: DecisionObjetivo['tipo']) =>
    deLaSemana.filter((decision) => decision.tipo === tipo).map((decision) => decision.descripcion);
  const siguiente = sumarSemanas(semana.semana, 1);

  const cumplido = sinRepetir([
    ...dichas('MINI_HECHO'),
    ...semana.reajustes.filter((r) => r.tipo === 'SUBIR_META').map((r) => r.de.titulo),
  ]);
  const enParte = dichas('MINI_EN_PARTE');
  const noHechos = dichas('MINI_NO_HECHO');
  const cambiados = semana.reajustes.filter((r) => r.tipo !== 'SUBIR_META').map((r) => r.de.titulo);
  const noCumplido: ResumenDelDomingo['noCumplido'] = [
    ...enParte.map((titulo) => ({ titulo, motivo: 'EN_PARTE' as const })),
    ...noHechos.map((titulo) => ({ titulo, motivo: 'NO_HECHO' as const })),
    ...cambiados.map((titulo) => ({ titulo, motivo: 'CAMBIADO' as const })),
    ...sinResponder.map((titulo) => ({ titulo, motivo: 'SIN_RESPUESTA' as const })),
  ];

  const nuevosDelCierre = semana.reajustes.flatMap((r) => (r.a ? [r.a.titulo] : []));
  const nuevosEnCurso = minis
    .filter((mini) => mini.estado === 'EN_CURSO' && mini.semanaInicio >= siguiente)
    .map((mini) => mini.titulo);
  return { cumplido, noCumplido, nuevo: sinRepetir([...nuevosDelCierre, ...nuevosEnCurso]) };
}

export const estaVacio = (resumen: ResumenDelDomingo) =>
  resumen.cumplido.length === 0 && resumen.noCumplido.length === 0 && resumen.nuevo.length === 0;
