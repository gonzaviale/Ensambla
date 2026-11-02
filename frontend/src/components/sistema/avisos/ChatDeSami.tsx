import { useEffect, useRef, useState, type FormEvent, type PointerEvent } from 'react';
import { useAbrirAvisos } from '@/hooks/avisos/useAbrirAvisos';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import type { Aviso } from '@/lib/sistema/alertas';
import {
  inicio,
  responder,
  type ContextoDeChat,
  type ItemDeChat,
  type Respuesta,
} from '@/lib/sistema/chatDeSami';
import { Icon } from '../Icon';

export type Mensaje = { id: number; rol: 'yo' | 'sami'; texto: string; respuesta?: Respuesta };

const PENSANDO_MS = 450;
const MARGEN = 8;
const ANCHO_DE_CELULAR = 640;
const entre = (valor: number, minimo: number, maximo: number) =>
  Math.min(Math.max(valor, minimo), Math.max(minimo, maximo));
let contador = 0;
const nuevo = (rol: Mensaje['rol'], texto: string, respuesta?: Respuesta): Mensaje => ({
  id: ++contador,
  rol,
  texto,
  respuesta,
});

type Props = {
  contexto: ContextoDeChat;
  mensajes: Mensaje[];
  onMensajes: (actualizar: (anteriores: Mensaje[]) => Mensaje[]) => void;
  onCerrar: () => void;
  onAyuda: () => void;
};

