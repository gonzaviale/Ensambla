import { useId } from 'react';
import { gsap } from 'gsap';
import { Bloque, DefsBloques } from './Bloque';
import { BRAZOS, crearDibujante, estadoInicial, type BrazoId } from './brazos';
import { crearCara } from './cara';
import type { ColorBloque } from './colores';
import { arco, type Punto } from './geometria';
import { Personaje } from './Personaje';
import { useEscena } from './useEscena';

// Escena «Conocé a Sami»: los bloques llegan en una cinta por la izquierda, Sami los toma, los
// pasa de un brazo al otro y los ensambla a la derecha hasta formar la E de la marca (el bloque
// grande, naranja, es la persona que arma su sistema). Todo en una línea de tiempo de GSAP.

const CABEZA: Punto = { x: 462, y: 262 };
const SUELO = 430;
const CINTA_Y = 388;
const ANCHO = 104;
const ALTO = 76;
const Y_CINTA = CINTA_Y - 7 - ALTO / 2;
const RECIBE: Punto = { x: 204, y: Y_CINTA };
const PECHO: Punto = { x: CABEZA.x, y: 348 };
const SEPARACION = 2.8;

// Columnas y filas del tablero donde se arma la E (izquierda: naranja grande y petróleo).
const COL_IZQ = 664;
const COL_DER = 776;
const FILAS = [216, 302, 388];

type Pieza = { color: ColorBloque; destino: Punto };
const PIEZAS: Pieza[] = [
  { color: 'coral', destino: { x: COL_DER, y: FILAS[2] ?? 388 } },
  { color: 'celeste', destino: { x: COL_DER, y: FILAS[1] ?? 302 } },
  { color: 'amarillo', destino: { x: COL_DER, y: FILAS[0] ?? 216 } },
  { color: 'petroleo', destino: { x: COL_IZQ, y: FILAS[2] ?? 388 } },
  { color: 'naranja', destino: { x: COL_IZQ, y: FILAS[1] ?? 302 } },
  { color: 'naranja', destino: { x: COL_IZQ, y: FILAS[0] ?? 216 } },
];

const CONFETI = [
  { x: -60, y: -150, g: 40, c: '#ee8000' },
  { x: -20, y: -190, g: -70, c: '#ebce32' },
  { x: 30, y: -160, g: 110, c: '#f85f5a' },
  { x: 70, y: -200, g: -30, c: '#4eb2d4' },
  { x: 110, y: -140, g: 80, c: '#00506a' },
  { x: -100, y: -110, g: -120, c: '#f85f5a' },
  { x: 130, y: -90, g: 60, c: '#ebce32' },
  { x: -10, y: -120, g: 20, c: '#4eb2d4' },
  { x: 50, y: -100, g: -90, c: '#ee8000' },
  { x: -80, y: -190, g: 100, c: '#4eb2d4' },
] as const;

const aLocal = (p: Punto): Punto => ({ x: p.x - CABEZA.x, y: p.y - CABEZA.y });
const TEXTO_INICIO = 'Contame qué vendés…';
const TEXTO_FIN = '¡Listo! Tu sistema está armado';

