import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { DotsThreeIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { Button } from './Button';

export type MenuItem = {
  label: string;
  icon?: ReactNode;
  onSelect: () => void;
  tone?: 'danger';
  disabled?: boolean;
};

type MenuProps = { label: string; items: MenuItem[]; disabled?: boolean; className?: string };

// Menú desplegable accesible: flechas, Inicio/Fin, Esc devuelve el foco al botón y tocar afuera cierra.
export function Menu({ label, items, disabled, className }: MenuProps) {
  const [abierto, setAbierto] = useState(false);
  const raiz = useRef<HTMLDivElement>(null);
  const boton = useRef<HTMLButtonElement>(null);
  const lista = useRef<HTMLDivElement>(null);
  const id = useId();

  useEffect(() => {
    if (!abierto) return;
    lista.current?.querySelector<HTMLElement>('[role="menuitem"]:not([disabled])')?.focus();
    const alTocarAfuera = (event: PointerEvent) => {
      if (!raiz.current?.contains(event.target as Node)) setAbierto(false);
    };
    document.addEventListener('pointerdown', alTocarAfuera);
    return () => document.removeEventListener('pointerdown', alTocarAfuera);
  }, [abierto]);

  function alTeclear(event: KeyboardEvent) {
    if (!abierto) return;
    const opciones = Array.from(
      lista.current?.querySelectorAll<HTMLElement>('[role="menuitem"]:not([disabled])') ?? [],
    );
    const actual = opciones.indexOf(document.activeElement as HTMLElement);
    const ir = (i: number) => opciones[(i + opciones.length) % opciones.length]?.focus();
    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        event.stopPropagation();
        setAbierto(false);
        boton.current?.focus();
        break;
      case 'ArrowDown':
        event.preventDefault();
        ir(actual + 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        ir(actual - 1);
        break;
      case 'Home':
        event.preventDefault();
        ir(0);
        break;
      case 'End':
        event.preventDefault();
        ir(opciones.length - 1);
        break;
      case 'Tab':
        setAbierto(false);
        break;
    }
  }

  return (
    <div ref={raiz} className={cn('relative', className)} onKeyDown={alTeclear}>
      <Button
        ref={boton}
        iconOnly
        variant="ghost"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls={abierto ? id : undefined}
        disabled={disabled}
        onClick={() => setAbierto((v) => !v)}
      >
        <DotsThreeIcon aria-hidden size={28} weight="bold" />
      </Button>
      {abierto && (
        <div
          ref={lista}
          id={id}
          role="menu"
          aria-label={label}
          className="absolute top-full right-0 z-(--z-nav) mt-1 flex min-w-56 flex-col gap-0.5 rounded-lg border border-line bg-surface p-1.5 shadow-md"
        >
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              disabled={item.disabled}
              onClick={() => {
                setAbierto(false);
                item.onSelect();
              }}
              className={cn(
                'flex min-h-12 w-full cursor-pointer items-center gap-3 rounded-md px-3 text-left text-base font-semibold hover:bg-fg/6 disabled:cursor-not-allowed disabled:text-muted',
                item.tone === 'danger' ? 'text-bad' : 'text-fg',
              )}
            >
              {item.icon && (
                <span aria-hidden className="flex">
                  {item.icon}
                </span>
              )}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
