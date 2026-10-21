export type Punto = { x: number; y: number };

const r1 = (n: number) => Math.round(n * 10) / 10;
const PUNTOS = 24;

type Cubica = { p0: Punto; c1: Punto; c2: Punto; p3: Punto };

function cubica(hombro: Punto, punta: Punto, comba: number): Cubica {
  const dx = punta.x - hombro.x;
  const dy = punta.y - hombro.y;
  const largo = Math.hypot(dx, dy) || 1;
  const nx = (-dy / largo) * comba * largo * 0.3;
  const ny = (dx / largo) * comba * largo * 0.3;
  return {
    p0: hombro,
    c1: { x: hombro.x + dx * 0.3 + nx, y: hombro.y + dy * 0.3 + ny },
    c2: { x: hombro.x + dx * 0.78 - nx * 0.55, y: hombro.y + dy * 0.78 - ny * 0.55 },
    p3: punta,
  };
}

function evaluar({ p0, c1, c2, p3 }: Cubica, t: number): Punto {
  const u = 1 - t;
  const a = u * u * u;
  const b = 3 * u * u * t;
  const c = 3 * u * t * t;
  const d = t * t * t;
  return {
    x: a * p0.x + b * c1.x + c * c2.x + d * p3.x,
    y: a * p0.y + b * c1.y + c * c2.y + d * p3.y,
  };
}

function derivada({ p0, c1, c2, p3 }: Cubica, t: number): Punto {
  const u = 1 - t;
  return {
    x: 3 * u * u * (c1.x - p0.x) + 6 * u * t * (c2.x - c1.x) + 3 * t * t * (p3.x - c2.x),
    y: 3 * u * u * (c1.y - p0.y) + 6 * u * t * (c2.y - c1.y) + 3 * t * t * (p3.y - c2.y),
  };
}

/**
 * Línea central de un brazo: curva cúbica del hombro a la punta. Con `rizo` (0 a 1) el último tramo
 * se enrosca como la punta de un tentáculo en reposo; `sentido` (±1) elige para qué lado.
 */
export function lineaCentral(
  hombro: Punto,
  punta: Punto,
  comba: number,
  rizo = 0,
  sentido = 1,
): Punto[] {
  const curva = cubica(hombro, punta, comba);
  const corte = 1 - 0.36 * rizo;
  const nb = Math.max(3, Math.round(PUNTOS * corte));
  const pts: Punto[] = [];
  for (let i = 0; i <= nb; i++) pts.push(evaluar(curva, (corte * i) / nb));
  if (rizo > 0.01) {
    const d = derivada(curva, corte);
    let angulo = Math.atan2(d.y, d.x);
    const largoResto = 0.36 * rizo * (Math.hypot(punta.x - hombro.x, punta.y - hombro.y) * 1.12);
    const nr = PUNTOS - nb;
    const paso = largoResto / Math.max(1, nr);
    let p = pts[nb] ?? punta;
    for (let j = 1; j <= nr; j++) {
      const u = j / nr;
      angulo += sentido * (0.1 + 0.62 * u) * rizo;
      p = {
        x: p.x + Math.cos(angulo) * paso * (1 - 0.4 * u),
        y: p.y + Math.sin(angulo) * paso * (1 - 0.4 * u),
      };
      pts.push(p);
    }
  }
  return pts;
}

/** Camino suave (curvas cuadráticas por los puntos medios) a partir del segundo punto. */
function suave(p: Punto[]): string {
  let s = '';
  for (let i = 1; i < p.length - 1; i++) {
    const a = p[i];
    const b = p[i + 1];
    if (!a || !b) continue;
    s += `Q${r1(a.x)} ${r1(a.y)} ${r1((a.x + b.x) / 2)} ${r1((a.y + b.y) / 2)}`;
  }
  const ultimo = p[p.length - 1];
  return ultimo ? `${s}L${r1(ultimo.x)} ${r1(ultimo.y)}` : s;
}

export type OpcionesBrazo = {
  rizo?: number;
  sentido?: number;
  anchoBase?: number;
  anchoPunta?: number;
  /** Lado de las ventosas: 1, -1 o 0 (sin ventosas). */
  ventosas?: number;
};

