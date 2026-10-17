import { useEffect, useId, useRef } from 'react';

interface Punto {
  x: number;
  y: number;
}

const COLORES = {
  naranja: '#ee8000',
  amarillo: '#ebce32',
  coral: '#f85f5a',
  celeste: '#4eb2d4',
  petroleo: '#00506a',
} as const;

// Duración del ciclo (s) y momento en que la E ya está armada, para mostrarla quieta.
const DURACION = 12;
const INSTANTE_ARMADO = DURACION * 0.8;

// Cada bloque tiene su lugar en la E del isotipo (`destino`) y un punto de reposo donde Sami lo sostiene.
const BLOQUES = [
  {
    color: 'naranja',
    w: 75,
    h: 110,
    destino: { x: 387.5, y: 165 },
    reposo: { x: 292, y: 100 },
    hombro: { x: 214, y: 150 },
    comba: 1,
  },
  {
    color: 'amarillo',
    w: 75,
    h: 50,
    destino: { x: 472.5, y: 135 },
    reposo: { x: 305, y: 196 },
    hombro: { x: 250, y: 200 },
    comba: -1,
  },
  {
    color: 'celeste',
    w: 75,
    h: 50,
    destino: { x: 472.5, y: 195 },
    reposo: { x: 300, y: 265 },
    hombro: { x: 246, y: 248 },
    comba: 1,
  },
  {
    color: 'petroleo',
    w: 75,
    h: 50,
    destino: { x: 387.5, y: 255 },
    reposo: { x: 262, y: 350 },
    hombro: { x: 206, y: 282 },
    comba: -1,
  },
  {
    color: 'coral',
    w: 75,
    h: 50,
    destino: { x: 472.5, y: 255 },
    reposo: { x: 344, y: 350 },
    hombro: { x: 228, y: 278 },
    comba: 1,
  },
] as const;

const SUAVE = '.4 0 .2 1';

const redondeo = (n: number) => Math.round(n * 10) / 10;

// Brazo como curva cúbica del hombro a la punta; `comba` curva el brazo hacia un lado u otro.
function brazo(hombro: Punto, punta: Punto, comba: number): string {
  const dx = punta.x - hombro.x;
  const dy = punta.y - hombro.y;
  const largo = Math.hypot(dx, dy) || 1;
  const nx = (-dy / largo) * comba * largo * 0.28;
  const ny = (dx / largo) * comba * largo * 0.28;
  const c1 = { x: hombro.x + dx * 0.3 + nx, y: hombro.y + dy * 0.3 + ny };
  const c2 = { x: hombro.x + dx * 0.75 - nx * 0.6, y: hombro.y + dy * 0.75 - ny * 0.6 };
  return `M${hombro.x} ${hombro.y}C${redondeo(c1.x)} ${redondeo(c1.y)} ${redondeo(c2.x)} ${redondeo(c2.y)} ${punta.x} ${punta.y}`;
}

const lista = (valores: (string | number)[]) => valores.join(';');
const splines = (tramos: number) => Array.from({ length: tramos }, () => SUAVE).join(';');

// Instantes (fracción del ciclo) en que cada bloque empieza a viajar, llega a la E y suelta.
function tiempos(i: number) {
  const sale = 0.04 + i * 0.13;
  const llega = sale + 0.1;
  const vuelve = llega + 0.09;
  return { sale, llega, vuelve };
}

// Brazos que no cargan nada: se mecen y uno saluda.
const BRAZOS_LIBRES = [
  { d: 'M126 232C96 214 70 186 62 146', saludo: 'M126 232C88 224 56 196 80 140', ancho: 22 },
  {
    d: 'M136 278C112 322 66 340 48 306',
    giro: [-3, 3] as const,
    centro: { x: 136, y: 278 },
    ancho: 22,
  },
  {
    d: 'M172 288C170 340 138 366 112 350',
    giro: [2.5, -2.5] as const,
    centro: { x: 172, y: 288 },
    ancho: 22,
  },
] as const;

