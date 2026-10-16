import { useCallback, useEffect, useId, useRef } from 'react';
import { useNavigate, useRouterState } from '@tanstack/react-router';
import { LightbulbIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { normalizarRuta, selectorDeAncla, type PasoRecorrido } from '@/lib/recorrido/pasos';

const CONSULTA_TARJETA_FLOTANTE = '(min-width: 640px)';
const MARGEN = 16;
const SEPARACION = 18;
const PERIODO_BUSQUEDA = 250;

/** El elemento que se resalta en la pantalla real: el del ancla del paso o, si falta, el alternativo. */
function buscarAncla(paso: PasoRecorrido): Element | null {
  for (const ancla of [paso.ancla, paso.alternativa]) {
    if (!ancla) continue;
    const elemento = document.querySelector(selectorDeAncla(ancla));
    if (elemento) return elemento;
  }
  return null;
}

function llevarALaVista(elemento: Element, escritorio: boolean) {
  if (typeof elemento.scrollIntoView !== 'function') return;
  const el = elemento as HTMLElement;
  const previo = el.style.scrollMargin;
  // Deja lugar para la barra superior; la posición se lee en el momento de pedir el desplazamiento.
  el.style.scrollMargin = '88px';
  const reducir =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  el.scrollIntoView({
    block: escritorio ? 'center' : 'start',
    behavior: reducir ? 'auto' : 'smooth',
  });
  el.style.scrollMargin = previo;
}

// El recorrido guiado: una tarjeta flotante (no modal) que explica la pantalla real y resalta el
// elemento del que habla. Nunca bloquea: si el elemento no aparece, la tarjeta queda centrada.
export function RecorridoGuiado() {
  const { activo, actual, paso, total, esUltimo, siguiente, anterior, cerrar } = useRecorrido();
  if (!activo || !actual) return null;
  return (
    <Guia
      actual={actual}
      paso={paso}
      total={total}
      esUltimo={esUltimo}
      onSiguiente={siguiente}
      onAnterior={anterior}
      onCerrar={cerrar}
    />
  );
}

type GuiaProps = {
  actual: PasoRecorrido;
  paso: number;
  total: number;
  esUltimo: boolean;
  onSiguiente: () => void;
  onAnterior: () => void;
  onCerrar: () => void;
};

function Guia({ actual, paso, total, esUltimo, onSiguiente, onAnterior, onCerrar }: GuiaProps) {
  const navegar = useNavigate();
  const pathname = useRouterState({ select: (estado) => estado.location.pathname });
  const enRuta = normalizarRuta(pathname) === normalizarRuta(actual.ruta);
  const escritorio = useMediaQuery(CONSULTA_TARJETA_FLOTANTE);
  const tarjeta = useRef<HTMLDivElement>(null);
  const anillo = useRef<HTMLDivElement>(null);
  const tituloId = useId();
  const textoId = useId();
  const alCerrar = useRef(onCerrar);
  useEffect(() => {
    alCerrar.current = onCerrar;
  });

  // Si el paso es de otra pantalla, se navega una sola vez (si algo la redirige, no se insiste).
  const pasoNavegado = useRef<string | null>(null);
  useEffect(() => {
    if (enRuta || pasoNavegado.current === actual.id) return;
    pasoNavegado.current = actual.id;
    void navegar({ to: actual.ruta });
  }, [enRuta, actual.id, actual.ruta, navegar]);

  // Resalta el elemento y acomoda la tarjeta; se repite mientras la pantalla carga o cambia.
  const acomodar = useCallback(
    (elemento: Element | null) => {
      const caja = elemento?.getBoundingClientRect();
      const visible = caja && (caja.width > 0 || caja.height > 0) ? caja : null;

      const resaltado = anillo.current;
      if (resaltado) {
        if (visible) {
          resaltado.style.display = 'block';
          resaltado.style.top = `${visible.top - 6}px`;
          resaltado.style.left = `${visible.left - 6}px`;
          resaltado.style.width = `${visible.width + 12}px`;
          resaltado.style.height = `${visible.height + 12}px`;
        } else {
          resaltado.style.display = 'none';
        }
      }

      const flotante = tarjeta.current;
      if (!flotante) return;
      if (!escritorio) {
        // En el celular la tarjeta va fija abajo, encima de la barra inferior (lo resuelven las clases).
        flotante.style.top = '';
        flotante.style.left = '';
        return;
      }
      const anchoPantalla = document.documentElement.clientWidth || window.innerWidth;
      const altoPantalla = window.innerHeight;
      const ancho = flotante.offsetWidth;
      const alto = flotante.offsetHeight;
      const maxIzquierda = Math.max(MARGEN, anchoPantalla - ancho - MARGEN);
      let top: number;
      let left: number;
      if (!visible) {
        top = Math.max(MARGEN, (altoPantalla - alto) / 2);
        left = Math.max(MARGEN, (anchoPantalla - ancho) / 2);
      } else if (visible.bottom + SEPARACION + alto <= altoPantalla - MARGEN) {
        top = visible.bottom + SEPARACION;
        left = Math.min(Math.max(visible.left, MARGEN), maxIzquierda);
      } else if (visible.top - SEPARACION - alto >= MARGEN) {
        top = visible.top - SEPARACION - alto;
        left = Math.min(Math.max(visible.left, MARGEN), maxIzquierda);
      } else {
        // El elemento ocupa casi toda la pantalla: la tarjeta va en la esquina, encima.
        top = Math.max(MARGEN, altoPantalla - alto - MARGEN);
        left = maxIzquierda;
      }
      flotante.style.top = `${top}px`;
      flotante.style.left = `${left}px`;
    },
    [escritorio],
  );

  useEffect(() => {
    if (!enRuta) {
      acomodar(null);
      return;
    }
    let resaltado: Element | null = null;
    const actualizar = () => {
      const elemento = buscarAncla(actual);
      if (elemento && elemento !== resaltado) llevarALaVista(elemento, escritorio);
      resaltado = elemento;
      acomodar(elemento);
    };
    actualizar();
    // La pantalla puede tardar en dibujar el elemento: se vuelve a buscar sin bloquear nada.
    const intervalo = window.setInterval(actualizar, PERIODO_BUSQUEDA);
    window.addEventListener('resize', actualizar);
    window.addEventListener('scroll', actualizar, true);
    return () => {
      window.clearInterval(intervalo);
      window.removeEventListener('resize', actualizar);
      window.removeEventListener('scroll', actualizar, true);
    };
  }, [enRuta, actual, escritorio, acomodar]);

  // El foco se mueve a la tarjeta en cada paso (y al llegar a la pantalla del paso).
  useEffect(() => {
    tarjeta.current?.focus({ preventScroll: true });
  }, [actual.id, enRuta]);

  // Al cerrar, el foco vuelve a donde estaba antes de empezar.
  useEffect(() => {
    const previo = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    return () => {
      if (previo && previo !== document.body && document.contains(previo)) previo.focus();
      else document.getElementById('contenido')?.focus();
    };
  }, []);

  // Esc cierra el recorrido, salvo que haya una hoja o un diálogo abierto: ahí Esc es de ellos.
  useEffect(() => {
    const alTeclear = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || event.defaultPrevented) return;
      if (document.querySelector('dialog[open]')) return;
      alCerrar.current();
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, []);

  return (
    <>
      <div
        ref={anillo}
        aria-hidden
        style={{ display: 'none' }}
        className="pointer-events-none fixed z-54 rounded-lg shadow-[0_0_0_4px_var(--color-accent),0_0_0_9999px_rgb(38_52_67/0.4)] motion-safe:transition-[top,left,width,height] motion-safe:duration-200"
      />
      <div
        ref={tarjeta}
        role="dialog"
        aria-modal="false"
        aria-labelledby={tituloId}
        aria-describedby={textoId}
        tabIndex={-1}
        className="fixed z-55 flex flex-col gap-2.5 rounded-xl border border-line bg-surface p-4 text-fg shadow-lg focus:outline-none max-sm:inset-x-3 max-sm:bottom-[calc(4rem+env(safe-area-inset-bottom)+0.75rem)] max-sm:max-h-[60dvh] max-sm:overflow-y-auto sm:w-96 sm:max-w-[calc(100vw-2rem)] sm:p-5"
      >
        <p className="m-0 text-caption font-bold tracking-widest text-accent-800 uppercase">
          {`Paso ${paso + 1} de ${total}`}
        </p>
        <h2 id={tituloId} className="m-0 text-lead leading-tight font-bold break-words sm:text-h3">
          {actual.titulo}
        </h2>
        <div id={textoId} className="flex flex-col gap-2.5">
          <p className="m-0 text-base leading-snug text-soft sm:text-body">{actual.texto}</p>
          {actual.sugerencia && (
            <p className="m-0 flex items-start gap-2 rounded-md bg-amber-100 p-2.5 text-hint font-semibold text-fg">
              <LightbulbIcon
                aria-hidden
                size={22}
                weight="fill"
                className="mt-0.5 shrink-0 text-amber-700"
              />
              <span>{actual.sugerencia}</span>
            </p>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <Button onClick={onAnterior} disabled={paso === 0}>
            Anterior
          </Button>
          <Button variant="solid" onClick={onSiguiente}>
            {esUltimo ? 'Terminar' : 'Siguiente'}
          </Button>
          {!esUltimo && (
            <Button variant="ghost" className="sm:ml-auto" onClick={onCerrar}>
              Saltar el recorrido
            </Button>
          )}
        </div>
      </div>
    </>
  );
}
