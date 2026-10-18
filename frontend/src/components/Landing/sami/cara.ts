import { gsap } from 'gsap';
import { mirada, type Punto } from './geometria';

// Gestos de la cara de Sami (mirada, parpadeo, alegría, sorpresa) compartidos por las escenas.

type Buscar = (selector: string) => Element[];

export function crearCara(buscar: Buscar, cabeza: Punto) {
  const pupilas = buscar('[data-sami="pupila"]');
  const ojos = buscar('[data-sami="ojo"]');
  const parpados = buscar('[data-sami="parpado"]');
  const cejas = buscar('[data-sami="ceja"]');
  const feliz = buscar('[data-sami="feliz"]');
  const boca = buscar('[data-sami="boca"]');
  const bocaAbierta = buscar('[data-sami="bocaAbierta"]');
  const cabezas = buscar('[data-sami="cabeza"]');
  gsap.set(parpados, { scaleY: 0, transformOrigin: '50% 0%' });
  gsap.set(ojos, { transformOrigin: '50% 50%' });
  gsap.set(cabezas, { transformOrigin: '50% 100%' });

  return {
    cabezas,
    pupilas,
    ojos,
    cejas,
    mirar(tl: gsap.core.Timeline, hacia: Punto, t: number, dur = 0.45) {
      tl.to(pupilas, { ...mirada(cabeza, hacia), duration: dur, ease: 'power2.out' }, t);
    },
    /** Parpadeo cada tanto, en una línea de tiempo aparte. */
    parpadeo() {
      const tl = gsap.timeline({ repeat: -1, repeatDelay: 3.1 });
      tl.to(parpados, { scaleY: 1, duration: 0.08, ease: 'power1.in' });
      tl.to(parpados, { scaleY: 0, duration: 0.13, ease: 'power1.out' });
      return tl;
    },
    /** Ojos cerrados de risa y boca abierta. */
    alegre(tl: gsap.core.Timeline, t: number) {
      tl.to(ojos, { opacity: 0, duration: 0.1 }, t);
      tl.to(feliz, { opacity: 1, duration: 0.12 }, t);
      tl.to(boca, { opacity: 0, duration: 0.08 }, t);
      tl.to(bocaAbierta, { opacity: 1, duration: 0.12 }, t);
      tl.to(cejas, { y: -7, duration: 0.2, ease: 'back.out(2)' }, t);
    },
    /** Vuelve a la cara de siempre. */
    serena(tl: gsap.core.Timeline, t: number) {
      tl.to(ojos, { opacity: 1, scale: 1, duration: 0.12 }, t);
      tl.to(feliz, { opacity: 0, duration: 0.1 }, t);
      tl.to(boca, { opacity: 1, duration: 0.12 }, t);
      tl.to(bocaAbierta, { opacity: 0, duration: 0.1 }, t);
      tl.to(cejas, { y: 0, duration: 0.25 }, t);
    },
    /** Ojos grandes y boca en «o»: algo llamó su atención. */
    sorpresa(tl: gsap.core.Timeline, t: number) {
      tl.to(ojos, { scale: 1.12, duration: 0.15, ease: 'back.out(3)' }, t);
      tl.to(cejas, { y: -10, duration: 0.18, ease: 'back.out(3)' }, t);
      tl.to(boca, { opacity: 0, duration: 0.06 }, t);
      tl.to(bocaAbierta, { opacity: 1, duration: 0.1 }, t);
    },
    /** Cuadro inmediato (sin animar) para «reducir movimiento». */
    fijarAlegre() {
      gsap.set(ojos, { opacity: 0 });
      gsap.set(feliz, { opacity: 1 });
      gsap.set(boca, { opacity: 0 });
      gsap.set(bocaAbierta, { opacity: 1 });
      gsap.set(cejas, { y: -7 });
    },
  };
}
