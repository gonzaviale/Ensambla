import { useMemo, useState } from 'react';
import { nombreDePieza } from '@/lib/catalogo/piezas';
import { ensamblar } from '@/lib/ensamblador';
import { definicion, respuestasQueDesbloquea } from '@/lib/respuestas/definiciones';
import type { Ensamblado, PiezaId, RespuestaId } from '@/types/negocio';

export type RespuestaDesbloqueada = {
  id: RespuestaId;
  pregunta: string;
  /** El bloque que, al sumarse, la dejó lista. */
  bloque: string;
};

// Frases de ejemplo de la demo: cada una cuenta un negocio distinto.
export const EJEMPLOS_RELATO = [
  'Vendo viandas por encargo y cobro una seña',
  'Tengo un almacén y a veces fío a los vecinos',
  'Hago tortas y anoto lo que gasto en ingredientes',
] as const;

/**
 * Estado de la demo «Se ensambla solo» de la landing: lo que la persona cuenta → piezas con su
 * motivo → respuestas que se desbloquean. Es todo local: no llama a la API.
 */
export function useEnsamblarRelato(inicial: string = EJEMPLOS_RELATO[0]) {
  const [texto, setTexto] = useState<string>(inicial);

  const ensamblado: Ensamblado | null = useMemo(
    () => (texto.trim() === '' ? null : ensamblar(texto)),
    [texto],
  );

  // Se suman las piezas de a una, en el orden en que se acomodan, y se anota qué respuesta
  // deja lista cada una.
  const desbloqueadas: RespuestaDesbloqueada[] = useMemo(() => {
    if (!ensamblado) return [];
    const presentes: PiezaId[] = [];
    const resultado: RespuestaDesbloqueada[] = [];
    for (const { id: pieza } of ensamblado.piezas) {
      for (const id of respuestasQueDesbloquea(pieza, presentes)) {
        resultado.push({
          id,
          pregunta: definicion(id).pregunta,
          bloque: nombreDePieza(pieza, ensamblado.rubro),
        });
      }
      presentes.push(pieza);
    }
    return resultado;
  }, [ensamblado]);

  return { texto, setTexto, ensamblado, desbloqueadas };
}
