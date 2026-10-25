import { useEffect, useRef, useState, type PointerEvent } from 'react';
import { createPortal } from 'react-dom';
import { useAbrirAvisos } from '@/hooks/avisos/useAbrirAvisos';
import type { Aviso } from '@/lib/sistema/alertas';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { ICONO_DE_AVISO } from './presentacion';

const NIVEL = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;

// Cuánto hay que deslizar un aviso (en píxeles) para sacarlo del menú.
const DESLIZAR_PARA_OCULTAR = 80;

// La campana de avisos del encabezado: el número y el color dicen cuántos hay y si alguno es urgente.
// Al tocarla se abre un panel desde la derecha con un resumen de los avisos. Deslizar uno lo saca del menú, pero sigue en la
// pantalla de avisos: descartar de verdad se hace sólo allá.
export function Campana() {
  const avisos = useSistemaStore((estado) => estado.avisos);
  const ocultos = useSistemaStore((estado) => estado.ocultosDeCampana);
  const ocultar = useSistemaStore((estado) => estado.ocultarDeCampana);
  const bloqueados = useSistemaStore((estado) => estado.accesoAvisos?.bloqueadas ?? 0);
  const abrirAvisos = useAbrirAvisos();
  const [abierto, setAbierto] = useState(false);
  const cerrarPanel = useRef<HTMLButtonElement>(null);
  const boton = useRef<HTMLButtonElement>(null);
  const visibles = avisos.filter((aviso) => !ocultos.includes(aviso.id));
  const total = visibles.length + bloqueados;
  const urgente = visibles.some((aviso) => aviso.prioridad >= 2);

  // Se cierra tocando el fondo, con la ✕ o con Escape; al cerrar, el foco vuelve a la campana.
  const cerrar = () => {
    setAbierto(false);
    boton.current?.focus();
  };
  useEffect(() => {
    if (!abierto) return;
    cerrarPanel.current?.focus();
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') cerrar();
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, [abierto]);

  const ir = (foco: string | null) => {
    setAbierto(false);
    void abrirAvisos(foco);
  };

  return (
    <>
      <button
        ref={boton}
        type="button"
        className={`alert-bell ${urgente ? 'urgente' : ''}`.trim()}
        aria-label={`Avisos de Sami: ${total}${bloqueados ? `, ${bloqueados} disponibles con Premium` : ''}`}
        aria-expanded={abierto}
        aria-controls={abierto ? 'menu-de-la-campana' : undefined}
        title="Avisos de Sami"
        onClick={() => setAbierto((actual) => !actual)}
      >
        <Icon name="bell" />
        {total > 0 && <span className="alert-badge">{total > 9 ? '9+' : total}</span>}
      </button>
      {/* El panel va al final de `.mk-root` (donde aplican los estilos del sistema), fuera del encabezado. */}
      {abierto &&
        createPortal(
          <>
            <div className="campana-fondo" aria-hidden="true" onClick={cerrar} />
            <div
              id="menu-de-la-campana"
              className="campana-menu"
              role="dialog"
              aria-label="Avisos recientes"
            >
              <button
                ref={cerrarPanel}
                type="button"
                className="campana-cerrar"
                aria-label="Cerrar los avisos"
                onClick={cerrar}
              >
                <Icon name="close" />
              </button>
              <div className="campana-menu-cabeza">
                <strong>
                  Avisos de Sami
                  {visibles.length > 0 && <span className="campana-cuenta">{visibles.length}</span>}
                </strong>
                {visibles.length > 0 && <span>Deslizá un aviso para sacarlo de acá</span>}
              </div>
              {visibles.length > 0 ? (
                <ul className="campana-lista">
                  {visibles.map((aviso) => (
                    <AvisoDeLaCampana
                      key={aviso.id}
                      aviso={aviso}
                      onAbrir={() => ir(aviso.id)}
                      onOcultar={() => ocultar(aviso.id)}
                    />
                  ))}
                </ul>
              ) : (
                <p className="campana-vacia">No tenés avisos nuevos.</p>
              )}
              {bloqueados > 0 && (
                <p className="campana-premium">
                  {bloqueados === 1 ? 'Hay 1 aviso más' : `Hay ${bloqueados} avisos más`} con
                  Premium.
                </p>
              )}
              <div className="campana-pie">
                <img src="/mockup/sami-detective.webp" alt="" />
                <div>
                  <p>¿Querés el detalle? Te lo cuento en Avisos.</p>
                  <button type="button" className="campana-ver-todos" onClick={() => ir(null)}>
                    Ver todos los avisos <Icon name="arrow" />
                  </button>
                </div>
              </div>
            </div>
          </>,
          boton.current?.closest('.mk-root') ?? document.body,
        )}
    </>
  );
}

type PropsAviso = { aviso: Aviso; onAbrir: () => void; onOcultar: () => void };

// Un aviso resumido: ícono, título y de qué cuaderno es. Se desliza hacia un costado para ocultarlo.
function AvisoDeLaCampana({ aviso, onAbrir, onOcultar }: PropsAviso) {
  const inicio = useRef<number | null>(null);
  const movido = useRef(false);
  const [desplazamiento, setDesplazamiento] = useState(0);
  const [saliendo, setSaliendo] = useState(false);

  const alApoyar = (evento: PointerEvent<HTMLLIElement>) => {
    if (evento.pointerType === 'mouse' && evento.button !== 0) return;
    inicio.current = evento.clientX;
    movido.current = false;
  };
  const alMover = (evento: PointerEvent<HTMLLIElement>) => {
    if (inicio.current === null) return;
    const dx = evento.clientX - inicio.current;
    if (Math.abs(dx) > 6 && !movido.current) {
      movido.current = true;
      evento.currentTarget.setPointerCapture?.(evento.pointerId);
    }
    if (movido.current) setDesplazamiento(dx);
  };
  const alSoltar = () => {
    if (inicio.current === null) return;
    inicio.current = null;
    if (Math.abs(desplazamiento) >= DESLIZAR_PARA_OCULTAR) {
      setSaliendo(true);
      setDesplazamiento(desplazamiento > 0 ? 400 : -400);
      window.setTimeout(onOcultar, 180);
    } else {
      setDesplazamiento(0);
    }
  };

  return (
    <li
      className={`campana-aviso p${aviso.prioridad} ${saliendo ? 'saliendo' : ''}`.trim()}
      style={desplazamiento ? { transform: `translateX(${desplazamiento}px)` } : undefined}
      onPointerDown={alApoyar}
      onPointerMove={alMover}
      onPointerUp={alSoltar}
      onPointerCancel={alSoltar}
    >
      <button
        type="button"
        className="campana-aviso-abrir"
        onClick={(evento) => {
          // Un deslizamiento no cuenta como toque.
          if (movido.current) {
            evento.preventDefault();
            movido.current = false;
            return;
          }
          onAbrir();
        }}
      >
        <span className="campana-aviso-icono" aria-hidden="true">
          <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
        </span>
        <span className="campana-aviso-texto">
          <span className="campana-aviso-nivel">{NIVEL[aviso.prioridad]}</span>
          <strong>{aviso.titulo}</strong>
          <span className="campana-aviso-sistema">{aviso.sistema}</span>
        </span>
      </button>
      <button
        type="button"
        className="campana-aviso-ocultar"
        aria-label={`Sacar de acá: ${aviso.titulo}`}
        title="Sacar de acá (sigue en Avisos)"
        onClick={onOcultar}
      >
        <Icon name="close" />
      </button>
    </li>
  );
}
