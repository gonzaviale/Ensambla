import { BRAZOS, ORDEN_ATRAS, ORDEN_FRENTE, VENTOSAS_POR_BRAZO, type BrazoId } from './brazos';

// Sami, el asistente de Ensambla: personaje de plastilina naranja con ojos grandes. Se dibuja en
// coordenadas locales (centro de la cabeza en 0, 0) y cada escena lo coloca con un `translate`.
// Brazos, párpados, pupilas, cejas y boca llevan `data-*` para que GSAP los anime desde afuera.

const CUERPO =
  'M-98 70C-118 40-126-30-108-80C-88-132-46-156 0-156C46-156 88-132 108-80C126-30 118 40 98 70C84 96 44 110 0 110C-44 110-84 96-98 70Z';

const PECAS = [
  [-30, -122, 4.2, 0.3],
  [-8, -134, 3, 0.26],
  [26, -120, 5, 0.3],
  [54, -102, 3.4, 0.28],
  [-62, -98, 3, 0.26],
  [-84, -68, 4.2, 0.3],
  [74, -72, 3.2, 0.28],
  [12, -98, 2.6, 0.24],
  [-44, -74, 2.8, 0.24],
  [92, -42, 2.6, 0.24],
  [-96, -34, 2.6, 0.22],
] as const;

function Brazo({ id, relleno }: { id: BrazoId; relleno: string }) {
  return (
    <g>
      <path data-brazo={id} fill={relleno} />
      <path
        data-luz={id}
        fill="none"
        stroke="#fff"
        strokeOpacity=".26"
        strokeWidth="7"
        strokeLinecap="round"
      />
      {Array.from({ length: BRAZOS[id].ventosas === 0 ? 0 : VENTOSAS_POR_BRAZO }, (_, k) => (
        <circle key={k} data-ventosa={id} r="3" fill="#ffe0b8" stroke="#e08a3c" strokeWidth="1.4" />
      ))}
    </g>
  );
}

type Ojo = { cx: number; giro: number; id: string };
const OJOS: Ojo[] = [
  { cx: -46, giro: -7, id: 'izq' },
  { cx: 46, giro: 7, id: 'der' },
];

