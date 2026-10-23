import { useId } from 'react';
import { gsap } from 'gsap';
import { BRAZOS, crearDibujante, estadoInicial } from './brazos';
import { crearCara } from './cara';
import type { Punto } from './geometria';
import { Personaje } from './Personaje';
import { useEscena } from './useEscena';

// Escena «Sami vigila»: Sami, sentado frente a la pared de monitores, los mira uno por uno. En uno
// se dibuja la curva de ventas, en otro se arma el gráfico de productos, sube el stock y, de pronto,
// aparece un aviso de precio: Sami se sorprende, lo señala con un tentáculo y lo anota.

const CABEZA: Punto = { x: 480, y: 322 };

const M = {
  ventas: { x: 36, y: 28, w: 232, h: 150 },
  productos: { x: 36, y: 196, w: 232, h: 150 },
  estado: { x: 340, y: 18, w: 280, h: 128 },
  stock: { x: 692, y: 28, w: 232, h: 150 },
  precios: { x: 692, y: 196, w: 232, h: 150 },
} as const;
const CENTRO = {
  ventas: { x: 152, y: 103 },
  productos: { x: 152, y: 271 },
  estado: { x: 480, y: 82 },
  stock: { x: 808, y: 103 },
  precios: { x: 808, y: 271 },
} as const;

const CURVA = 'M54 156L88 138L120 146L154 112L186 120L218 90L250 76';
const AREA = `${CURVA}L250 164L54 164Z`;
const CIRCUNFERENCIA = 2 * Math.PI * 38;
const SEGMENTOS = [
  { color: '#4eb2d4', parte: 0.4 },
  { color: '#ebce32', parte: 0.28 },
  { color: '#f85f5a', parte: 0.2 },
  { color: '#e9f4f8', parte: 0.12 },
] as const;
const BARRAS = [
  { alto: 78, color: '#4eb2d4' },
  { alto: 64, color: '#4eb2d4' },
  { alto: 52, color: '#ebce32' },
  { alto: 36, color: '#ebce32' },
  { alto: 16, color: '#f85f5a' },
] as const;
const FILAS_PRECIOS = [
  { nombre: 'Leche', precio: '$ 1.150' },
  { nombre: 'Pan', precio: '$ 900' },
  { nombre: 'Yerba', precio: '$ 2.400' },
] as const;

const TIERRA_PAPEL: Punto = { x: 262, y: 474 };
const PUNTO_AVISO: Punto = { x: 716, y: 312 };
const aLocal = (p: Punto): Punto => ({ x: p.x - CABEZA.x, y: p.y - CABEZA.y });