export type Ventosa = { x: number; y: number; r: number };
export type TrazoBrazo = { cuerpo: string; luz: string; ventosas: Ventosa[] };

const LUZ = { x: -0.55, y: -0.83 };
const VENTOSAS_POR_BRAZO = 6;

/** Forma de un brazo: contorno afinado hacia la punta, un reflejo de luz y las ventosas. */
export function trazoBrazo(
  hombro: Punto,
  punta: Punto,
  comba: number,
  { rizo = 0, sentido = 1, anchoBase = 46, anchoPunta = 15, ventosas = 0 }: OpcionesBrazo = {},
): TrazoBrazo {
  const pts = lineaCentral(hombro, punta, comba, rizo, sentido);
  const n = pts.length;
  const normales: Punto[] = [];
  const anchos: number[] = [];
  for (let i = 0; i < n; i++) {
    const a = pts[Math.max(0, i - 1)] ?? pts[i] ?? hombro;
    const b = pts[Math.min(n - 1, i + 1)] ?? pts[i] ?? punta;
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const largo = Math.hypot(dx, dy) || 1;
    normales.push({ x: -dy / largo, y: dx / largo });
    const u = i / (n - 1);
    anchos.push(anchoBase + (anchoPunta - anchoBase) * Math.pow(u, 0.85));
  }

  const lado = (signo: number) =>
    pts.map((p, i) => {
      const nm = normales[i] ?? { x: 0, y: 0 };
      const w = (anchos[i] ?? 0) / 2;
      return { x: p.x + nm.x * w * signo, y: p.y + nm.y * w * signo };
    });
  const izq = lado(1);
  const der = lado(-1).reverse();
  const radioPunta = (anchos[n - 1] ?? 0) / 2;
  const i0 = izq[0];
  const d0 = der[0];
  const cuerpo =
    i0 && d0
      ? `M${r1(i0.x)} ${r1(i0.y)}${suave(izq)}A${r1(radioPunta)} ${r1(radioPunta)} 0 0 0 ${r1(d0.x)} ${r1(d0.y)}${suave(der)}Z`
      : '';

  const reflejo = pts.map((p, i) => {
    const nm = normales[i] ?? { x: 0, y: 0 };
    const hacia = nm.x * LUZ.x + nm.y * LUZ.y;
    const w = anchos[i] ?? 0;
    return { x: p.x + nm.x * hacia * w * 0.24, y: p.y + nm.y * hacia * w * 0.24 };
  });
  const tramo = reflejo.slice(2, Math.max(3, n - 3));
  const r0 = tramo[0];
  const luz = r0 ? `M${r1(r0.x)} ${r1(r0.y)}${suave(tramo)}` : '';

  const lista: Ventosa[] = [];
  if (ventosas !== 0) {
    for (let k = 0; k < VENTOSAS_POR_BRAZO; k++) {
      const i = Math.round((0.34 + (k * 0.5) / VENTOSAS_POR_BRAZO) * (n - 1));
      const p = pts[i];
      const nm = normales[i];
      const w = anchos[i] ?? 0;
      if (!p || !nm) continue;
      lista.push({
        x: p.x + nm.x * ventosas * w * 0.17,
        y: p.y + nm.y * ventosas * w * 0.17,
        r: Math.max(2, w * 0.17),
      });
    }
  }
  return { cuerpo, luz, ventosas: lista };
}

/** Punto de un arco entre dos puntos: sube `apice` píxeles en la mitad del recorrido (t de 0 a 1). */
export function arco(desde: Punto, hasta: Punto, apice: number, t: number): Punto {
  return {
    x: desde.x + (hasta.x - desde.x) * t,
    y: desde.y + (hasta.y - desde.y) * t - Math.sin(Math.PI * t) * apice,
  };
}

/** Hacia dónde miran las pupilas: dirección al objetivo, con un desplazamiento máximo. */
export function mirada(desde: Punto, hacia: Punto, maximo = 11): Punto {
  const dx = hacia.x - desde.x;
  const dy = hacia.y - desde.y;
  const largo = Math.hypot(dx, dy) || 1;
  const escala = Math.min(1, largo / 140) * maximo;
  return { x: (dx / largo) * escala, y: (dy / largo) * escala * 0.7 };
}
