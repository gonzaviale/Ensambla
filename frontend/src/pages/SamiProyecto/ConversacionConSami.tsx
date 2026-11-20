import type { FormEvent, RefObject } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { BurbujasDeConversacion } from '@/components/sistema/BurbujasDeConversacion';
import { Icon } from '@/components/sistema/Icon';
import { Sami } from '@/components/sistema/Sami';
import type { useDictado } from '@/hooks/sistemas/useDictado';
import type { useSamiProyecto } from '@/hooks/sistemas/useSamiProyecto';

type SamiDelProyecto = ReturnType<typeof useSamiProyecto>;
type Dictado = ReturnType<typeof useDictado>;

function CabeceraDeLaConversacion({ sami }: { sami: SamiDelProyecto }) {
  return (
    <header className="conversation-head">
      <Sami pose={sami.pensando ? 'think' : 'hello'} />
      <div>
        <h1>Armemos tu proyecto.</h1>
        <p>
          <span className="live-dot" />
          Sami · Tu guía
        </p>
      </div>
    </header>
  );
}

function MensajesDeLaConversacion({
  sami,
  referencia,
}: {
  sami: SamiDelProyecto;
  referencia: RefObject<HTMLDivElement | null>;
}) {
  return (
    <div className="conversation-scroll" ref={referencia}>
      <div className="conversation-messages" aria-live="polite">
        {sami.mensajes.map((mensaje, indice) => (
          <BurbujasDeConversacion key={indice} quien={mensaje.quien} texto={mensaje.texto} />
        ))}
        {sami.pensando && (
          <div className="thinking-bubble">
            <Sami pose="think" />
            <div>
              <strong>Estoy pensando qué te conviene…</strong>
              <span>
                <i />
                <i />
                <i />
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function RespuestasRapidas({ sami }: { sami: SamiDelProyecto }) {
  if (!sami.pregunta) return null;
  return (
    <div className="feedback-chips" role="group" aria-label="Respuestas rápidas">
      {sami.pregunta.opciones.map((opcion) => (
        <button
          key={opcion.id}
          type="button"
          disabled={sami.pensando}
          onClick={() => void sami.elegir(opcion)}
        >
          {opcion.texto}
        </button>
      ))}
    </div>
  );
}

type FormularioProps = {
  sami: SamiDelProyecto;
  entrada: string;
  onEntrada: (texto: string) => void;
  dictado: Dictado;
  onEnviar: (evento: FormEvent) => void;
};

function FormularioDeDescripcion({ sami, entrada, onEntrada, dictado, onEnviar }: FormularioProps) {
  return (
    <form className="guide-composer" onSubmit={onEnviar}>
      <label htmlFor="sami-descripcion">Describí tu negocio</label>
      <textarea
        id="sami-descripcion"
        placeholder="Vendo velas y anoto quién me debe…"
        maxLength={2500}
        disabled={sami.pensando}
        value={entrada}
        onChange={(evento) => onEntrada(evento.target.value)}
      />
      <div className="row between">
        <Btn
          cls={dictado.escuchando ? 'soft small' : 'ghost small'}
          aria-label={dictado.escuchando ? 'Dejar de dictar' : 'Dictar con la voz'}
          aria-pressed={dictado.escuchando}
          disabled={sami.pensando}
          onClick={dictado.alternar}
        >
          <Icon name="mic" />
        </Btn>
        <button className="btn primary" type="submit" disabled={sami.pensando}>
          Enviar a Sami <Icon name="arrow" />
        </button>
      </div>
    </form>
  );
}

function ReinicioDeLaConversacion({ sami }: { sami: SamiDelProyecto }) {
  return (
    <div className="feedback-chips">
      <button type="button" disabled={sami.creando} onClick={sami.reiniciar}>
        Empezar de nuevo
      </button>
    </div>
  );
}

type ConversacionProps = FormularioProps & { referencia: RefObject<HTMLDivElement | null> };

// El panel de la izquierda: lo que se dijeron Sami y la persona, y cómo seguir (responder, describir o reiniciar).
export function ConversacionConSami(props: ConversacionProps) {
  const { sami, referencia } = props;
  const conversando = !sami.propuesta && !sami.creado;
  return (
    <aside className="sami-conversation">
      <CabeceraDeLaConversacion sami={sami} />
      <MensajesDeLaConversacion sami={sami} referencia={referencia} />
      <RespuestasRapidas sami={sami} />
      {conversando && !sami.pregunta && <FormularioDeDescripcion {...props} />}
      {!conversando && <ReinicioDeLaConversacion sami={sami} />}
      <p className="conversation-foot">Vos decidís qué queda y cuándo está listo.</p>
    </aside>
  );
}
