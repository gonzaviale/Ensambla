import { useEffect, useRef } from 'react';

// Muestra con una animación los elementos `.revelar` del contenedor cuando entran en pantalla.
export function useRevelado<T extends HTMLElement>() {
  const contenedor = useRef<T>(null);

  useEffect(() => {
    const elementos = Array.from(
      contenedor.current?.querySelectorAll<HTMLElement>('.revelar:not(.visible)') ?? [],
    );
    if (typeof IntersectionObserver === 'undefined') {
      elementos.forEach((el) => el.classList.add('visible'));
      return;
    }
    const observador = new IntersectionObserver(
      (entradas) => {
        entradas.forEach((entrada) => {
          if (!entrada.isIntersecting) return;
          entrada.target.classList.add('visible');
          observador.unobserve(entrada.target);
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' },
    );
    elementos.forEach((el) => observador.observe(el));
    return () => observador.disconnect();
  }, []);

  return contenedor;
}
