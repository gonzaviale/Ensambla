import { Button } from './Button';
import { cn } from '@/lib/cn';

type ToastProps = { mensaje: string; tipo: 'exito' | 'error'; onClose: () => void };

export function Toast({ mensaje, tipo, onClose }: ToastProps) {
  return (
    <div
      className={cn(
        'fixed right-4 bottom-4 left-4 z-50 flex items-start gap-3 rounded-xl border bg-surface p-4 shadow-lg sm:left-auto sm:max-w-md',
        tipo === 'exito' ? 'border-ok/30' : 'border-bad/30',
      )}
    >
      <p
        role={tipo === 'exito' ? 'status' : 'alert'}
        aria-atomic="true"
        className={cn('m-0 flex-1 text-sm', tipo === 'exito' ? 'text-ok' : 'text-bad')}
      >
        {mensaje}
      </p>
      <Button aria-label="Cerrar aviso" onClick={onClose} className="shrink-0">
        Cerrar
      </Button>
    </div>
  );
}
