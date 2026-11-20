import { useMemo } from 'react';
import { matrizDelQr, trazoDelQr } from '@/lib/cobro/codigoQr';

const BORDE_BLANCO = 2;

interface Props {
  texto: string;
  descripcion: string;
}

export function CodigoQr({ texto, descripcion }: Props) {
  const matriz = useMemo(() => matrizDelQr(texto), [texto]);
  const lado = matriz.length + BORDE_BLANCO * 2;

  return (
    <svg
      className="codigo-qr"
      viewBox={`${-BORDE_BLANCO} ${-BORDE_BLANCO} ${lado} ${lado}`}
      role="img"
      aria-label={descripcion}
      shapeRendering="crispEdges"
    >
      <rect x={-BORDE_BLANCO} y={-BORDE_BLANCO} width={lado} height={lado} fill="#fff" />
      <path d={trazoDelQr(matriz)} fill="#000" />
    </svg>
  );
}
