import { cn } from '@/lib/cn';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'solid' | 'danger' | 'danger-solid';
export type ButtonSize = 'compact' | 'md' | 'lg';

const base =
  'relative inline-flex cursor-pointer items-center justify-center gap-2 rounded-md border border-transparent text-center font-bold leading-tight text-fg no-underline transition-colors disabled:cursor-not-allowed disabled:border-transparent disabled:bg-fg/10 disabled:text-muted disabled:shadow-none aria-disabled:cursor-not-allowed aria-disabled:bg-fg/10 aria-disabled:text-muted';

// El naranja de marca es la acción principal de la pantalla (una sola, sólida); el texto va en
// pizarra para cumplir contraste AA. Las acciones de apoyo van en contorno o como texto.
const variants: Record<ButtonVariant, string> = {
  solid:
    'border-accent bg-accent shadow-button hover:border-accent-400 hover:bg-accent-400 hover:text-fg active:bg-accent-400 active:shadow-none',
  primary:
    'border-petrol bg-petrol text-white shadow-petrol-button hover:border-petrol-700 hover:bg-petrol-700 hover:text-white active:bg-petrol-700',
  secondary:
    'border-line bg-surface text-fg hover:border-petrol/30 hover:bg-soft-surface hover:text-fg active:bg-petrol-100',
  ghost: 'text-petrol hover:bg-petrol-100 hover:text-petrol-700 active:bg-petrol-100',
  danger: 'border-bad bg-surface text-bad hover:bg-coral-100 hover:text-bad active:bg-coral-100',
  'danger-solid': 'border-bad bg-bad text-white hover:bg-bad/90 hover:text-white',
};

// 48 px por defecto (T-033). "compact" (40 px) es sólo para celdas de tabla en escritorio y
// conserva un área de toque de 44 px con un margen invisible.
const sizes: Record<ButtonSize, string> = {
  compact:
    'min-h-10 px-3 py-1.5 text-hint before:absolute before:-inset-y-[2px] before:inset-x-0 before:content-[""]',
  md: 'min-h-12 px-5 py-2.5 text-base',
  lg: 'min-h-14 px-6 py-3 text-body',
};

const iconOnlySizes: Record<ButtonSize, string> = {
  compact: 'size-10 min-h-10 px-0',
  md: 'size-12 min-h-12 px-0',
  lg: 'size-14 min-h-14 px-0',
};

type ButtonClassesOptions = {
  variant?: ButtonVariant;
  size?: ButtonSize;
  iconOnly?: boolean;
  fullWidth?: boolean;
  className?: string;
};

export function buttonClasses({
  variant = 'secondary',
  size = 'md',
  iconOnly,
  fullWidth,
  className,
}: ButtonClassesOptions = {}) {
  return cn(
    base,
    variants[variant],
    iconOnly ? iconOnlySizes[size] : sizes[size],
    fullWidth && 'w-full',
    className,
  );
}
