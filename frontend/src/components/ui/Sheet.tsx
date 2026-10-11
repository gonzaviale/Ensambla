import { useEffect, useId, useRef, type ReactNode } from 'react';
import { XIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { Button } from './Button';

export type SheetProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: ReactNode;
  children: ReactNode;
  /** Pie fijo con las acciones del panel. */
  footer?: ReactNode;
  /** "sheet": hoja desde abajo (celular) o panel lateral (>= lg). "center": diálogo centrado. */
  placement?: 'sheet' | 'center';
  /** "alertdialog" para confirmaciones que interrumpen. */
  role?: 'dialog' | 'alertdialog';
  /** Pantalla casi completa en el celular (visor de foto). */
  tall?: boolean;
  /** Impide cerrar con Esc, la X o el fondo (por ejemplo mientras se guarda). */
  locked?: boolean;
  className?: string;
};

const FOCUSABLE =
  'input:not([disabled]),select:not([disabled]),textarea:not([disabled]),button:not([disabled]),a[href],[tabindex]:not([tabindex="-1"])';

// Panel modal sobre <dialog>: foco atrapado, Esc, fondo inerte y retorno del foco al cerrarse.
// En celular sube desde abajo; en escritorio es un panel lateral anclado a la ventana.
export function Sheet({ open, ...props }: SheetProps) {
  if (!open) return null;
  return <SheetBody {...props} />;
}

function SheetBody({
  onClose,
  title,
  description,
  children,
  footer,
  placement = 'sheet',
  role = 'dialog',
  tall,
  locked,
  className,
}: Omit<SheetProps, 'open'>) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descId = useId();
  const alCerrar = useRef(onClose);
  const bloqueado = useRef(locked);
  useEffect(() => {
    alCerrar.current = onClose;
    bloqueado.current = locked;
  });

  useEffect(() => {
    const el = dialog.current;
    if (!el) return;
    const previo = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (typeof el.showModal === 'function') el.showModal();
    else el.setAttribute('open', '');
    const inicial =
      el.querySelector<HTMLElement>('[data-autofocus]') ??
      el.querySelector<HTMLElement>(`[data-sheet-body] :is(${FOCUSABLE})`) ??
      el.querySelector<HTMLElement>(FOCUSABLE);
    inicial?.focus();
    const raiz = document.documentElement;
    const overflow = raiz.style.overflow;
    raiz.style.overflow = 'hidden';
    return () => {
      raiz.style.overflow = overflow;
      if (typeof el.close === 'function' && el.open) el.close();
      if (previo && document.contains(previo)) previo.focus();
    };
  }, []);

  const centrado = placement === 'center';

  return (
    <dialog
      ref={dialog}
      role={role}
      aria-labelledby={titleId}
      aria-describedby={description ? descId : undefined}
      onCancel={(event) => {
        event.preventDefault();
        if (!bloqueado.current) alCerrar.current();
      }}
      onKeyDown={(event) => {
        // Respaldo para entornos sin <dialog> modal nativo.
        if (event.key === 'Escape' && !bloqueado.current) {
          event.stopPropagation();
          alCerrar.current();
        }
      }}
      onClick={(event) => {
        if (event.target === dialog.current && !bloqueado.current) alCerrar.current();
      }}
      className={cn(
        'fixed flex flex-col overflow-hidden border-0 bg-surface p-0 text-fg shadow-lg backdrop:bg-fg/55',
        centrado
          ? 'inset-0 m-auto h-fit max-h-[90dvh] w-[calc(100%-2rem)] max-w-lg animate-fade-in rounded-xl'
          : [
              'inset-x-0 top-auto bottom-0 m-0 w-full max-w-none animate-sheet-in rounded-t-xl',
              tall ? 'h-[92dvh] max-h-[92dvh]' : 'max-h-[90dvh]',
              'lg:inset-y-0 lg:right-0 lg:left-auto lg:h-dvh lg:max-h-dvh lg:w-md lg:rounded-t-none lg:rounded-l-xl',
            ],
        className,
      )}
    >
      <header className="flex items-start justify-between gap-3 border-b border-line px-5 py-4">
        <div className="min-w-0">
          <h2 id={titleId} className="m-0 text-h3 break-words">
            {title}
          </h2>
          {description && (
            <div id={descId} className="mt-1 text-hint text-soft">
              {description}
            </div>
          )}
        </div>
        {!locked && (
          <Button variant="ghost" iconOnly aria-label="Cerrar" onClick={() => alCerrar.current()}>
            <XIcon aria-hidden size={24} />
          </Button>
        )}
      </header>
      <div data-sheet-body className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
        {children}
      </div>
      {footer && (
        <footer className="flex flex-wrap justify-end gap-3 border-t border-line bg-canvas px-5 py-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
          {footer}
        </footer>
      )}
    </dialog>
  );
}
