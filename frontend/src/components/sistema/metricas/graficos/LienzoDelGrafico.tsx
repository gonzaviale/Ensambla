import type { ReactElement } from 'react';
import { ResponsiveContainer } from 'recharts';

type Props = { alto: number; children: ReactElement };

// El dibujo toma el ancho de su lugar. Antes de medirlo (y en las pruebas, que no tienen pantalla) parte de un tamaño
// razonable en lugar de no dibujar nada.
export function LienzoDelGrafico({ alto, children }: Props) {
  return (
    <ResponsiveContainer width="100%" height={alto} initialDimension={{ width: 360, height: alto }}>
      {children}
    </ResponsiveContainer>
  );
}
