// «El relato prende los bloques» (docs/diferencial-red-proactiva.md, §3): el comercio cuenta cómo
// trabaja con sus palabras y la app deduce qué partes de Ensambla le sirven. Lógica pura y sin
// inteligencia artificial: palabras clave. En el producto real lo haría el modelo de lenguaje.

import type { TipoAlerta } from '@/types/red';

/* ---------- rubros y reglas sugeridas ---------- */

export const RUBROS = [
  'Almacén',
  'Kiosco',
  'Despensa',
  'Verdulería',
  'Ferretería',
  'Carnicería',
  'Panadería',
  'Librería',
  'Otro',
] as const;

export type ReglaSugerida = {
  /** Ganancia general que se suma al costo, en %. */
  margen: number;
  /** Se redondea el precio hacia arriba a múltiplos de este valor. */
  redondeo: number;
  /** Por debajo de esta ganancia se avisa «precio en riesgo». */
  margenMinimo: number;
};

const REGLA_GENERAL: ReglaSugerida = { margen: 35, redondeo: 10, margenMinimo: 12 };

const REGLAS_POR_RUBRO: Record<string, ReglaSugerida> = {
  Almacén: { margen: 33, redondeo: 10, margenMinimo: 12 },
  Kiosco: { margen: 50, redondeo: 50, margenMinimo: 20 },
  Despensa: { margen: 35, redondeo: 10, margenMinimo: 12 },
  Verdulería: { margen: 40, redondeo: 10, margenMinimo: 15 },
  Ferretería: { margen: 45, redondeo: 50, margenMinimo: 18 },
  Carnicería: { margen: 30, redondeo: 10, margenMinimo: 10 },
  Panadería: { margen: 45, redondeo: 10, margenMinimo: 15 },
  Librería: { margen: 40, redondeo: 10, margenMinimo: 15 },
};

/** Reglas de precio que suelen andar bien en el rubro (punto de partida, siempre editable). */
export function sugerirReglas(rubro: string | null | undefined): ReglaSugerida {
  return (rubro && REGLAS_POR_RUBRO[rubro]) || REGLA_GENERAL;
}

/* ---------- bloques ---------- */

export type BloqueId =
  'listas' | 'reglas' | 'margen' | 'stock' | 'cuaderno' | 'comparar' | 'ofertas';

export type DefinicionBloque = {
  id: BloqueId;
  nombre: string;
  /** Qué hace, en una línea y sin jerga. */
  descripcion: string;
  /** Aviso de la red que se prende o se apaga con este bloque, si corresponde. */
  aviso: TipoAlerta[];
};

export const BLOQUES: Record<BloqueId, DefinicionBloque> = {
  listas: {
    id: 'listas',
    nombre: 'Listas de proveedores',
    descripcion: 'Reenviás la lista y te mostramos qué cambió.',
    aviso: ['lista-nueva'],
  },
  reglas: {
    id: 'reglas',
    nombre: 'Reglas de precio',
    descripcion: 'Te proponemos el precio nuevo con tu ganancia de siempre.',
    aviso: [],
  },
  margen: {
    id: 'margen',
    nombre: 'Avisos de margen',
    descripcion: 'Te avisamos cuando un precio te deja ganar muy poco.',
    aviso: ['precio-en-riesgo', 'cambio-raro'],
  },
  stock: {
    id: 'stock',
    nombre: 'Stock por agotarse',
    descripcion: 'Te avisamos antes de que se te acabe un producto.',
    aviso: ['stock-por-agotarse'],
  },
  cuaderno: {
    id: 'cuaderno',
    nombre: 'Cuaderno',
    descripcion: 'Le sacás una foto a tu cuaderno y lo pasamos al sistema.',
    aviso: [],
  },
  comparar: {
    id: 'comparar',
    nombre: 'Comparar proveedores',
    descripcion: 'Te mostramos quién te vende más barato cada producto.',
    aviso: ['mejor-proveedor', 'compras-caro'],
  },
  ofertas: {
    id: 'ofertas',
    nombre: 'Ofertas',
    descripcion: 'Te avisamos de las ofertas de lo que vendés.',
    aviso: ['oferta-que-sirve'],
  },
};

