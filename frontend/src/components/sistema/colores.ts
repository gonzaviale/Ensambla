import type { CSSProperties } from 'react';
import { COLORS, TINTS } from '@/lib/sistema/catalogo';
import type { Color } from '@/lib/sistema/tipos';

// Los bloques se pintan con dos variables de color: el acento y su fondo suave (vars() del mockup).
export const varsDeColor = (color: Color): CSSProperties =>
  ({
    '--accent': COLORS[color] ?? COLORS.orange,
    '--tint': TINTS[color] ?? TINTS.orange,
  }) as CSSProperties;
