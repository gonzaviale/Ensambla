import type { ComponentProps } from 'react';
import { cn } from '@/lib/cn';

type PanelProps = ComponentProps<'section'> & {
  tight?: boolean;
  /** Sin sombra y sobre Marfil: para vacíos y avisos que no deben competir. */
  flat?: boolean;
  /** Tarjeta que se puede abrir: sube un nivel de sombra al pasar el mouse. */
  interactive?: boolean;
};

// Tarjeta blanca, radio único y sombra suave (manual de marca): se agrupa por espacio, no por bordes.
export function Panel({ tight, flat, interactive, className, ...props }: PanelProps) {
  return (
    <section
      className={cn(
        'min-w-0 rounded-lg',
        flat ? 'border border-line bg-canvas' : 'border border-line/60 bg-surface shadow-sm',
        interactive && 'transition-shadow hover:shadow-md',
        tight ? 'px-4 py-3.5' : 'p-5',
        className,
      )}
      {...props}
    />
  );
}
