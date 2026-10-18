import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

export type BadgeTone = 'neutral' | 'accent' | 'ok' | 'warn' | 'bad';

const tones: Record<BadgeTone, string> = {
  neutral: 'bg-soft-surface text-soft',
  accent: 'bg-petrol-100 text-petrol',
  ok: 'bg-ok/12 text-ok',
  warn: 'bg-amber-100 text-amber-700',
  bad: 'bg-coral-100 text-bad',
};

type BadgeProps = { tone?: BadgeTone; icon?: ReactNode; children: ReactNode; className?: string };

// Estado compacto: siempre ícono + texto, nunca sólo color.
export function Badge({ tone = 'neutral', icon, children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-caption font-semibold',
        tones[tone],
        className,
      )}
    >
      {icon && (
        <span aria-hidden className="flex shrink-0">
          {icon}
        </span>
      )}
      {children}
    </span>
  );
}
