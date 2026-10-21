import { useEffect, useState } from 'react';
import { cn } from '@/lib/cn';
import { subirArriba } from '@/hooks/landing/subirArriba';

// Aparece al bajar un poco y lleva de vuelta al principio de la página.
const DESPLAZAMIENTO_MINIMO = 480;

export function BotonSubir() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const revisar = () => setVisible(window.scrollY > DESPLAZAMIENTO_MINIMO);
    revisar();
    window.addEventListener('scroll', revisar, { passive: true });
    return () => window.removeEventListener('scroll', revisar);
  }, []);

  return (
    <button
      type="button"
      className={cn('subir', visible && 'subir--visible')}
      aria-label="Volver arriba"
      tabIndex={visible ? 0 : -1}
      onClick={subirArriba}
    >
      <svg viewBox="0 0 24 24" width="24" height="24" fill="none" aria-hidden="true">
        <path
          d="M12 19V5M5 12l7-7 7 7"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </button>
  );
}
