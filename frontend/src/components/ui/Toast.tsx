import { useEffect, useRef } from 'react';
import { CheckCircleIcon, WarningCircleIcon } from '@phosphor-icons/react';
import { Button } from './Button';
import { cn } from '@/lib/cn';

type ToastProps = {
  mensaje: string;
  tipo: 'exito' | 'error';
  onClose: () => void;
  /** Acción puntual, por ejemplo "Deshacer". */
  accion?: { etiqueta: string; onClick: () => void };
  /** Milisegundos hasta cerrarse solo; los errores se quedan hasta que se cierran. */
  duracion?: number;
};

const DURACION_EXITO = 6000;

// Aviso flotante: por encima de la barra inferior del celular, autocierre a los 6 s (éxito) y
// anuncio por lector de pantalla.
export function Toast({ mensaje, tipo, onClose, accion, duracion }: ToastProps) {
  const cerrar = useRef(onClose);
  useEffect(() => {
    cerrar.current = onClose;
  });
  const ms = duracion ?? (tipo === 'exito' ? DURACION_EXITO : 0);

  useEffect(() => {
    if (!ms) return;
    const id = window.setTimeout(() => cerrar.current(), ms);
    return () => window.clearTimeout(id);
  }, [ms, mensaje]);

  const Icono = tipo === 'exito' ? CheckCircleIcon : WarningCircleIcon;

  return (
    <div
      className={cn(
        'fixed right-4 bottom-[calc(5rem+env(safe-area-inset-bottom))] left-4 z-(--z-toast) flex items-start gap-3 rounded-xl border bg-surface p-4 shadow-lg md:right-4 md:bottom-4 md:left-auto md:max-w-md',
        tipo === 'exito' ? 'border-ok/40' : 'border-bad/40',
      )}
    >
      <Icono
        aria-hidden
        size={28}
        weight="fill"
        className={cn('mt-0.5 shrink-0', tipo === 'exito' ? 'text-ok' : 'text-bad')}
      />
      <p
        role={tipo === 'exito' ? 'status' : 'alert'}
        aria-atomic="true"
        className="m-0 min-w-0 flex-1 text-base font-semibold text-fg"
      >
        {mensaje}
      </p>
      <div className="flex shrink-0 flex-wrap gap-1">
        {accion && (
          <Button variant="primary" size="compact" onClick={accion.onClick}>
            {accion.etiqueta}
          </Button>
        )}
        <Button size="compact" aria-label="Cerrar aviso" onClick={onClose}>
          Cerrar
        </Button>
      </div>
    </div>
  );
}