function construir(svg: SVGSVGElement, reducido: boolean): gsap.core.Timeline | null {
  const buscar = gsap.utils.selector(svg);
  const estado = estadoInicial();
  Object.assign(estado.izqTrab, { ...aLocal(TIERRA_PAPEL), rizo: 0 });
  const cara = crearCara(buscar, CABEZA);
  const redibujar = crearDibujante(svg);
  const lapiz = buscar('[data-lapiz]');
  const dibujar = () => {
    redibujar(estado);
    gsap.set(lapiz, { x: CABEZA.x + estado.izqTrab.x - 12, y: CABEZA.y + estado.izqTrab.y + 20 });
  };

  const curva = buscar('[data-curva]');
  const area = buscar('[data-area]');
  const punto = buscar('[data-punto]');
  const segmentos = buscar('[data-segmento]');
  const barras = buscar('[data-barra]');
  const todoBien = buscar('[data-estado="bien"]');
  const hayAviso = buscar('[data-estado="aviso"]');
  const tarjeta = buscar('[data-tarjeta]');
  const filas = buscar('[data-fila]');
  const garabato = buscar('[data-garabato]');
  const vapor = buscar('[data-vapor]');
  const luces = buscar('[data-luz-led]');
  const largo = (curva[0] as SVGPathElement | undefined)?.getTotalLength?.() ?? 260;
  const largoGarabato = (garabato[0] as SVGPathElement | undefined)?.getTotalLength?.() ?? 120;
  const dasSegmento = (k: number) => {
    const s = SEGMENTOS[k];
    return s ? s.parte * CIRCUNFERENCIA : 0;
  };

  gsap.set(barras, { transformOrigin: '50% 100%' });
  gsap.set([punto, tarjeta], { transformOrigin: '50% 50%' });

  // Cuadro final: todo dibujado, el aviso a la vista y Sami señalándolo.
  if (reducido) {
    gsap.set(curva, { strokeDasharray: largo, strokeDashoffset: 0 });
    gsap.set(area, { opacity: 1 });
    gsap.set(segmentos, {
      attr: { 'stroke-dasharray': (k: number) => `${dasSegmento(k)} ${CIRCUNFERENCIA}` },
    });
    gsap.set(barras, { scaleY: 1 });
    gsap.set(todoBien, { opacity: 0 });
    gsap.set(hayAviso, { opacity: 1 });
    gsap.set(tarjeta, { opacity: 1, scale: 1 });
    gsap.set(filas, { opacity: 0.35 });
    gsap.set(garabato, { strokeDasharray: largoGarabato, strokeDashoffset: 0 });
    Object.assign(estado.derExt, { ...aLocal(PUNTO_AVISO), rizo: 0 });
    gsap.set(cara.pupilas, { x: 8, y: 0 });
    dibujar();
    return null;
  }

  const maestra = gsap.timeline({ onUpdate: dibujar });

  // Vida propia: respira, parpadea, sube el vapor de la taza y titilan las luces de la consola.
  const vida = gsap.timeline();
  vida.to(cara.cabezas, { y: -4, duration: 2.3, yoyo: true, repeat: -1, ease: 'sine.inOut' }, 0);
  vida.add(cara.parpadeo(), 1.4);
  vapor.forEach((el, i) =>
    vida.fromTo(
      el,
      { y: 6, opacity: 0 },
      { y: -16, opacity: 0.7, duration: 1.6, repeat: -1, ease: 'sine.out', yoyo: false },
      i * 0.5,
    ),
  );
  luces.forEach((el, i) =>
    vida.to(
      el,
      { opacity: 0.35, duration: 0.7 + (i % 3) * 0.3, yoyo: true, repeat: -1, ease: 'sine.inOut' },
      i * 0.2,
    ),
  );
  const mecer = (
    id: 'atrasIzq' | 'atrasDer' | 'izqCent' | 'derCent',
    dx: number,
    dy: number,
    dur: number,
    demora: number,
  ) =>
    vida.to(
      estado[id],
      { x: `+=${dx}`, y: `+=${dy}`, duration: dur, yoyo: true, repeat: -1, ease: 'sine.inOut' },
      demora,
    );
  mecer('atrasIzq', -8, 4, 2.4, 0.2);
  mecer('atrasDer', 8, 4, 2.6, 0.5);
  mecer('izqCent', -5, 3, 2, 0.8);
  mecer('derCent', 5, 3, 2.2, 0.3);
  maestra.add(vida, 0);

  const ciclo = gsap.timeline({ repeat: -1, repeatDelay: 0.6 });
  ciclo.set(curva, { strokeDasharray: largo, strokeDashoffset: largo }, 0);
  ciclo.set(area, { opacity: 0 }, 0);
  ciclo.set(punto, { scale: 0, opacity: 0 }, 0);
  ciclo.set(segmentos, { attr: { 'stroke-dasharray': `0 ${CIRCUNFERENCIA}` } }, 0);
  ciclo.set(barras, { scaleY: 0 }, 0);
  ciclo.set(todoBien, { opacity: 1 }, 0);
  ciclo.set(hayAviso, { opacity: 0 }, 0);
  ciclo.set(tarjeta, { opacity: 0, scale: 0.5, rotation: 0 }, 0);
  ciclo.set(filas, { opacity: 1 }, 0);
  ciclo.set(garabato, { strokeDasharray: largoGarabato, strokeDashoffset: largoGarabato }, 0);
  ciclo.set(cara.pupilas, { x: 0, y: 0 }, 0);

  const inclinar = (grados: number, t: number) =>
    ciclo.to(cara.cabezas, { rotation: grados, duration: 0.7, ease: 'power2.inOut' }, t);

  // 1) Ventas: se dibuja la curva
  cara.mirar(ciclo, CENTRO.ventas, 0.3);
  inclinar(-3, 0.3);
  ciclo.to(curva, { strokeDashoffset: 0, duration: 1.6, ease: 'power1.inOut' }, 0.6);
  ciclo.to(area, { opacity: 1, duration: 0.6 }, 1.8);
  ciclo.to(punto, { scale: 1, opacity: 1, duration: 0.4, ease: 'back.out(3)' }, 2.1);

  // 2) Productos: se arma el gráfico
  cara.mirar(ciclo, CENTRO.productos, 2.5);
  inclinar(-4, 2.5);
  SEGMENTOS.forEach((_, k) =>
    ciclo.to(
      segmentos[k] ?? [],
      {
        attr: { 'stroke-dasharray': `${dasSegmento(k)} ${CIRCUNFERENCIA}` },
        duration: 0.5,
        ease: 'power2.out',
      },
      2.8 + k * 0.28,
    ),
  );

  // 3) Estado general: todo en orden
  cara.mirar(ciclo, CENTRO.estado, 4.4);
  inclinar(0, 4.4);
  ciclo.to(
    todoBien,
    { scale: 1.03, duration: 0.25, yoyo: true, repeat: 1, transformOrigin: '50% 50%' },
    4.7,
  );

  // 4) Stock: suben las barras
  cara.mirar(ciclo, CENTRO.stock, 5.3);
  inclinar(3, 5.3);
  ciclo.to(barras, { scaleY: 1, duration: 0.6, stagger: 0.12, ease: 'power2.out' }, 5.5);

  // 5) Precios: aparece un aviso
  cara.mirar(ciclo, CENTRO.precios, 6.9);
  inclinar(3, 6.9);
  ciclo.to(todoBien, { opacity: 0, duration: 0.25 }, 7.4);
  ciclo.to(hayAviso, { opacity: 1, duration: 0.25 }, 7.4);
  ciclo.to(filas, { opacity: 0.3, duration: 0.3 }, 7.4);
  ciclo.to(tarjeta, { opacity: 1, scale: 1, duration: 0.5, ease: 'back.out(2.6)' }, 7.5);
  cara.sorpresa(ciclo, 7.55);
  ciclo.to(tarjeta, { rotation: 2.2, duration: 0.07, yoyo: true, repeat: 5, ease: 'none' }, 8.1);

  // 6) Lo señala con un tentáculo y lo anota
  const p = aLocal(PUNTO_AVISO);
  ciclo.to(estado.derExt, { x: p.x, y: p.y, rizo: 0, duration: 0.8, ease: 'power2.inOut' }, 8.0);
  cara.serena(ciclo, 9.2);
  ciclo.to(cara.cabezas, { y: -8, duration: 0.18, yoyo: true, repeat: 3, ease: 'sine.inOut' }, 9.3);
  ciclo.to(garabato, { strokeDashoffset: 0, duration: 2.0, ease: 'none' }, 9.4);
  ciclo.to(
    estado.izqTrab,
    { x: '+=26', y: '-=6', duration: 0.16, yoyo: true, repeat: 11, ease: 'sine.inOut' },
    9.4,
  );
  cara.mirar(ciclo, TIERRA_PAPEL, 10.2, 0.4);

  // Vuelta a empezar
  cara.mirar(ciclo, { x: CABEZA.x, y: CABEZA.y }, 11.6, 0.5);
  inclinar(0, 11.6);
  ciclo.to(
    estado.derExt,
    { ...BRAZOS.derExt.reposo, rizo: BRAZOS.derExt.rizo, duration: 0.8, ease: 'power2.inOut' },
    11.6,
  );
  ciclo.to([tarjeta, hayAviso], { opacity: 0, duration: 0.4 }, 12.2);
  ciclo.to(todoBien, { opacity: 1, duration: 0.4 }, 12.3);
  ciclo.to(filas, { opacity: 1, duration: 0.4 }, 12.3);
  maestra.add(ciclo, 0);

  dibujar();
  return maestra;
}

