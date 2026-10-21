import { COLORES, COLORES_BLOQUE, type ColorBloque } from './colores';

// Bloques de Sami: esquinas redondeadas como los módulos de la marca, con un poco de luz y volumen
// de plastilina. Se dibujan centrados en (0, 0); `DefsBloques` define los degradados una sola vez.

export function DefsBloques({ prefijo }: { prefijo: string }) {
  return (
    <defs>
      {COLORES.map((color) => {
        const c = COLORES_BLOQUE[color];
        return (
          <linearGradient key={color} id={`${prefijo}-b-${color}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor={c.luz} />
            <stop offset="0.28" stopColor={c.base} />
            <stop offset="1" stopColor={c.base} />
          </linearGradient>
        );
      })}
    </defs>
  );
}

type BloqueProps = {
  color: ColorBloque;
  ancho: number;
  alto: number;
  prefijo: string;
};

export function Bloque({ color, ancho, alto, prefijo }: BloqueProps) {
  const c = COLORES_BLOQUE[color];
  const x = -ancho / 2;
  const y = -alto / 2;
  return (
    <g>
      <rect x={x} y={y + 7} width={ancho} height={alto} rx="15" fill={c.sombra} />
      <rect x={x} y={y} width={ancho} height={alto} rx="15" fill={`url(#${prefijo}-b-${color})`} />
      <rect
        x={x + 1}
        y={y + 1}
        width={ancho - 2}
        height={alto - 2}
        rx="14"
        fill="none"
        stroke="#fff"
        strokeOpacity=".16"
        strokeWidth="2"
      />
      <rect
        x={x + 12}
        y={y + 9}
        width={Math.min(ancho - 24, 46)}
        height="6"
        rx="3"
        fill="#fff"
        opacity=".28"
      />
    </g>
  );
}