export const ORDEN_BLOQUES: BloqueId[] = [
  'listas',
  'reglas',
  'margen',
  'stock',
  'cuaderno',
  'comparar',
  'ofertas',
];

/** Los bloques que siempre se prenden: son la base de la propuesta. */
const BLOQUES_BASE: BloqueId[] = ['listas', 'reglas', 'margen'];

/* ---------- interpretación ---------- */

export type Relato = {
  /** Rubro elegido o, si no hay, el que se deduce del texto. */
  rubro: string | null;
  /** Cuántos proveedores nombra («dos mayoristas» → 2); null si no lo dice. */
  proveedores: number | null;
  nombraCuaderno: boolean;
  nombraWhatsapp: boolean;
  /** Habla de faltantes, de quedarse sin mercadería. */
  hablaDeFaltantes: boolean;
  /** Dice que cambia los precios a mano. */
  cambiaPreciosAMano: boolean;
  /** Bloques que se prenden, en orden de pantalla. */
  bloques: BloqueId[];
  /** Por qué se prendió cada bloque, en una línea. */
  motivos: Partial<Record<BloqueId, string>>;
  /** Frases cortas de «Entendí esto: …». */
  entendido: string[];
};

const NUMEROS: Record<string, number> = {
  un: 1,
  uno: 1,
  una: 1,
  dos: 2,
  tres: 3,
  cuatro: 4,
  cinco: 5,
  seis: 6,
  siete: 7,
  ocho: 8,
  nueve: 9,
  diez: 10,
};

const PALABRAS_RUBRO: Array<[string, RegExp]> = [
  ['Almacén', /\balmacen\b/],
  ['Kiosco', /\b(kiosco|kiosko|quiosco)\b/],
  ['Despensa', /\bdespensa\b/],
  ['Verdulería', /\b(verduleria|fruteria|verdura)\b/],
  ['Ferretería', /\b(ferreteria|corralon)\b/],
  ['Carnicería', /\b(carniceria|carne)\b/],
  ['Panadería', /\b(panaderia|panificados)\b/],
  ['Librería', /\b(libreria|papeleria)\b/],
];

const PROVEEDORES = /\b(mayoristas?|proveedor(?:es)?|distribuidor(?:as|es)?|viajantes?)\b/;

function normalizar(texto: string): string {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, ' ').trim();
}

function detectarRubro(texto: string): string | null {
  const coincidencias = PALABRAS_RUBRO.filter(([, patron]) => patron.test(texto));
  // Si menciona varios, gana el que aparece primero en el relato.
  let mejor: { rubro: string; indice: number } | null = null;
  for (const [rubro, patron] of coincidencias) {
    const indice = texto.search(patron);
    if (!mejor || indice < mejor.indice) mejor = { rubro, indice };
  }
  return mejor?.rubro ?? null;
}

function contarProveedores(texto: string): number | null {
  // «dos mayoristas», «2 proveedores», «tres grandes distribuidoras», «un solo proveedor».
  const numero = /\b(\d{1,2}|un|uno|una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez)\b/g;
  let total: number | null = null;
  for (const coincidencia of texto.matchAll(numero)) {
    const despues = texto.slice(
      (coincidencia.index ?? 0) + coincidencia[0].length,
      (coincidencia.index ?? 0) + coincidencia[0].length + 30,
    );
    const palabras = despues.trim().split(' ');
    const ventana = palabras.slice(0, 3).join(' ');
    if (PROVEEDORES.test(ventana)) {
      const palabra = coincidencia[0];
      const valor = /^\d/.test(palabra) ? Number(palabra) : (NUMEROS[palabra] ?? null);
      if (valor && valor > 0) total = Math.max(total ?? 0, valor);
    }
  }
  return total;
}

const CUADERNO =
  /\b(cuaderno|libreta|anoto|anotado|anotados|apunto|apuntes|agenda|papel|manuscrit\w*)\b/;
const WHATSAPP = /\b(whatsapp|wasap|wsp|guasap)\b/;
const FALTANTES =
  /\b(me quedo sin|quedo sin|me quedo corto|se me acaba|se me acabo|se me terminan?|me falta|me faltan|faltante|faltantes|sin stock|agota|agotan|agotado|faltar)\b/;
