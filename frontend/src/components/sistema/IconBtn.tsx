import type { ComponentProps } from 'react';
import { Icon } from './Icon';
import type { NombreDeIcono } from './iconos';

type IconBtnProps = Omit<ComponentProps<'button'>, 'className' | 'children' | 'title'> & {
  name: NombreDeIcono;
  /** Texto de la ayuda y del lector de pantalla. */
  label: string;
};

export function IconBtn({ name, label, type = 'button', ...props }: IconBtnProps) {
  return (
    <button type={type} className="icon-btn" title={label} aria-label={label} {...props}>
      <Icon name={name} />
    </button>
  );
}
