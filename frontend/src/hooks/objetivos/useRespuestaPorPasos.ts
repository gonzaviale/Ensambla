import { useState } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import { quePasoPosible } from '@/lib/respuestaManual';
import type { Hizo, MiniManual, QuePaso } from '@/types/objetivos';
import { useResponderMiniObjetivo } from './mutations';

export const PASOS = ['hizo', 'quePaso', 'comentario'] as const;
export type Paso = (typeof PASOS)[number];

const quePasoQueSigueValiendo = (hizo: Hizo, quePaso: QuePaso | null) =>
  quePaso && quePasoPosible(hizo, quePaso) ? quePaso : null;

/** La respuesta de un mini-objetivo manual contada de a una pregunta por vez, con la posibilidad de volver. */
export function useRespuestaPorPasos(objetivoId: string, mini: MiniManual) {
  const responder = useResponderMiniObjetivo(objetivoId);
  const [indice, setIndice] = useState(0);
  const [hizo, setHizo] = useState<Hizo | null>(null);
  const [quePaso, setQuePaso] = useState<QuePaso | null>(null);
  const [comentario, setComentario] = useState('');

  const avanzar = () => setIndice((actual) => Math.min(actual + 1, PASOS.length - 1));
  const volver = () => setIndice((actual) => Math.max(actual - 1, 0));

  const elegirHizo = (elegido: Hizo) => {
    setHizo(elegido);
    setQuePaso(quePasoQueSigueValiendo(elegido, quePaso));
    avanzar();
  };

  const elegirQuePaso = (elegido: QuePaso) => {
    setQuePaso(elegido);
    avanzar();
  };

  const enviar = () => {
    if (!hizo || !quePaso || responder.isPending) return;
    responder.mutate({ miniId: mini.id, respuesta: { hizo, quePaso, comentario } });
  };

  return {
    paso: PASOS[indice] ?? 'hizo',
    indice,
    hizo,
    quePaso,
    comentario,
    cambiarComentario: setComentario,
    elegirHizo,
    elegirQuePaso,
    volver,
    enviar,
    sePuedeEnviar: hizo !== null && quePaso !== null && !responder.isPending,
    enviando: responder.isPending,
    enviada: responder.isSuccess,
    error: leerErrorServidor(responder.error),
  };
}

export type RespuestaPorPasos = ReturnType<typeof useRespuestaPorPasos>;
