import { useId } from 'react';
import { Link } from '@tanstack/react-router';
import { Panel, buttonClasses } from '@/components/ui';
import { TendenciaRespuesta } from '@/components/Respuestas/TendenciaRespuesta';
import type { RespuestaVista } from '@/types/negocio';

// Una respuesta lista, grande: la pregunta chica, el titular enorme y el detalle. Enlaza a /respuestas.
export function RespuestaGrande({ vista }: { vista: RespuestaVista }) {
  const idTitulo = useId();
  const { definicion, resultado } = vista;
  if (resultado.estado !== 'lista') return null;

  return (
    <Panel aria-labelledby={idTitulo} className="flex flex-col items-start gap-3">
      <h2 id={idTitulo} className="m-0 text-body font-semibold text-soft">
        {definicion.pregunta}
      </h2>
      <p className="m-0 text-h1 leading-tight font-bold break-words">{resultado.titular}</p>
      <p className="m-0 text-body text-soft">{resultado.detalle}</p>
      <TendenciaRespuesta tendencia={resultado.tendencia} />
      <Link
        to="/respuestas"
        aria-label={`Ver detalle: ${definicion.pregunta}`}
        className={buttonClasses({ variant: 'ghost', size: 'compact', className: 'mt-auto' })}
      >
        Ver detalle
      </Link>
    </Panel>
  );
}

// Está el bloque pero todavía no hay nada cargado.
export function RespuestaSinDatos({ pregunta }: { pregunta: string }) {
  const idTitulo = useId();
  return (
    <Panel flat aria-labelledby={idTitulo} className="flex flex-col items-start gap-3">
      <h2 id={idTitulo} className="m-0 text-body font-semibold text-soft">
        {pregunta}
      </h2>
      <p className="m-0 text-lead font-semibold">Todavía no hay datos: contanos qué pasó</p>
      <p className="m-0 text-body text-soft">
        Con lo primero que cuentes, acá aparece tu respuesta.
      </p>
      <Link to="/subir" className={buttonClasses({ variant: 'primary' })}>
        Contar qué pasó
      </Link>
    </Panel>
  );
}