const A_MANO = /\b(a mano|manualmente|uno por uno|cambio los precios|los precios los cambio)\b/;
const COMPARAR = /\b(compar\w+|mas barato|mas baratos|mas caro|mejor precio|quien vende)\b/;
const OFERTAS = /\b(oferta|ofertas|promo|promos|promocion|promociones|descuento|descuentos)\b/;

/**
 * Interpreta lo que el comercio contó y devuelve qué bloques se prenden y qué datos se detectaron.
 * `rubro` es el elegido con las fichas; si falta, se deduce del texto.
 */
export function interpretarRelato(texto: string, rubro?: string | null): Relato {
  const limpio = normalizar(texto);
  const rubroDetectado = detectarRubro(limpio);
  const rubroFinal = rubro?.trim() ? rubro.trim() : rubroDetectado;
  const proveedores = contarProveedores(limpio);
  const nombraCuaderno = CUADERNO.test(limpio);
  const nombraWhatsapp = WHATSAPP.test(limpio);
  const hablaDeFaltantes = FALTANTES.test(limpio);
  const cambiaPreciosAMano = A_MANO.test(limpio);
  const quiereComparar = COMPARAR.test(limpio);
  const hablaDeOfertas = OFERTAS.test(limpio);

  const motivos: Partial<Record<BloqueId, string>> = {};
  const prendidos = new Set<BloqueId>(BLOQUES_BASE);

  motivos.listas = nombraWhatsapp
    ? 'Dijiste que te llegan las listas por WhatsApp.'
    : proveedores
      ? `Le comprás a ${proveedores === 1 ? 'un proveedor' : `${proveedores} proveedores`}.`
      : 'Es la base de Ensambla: tus listas de precios.';
  motivos.reglas = cambiaPreciosAMano
    ? 'Dijiste que cambiás los precios a mano.'
    : 'Para que el precio nuevo salga con tu ganancia, sin cuentas.';
  motivos.margen = 'Para que no vendas por debajo de lo que te costó.';

  if (hablaDeFaltantes) {
    prendidos.add('stock');
    motivos.stock = 'Hablaste de quedarte sin mercadería.';
  }
  if (nombraCuaderno) {
    prendidos.add('cuaderno');
    motivos.cuaderno = 'Nombraste tu cuaderno.';
  }
  if ((proveedores ?? 0) >= 2 || quiereComparar) {
    prendidos.add('comparar');
    motivos.comparar =
      (proveedores ?? 0) >= 2
        ? `Le comprás a ${proveedores} proveedores: conviene compararlos.`
        : 'Mencionaste comparar precios.';
  }
  if (hablaDeOfertas) {
    prendidos.add('ofertas');
    motivos.ofertas = 'Mencionaste ofertas.';
  }

  const bloques = ORDEN_BLOQUES.filter((id) => prendidos.has(id));

  const entendido: string[] = [];
  if (rubroFinal && rubroFinal !== 'Otro') {
    const minuscula = rubroFinal.toLowerCase();
    entendido.push(`tenés ${minuscula.endsWith('a') ? 'una' : 'un'} ${minuscula}`);
  }
  if (proveedores) {
    entendido.push(
      proveedores === 1 ? 'le comprás a un proveedor' : `le comprás a ${proveedores} proveedores`,
    );
  }
  if (nombraWhatsapp) entendido.push('te llegan las listas por WhatsApp');
  if (nombraCuaderno) entendido.push('llevás un cuaderno');
  if (cambiaPreciosAMano) entendido.push('cambiás los precios a mano');
  if (hablaDeFaltantes) entendido.push('a veces te quedás sin mercadería');
  if (hablaDeOfertas) entendido.push('te interesan las ofertas');

  return {
    rubro: rubroFinal,
    proveedores,
    nombraCuaderno,
    nombraWhatsapp,
    hablaDeFaltantes,
    cambiaPreciosAMano,
    bloques,
    motivos,
    entendido,
  };
}

/** Une las frases de `entendido` con comas y «y»: «tenés un almacén, le comprás a dos proveedores y …». */
export function unirFrases(frases: string[]): string {
  if (frases.length <= 1) return frases[0] ?? '';
  return `${frases.slice(0, -1).join(', ')} y ${frases[frases.length - 1]}`;
}
