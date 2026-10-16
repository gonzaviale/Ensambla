import { useState } from 'react';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { useSumarPieza } from '@/hooks/negocio/useArmarSistema';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { pieza } from '@/lib/catalogo/piezas';
import { definicion, respuestasQueDesbloquea } from '@/lib/respuestas/definiciones';
import type { PiezaId } from '@/types/negocio';
import { rubroDelSistema } from './negocioUtil';

export type AvisoSumar = { tipo: 'exito' | 'error'; mensaje: string };

/**
 * «Sumar Gastos» desde Inicio y Respuestas: suma la pieza al sistema y avisa qué respuesta se
 * desbloqueó. `tope` es verdadero cuando el plan Gratis ya llegó a su máximo de bloques.
 */
export function useSumarBloque() {
  const negocio = useMiNegocio();
  const plan = usePlanActual();
  const sumarPieza = useSumarPieza(negocio.sistemaId ?? '');
  const [sumando, setSumando] = useState<PiezaId | null>(null);
  const [aviso, setAviso] = useState<AvisoSumar | null>(null);

  const limite = plan.definicion.bloques;
  const tope =
    !plan.incluye('bloques-ilimitados') &&
    limite !== null &&
    negocio.piezasPresentes.length >= limite;

  async function sumar(id: PiezaId) {
    if (sumando) return;
    const desbloqueadas = respuestasQueDesbloquea(id, negocio.piezasPresentes);
    setSumando(id);
    setAviso(null);
    try {
      await sumarPieza.mutateAsync({ id, rubro: rubroDelSistema(negocio.presentes) });
      const primera = desbloqueadas[0];
      setAviso({
        tipo: 'exito',
        mensaje: primera
          ? `Listo: ya podés ver ${definicion(primera).pregunta}`
          : `Listo: sumaste ${pieza(id).nombre}.`,
      });
    } catch {
      setAviso({ tipo: 'error', mensaje: 'No pudimos sumar el bloque. Probá de nuevo.' });
    } finally {
      setSumando(null);
    }
  }

  return { sumar, sumando, aviso, cerrarAviso: () => setAviso(null), tope };
}