// El chat de Sami, chico y al costado: se le pregunta por el stock, las ventas o los avisos del cuaderno abierto.
export function ChatDeSami({ contexto, mensajes, onMensajes, onCerrar, onAyuda }: Props) {
  const [texto, setTexto] = useState('');
  const [pensando, setPensando] = useState(false);
  const lista = useRef<HTMLDivElement>(null);
  const campo = useRef<HTMLInputElement>(null);
  const temporizador = useRef<number | undefined>(undefined);
  const acciones = useAccionesDeAvisos();
  const abrirAvisos = useAbrirAvisos();
  const [expandido, setExpandido] = useState(false);
  // Dónde lo dejó la persona al arrastrarlo; sin arrastrar, queda en la esquina.
  const [posicion, setPosicion] = useState<{ x: number; y: number } | null>(null);
  const panel = useRef<HTMLElement>(null);
  const agarre = useRef<{ dx: number; dy: number } | null>(null);

  // Al abrirlo por primera vez Sami saluda y muestra lo más importante.
  useEffect(() => {
    if (mensajes.length) return;
    const bienvenida = inicio(contexto);
    onMensajes(() => [nuevo('sami', bienvenida.texto, bienvenida)]);
    // Sólo la primera vez: el saludo no se repite si cambian los avisos.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    campo.current?.focus();
    return () => window.clearTimeout(temporizador.current);
  }, []);

  useEffect(() => {
    lista.current?.scrollTo?.({ top: lista.current.scrollHeight, behavior: 'smooth' });
  }, [mensajes, pensando]);

  useEffect(() => {
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') onCerrar();
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, [onCerrar]);

  const preguntar = (pregunta: string) => {
    const limpia = pregunta.trim();
    if (!limpia || pensando) return;
    onMensajes((anteriores) => [...anteriores, nuevo('yo', limpia)]);
    setTexto('');
    setPensando(true);
    temporizador.current = window.setTimeout(() => {
      const respuesta = responder(limpia, contexto);
      onMensajes((anteriores) => [...anteriores, nuevo('sami', respuesta.texto, respuesta)]);
      setPensando(false);
    }, PENSANDO_MS);
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    preguntar(texto);
  };

  // Ir a lo que representa un aviso o un producto: el chat se guarda para que se vea lo marcado.
  const ver = (item: ItemDeChat) => {
    onCerrar();
    if (item.aviso) acciones.ver(item.aviso as Aviso);
    else if (item.destino) void acciones.irAlObjeto(item.destino);
  };

  // Se mueve agarrando la cabecera (no en el celular, donde ocupa el ancho, ni ampliado).
  const empezarArrastre = (evento: PointerEvent<HTMLElement>) => {
    if (expandido || window.innerWidth <= ANCHO_DE_CELULAR) return;
    if ((evento.target as HTMLElement).closest('button')) return;
    const caja = panel.current?.getBoundingClientRect();
    if (!caja) return;
    agarre.current = { dx: evento.clientX - caja.left, dy: evento.clientY - caja.top };
    evento.currentTarget.setPointerCapture?.(evento.pointerId);
  };
  const arrastrar = (evento: PointerEvent<HTMLElement>) => {
    const origen = agarre.current;
    const caja = panel.current?.getBoundingClientRect();
    if (!origen || !caja) return;
    setPosicion({
      x: entre(evento.clientX - origen.dx, MARGEN, window.innerWidth - caja.width - MARGEN),
      y: entre(evento.clientY - origen.dy, MARGEN, window.innerHeight - caja.height - MARGEN),
    });
  };
  const soltar = () => {
    agarre.current = null;
  };

  const ultima = [...mensajes].reverse().find((mensaje) => mensaje.rol === 'sami');
  const sugerencias = ultima?.respuesta?.sugerencias ?? [];

  return (
    <section
      ref={panel}
      className={`sami-chat ${expandido ? 'expandido' : ''}`.trim()}
      role="dialog"
      aria-label="Chat con Sami"
      style={
        !expandido && posicion
          ? { left: posicion.x, top: posicion.y, right: 'auto', bottom: 'auto' }
          : undefined
      }
    >
      <header
        className="sami-chat-cabeza"
        onPointerDown={empezarArrastre}
        onPointerMove={arrastrar}
        onPointerUp={soltar}
        onPointerCancel={soltar}
        onDoubleClick={(evento) => {
          if (!(evento.target as HTMLElement).closest('button')) setExpandido((actual) => !actual);
        }}
      >
        <img src="/mockup/sami-boton.webp" alt="" />
        <div>
          <strong>Sami</strong>
          <span>{contexto.sistema.name}</span>
        </div>
        <button
          type="button"
          className="sami-chat-ampliar"
          aria-label={expandido ? 'Achicar el chat' : 'Agrandar el chat'}
          aria-pressed={expandido}
          onClick={() => setExpandido((actual) => !actual)}
        >
          <Icon name="expand" />
        </button>
        <button type="button" aria-label="Cerrar el chat" onClick={onCerrar}>
          <Icon name="close" />
        </button>
      </header>
      <div className="sami-chat-mensajes" ref={lista} aria-live="polite">
        {mensajes.map((mensaje) =>
          mensaje.rol === 'yo' ? (
            <p key={mensaje.id} className="sami-chat-burbuja yo">
              {mensaje.texto}
            </p>
          ) : (
            <div key={mensaje.id} className="sami-chat-burbuja sami">
              <p>{mensaje.texto}</p>
              {mensaje.respuesta?.items && mensaje.respuesta.items.length > 0 && (
                <ul className="sami-chat-items">
                  {mensaje.respuesta.items.map((item) => (
                    <li key={item.id} className={item.nivel ? `p${item.nivel}` : ''}>
                      <div>
                        <strong>{item.titulo}</strong>
                        {item.detalle && <small>{item.detalle}</small>}
                      </div>
                      {(item.aviso || item.destino) && (
                        <button
                          type="button"
                          aria-label={`Ver: ${item.titulo}`}
                          onClick={() => ver(item)}
                        >
                          Ver <Icon name="arrow" />
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {mensaje.respuesta?.verTodos ? (
                <button
                  type="button"
                  className="sami-chat-todos"
                  onClick={() => {
                    onCerrar();
                    void abrirAvisos();
                  }}
                >
                  Ver los {mensaje.respuesta.verTodos} avisos <Icon name="arrow" />
                </button>
              ) : null}
            </div>
          ),
        )}
        {pensando && (
          <p className="sami-chat-burbuja sami pensando" aria-label="Sami está escribiendo">
            <span />
            <span />
            <span />
          </p>
        )}
      </div>
      {sugerencias.length > 0 && !pensando && (
        <div className="sami-chat-sugerencias">
          {sugerencias.map((sugerencia) => (
            <button key={sugerencia} type="button" onClick={() => preguntar(sugerencia)}>
              {sugerencia}
            </button>
          ))}
        </div>
      )}
      <form className="sami-chat-form" onSubmit={enviar}>
        <input
          ref={campo}
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          aria-label="Escribile a Sami"
          placeholder="Preguntale por stock, ventas…"
          autoComplete="off"
        />
        <button type="submit" aria-label="Enviar" disabled={!texto.trim() || pensando}>
          <Icon name="arrow" />
        </button>
      </form>
      <button type="button" className="sami-chat-ayuda" onClick={onAyuda}>
        <Icon name="help" /> ¿Una mano con esto?
      </button>
    </section>
  );
}