function construir(svg: SVGSVGElement, reducido: boolean): gsap.core.Timeline | null {
  const buscar = gsap.utils.selector(svg);
  const estado = estadoInicial();
  const dibujar = crearDibujante(svg);
  const cara = crearCara(buscar, CABEZA);
  const chip = svg.parentElement?.querySelector('[data-chip]');
  const bloque = (i: number) => buscar(`[data-bloque="${i}"]`);
  const todos = buscar('[data-bloque]');
  const union = buscar('[data-union]');
  const confeti = buscar('[data-confeti]');
  const reposoBrazo = (id: BrazoId) => ({ ...BRAZOS[id].reposo, rizo: BRAZOS[id].rizo });

  gsap.set(todos, { transformOrigin: '50% 100%' });
  PIEZAS.forEach((p, i) => gsap.set(bloque(i), { x: p.destino.x, y: p.destino.y }));

  // Cuadro final: E armada, Sami contento con los brazos arriba (también para «reducir movimiento»).
  if (reducido) {
    gsap.set(todos, { opacity: 1 });
    gsap.set(union, { opacity: 1, scaleX: 1 });
    cara.fijarAlegre();
    Object.assign(estado.izqExt, { x: -152, y: -122, rizo: 0.45 });
    Object.assign(estado.derExt, { x: 152, y: -124, rizo: 0.45 });
    if (chip) chip.textContent = TEXTO_FIN;
    dibujar(estado);
    return null;
  }

  const maestra = gsap.timeline({ onUpdate: () => dibujar(estado) });

  // Vida propia: respira, parpadea, la cinta avanza y los brazos libres se mecen.
  const vida = gsap.timeline();
  vida.to(cara.cabezas, { y: -5, duration: 2.1, yoyo: true, repeat: -1, ease: 'sine.inOut' }, 0);
  vida.add(cara.parpadeo(), 1.2);
  vida.to(
    buscar('[data-cinta="marcas"]'),
    { attr: { 'stroke-dashoffset': -26 }, duration: 0.7, repeat: -1, ease: 'none' },
    0,
  );
  buscar('[data-cinta="rodillo"]').forEach((el, i) =>
    vida.to(
      el,
      { rotation: 360, svgOrigin: `${28 + i * 46} 412`, duration: 1.4, repeat: -1, ease: 'none' },
      0,
    ),
  );
  const mecer = (id: BrazoId, dx: number, dy: number, dur: number, demora = 0) =>
    vida.to(
      estado[id],
      { x: `+=${dx}`, y: `+=${dy}`, duration: dur, yoyo: true, repeat: -1, ease: 'sine.inOut' },
      demora,
    );
  mecer('atrasIzq', -10, 5, 2.3, 0.2);
  mecer('atrasDer', 10, 5, 2.5, 0.5);
  mecer('izqCent', -6, 4, 1.9, 0.8);
  mecer('derCent', 6, 4, 2.1, 0.3);
  maestra.add(vida, 0);

  // Ciclo: pieza por pieza hasta armar la E; después, a festejar.
  const ciclo = gsap.timeline({ repeat: -1, repeatDelay: 0.8 });
  ciclo.set(todos, { opacity: 0 }, 0);
  ciclo.set(union, { opacity: 0, scaleX: 0.3, transformOrigin: '50% 50%' }, 0);
  ciclo.set(confeti, { opacity: 0, x: 0, y: 0, rotation: 0 }, 0);
  ciclo.call(() => chip && (chip.textContent = TEXTO_INICIO), undefined, 0);

  const llevar = (
    i: number,
    brazo: BrazoId,
    desde: Punto,
    hasta: Punto,
    apice: number,
    t: number,
    dur: number,
  ) => {
    const avance = { t: 0 };
    ciclo.to(
      avance,
      {
        t: 1,
        duration: dur,
        ease: 'power1.inOut',
        onUpdate: () => {
          const pos = arco(desde, hasta, apice, avance.t);
          gsap.set(bloque(i), { x: pos.x, y: pos.y });
          const l = aLocal(pos);
          Object.assign(estado[brazo], { x: l.x, y: l.y });
        },
      },
      t,
    );
  };
  const llevarBrazo = (id: BrazoId, destino: Punto, t: number, dur: number) => {
    const l = aLocal(destino);
    ciclo.to(estado[id], { x: l.x, y: l.y, rizo: 0, duration: dur, ease: 'power2.out' }, t);
  };
  const soltarBrazo = (id: BrazoId, t: number) =>
    ciclo.to(estado[id], { ...reposoBrazo(id), duration: 0.7, ease: 'power2.inOut' }, t);

  PIEZAS.forEach((pieza, i) => {
    const t = i * SEPARACION;
    const el = bloque(i);
    // 1) Llega en la cinta
    ciclo.set(el, { x: -90, y: Y_CINTA, opacity: 1, scale: 1, rotation: 0 }, t);
    ciclo.to(el, { x: RECIBE.x, duration: 1.45, ease: 'none' }, t);
    cara.mirar(ciclo, RECIBE, t + 0.3);
    // 2) La mano izquierda lo toma y lo trae al pecho
    llevarBrazo('izqTrab', RECIBE, t + 1.0, 0.5);
    llevar(i, 'izqTrab', RECIBE, PECHO, 54, t + 1.5, 0.8);
    cara.mirar(ciclo, PECHO, t + 1.6);
    // 3) Se lo pasa a la derecha: un guiño de aprobación
    ciclo.to(
      el,
      { scale: 1.07, duration: 0.14, yoyo: true, repeat: 1, ease: 'sine.inOut' },
      t + 2.25,
    );
    ciclo.to(cara.cejas, { y: -6, duration: 0.14, yoyo: true, repeat: 1 }, t + 2.25);
    llevarBrazo('derTrab', PECHO, t + 1.95, 0.4);
    soltarBrazo('izqTrab', t + 2.4);
    // 4) La derecha lo lleva a su lugar en el tablero
    llevar(i, 'derTrab', PECHO, pieza.destino, 104, t + 2.45, 0.85);
    cara.mirar(ciclo, pieza.destino, t + 2.45);
    ciclo.to(
      el,
      { scaleY: 0.9, scaleX: 1.04, duration: 0.09, yoyo: true, repeat: 1, ease: 'sine.out' },
      t + 3.3,
    );
    soltarBrazo('derTrab', t + 3.4);
  });

  // Final: los dos naranjas se unen en el bloque grande y Sami festeja
  const tFin = PIEZAS.length * SEPARACION + 0.7;
  cara.mirar(ciclo, { x: 700, y: 280 }, tFin - 0.5, 0.3);
  ciclo.to(union, { opacity: 1, scaleX: 1, duration: 0.35, ease: 'back.out(2)' }, tFin);
  ciclo.to(
    [bloque(4), bloque(5), union],
    { scale: 1.05, duration: 0.18, yoyo: true, repeat: 1, ease: 'sine.inOut' },
    tFin + 0.3,
  );
  cara.alegre(ciclo, tFin + 0.3);
  ciclo.to(
    estado.izqExt,
    { x: -152, y: -122, rizo: 0.45, duration: 0.7, ease: 'back.out(1.6)' },
    tFin + 0.3,
  );
  ciclo.to(
    estado.derExt,
    { x: 152, y: -124, rizo: 0.45, duration: 0.7, ease: 'back.out(1.6)' },
    tFin + 0.35,
  );
  ciclo.to(
    estado.izqExt,
    { x: '-=14', y: '-=10', duration: 0.3, yoyo: true, repeat: 5, ease: 'sine.inOut' },
    tFin + 1.0,
  );
  ciclo.to(
    estado.derExt,
    { x: '+=14', y: '-=10', duration: 0.3, yoyo: true, repeat: 5, ease: 'sine.inOut' },
    tFin + 1.05,
  );
  ciclo.to(
    cara.cabezas,
    { y: -18, duration: 0.2, yoyo: true, repeat: 3, ease: 'power2.out' },
    tFin + 0.5,
  );
  ciclo.call(() => chip && (chip.textContent = TEXTO_FIN), undefined, tFin + 0.3);
  confeti.forEach((el, k) => {
    const c = CONFETI[k % CONFETI.length];
    if (!c) return;
    ciclo.to(el, { opacity: 1, duration: 0.1 }, tFin + 0.35);
    ciclo.to(
      el,
      { x: c.x, y: c.y, rotation: c.g * 3, duration: 1.1, ease: 'power2.out' },
      tFin + 0.35,
    );
    ciclo.to(el, { y: c.y + 70, opacity: 0, duration: 0.9, ease: 'power1.in' }, tFin + 1.45);
  });

  // Vuelta a empezar
  const tVuelta = tFin + 3.8;
  cara.serena(ciclo, tVuelta);
  ciclo.to(
    estado.izqExt,
    { ...reposoBrazo('izqExt'), duration: 0.8, ease: 'power2.inOut' },
    tVuelta,
  );
  ciclo.to(
    estado.derExt,
    { ...reposoBrazo('derExt'), duration: 0.8, ease: 'power2.inOut' },
    tVuelta,
  );
  ciclo.to([...todos, ...union], { opacity: 0, duration: 0.6 }, tVuelta + 0.3);
  maestra.add(ciclo, 0);

  dibujar(estado);
  return maestra;
}

