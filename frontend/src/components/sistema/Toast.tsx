import { useEffect } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';

const DURACION_MS = 4200;

// El aviso breve de abajo. Mismo nodo `#toast.toast` que el mockup; `.visible` lo muestra.
export function Toast() {
  const aviso = useSistemaStore((estado) => estado.aviso);
  const cerrarAviso = useSistemaStore((estado) => estado.cerrarAviso);

  useEffect(() => {
    if (!aviso) return;
    const temporizador = setTimeout(cerrarAviso, DURACION_MS);
    return () => clearTimeout(temporizador);
  }, [aviso, cerrarAviso]);

  return (
    <div
      id="toast"
      className={`toast ${aviso ? 'visible' : ''}`.trim()}
      role="status"
      aria-live="polite"
    >
      {aviso?.texto}
    </div>
  );
}
