import { useEffect } from 'react';
import { gsap } from 'gsap';
import { ScrollToPlugin } from 'gsap/ScrollToPlugin';

gsap.registerPlugin(ScrollToPlugin);

// Los enlaces internos de la landing («#sami», «#el-problema»…) bajan con un desplazamiento suave
// en vez de saltar de golpe a la sección. Con «reducir movimiento» el salto es directo. No se toca
// la URL: el router saltaría a la sección al instante y taparía el desplazamiento.
export function useDesplazamientoSuave() {
  useEffect(() => {
    const alHacerClic = (e: MouseEvent) => {
      if (
        e.defaultPrevented ||
        e.button !== 0 ||
        e.metaKey ||
        e.ctrlKey ||
        e.shiftKey ||
        e.altKey
      ) {
        return;
      }
      const enlace = e.target instanceof Element ? e.target.closest('a[href^="#"]') : null;
      if (!(enlace instanceof HTMLAnchorElement)) return;
      const id = decodeURIComponent(enlace.hash.slice(1));
      const destino = id ? document.getElementById(id) : null;
      if (!destino) return;

      e.preventDefault();
      const barra = document.querySelector('header.sticky')?.getBoundingClientRect().height ?? 0;
      const y = Math.max(0, destino.getBoundingClientRect().top + window.scrollY - barra);
      const reducido =
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (reducido) {
        window.scrollTo(0, y);
      } else {
        const distancia = Math.abs(y - window.scrollY);
        gsap.to(window, {
          scrollTo: { y, autoKill: true },
          duration: Math.min(1.8, Math.max(0.8, distancia / 1300)),
          ease: 'power3.inOut',
          overwrite: true,
        });
      }
      destino.setAttribute('tabindex', '-1');
      destino.focus({ preventScroll: true });
    };
    document.addEventListener('click', alHacerClic);
    return () => document.removeEventListener('click', alHacerClic);
  }, []);
}