export function Personaje({ prefijo }: { prefijo: string }) {
  const p = (nombre: string) => `${prefijo}-${nombre}`;
  const ref = (nombre: string) => `url(#${p(nombre)})`;
  return (
    <g>
      <defs>
        <radialGradient
          id={p('cuerpo')}
          gradientUnits="userSpaceOnUse"
          cx="-44"
          cy="-96"
          r="330"
          fx="-52"
          fy="-110"
        >
          <stop offset="0" stopColor="#ffb868" />
          <stop offset="0.3" stopColor="#fb9226" />
          <stop offset="0.62" stopColor="#ef7611" />
          <stop offset="1" stopColor="#d85f06" />
        </radialGradient>
        <linearGradient id={p('sombraPie')} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#8a3300" stopOpacity="0" />
          <stop offset="1" stopColor="#8a3300" stopOpacity=".3" />
        </linearGradient>
        <linearGradient id={p('ojoSombra')} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#e6d3b8" />
          <stop offset="0.35" stopColor="#fffaf0" />
          <stop offset="1" stopColor="#fffaf0" />
        </linearGradient>
        <radialGradient id={p('iris')} cx="50%" cy="68%" r="62%">
          <stop offset="0" stopColor="#5aa0e6" />
          <stop offset="0.55" stopColor="#1f4f86" />
          <stop offset="1" stopColor="#102a4a" />
        </radialGradient>
        <filter id={p('suave')} x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="4.5" />
        </filter>
        <filter id={p('brillo')} x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="2.4" />
        </filter>
        {OJOS.map(({ cx, giro, id }) => (
          <clipPath key={id} id={p(`ojo-${id}`)}>
            <ellipse cx={cx} cy="8" rx="32" ry="37" transform={`rotate(${giro} ${cx} 8)`} />
          </clipPath>
        ))}
        <clipPath id={p('cuerpoClip')}>
          <path d={CUERPO} />
        </clipPath>
      </defs>

      {/* Brazos de atrás: nacen detrás del cuerpo */}
      {ORDEN_ATRAS.map((id) => (
        <Brazo key={id} id={id} relleno={ref('cuerpo')} />
      ))}

      <g data-sami="cabeza">
        <path d={CUERPO} fill={ref('cuerpo')} />
      </g>

      {/* Brazos de frente (sostienen cosas); el nacimiento queda tapado por el cuerpo */}
      {ORDEN_FRENTE.map((id) => (
        <Brazo key={id} id={id} relleno={ref('cuerpo')} />
      ))}
      {ORDEN_FRENTE.map((id) => (
        <ellipse
          key={id}
          cx={BRAZOS[id].hombro.x}
          cy={BRAZOS[id].hombro.y}
          rx="30"
          ry="26"
          fill={ref('cuerpo')}
        />
      ))}

      {/* Cara, luces y sombras: sobre los brazos de frente */}
      <g data-sami="cabeza">
        {/* Sombra suave de la panza y luz de borde */}
        <g clipPath={ref('cuerpoClip')}>
          <rect x="-130" y="30" width="260" height="90" fill={ref('sombraPie')} />
          <ellipse
            cx="96"
            cy="-30"
            rx="26"
            ry="96"
            fill="#ffc07a"
            opacity=".22"
            filter={ref('suave')}
          />
        </g>
        {PECAS.map(([x, y, r, o], i) => (
          <circle key={i} cx={x} cy={y} r={r} fill="#d4600a" opacity={o} />
        ))}
        {/* Reflejos de la cabeza */}
        <ellipse
          cx="-48"
          cy="-104"
          rx="38"
          ry="18"
          fill="#fff"
          opacity=".34"
          filter={ref('brillo')}
          transform="rotate(-30 -48 -104)"
        />
        <ellipse
          cx="-86"
          cy="-64"
          rx="5"
          ry="11"
          fill="#fff"
          opacity=".26"
          filter={ref('brillo')}
        />

        {/* Mejillas */}
        <g filter={ref('suave')}>
          <ellipse cx="-76" cy="46" rx="18" ry="11" fill="#ff5a5a" opacity=".4" />
          <ellipse cx="76" cy="46" rx="18" ry="11" fill="#ff5a5a" opacity=".4" />
        </g>

        {/* Cejas */}
        <path
          data-sami="ceja"
          d="M-76 -42Q-52 -60-24 -50"
          fill="none"
          stroke="#7a3204"
          strokeWidth="4.6"
          strokeLinecap="round"
        />
        <path
          data-sami="ceja"
          d="M24 -50Q52 -60 76 -42"
          fill="none"
          stroke="#7a3204"
          strokeWidth="4.6"
          strokeLinecap="round"
        />

        {/* Ojos */}
        {OJOS.map(({ cx, giro, id }) => (
          <g key={id} data-sami="ojo">
            <g clipPath={ref(`ojo-${id}`)}>
              <ellipse
                cx={cx}
                cy="8"
                rx="32"
                ry="37"
                fill={ref('ojoSombra')}
                transform={`rotate(${giro} ${cx} 8)`}
              />
              <g data-sami="pupila">
                <circle cx={cx + 3} cy="12" r="23" fill={ref('iris')} />
                <circle cx={cx + 3} cy="12" r="12" fill="#06101d" />
                <circle cx={cx - 5} cy="2" r="7.5" fill="#fff" />
                <circle cx={cx + 10} cy="22" r="3.4" fill="#fff" opacity=".9" />
              </g>
              <g transform={`rotate(${giro} ${cx} 8)`}>
                <ellipse data-sami="parpado" cx={cx} cy="8" rx="34" ry="39" fill={ref('cuerpo')} />
              </g>
            </g>
          </g>
        ))}
        {/* Ojos felices (cerrados, sonriendo) */}
        <path
          data-sami="feliz"
          d="M-70 16Q-46-14-22 16M22 16Q46-14 70 16"
          fill="none"
          stroke="#3a1604"
          strokeWidth="6"
          strokeLinecap="round"
          opacity="0"
        />

        {/* Boca */}
        <path
          data-sami="boca"
          d="M-17 58Q0 73 17 58"
          fill="none"
          stroke="#6e2208"
          strokeWidth="4.6"
          strokeLinecap="round"
        />
        <g data-sami="bocaAbierta" opacity="0">
          <path d="M-20 54Q0 66 20 54Q18 85 0 87Q-18 85-20 54Z" fill="#7a1c10" />
          <ellipse cx="0" cy="80" rx="10" ry="5.5" fill="#ff7a6a" />
        </g>
      </g>
    </g>
  );
}
