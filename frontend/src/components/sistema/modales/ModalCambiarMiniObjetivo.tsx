import { ArrowsClockwiseIcon } from '@phosphor-icons/react';
import { Btn } from '../Btn';
import { Modal } from '../Modal';
import { varsDeColor } from '../colores';
import { useReemplazarMiniObjetivo } from '@/hooks/objetivos/mutations';
import { useCandidatos } from '@/hooks/objetivos/queries';
import { formatoPesos } from '@/lib/objetivos';
import { QUE_SE_MIDE, valorDeMini } from '@/lib/objetivosAutogestion';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MiniObjetivo } from '@/types/objetivos';

type Props = {
  objetivoId: string;
  mini: MiniObjetivo;
};

// Las alternativas que Sami calcula para el lugar de un mini-objetivo; la persona elige una o se queda con el que tiene.
// Usa el mismo modal que «Quitar» (abrirModal({ tipo: 'cambiarMini' })).
export function ModalCambiarMiniObjetivo({ objetivoId, mini }: Props) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const candidatos = useCandidatos(objetivoId, mini.id);
  const reemplazar = useReemplazarMiniObjetivo(objetivoId);

  return (
    <Modal clase="cambiar-mini">
      <div className="icon-tile large" style={varsDeColor('blue')}>
        <ArrowsClockwiseIcon aria-hidden size={24} weight="bold" />
      </div>
      <h2 id="modal-title">Cambiar «{mini.titulo}»</h2>
      <div className="cambiar-mini-actual">
        <span>Lo que tenés ahora</span>
        <strong>{mini.titulo}</strong>
        <p>
          Hoy: {valorDeMini(mini.tipo, mini.valorActual)} · Meta:{' '}
          {valorDeMini(mini.tipo, mini.meta)}
        </p>
      </div>
      <p>Sami te propone estos en su lugar. El que tenías queda en tu historial.</p>
      {candidatos.isPending ? (
        <p role="status">Sami está buscando alternativas…</p>
      ) : candidatos.isError ? (
        <p role="alert">No pudimos traer las alternativas. Probá de nuevo en un rato.</p>
      ) : candidatos.data.length === 0 ? (
        <p>Por ahora no hay otra alternativa con lo que cargaste en tus cuadernos.</p>
      ) : (
        <ul className="cambiar-mini-lista">
          {candidatos.data.map((candidato) => (
            <li key={`${candidato.tipo}-${candidato.registroId ?? candidato.titulo}`}>
              <div>
                <span className="cambiar-mini-tipo">{QUE_SE_MIDE[candidato.tipo]}</span>
                <strong>{candidato.titulo}</strong>
                <p>{candidato.justificacion}</p>
                <p className="cambiar-mini-meta">
                  Meta: {valorDeMini(candidato.tipo, candidato.meta)}
                  {candidato.aporteEstimado > 0 && (
                    <span className="objetivos-paso-efecto">
                      +{formatoPesos(candidato.aporteEstimado)} por mes
                    </span>
                  )}
                </p>
              </div>
              <Btn
                cls="primary"
                disabled={reemplazar.isPending}
                onClick={() =>
                  reemplazar.mutate({ miniId: mini.id, candidato }, { onSuccess: cerrarModal })
                }
              >
                Elegir este
              </Btn>
            </li>
          ))}
        </ul>
      )}
      {reemplazar.isError && (
        <p role="alert">No pudimos hacer el cambio. Probá de nuevo en un rato.</p>
      )}
      <div className="modal-footer">
        <Btn cls="secondary" onClick={cerrarModal}>
          Me quedo con el que tengo
        </Btn>
      </div>
    </Modal>
  );
}