// Sami, el pulpo de Ensambla, acomodando los cinco bloques hasta formar la E del isotipo.
export function SamiEscena() {
  const id = useId().replace(/:/g, '');
  const svg = useRef<SVGSVGElement>(null);

  // Con "reducir movimiento" la escena queda quieta, con la E ya armada.
  useEffect(() => {
    const el = svg.current;
    if (!el || typeof window.matchMedia !== 'function') return;
    const pedido = window.matchMedia('(prefers-reduced-motion: reduce)');
    const aplicar = () => {
      if (typeof el.pauseAnimations !== 'function') return;
      if (pedido.matches) {
        el.pauseAnimations();
        el.setCurrentTime(INSTANTE_ARMADO);
      } else {
        el.unpauseAnimations();
      }
    };
    aplicar();
    pedido.addEventListener('change', aplicar);
    return () => pedido.removeEventListener('change', aplicar);
  }, []);

  return (
    <svg
      ref={svg}
      className="sami__svg"
      viewBox="0 0 560 420"
      role="img"
      aria-label="Sami, el pulpo de Ensambla, acomoda cinco bloques hasta formar la E del logo"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <radialGradient id={`${id}-cuerpo`} cx="38%" cy="28%" r="80%">
          <stop offset="0" stopColor="#ffb04a" />
          <stop offset="0.55" stopColor="#ee8000" />
          <stop offset="1" stopColor="#d46f00" />
        </radialGradient>
      </defs>

      {/* Guía punteada de la E: el lugar donde Sami va a dejar cada bloque */}
      {BLOQUES.map(({ color, w, h, destino }) => (
        <rect
          key={`guia-${color}`}
          x={destino.x - w / 2}
          y={destino.y - h / 2}
          width={w}
          height={h}
          rx="12"
          fill="none"
          stroke="rgba(0,80,106,.28)"
          strokeWidth="2"
          strokeDasharray="6 7"
        />
      ))}

      {/* Brazos libres */}
      {BRAZOS_LIBRES.map((b, i) => (
        <g key={i}>
          <path
            d={b.d}
            fill="none"
            stroke={COLORES.naranja}
            strokeWidth={b.ancho}
            strokeLinecap="round"
          >
            {'saludo' in b && (
              <animate
                attributeName="d"
                values={lista([b.d, b.saludo, b.d])}
                dur="3.2s"
                repeatCount="indefinite"
                calcMode="spline"
                keyTimes="0;.5;1"
                keySplines={splines(2)}
              />
            )}
          </path>
          {'giro' in b && (
            <animateTransform
              attributeName="transform"
              type="rotate"
              values={`${b.giro[0]} ${b.centro.x} ${b.centro.y};${b.giro[1]} ${b.centro.x} ${b.centro.y};${b.giro[0]} ${b.centro.x} ${b.centro.y}`}
              dur="4.5s"
              repeatCount="indefinite"
              calcMode="spline"
              keyTimes="0;.5;1"
              keySplines={splines(2)}
            />
          )}
        </g>
      ))}

      {/* Brazos que llevan los bloques: del reposo a su lugar en la E y de vuelta */}
      {BLOQUES.map(({ color, reposo, destino, hombro, comba }, i) => {
        const { sale, llega, vuelve } = tiempos(i);
        const enReposo = brazo(hombro, reposo, comba);
        const enDestino = brazo(hombro, destino, comba);
        return (
          <path
            key={`brazo-${color}`}
            d={enReposo}
            fill="none"
            stroke={COLORES.naranja}
            strokeWidth="22"
            strokeLinecap="round"
          >
            <animate
              attributeName="d"
              values={lista([enReposo, enReposo, enDestino, enReposo, enReposo])}
              keyTimes={lista([0, sale, llega, vuelve, 1])}
              keySplines={splines(4)}
              calcMode="spline"
              dur={`${DURACION}s`}
              repeatCount="indefinite"
            />
          </path>
        );
      })}

      {/* Cuerpo */}
      <ellipse cx="180" cy="205" rx="82" ry="80" fill={`url(#${id}-cuerpo)`} />
      <ellipse
        cx="146"
        cy="148"
        rx="30"
        ry="16"
        fill="#fff"
        opacity="0.18"
        transform="rotate(-24 146 148)"
      />
      <g className="sami__ojos">
        <ellipse cx="150" cy="198" rx="21" ry="25" fill="#fff" />
        <ellipse cx="210" cy="198" rx="21" ry="25" fill="#fff" />
        <circle cx="154" cy="203" r="11" fill="#00303f" />
        <circle cx="206" cy="203" r="11" fill="#00303f" />
        <circle cx="158" cy="198" r="3.6" fill="#fff" />
        <circle cx="210" cy="198" r="3.6" fill="#fff" />
      </g>
      <circle cx="124" cy="232" r="9" fill={COLORES.coral} opacity="0.35" />
      <circle cx="236" cy="232" r="9" fill={COLORES.coral} opacity="0.35" />
      <path
        d="M160 236Q180 256 200 236"
        fill="none"
        stroke="#00303f"
        strokeWidth="5"
        strokeLinecap="round"
      />

      {/* Bloques: aparecen sostenidos por Sami, viajan a la E, se quedan y vuelven a empezar */}
      {BLOQUES.map(({ color, w, h, destino, reposo }, i) => {
        const { sale, llega } = tiempos(i);
        const dx = reposo.x - destino.x;
        const dy = reposo.y - destino.y;
        const reposoT = `${dx} ${dy}`;
        return (
          <g key={`bloque-${color}`} transform={`translate(${reposoT})`}>
            <animateTransform
              attributeName="transform"
              type="translate"
              values={lista([reposoT, reposoT, '0 0', '0 0', reposoT, reposoT])}
              keyTimes={lista([0, sale, llega, 0.92, 0.95, 1])}
              dur={`${DURACION}s`}
              repeatCount="indefinite"
              calcMode="linear"
            />
            <animate
              attributeName="opacity"
              values="1;1;0;0;1;1"
              keyTimes="0;.88;.92;.95;.99;1"
              dur={`${DURACION}s`}
              repeatCount="indefinite"
            />
            <rect
              x={destino.x - w / 2}
              y={destino.y - h / 2}
              width={w}
              height={h}
              rx="12"
              fill={COLORES[color]}
            />
            <rect
              x={destino.x - w / 2 + 6}
              y={destino.y - h / 2 + 6}
              width={w - 12}
              height={Math.min(14, h * 0.22)}
              rx="6"
              fill="#fff"
              opacity="0.22"
            />
          </g>
        );
      })}
    </svg>
  );
}

// Cara de Sami para usar como avatar en las burbujas de conversación.
export function SamiCara({ className }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 48 48" className={className} xmlns="http://www.w3.org/2000/svg">
      <circle cx="13" cy="38" r="5.5" fill={COLORES.naranja} />
      <circle cx="24" cy="41" r="5.5" fill={COLORES.naranja} />
      <circle cx="35" cy="38" r="5.5" fill={COLORES.naranja} />
      <circle cx="24" cy="22" r="19" fill={COLORES.naranja} />
      <ellipse cx="17" cy="20" rx="5.2" ry="6" fill="#fff" />
      <ellipse cx="31" cy="20" rx="5.2" ry="6" fill="#fff" />
      <circle cx="18" cy="21.5" r="2.7" fill="#00303f" />
      <circle cx="30" cy="21.5" r="2.7" fill="#00303f" />
      <path
        d="M19 30Q24 35 29 30"
        fill="none"
        stroke="#00303f"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
    </svg>
  );
}
