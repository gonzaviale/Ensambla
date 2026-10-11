import type { ComponentProps } from 'react';
import { CircleNotchIcon } from '@phosphor-icons/react';
import { buttonClasses, type ButtonSize, type ButtonVariant } from './buttonStyles';

type ButtonProps = ComponentProps<'button'> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Botón cuadrado con sólo un ícono: exige `aria-label`. */
  iconOnly?: boolean;
  fullWidth?: boolean;
  /** Muestra un indicador, deshabilita el botón y avisa "ocupado" a los lectores. */
  loading?: boolean;
};

export function Button({
  variant,
  size,
  iconOnly,
  fullWidth,
  loading,
  className,
  type = 'button',
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      aria-busy={loading || undefined}
      disabled={disabled || loading}
      className={buttonClasses({ variant, size, iconOnly, fullWidth, className })}
      {...props}
    >
      {loading && <CircleNotchIcon aria-hidden className="animate-spin" size={20} />}
      {children}
    </button>
  );
}
