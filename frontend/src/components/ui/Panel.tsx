import type { ComponentProps } from 'react';
import { cn } from '@/lib/cn';

type PanelProps = ComponentProps<'section'> & { tight?: boolean; flat?: boolean };

export function Panel({ tight, flat, className, ...props }: PanelProps) {
  return (
    <section
      className={cn(
        'rounded-lg border border-line',
        flat ? 'bg-canvas' : 'bg-surface',
        tight ? 'px-4 py-3.5' : 'p-5',
        className,
      )}
      {...props}
    />
  );
}
