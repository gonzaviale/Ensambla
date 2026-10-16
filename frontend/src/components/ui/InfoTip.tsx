import {
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type PointerEvent as PunteroReact,
  type ReactNode,
} from 'react';
import { InfoIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

type InfoTipProps = {
  /** De qué trata la ayuda, en pocas palabras: «ganancia sobre el costo». Forma el nombre del botón. */
  tema: string;
  /** El texto de la ayuda: una a tres frases, con un ejemplo concreto. */
  children: ReactNode;
  className?: string;
};

type Posicion = { top: number; left: number; ancho: number };

const ANCHO_MAXIMO = 320;
const MARGEN = 16;
const SEPARACION = 8;
const DEMORA_CIERRE = 150;

// Ayuda contextual: un botón con la «i» que abre un globo con la explicación. Se abre al tocar o con
// Enter, y también al pasar el mouse o enfocar con el teclado. Se cierra con Esc, al tocar afuera y
// al perder el foco. El texto queda vinculado al botón con `aria-describedby` mientras está abierto.
// Ponelo al lado del título o del texto que explica, no adentro de un encabezado.
export function InfoTip({ tema, children, className }: InfoTipProps) {
  const [abierto, setAbierto] = useState(false);
  const [posicion, setPosicion] = useState<Posicion | null>(null);
  const idGlobo = useId();
  const raiz = useRef<HTMLSpanElement>(null);
  const boton = useRef<HTMLButtonElement>(null);
  const globo = useRef<HTMLSpanElement>(null);
  /** Abierto con un toque o un Enter: no se cierra solo al sacar el mouse. */
  const fijo = useRef(false);
  /** El foco llegó por un toque o un clic: el clic se ocupa de abrir, no el foco. */
  const porPuntero = useRef(false);
  const demora = useRef<number | undefined>(undefined);

  const cerrar = useCallback(() => {
    window.clearTimeout(demora.current);
    fijo.current = false;
    porPuntero.current = false;
    setAbierto(false);
    setPosicion(null);
  }, []);

  const colocar = useCallback(() => {
    const elBoton = boton.current;
    const elGlobo = globo.current;
    if (!elBoton || !elGlobo) return;
    const caja = elBoton.getBoundingClientRect();
    const anchoPantalla = document.documentElement.clientWidth || window.innerWidth;
    const altoPantalla = window.innerHeight;
    const ancho = Math.max(0, Math.min(ANCHO_MAXIMO, anchoPantalla - 2 * MARGEN));
    const alto = elGlobo.offsetHeight;
    // Centrado en el botón, pero siempre dentro de la pantalla (también a 360 px).
    const izquierda = Math.max(
      MARGEN,
      Math.min(caja.left + caja.width / 2 - ancho / 2, anchoPantalla - ancho - MARGEN),
    );
    const cabeAbajo = caja.bottom + SEPARACION + alto <= altoPantalla - MARGEN;
    const cabeArriba = caja.top - SEPARACION - alto >= MARGEN;
    const arriba = !cabeAbajo && cabeArriba;
    const top = arriba ? caja.top - SEPARACION - alto : caja.bottom + SEPARACION;
    setPosicion((actual) =>
      actual && actual.top === top && actual.left === izquierda && actual.ancho === ancho
        ? actual
        : { top, left: izquierda, ancho },
    );
  }, []);

  useLayoutEffect(() => {
    if (!abierto) return;
    colocar();
    window.addEventListener('resize', colocar);
    window.addEventListener('scroll', colocar, true);
    return () => {
      window.removeEventListener('resize', colocar);
      window.removeEventListener('scroll', colocar, true);
    };
  }, [abierto, colocar]);

  useEffect(() => {
    if (!abierto) return;
    // En captura: Esc cierra la ayuda antes que cualquier hoja o recorrido que esté abierto.
    const alTeclear = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      event.stopPropagation();
      cerrar();
    };
    const alTocarAfuera = (event: PointerEvent) => {
      if (!raiz.current?.contains(event.target as Node)) cerrar();
    };
    document.addEventListener('keydown', alTeclear, true);
    document.addEventListener('pointerdown', alTocarAfuera);
    return () => {
      document.removeEventListener('keydown', alTeclear, true);
      document.removeEventListener('pointerdown', alTocarAfuera);
    };
  }, [abierto, cerrar]);

  useEffect(() => () => window.clearTimeout(demora.current), []);

  function alEntrarElMouse(event: PunteroReact) {
    if (event.pointerType !== 'mouse') return;
    window.clearTimeout(demora.current);
    setAbierto(true);
  }

  function alSalirElMouse(event: PunteroReact) {
    if (event.pointerType !== 'mouse' || fijo.current) return;
    if (document.activeElement === boton.current) return;
    window.clearTimeout(demora.current);
    demora.current = window.setTimeout(cerrar, DEMORA_CIERRE);
  }

  function alHacerClic() {
    porPuntero.current = false;
    if (abierto && fijo.current) {
      cerrar();
      return;
    }
    window.clearTimeout(demora.current);
    fijo.current = true;
    setAbierto(true);
  }

  return (
    <span
      ref={raiz}
      className={cn('relative inline-flex align-middle', className)}
      onPointerEnter={alEntrarElMouse}
      onPointerLeave={alSalirElMouse}
    >
      <button
        ref={boton}
        type="button"
        aria-label={`¿Qué es esto? ${tema}`}
        aria-expanded={abierto}
        aria-describedby={abierto ? idGlobo : undefined}
        onPointerDown={() => {
          porPuntero.current = true;
        }}
        onFocus={() => {
          if (!porPuntero.current) setAbierto(true);
        }}
        onBlur={cerrar}
        onClick={alHacerClic}
        className="-my-2 inline-flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent p-0 text-accent-800 hover:bg-accent-100 hover:text-accent-900"
      >
        <InfoIcon aria-hidden size={24} weight="fill" />
      </button>
      {abierto && (
        <span
          ref={globo}
          id={idGlobo}
          role="tooltip"
          style={
            posicion
              ? { top: posicion.top, left: posicion.left, width: posicion.ancho }
              : { top: 0, left: 0, width: ANCHO_MAXIMO, visibility: 'hidden' }
          }
          className="fixed z-(--z-toast) block max-w-[calc(100vw-2rem)] rounded-lg border border-line bg-surface px-4 py-3 text-left text-body leading-snug font-normal text-fg shadow-md"
        >
          {children}
        </span>
      )}
    </span>
  );
}
