import { useState } from 'react';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { PREGUNTAS, type Pregunta } from '@/lib/preguntasDeSami';
import type { RespuestasDeSami } from '@/lib/respuestasDeSami';

type Parametros = {
  respuestas: RespuestasDeSami;
  onResponder: (respuestas: RespuestasDeSami) => void;
  onTerminar: () => void;
};

/** Sami pregunta de a una: cuál toca ahora, qué ya se contestó y cómo avanzar o volver. */
export function useConversacionConSami({ respuestas, onResponder, onTerminar }: Parametros) {
  const [indice, setIndice] = useState(0);
  const negocio = useConfiguracionNegocio().data;
  const localidad = negocio?.ubicacion?.localidad ?? negocio?.zona ?? 'tu local actual';
  const esLaUltima = indice === PREGUNTAS.length - 1;

  const responder = (pregunta: Pregunta, valor: string) => {
    onResponder({ ...respuestas, [pregunta.clave]: valor });
    if (esLaUltima) onTerminar();
    else setIndice(indice + 1);
  };

  return {
    numero: indice + 1,
    total: PREGUNTAS.length,
    actual: PREGUNTAS[indice],
    anteriores: PREGUNTAS.slice(0, indice),
    localidad,
    puedeVolver: indice > 0,
    volver: () => setIndice(indice - 1),
    responder,
  };
}
