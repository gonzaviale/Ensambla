import type { ReactNode } from 'react';

// Barra de acciones fija abajo (encima de la navegación del celular). Deja un espacio igual en
// el flujo para que nada quede tapado al llegar al final de la página.
export function StickyActionBar({ children, label }: { children: ReactNode; label?: string }) {
  return (
    <>
      <div aria-hidden className="h-32 sm:h-24" />
      <div
        role="region"
        aria-label={label ?? 'Acciones'}
        className="fixed inset-x-0 bottom-[calc(4rem+env(safe-area-inset-bottom))] z-(--z-nav) border-t border-line bg-surface shadow-lg md:bottom-0"
      >
        <div className="mx-auto flex w-full max-w-300 flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4 md:px-8">
          {children}
        </div>
      </div>
    </>
  );
}
