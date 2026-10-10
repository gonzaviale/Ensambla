import { cn } from '@/lib/cn';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'solid';
export type ButtonSize = 'md' | 'sm';

const base =
  'inline-flex shrink-0 cursor-pointer items-center justify-center gap-1.5 rounded-md border border-transparent font-semibold leading-tight whitespace-nowrap text-fg no-underline transition-colors disabled:cursor-not-allowed disabled:opacity-45';

// El naranja de marca se usa para la acción principal; el texto va en pizarra para cumplir contraste AA.
const variants: Record<ButtonVariant, string> = {
  primary:
    'border-accent-700 text-accent-800 hover:bg-accent/10 hover:text-accent-900 active:bg-accent/20',
  secondary: 'border-fg/20 hover:bg-fg/6 hover:text-fg active:bg-fg/12',
  ghost: 'px-1 text-accent-800 hover:bg-accent/10 hover:text-accent-900 active:bg-accent/20',
  solid:
    'border-accent bg-accent text-fg hover:border-accent-400 hover:bg-accent-400 hover:text-fg',
};

const sizes: Record<ButtonSize, string> = {
  md: 'px-5 py-2.5 text-base',
  sm: 'px-4 py-2 text-[15px]',
};

type ButtonClassesOptions = { variant?: ButtonVariant; size?: ButtonSize; className?: string };

export function buttonClasses({
  variant = 'secondary',
  size = 'md',
  className,
}: ButtonClassesOptions = {}) {
  return cn(base, variants[variant], sizes[size], className);
}
