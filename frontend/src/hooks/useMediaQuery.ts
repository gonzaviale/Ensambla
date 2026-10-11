import { useSyncExternalStore } from 'react';

// Escritorio: desde 1024 px (breakpoint `lg` de Tailwind). La revisión del cuaderno muestra una
// tabla ahí y una tarjeta por renglón en pantallas más chicas.
export const CONSULTA_ESCRITORIO = '(min-width: 1024px)';

/**
 * Sigue una media query. Sin `matchMedia` (jsdom, render en servidor) devuelve `porDefecto`, así
 * los tests ven la versión de escritorio salvo que simulen `matchMedia`.
 */
export function useMediaQuery(consulta: string, porDefecto = true) {
  return useSyncExternalStore(
    (avisar) => {
      if (typeof window.matchMedia !== 'function') return () => undefined;
      const lista = window.matchMedia(consulta);
      lista.addEventListener('change', avisar);
      return () => lista.removeEventListener('change', avisar);
    },
    () =>
      typeof window.matchMedia === 'function' ? window.matchMedia(consulta).matches : porDefecto,
    () => porDefecto,
  );
}

export const useEsEscritorio = () => useMediaQuery(CONSULTA_ESCRITORIO);
