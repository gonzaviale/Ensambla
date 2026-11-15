import { useEffect, useLayoutEffect, useRef, type KeyboardEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { MAXIMO_DEL_MENSAJE } from '@/lib/objetivosSami';
import type { MensajeDeConversacion } from '@/types/objetivos';

const EJEMPLOS = [
  'Quiero vender más yerba',
  'Quiero que más gente conozca mi local',
  'Quiero gastar menos en luz',
] as const;

const ALTO_MAXIMO_DEL_CAMPO = 120;
const SALUDO = 'Hola, soy Sami. Contame qué querés lograr con tu negocio y lo armamos juntos.';

type Props = {
  mensajes: MensajeDeConversacion[];
  /** Lo último que la persona mandó y todavía no tiene respuesta. */
  pendiente: string | null;
  texto: string;
  onTexto: (texto: string) => void;
  onEnviar: () => void;
  enviando: boolean;
  puedeSeguirHablando: boolean;
  yaHayPropuesta: boolean;
};

const esEnterSinSalto = (evento: KeyboardEvent<HTMLTextAreaElement>) =>
  evento.key === 'Enter' && !evento.shiftKey && !evento.nativeEvent.isComposing;

// El campo crece con lo que se escribe hasta un tope; scrollHeight no cuenta el borde y sin sumarlo muestra flechas.
function useCampoQueCrece(texto: string) {
  const campo = useRef<HTMLTextAreaElement>(null);
  useLayoutEffect(() => {
    const area = campo.current;
    if (!area) return;
    area.style.height = 'auto';
    const bordes = area.offsetHeight - area.clientHeight;
    area.style.height = `${Math.min(area.scrollHeight + bordes, ALTO_MAXIMO_DEL_CAMPO)}px`;
  }, [texto]);
  return campo;
}

function useBajarAlUltimoMensaje(cantidad: number, hayPendiente: boolean) {
  const lista = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (cantidad === 0 && !hayPendiente) return;
    lista.current?.scrollTo?.({ top: lista.current.scrollHeight, behavior: 'smooth' });
  }, [cantidad, hayPendiente]);
  return lista;
}

function CabezaDelChat() {
  return (
    <header className="sami-chat-cabeza">
      <img src="/mockup/sami-constructora.webp" alt="" />
      <div>
        <p className="sami-chat-estado">
          <strong>Sami</strong>
          <span>
            <span className="live-dot" />
            Conectado
          </span>
        </p>
        <span className="sami-chat-lema">Construyamos juntos</span>
      </div>
    </header>
  );
}

function Burbuja({ quien, texto }: { quien: 'sami' | 'yo'; texto: string }) {
  return (
    <li className={`sami-chat-burbuja ${quien}`}>
      <p>{texto}</p>
    </li>
  );
}

function SamiEscribiendo() {
  return (
    <li
      className="sami-chat-burbuja sami pensando"
      role="status"
      aria-label="Sami está escribiendo"
    >
      <span />
      <span />
      <span />
    </li>
  );
}

function HiloDeLaConversacion({ mensajes, pendiente }: Pick<Props, 'mensajes' | 'pendiente'>) {
  const esElPrimerMomento = mensajes.length === 0 && pendiente === null;
  return (
    <ol className="sami-hilo" aria-label="Conversación con Sami">
      {esElPrimerMomento && <Burbuja quien="sami" texto={SALUDO} />}
      {mensajes.map((mensaje, orden) => (
        <Burbuja
          key={`${orden}-${mensaje.rol}`}
          quien={mensaje.rol === 'SAMI' ? 'sami' : 'yo'}
          texto={mensaje.texto}
        />
      ))}
      {pendiente !== null && <Burbuja quien="yo" texto={pendiente} />}
      {pendiente !== null && <SamiEscribiendo />}
    </ol>
  );
}

function EjemplosParaEmpezar({ onElegir }: { onElegir: (ejemplo: string) => void }) {
  return (
    <div className="sami-chat-sugerencias">
      {EJEMPLOS.map((ejemplo) => (
        <button type="button" key={ejemplo} onClick={() => onElegir(ejemplo)}>
          {ejemplo}
        </button>
      ))}
    </div>
  );
}

function BarraDeEscritura(
  props: Pick<Props, 'texto' | 'onTexto' | 'onEnviar' | 'enviando' | 'yaHayPropuesta'>,
) {
  const { texto, onTexto, onEnviar, enviando, yaHayPropuesta } = props;
  const campo = useCampoQueCrece(texto);
  const alTeclear = (evento: KeyboardEvent<HTMLTextAreaElement>) => {
    if (!esEnterSinSalto(evento)) return;
    evento.preventDefault();
    onEnviar();
  };
  return (
    <form
      className="sami-chat-form"
      onSubmit={(evento) => {
        evento.preventDefault();
        onEnviar();
      }}
    >
      <textarea
        ref={campo}
        rows={1}
        value={texto}
        maxLength={MAXIMO_DEL_MENSAJE}
        aria-label={yaHayPropuesta ? '¿Querés cambiar algo?' : 'Contale tu objetivo a Sami'}
        placeholder={
          yaHayPropuesta
            ? 'Ej.: Prefiero algo para el local'
            : 'Ej.: Quiero que más gente conozca mi local'
        }
        onChange={(evento) => onTexto(evento.target.value)}
        onKeyDown={alTeclear}
      />
      <button type="submit" aria-label="Enviar" disabled={!texto.trim() || enviando}>
        <Icon name="arrow" />
      </button>
    </form>
  );
}

function PieDelChat(props: Props) {
  if (!props.puedeSeguirHablando) {
    return (
      <p className="input-hint sami-chat-tope">
        Llegaste al máximo de mensajes de esta conversación. Guardá lo que armaste.
      </p>
    );
  }
  const mostrarEjemplos = !props.yaHayPropuesta && props.pendiente === null;
  return (
    <>
      {mostrarEjemplos && <EjemplosParaEmpezar onElegir={props.onTexto} />}
      <BarraDeEscritura {...props} />
    </>
  );
}

// El mismo panel del chat de Sami, pero dentro de la página: se le cuenta el objetivo y contesta.
export function ChatDeObjetivos(props: Props) {
  const lista = useBajarAlUltimoMensaje(props.mensajes.length, props.pendiente !== null);
  return (
    <section className="sami-chat integrado" aria-label="Chat con Sami">
      <CabezaDelChat />
      <div className="sami-chat-mensajes" ref={lista}>
        <HiloDeLaConversacion mensajes={props.mensajes} pendiente={props.pendiente} />
      </div>
      <PieDelChat {...props} />
    </section>
  );
}
