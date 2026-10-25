import { useEffect } from 'react';

const NOMBRE_APP = 'Ensambla';

// Cada pantalla pone su título en la pestaña (WCAG 2.4.2): "Mis cuadernos · Ensambla".
export function useTituloPagina(titulo: string) {
  useEffect(() => {
    const anterior = document.title;
    document.title = `${titulo} · ${NOMBRE_APP}`;
    return () => {
      document.title = anterior;
    };
  }, [titulo]);
}
