import type { ComponentProps, ReactNode } from 'react';

type BtnProps = Omit<ComponentProps<'button'>, 'className'> & {
  /** Variantes del mockup, separadas por espacio: primary, secondary, soft, ghost, danger, orange, small, full. */
  cls?: string;
  children: ReactNode;
};

export function Btn({ cls = 'secondary', type = 'button', children, ...props }: BtnProps) {
  return (
    <button type={type} className={`btn ${cls}`} {...props}>
      {children}
    </button>
  );
}
