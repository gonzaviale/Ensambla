import { CATALOG } from '@/lib/sistema/catalogo';
import type { Color, TipoBloque } from '@/lib/sistema/tipos';
import { varsDeColor } from './colores';
import { Icon } from './Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

type IconTileProps = {
  /** El ícono y el color de un tipo de bloque, o uno propio con `name` y `color`. */
  tipo?: TipoBloque;
  name?: NombreDeIcono;
  color?: Color;
  className?: string;
};

export function IconTile({ tipo, name, color, className = '' }: IconTileProps) {
  const definicion = tipo ? CATALOG[tipo] : undefined;
  const icono = name ?? definicion?.icon ?? 'box';
  return (
    <span
      className={`icon-tile ${className}`.trim()}
      style={varsDeColor(color ?? definicion?.color ?? 'orange')}
    >
      <Icon name={icono} />
    </span>
  );
}
