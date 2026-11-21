import { CheckCircleIcon } from '@phosphor-icons/react';
import {
  PASOS,
  useRespuestaPorPasos,
  type Paso,
  type RespuestaPorPasos,
} from '@/hooks/objetivos/useRespuestaPorPasos';
import type { MiniManual } from '@/types/objetivos';
import type { ComponentType } from 'react';
import { PaginaComentario, PaginaHizo, PaginaQuePaso } from './PaginasDeRespuesta';

type Props = { objetivoId: string; mini: MiniManual };

const PAGINAS_DE_RESPUESTA: Record<Paso, ComponentType<{ respuesta: RespuestaPorPasos }>> = {
  hizo: PaginaHizo,
  quePaso: PaginaQuePaso,
  comentario: PaginaComentario,
};

// «¿Lo hiciste?», «¿Qué pasó?» y, si quiere, una línea: una pregunta por página, siempre en el mismo espacio para que
// la tarjeta no cambie de tamaño, y con «Volver» para corregir. Botones grandes para usar con el pulgar.
export function RespuestaRapida({ objetivoId, mini }: Props) {
  const respuesta = useRespuestaPorPasos(objetivoId, mini);
  if (respuesta.enviada) return <YaSeLoContaste />;
  const Pagina = PAGINAS_DE_RESPUESTA[respuesta.paso];
  return (
    <form
      className="respuesta-rapida"
      aria-label={`Contale a Sami cómo te fue con «${mini.titulo}»`}
      onSubmit={(evento) => {
        evento.preventDefault();
        respuesta.enviar();
      }}
    >
      <div className="respuesta-pagina">
        <Pagina respuesta={respuesta} />
        <ErrorDeRespuesta respuesta={respuesta} />
      </div>
      <PieDeRespuesta respuesta={respuesta} />
    </form>
  );
}

function YaSeLoContaste() {
  return (
    <p className="mini-objetivo-decidido si" role="status">
      <CheckCircleIcon aria-hidden size={16} weight="bold" /> Ya se lo contaste a Sami
    </p>
  );
}

function ErrorDeRespuesta({ respuesta }: { respuesta: RespuestaPorPasos }) {
  const { error } = respuesta;
  if (!error) return null;
  return (
    <p className="input-hint" role="alert">
      {error.campos.comentario ?? error.mensaje}
    </p>
  );
}

function PieDeRespuesta({ respuesta }: { respuesta: RespuestaPorPasos }) {
  return (
    <footer className="respuesta-pie">
      <BotonVolver respuesta={respuesta} />
      <span className="respuesta-progreso" aria-hidden>
        {PASOS.map((paso, posicion) => (
          <i key={paso} data-hecho={posicion <= respuesta.indice} />
        ))}
      </span>
      <span className="sr-only">
        Paso {respuesta.indice + 1} de {PASOS.length}
      </span>
      <BotonEnviar respuesta={respuesta} />
    </footer>
  );
}

function BotonVolver({ respuesta }: { respuesta: RespuestaPorPasos }) {
  if (respuesta.indice === 0) return null;
  return (
    <button type="button" className="btn ghost" onClick={respuesta.volver}>
      Volver
    </button>
  );
}

function BotonEnviar({ respuesta }: { respuesta: RespuestaPorPasos }) {
  if (respuesta.paso !== 'comentario') return null;
  return (
    <button type="submit" className="btn primary" disabled={!respuesta.sePuedeEnviar}>
      {respuesta.enviando ? 'Enviando…' : 'Contarle a Sami'}
    </button>
  );
}
