import { redondear, type Negocio } from './lecturaObjetivo';
import { formatoPesos } from './objetivos';

/** Cuánto se aleja la meta de lo que el negocio hace hoy, y un primer escalón para empezar. */
export type AlcanceDeLaMeta = {
  nivel: 'imposible' | 'lejos';
  mensaje: string;
  /** Un primer escalón: lo que queda hoy más un 30 %; null si hoy no queda nada con qué calcularlo. */
  sugerida: number | null;
};

/** Más que esto veces lo que queda hoy ya es «lejos»: se puede, pero lleva varios domingos. */
const VECES_LEJOS = 2;
const PRIMER_ESCALON = 1.3;

/**
 * Si la meta (por mes) se puede alcanzar con lo que vende el negocio hoy (últimos 30 días). «Imposible» es pedir
 * más de lo que se vende: ni sin pagar la mercadería ni los gastos quedaría esa plata. Sin ventas no se opina.
 */
export function alcanceDeLaMeta(meta: number, negocio?: Negocio): AlcanceDeLaMeta | null {
  if (!negocio?.hayVentas || negocio.vendiste <= 0 || meta <= 0) return null;
  const sugerida = negocio.queda > 0 ? redondear(negocio.queda * PRIMER_ESCALON) : null;
  const escalon = sugerida !== null && sugerida < meta ? sugerida : null;
  const empezar =
    escalon === null
      ? ''
      : ` Te conviene empezar por ${formatoPesos(escalon)} y subirla cuando llegues.`;
  if (meta > negocio.vendiste) {
    return {
      nivel: 'imposible',
      mensaje: `Hoy vendés unos ${formatoPesos(negocio.vendiste)} por mes. Aunque no pagaras la mercadería ni los gastos, no te quedarían ${formatoPesos(meta)}.${empezar}`,
      sugerida: escalon,
    };
  }
  if (negocio.queda > 0 && meta > negocio.queda * VECES_LEJOS) {
    return {
      nivel: 'lejos',
      mensaje: `Es más del doble de lo que te queda hoy (${formatoPesos(negocio.queda)} por mes). Se puede, pero lleva varias semanas: Sami va por escalones.${empezar}`,
      sugerida: escalon,
    };
  }
  return null;
}