export function EscenaBloques() {
  const prefijo = useId().replace(/:/g, '');
  const { svg, pausado, alternar, animable } = useEscena(construir);
  const x0 = COL_IZQ - ANCHO / 2;

  return (
    <div className="sami-lienzo">
      <svg
        ref={svg}
        className="sami-svg"
        viewBox="0 0 960 540"
        role="img"
        aria-label="Sami, el asistente de Ensambla, recibe bloques por una cinta a la izquierda, los toma, los pasa de un brazo al otro y los ensambla a la derecha hasta armar la E de la marca."
        xmlns="http://www.w3.org/2000/svg"
      >
        <DefsBloques prefijo={prefijo} />
        <defs>
          <pattern id={`${prefijo}-azulejo`} width="64" height="64" patternUnits="userSpaceOnUse">
            <rect width="64" height="64" fill="#fcf3e6" />
            <path d="M0 .75H64M.75 0V64" stroke="#f1e2cb" strokeWidth="1.5" />
          </pattern>
          <linearGradient id={`${prefijo}-piso`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#f3dcb6" />
            <stop offset="1" stopColor="#e8c796" />
          </linearGradient>
          <linearGradient id={`${prefijo}-cinta`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#4a4f57" />
            <stop offset="1" stopColor="#30343a" />
          </linearGradient>
        </defs>

        {/* Pared, ventana y piso */}
        <rect width="960" height={SUELO} fill={`url(#${prefijo}-azulejo)`} />
        <rect x="820" y="0" width="140" height="210" fill="#f6e7d0" />
        <rect y={SUELO} width="960" height={540 - SUELO} fill={`url(#${prefijo}-piso)`} />
        <rect y={SUELO} width="960" height="5" fill="#d9b985" opacity=".6" />

        {/* Tablero donde se arma la E: lugares marcados con línea de puntos */}
        <rect x={x0 - 14} y="166" width={ANCHO * 2 + 10 + 28} height="278" rx="22" fill="#ead8b8" />
        <rect
          x={x0 - 14}
          y="166"
          width={ANCHO * 2 + 10 + 28}
          height="278"
          rx="22"
          fill="none"
          stroke="#d3b98f"
          strokeWidth="2"
        />
        {[
          [COL_IZQ, FILAS[0], 'a'],
          [COL_IZQ, FILAS[1], 'b'],
          [COL_IZQ, FILAS[2], 'c'],
          [COL_DER, FILAS[0], 'd'],
          [COL_DER, FILAS[1], 'e'],
          [COL_DER, FILAS[2], 'f'],
        ].map(([cx, cy, k]) => (
          <rect
            key={String(k)}
            x={Number(cx) - ANCHO / 2 + 4}
            y={Number(cy) - ALTO / 2 + 4}
            width={ANCHO - 8}
            height={ALTO - 8}
            rx="12"
            fill="none"
            stroke="#c4a674"
            strokeWidth="2"
            strokeDasharray="7 7"
          />
        ))}

        {/* Cinta transportadora */}
        <rect x="0" y={CINTA_Y} width="262" height="46" rx="10" fill={`url(#${prefijo}-cinta)`} />
        <rect x="0" y={CINTA_Y} width="262" height="6" rx="3" fill="#6a707a" />
        <path
          data-cinta="marcas"
          d={`M0 ${CINTA_Y + 15}H262`}
          stroke="#22262b"
          strokeWidth="6"
          strokeDasharray="14 12"
          fill="none"
        />
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <g key={i} data-cinta="rodillo">
            <circle
              cx={28 + i * 46}
              cy="412"
              r="11"
              fill="#22262b"
              stroke="#585e68"
              strokeWidth="2"
            />
            <path
              d={`M${28 + i * 46} 404V420`}
              stroke="#585e68"
              strokeWidth="2.4"
              strokeLinecap="round"
            />
          </g>
        ))}

        {/* Sombras de Sami y del tablero */}
        <ellipse cx={CABEZA.x} cy={SUELO + 22} rx="190" ry="20" fill="#7a4a10" opacity=".16" />
        <ellipse
          cx={x0 + ANCHO + 5}
          cy={SUELO + 18}
          rx="150"
          ry="13"
          fill="#7a4a10"
          opacity=".14"
        />

        <g transform={`translate(${CABEZA.x} ${CABEZA.y})`}>
          <Personaje prefijo={prefijo} />
        </g>

        {PIEZAS.map((p, i) => (
          <g key={i} data-bloque={i}>
            <Bloque color={p.color} ancho={ANCHO} alto={ALTO} prefijo={prefijo} />
          </g>
        ))}
        <rect
          data-union
          x={x0}
          y={((FILAS[0] ?? 216) + (FILAS[1] ?? 302)) / 2 - 9}
          width={ANCHO}
          height="18"
          fill="#ee8000"
          opacity="0"
        />

        {CONFETI.map((c, k) => (
          <rect
            key={k}
            data-confeti
            x={(COL_IZQ + COL_DER) / 2 - 6}
            y={FILAS[0] ?? 216}
            width="12"
            height="12"
            rx="3"
            fill={c.c}
            opacity="0"
          />
        ))}
      </svg>
      <p className="sami-chip" data-chip aria-hidden="true">
        {TEXTO_INICIO}
      </p>
      {animable && (
        <button type="button" className="sami-pausa" onClick={alternar} aria-pressed={pausado}>
          {pausado ? 'Reanudar animación' : 'Pausar animación'}
        </button>
      )}
    </div>
  );
}
