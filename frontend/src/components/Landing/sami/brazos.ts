import { trazoBrazo, type Punto } from './geometria';

// Los ocho brazos de Sami, en coordenadas locales (centro de la cabeza en 0, 0). El estado de cada
// brazo (punta y enroscado) lo mueve GSAP; `crearDibujante` redibuja las formas en cada cuadro.

export type BrazoId =
  'atrasIzq' | 'atrasDer' | 'izqExt' | 'derExt' | 'izqCent' | 'derCent' | 'izqTrab' | 'derTrab';

export type EstadoBrazo = Punto & { rizo: number };

type Brazo = {
  hombro: Punto;
  reposo: Punto;
  comba: number;
  /** Enroscado en reposo (0 a 1). */
  rizo: number;
  sentido: number;
  /** Los brazos «de frente» se dibujan sobre el cuerpo (para sostener cosas). */
  frente: boolean;
  ventosas: number;
  anchoBase: number;
  anchoPunta: number;
};

export const BRAZOS: Record<BrazoId, Brazo> = {
  atrasIzq: {
    hombro: { x: -62, y: 84 },
    reposo: { x: -178, y: 146 },
    comba: 1,
    rizo: 1,
    sentido: 1,
    frente: false,
    ventosas: 0,
    anchoBase: 44,
    anchoPunta: 15,
  },
  atrasDer: {
    hombro: { x: 62, y: 84 },
    reposo: { x: 180, y: 146 },
    comba: -1,
    rizo: 1,
    sentido: -1,
    frente: false,
    ventosas: 0,
    anchoBase: 44,
    anchoPunta: 15,
  },
  izqExt: {
    hombro: { x: -80, y: 54 },
    reposo: { x: -214, y: 118 },
    comba: 1,
    rizo: 1,
    sentido: 1,
    frente: true,
    ventosas: 1,
    anchoBase: 46,
    anchoPunta: 15,
  },
  derExt: {
    hombro: { x: 80, y: 54 },
    reposo: { x: 216, y: 120 },
    comba: -1,
    rizo: 1,
    sentido: -1,
    frente: true,
    ventosas: -1,
    anchoBase: 46,
    anchoPunta: 15,
  },
  izqCent: {
    hombro: { x: -36, y: 92 },
    reposo: { x: -98, y: 162 },
    comba: 1,
    rizo: 1,
    sentido: 1,
    frente: false,
    ventosas: 0,
    anchoBase: 42,
    anchoPunta: 15,
  },
  derCent: {
    hombro: { x: 36, y: 92 },
    reposo: { x: 100, y: 162 },
    comba: -1,
    rizo: 1,
    sentido: -1,
    frente: false,
    ventosas: 0,
    anchoBase: 42,
    anchoPunta: 15,
  },
  izqTrab: {
    hombro: { x: -66, y: 80 },
    reposo: { x: -152, y: 160 },
    comba: 1,
    rizo: 0.8,
    sentido: 1,
    frente: true,
    ventosas: 1,
    anchoBase: 44,
    anchoPunta: 16,
  },
  derTrab: {
    hombro: { x: 66, y: 80 },
    reposo: { x: 154, y: 160 },
    comba: -1,
    rizo: 0.8,
    sentido: -1,
    frente: true,
    ventosas: -1,
    anchoBase: 44,
    anchoPunta: 16,
  },
};

export const ORDEN_ATRAS: BrazoId[] = ['atrasIzq', 'atrasDer', 'izqCent', 'derCent'];
export const ORDEN_FRENTE: BrazoId[] = ['izqExt', 'derExt', 'izqTrab', 'derTrab'];
export const ORDEN_BRAZOS: BrazoId[] = [...ORDEN_ATRAS, ...ORDEN_FRENTE];

export const VENTOSAS_POR_BRAZO = 6;

export function estadoInicial(): Record<BrazoId, EstadoBrazo> {
  return Object.fromEntries(
    ORDEN_BRAZOS.map((id) => [id, { ...BRAZOS[id].reposo, rizo: BRAZOS[id].rizo }]),
  ) as Record<BrazoId, EstadoBrazo>;
}

type Referencias = {
  id: BrazoId;
  cuerpo: Element | null;
  luz: Element | null;
  ventosas: Element[];
};

/** Prepara el redibujado de los brazos de un SVG; devuelve la función que se llama en cada cuadro. */
export function crearDibujante(raiz: Element) {
  const refs: Referencias[] = ORDEN_BRAZOS.map((id) => ({
    id,
    cuerpo: raiz.querySelector(`[data-brazo="${id}"]`),
    luz: raiz.querySelector(`[data-luz="${id}"]`),
    ventosas: Array.from(raiz.querySelectorAll(`[data-ventosa="${id}"]`)),
  }));
  return (estado: Record<BrazoId, EstadoBrazo>) => {
    for (const { id, cuerpo, luz, ventosas } of refs) {
      const b = BRAZOS[id];
      const e = estado[id];
      const t = trazoBrazo(b.hombro, e, b.comba, {
        rizo: e.rizo,
        sentido: b.sentido,
        anchoBase: b.anchoBase,
        anchoPunta: b.anchoPunta,
        ventosas: b.ventosas,
      });
      cuerpo?.setAttribute('d', t.cuerpo);
      luz?.setAttribute('d', t.luz);
      ventosas.forEach((el, k) => {
        const v = t.ventosas[k];
        if (!v) return;
        el.setAttribute('cx', String(Math.round(v.x * 10) / 10));
        el.setAttribute('cy', String(Math.round(v.y * 10) / 10));
        el.setAttribute('r', String(Math.round(v.r * 10) / 10));
      });
    }
  };
}
