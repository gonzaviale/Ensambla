import { useEffect, useRef, useState } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';
import { hoy } from '@/lib/sistema/formato';
import { ChatDeSami, type Mensaje } from './ChatDeSami';

// El Sami de la esquina: muestra cuántos avisos tiene la sección y, al tocarlo, abre un chat chico para preguntarle.
export function SamiFlotante({ sistemaId }: { sistemaId: string }) {
  const sistema = useSistemaStore((estado) =>
    estado.sistemas.find((item) => item.id === sistemaId),
  );
  const avisos = useSistemaStore((estado) => estado.avisos).filter(
    (aviso) => aviso.sistemaId === sistemaId,
  );
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [abierto, setAbierto] = useState(false);
  // La charla se conserva mientras la sección esté abierta, aunque se cierre el panel.
  const [mensajes, setMensajes] = useState<Mensaje[]>([]);
  const boton = useRef<HTMLButtonElement>(null);
  const abiertoAntes = useRef(false);
  const cantidad = avisos.length;
  const urgente = avisos.some((aviso) => aviso.prioridad === 3);

  // Al cerrar el chat vuelve el ícono y recupera el foco.
  useEffect(() => {
    if (abiertoAntes.current && !abierto) boton.current?.focus();
    abiertoAntes.current = abierto;
  }, [abierto]);

  if (!sistema) return null;
  return (
    <>
      {abierto && (
        <ChatDeSami
          contexto={{ sistema, avisos, hoy: hoy() }}
          mensajes={mensajes}
          onMensajes={(actualizar) => setMensajes(actualizar)}
          onCerrar={() => setAbierto(false)}
          onAyuda={() => {
            setAbierto(false);
            abrirModal({ tipo: 'ayuda' });
          }}
        />
      )}
      {!abierto && (
        <button
          ref={boton}
          type="button"
          className="sami-float"
          aria-label={
            cantidad
              ? `Hablar con Sami: ${cantidad === 1 ? '1 aviso' : `${cantidad} avisos`} en esta sección`
              : 'Hablar con Sami'
          }
          aria-expanded={abierto}
          title="Hablar con Sami"
          onClick={() => setAbierto((actual) => !actual)}
        >
          <img className="sami-float-img" src="/mockup/sami-boton.webp" alt="" />
          {cantidad > 0 && (
            <span
              className={`sami-float-numero ${urgente ? 'urgente' : ''}`.trim()}
              aria-hidden="true"
            >
              {cantidad > 99 ? '99+' : cantidad}
            </span>
          )}
        </button>
      )}
    </>
  );
}