function Monitor({
  x,
  y,
  w,
  h,
  prefijo,
}: {
  x: number;
  y: number;
  w: number;
  h: number;
  prefijo: string;
}) {
  return (
    <g>
      <rect
        x={x}
        y={y}
        width={w}
        height={h}
        rx="14"
        fill="#0a1822"
        stroke="#2a4d60"
        strokeWidth="2.5"
      />
      <rect
        x={x + 8}
        y={y + 8}
        width={w - 16}
        height={h - 22}
        rx="8"
        fill={`url(#${prefijo}-pantalla)`}
      />
      <path
        d={`M${x + 8} ${y + h - 14}L${x + 8} ${y + 16}Q${x + 8} ${y + 8} ${x + 16} ${y + 8}L${x + w * 0.5} ${y + 8}L${x + w * 0.3} ${y + h - 14}Z`}
        fill="#fff"
        opacity=".035"
      />
      <circle cx={x + w - 18} cy={y + h - 7} r="2.6" fill="#4eb2d4" data-luz-led />
    </g>
  );
}

export function EscenaMonitores() {
  const prefijo = useId().replace(/:/g, '');
  const { svg, pausado, alternar, animable } = useEscena(construir);
  const botones = [
    '#ee8000',
    '#ebce32',
    '#4eb2d4',
    '#f85f5a',
    '#ebce32',
    '#4eb2d4',
    '#ee8000',
    '#f85f5a',
  ];

  return (
    <div className="sami-lienzo">
      <svg
        ref={svg}
        className="sami-svg"
        viewBox="0 0 960 540"
        role="img"
        aria-label="Sami, el asistente de Ensambla, mira una pared de monitores: se dibuja la curva de ventas, se arma el gráfico de productos, sube el stock y aparece un aviso de precio que Sami señala y anota."
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id={`${prefijo}-fondo`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#10303f" />
            <stop offset="1" stopColor="#081c27" />
          </linearGradient>
          <linearGradient id={`${prefijo}-pantalla`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#14394d" />
            <stop offset="1" stopColor="#0c2432" />
          </linearGradient>
          <linearGradient id={`${prefijo}-area`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#4eb2d4" stopOpacity=".45" />
            <stop offset="1" stopColor="#4eb2d4" stopOpacity="0" />
          </linearGradient>
          <radialGradient id={`${prefijo}-luz`} cx="50%" cy="38%" r="60%">
            <stop offset="0" stopColor="#2f6a85" stopOpacity=".55" />
            <stop offset="1" stopColor="#2f6a85" stopOpacity="0" />
          </radialGradient>
        </defs>

        <rect width="960" height="540" fill={`url(#${prefijo}-fondo)`} />
        <rect width="960" height="540" fill={`url(#${prefijo}-luz)`} />

        {/* Monitores */}
        <Monitor {...M.ventas} prefijo={prefijo} />
        <text x="56" y="62" fontSize="17" fontWeight="800" fill="#cfe3ec">
          Ventas
        </text>
        <path d="M54 110H250M54 136H250M54 162H250" stroke="#2d5d70" strokeWidth="1.5" />
        <path data-area d={AREA} fill={`url(#${prefijo}-area)`} />
        <path
          data-curva
          d={CURVA}
          fill="none"
          stroke="#4eb2d4"
          strokeWidth="5.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle data-punto cx="250" cy="76" r="8" fill="#ebce32" />

        <Monitor {...M.productos} prefijo={prefijo} />
        <text x="56" y="230" fontSize="17" fontWeight="800" fill="#cfe3ec">
          Lo que más vendés
        </text>
        <g transform="rotate(-90 110 288)" fill="none" strokeWidth="18">
          {SEGMENTOS.map((s, k) => {
            const antes = SEGMENTOS.slice(0, k).reduce((a, b) => a + b.parte, 0);
            return (
              <circle
                key={s.color}
                data-segmento
                cx="110"
                cy="288"
                r="38"
                stroke={s.color}
                strokeDashoffset={-antes * CIRCUNFERENCIA}
                strokeDasharray={`0 ${CIRCUNFERENCIA}`}
              />
            );
          })}
        </g>
        {[0, 1, 2].map((i) => (
          <g key={i}>
            <circle cx="178" cy={262 + i * 26} r="6" fill={SEGMENTOS[i]?.color} />
            <rect
              x="192"
              y={257 + i * 26}
              width={54 - i * 10}
              height="9"
              rx="4.5"
              fill="#cfe3ec"
              opacity=".5"
            />
          </g>
        ))}

        <Monitor {...M.estado} prefijo={prefijo} />
        <g data-estado="bien">
          <circle cx="398" cy="82" r="26" fill="#4eb2d4" />
          <path
            d="M386 82l9 9 17-19"
            fill="none"
            stroke="#fff"
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <text x="438" y="76" fontSize="22" fontWeight="800" fill="#fff">
            Todo en orden
          </text>
          <text x="438" y="102" fontSize="15" fontWeight="600" fill="#9fc3d3">
            Sami está mirando
          </text>
        </g>
        <g data-estado="aviso" opacity="0">
          <circle cx="398" cy="82" r="26" fill="#f85f5a" />
          <text x="398" y="94" fontSize="34" fontWeight="800" fill="#fff" textAnchor="middle">
            !
          </text>
          <text x="438" y="76" fontSize="22" fontWeight="800" fill="#fff">
            Hay un aviso
          </text>
          <text x="438" y="102" fontSize="15" fontWeight="600" fill="#ffc9c6">
            Mirá los precios
          </text>
        </g>

        <Monitor {...M.stock} prefijo={prefijo} />
        <text x="712" y="62" fontSize="17" fontWeight="800" fill="#cfe3ec">
          Stock
        </text>
        <path d="M712 156H904" stroke="#2d5d70" strokeWidth="2" />
        {BARRAS.map((b, i) => (
          <rect
            key={i}
            data-barra
            x={718 + i * 36}
            y={156 - b.alto}
            width="24"
            height={b.alto}
            rx="5"
            fill={b.color}
          />
        ))}

        <Monitor {...M.precios} prefijo={prefijo} />
        <text x="712" y="230" fontSize="17" fontWeight="800" fill="#cfe3ec">
          Precios
        </text>
        {FILAS_PRECIOS.map((f, i) => (
          <g key={f.nombre} data-fila>
            <text x="714" y={262 + i * 26} fontSize="15" fontWeight="600" fill="#cfe3ec">
              {f.nombre}
            </text>
            <text
              x="902"
              y={262 + i * 26}
              fontSize="15"
              fontWeight="700"
              fill="#fff"
              textAnchor="end"
            >
              {f.precio}
            </text>
          </g>
        ))}
        <g data-tarjeta>
          <rect x="706" y="226" width="204" height="100" rx="14" fill="#f85f5a" />
          <circle cx="736" cy="276" r="17" fill="#fff" />
          <text x="736" y="283" fontSize="22" fontWeight="800" fill="#00384a" textAnchor="middle">
            !
          </text>
          <text x="762" y="270" fontSize="18" fontWeight="800" fill="#00384a">
            ¡Subió la yerba!
          </text>
          <text x="762" y="294" fontSize="15" fontWeight="700" fill="#00384a">
            Ajustá el precio
          </text>
        </g>

        {/* Consola */}
        <rect y="448" width="960" height="92" fill="#0d2431" />
        <rect y="448" width="960" height="6" fill="#1d4459" />
        {botones.map((c, i) => (
          <rect
            key={i}
            data-luz-led
            x={i < 4 ? 30 + i * 34 : 770 + (i - 4) * 36}
            y={i % 2 === 0 ? 480 : 506}
            width="22"
            height="16"
            rx="5"
            fill={c}
            opacity=".9"
          />
        ))}
        <circle cx="140" cy="512" r="9" fill="#25495c" />
        <circle cx="880" cy="500" r="11" fill="#25495c" />

        {/* Sombra y Sami */}
        <ellipse cx={CABEZA.x} cy="502" rx="210" ry="22" fill="#000" opacity=".28" />
        <g transform={`translate(${CABEZA.x} ${CABEZA.y})`}>
          <Personaje prefijo={prefijo} />
        </g>

        {/* Libreta con lo que Sami anota */}
        <g>
          <path d="M184 486L348 482L356 530L176 534Z" fill="#fdf6e6" />
          <path d="M192 502L342 499M190 516L346 512" stroke="#b9c9d0" strokeWidth="2" />
          <path
            data-garabato
            d="M214 500q10-10 18 0t18 0t18 0t18 0t18 0M214 516q10-10 18 0t18 0t18 0t18 0"
            fill="none"
            stroke="#00506a"
            strokeWidth="2.6"
            strokeLinecap="round"
          />
        </g>
        <g data-lapiz>
          <g transform="rotate(-52)">
            <rect x="8" y="-5" width="62" height="10" rx="2" fill="#ebce32" />
            <path d="M0 0L9-5V5Z" fill="#f4d9b0" />
            <path d="M0 0L3.4-1.8V1.8Z" fill="#3a2a1a" />
            <rect x="66" y="-5" width="6" height="10" fill="#c9d3d8" />
            <rect x="72" y="-5" width="9" height="10" rx="3" fill="#f85f5a" />
          </g>
        </g>

        {/* Taza de café con vapor */}
        <g transform="translate(-150 0)">
          <path d="M788 478h46v26q0 14-14 14h-18q-14 0-14-14z" fill="#f4f8fa" />
          <path d="M834 484h9a9 9 0 0 1 0 18h-9" fill="none" stroke="#f4f8fa" strokeWidth="5" />
          <ellipse cx="811" cy="478" rx="23" ry="5" fill="#6b3d1d" />
          {[798, 811, 824].map((x) => (
            <path
              key={x}
              data-vapor
              d={`M${x} 470q-7-9 0-18t0-18`}
              fill="none"
              stroke="#cfe3ec"
              strokeWidth="3.4"
              strokeLinecap="round"
              opacity="0"
            />
          ))}
        </g>
      </svg>
      {animable && (
        <button
          type="button"
          className="sami-pausa sami-pausa--clara"
          onClick={alternar}
          aria-pressed={pausado}
        >
          {pausado ? 'Reanudar animación' : 'Pausar animación'}
        </button>
      )}
    </div>
  );
}
