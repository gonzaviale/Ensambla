import { NOMBRE_RUBRO, PIEZAS, nombreDePieza, sinTildes } from '@/lib/catalogo/piezas';
import type { Ensamblado, PiezaEnsamblada, PiezaId, RubroId } from '@/types/negocio';

// El ensamblador convierte lo que la persona cuenta de su negocio en piezas del catálogo. Hoy es por
// reglas sobre palabras (simulado); la interfaz está pensada para reemplazarlo por un modelo de IA
// sin tocar las pantallas: texto libre → { rubro, piezas con su motivo }.

const SENALES_RUBRO: Record<Exclude<RubroId, 'otro'>, string[]> = {
  gastronomia: [
    'vianda',
    'comida',
    'cocin',
    'torta',
    'pastel',
    'empanada',
    'menu',
    'catering',
    'panader',
    'restaurante',
    'delivery',
    'plato',
  ],
  comercio: [
    'almacen',
    'kiosco',
    'despensa',
    'verduler',
    'carniceria',
    'comercio',
    'local',
    'tienda',
    'negocio de barrio',
    'autoservicio',
  ],
};

export function detectarRubro(texto: string): RubroId {
  const limpio = sinTildes(texto);
  let mejor: RubroId = 'otro';
  let puntaje = 0;
  for (const [rubro, palabras] of Object.entries(SENALES_RUBRO) as [RubroId, string[]][]) {
    const aciertos = palabras.filter((palabra) => limpio.includes(palabra)).length;
    if (aciertos > puntaje) {
      mejor = rubro;
      puntaje = aciertos;
    }
  }
  return mejor;
}

/**
 * Ensambla las piezas de un negocio: la base de su rubro más las que el relato pide.
 * `rubro` fuerza el rubro (por ejemplo, el que eligió la persona en un chip).
 */
export function ensamblar(relato: string, rubro?: RubroId): Ensamblado {
  const elegido = rubro ?? detectarRubro(relato);
  const limpio = sinTildes(relato);
  const piezas: PiezaEnsamblada[] = [];
  const agregadas = new Set<PiezaId>();

  for (const pieza of PIEZAS) {
    if (!pieza.base.includes(elegido)) continue;
    agregadas.add(pieza.id);
    piezas.push({
      id: pieza.id,
      nombre: nombreDePieza(pieza.id, elegido),
      motivo: `Es lo básico de ${NOMBRE_RUBRO[elegido].toLowerCase()}.`,
    });
  }
  for (const pieza of PIEZAS) {
    if (agregadas.has(pieza.id)) continue;
    const senal = pieza.senales.find((palabra) => limpio.includes(palabra));
    if (!senal) continue;
    agregadas.add(pieza.id);
    piezas.push({
      id: pieza.id,
      nombre: nombreDePieza(pieza.id, elegido),
      motivo: `La sumé porque mencionaste «${senal}».`,
    });
  }
  return { rubro: elegido, piezas };
}
