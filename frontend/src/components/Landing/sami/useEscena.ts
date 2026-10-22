import { useEffect, useRef, useState } from 'react';
import { gsap } from 'gsap';

// Ciclo de vida común de las escenas de Sami: se construye una sola vez con GSAP (todo dentro de
// una línea de tiempo maestra), se detiene cuando sale de la pantalla y se puede pausar a mano
// (WCAG 2.2.2). Con «reducir movimiento» no se anima: queda el cuadro final.

type Construccion = (svg: SVGSVGElement, reducido: boolean) => gsap.core.Timeline | null;

export function useEscena(construir: Construccion) {
  const svg = useRef<SVGSVGElement>(null);
  const maestra = useRef<gsap.core.Timeline | null>(null);
  const visible = useRef(false);
  const [pausado, setPausado] = useState(false);
  const [animable, setAnimable] = useState(true);
  const pausadoRef = useRef(false);

  useEffect(() => {
    const el = svg.current;
    if (!el) return;
    const reducido =
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    setAnimable(!reducido);

    const contexto = gsap.context(() => {
      maestra.current = construir(el, reducido);
      // Con la línea de tiempo pausada igual se aplica el primer cuadro (bloques ocultos, curva sin dibujar…).
      maestra.current?.time(0.001);
    }, el);

    const aplicar = () => {
      const m = maestra.current;
      if (!m) return;
      m.paused(!visible.current || pausadoRef.current);
    };
    aplicar();

    let observador: IntersectionObserver | undefined;
    if (typeof IntersectionObserver === 'function') {
      observador = new IntersectionObserver(
        ([entrada]) => {
          visible.current = Boolean(entrada?.isIntersecting);
          aplicar();
        },
        { threshold: 0.2 },
      );
      observador.observe(el);
    } else {
      visible.current = true;
      aplicar();
    }

    return () => {
      observador?.disconnect();
      contexto.revert();
      maestra.current = null;
    };
  }, [construir]);

  const alternar = () => {
    pausadoRef.current = !pausadoRef.current;
    setPausado(pausadoRef.current);
    maestra.current?.paused(!visible.current || pausadoRef.current);
  };

  return { svg, pausado, alternar, animable };
}
