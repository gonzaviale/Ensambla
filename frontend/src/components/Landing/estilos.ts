import type { CSSProperties } from 'react';

// Pasa variables CSS (--x, --d…) por `style`, que React no tipa.
export function variables(valores: Record<`--${string}`, string | number>): CSSProperties {
  return valores as CSSProperties;
}
