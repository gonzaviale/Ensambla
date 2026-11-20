import { normalize } from '../core';
import type { ProductoDeZona, Zona } from '../zona';
import { cuantosProductos, palabras, pesos } from './medidas';
import type { Aviso, TipoDeAviso } from './tipos';

export type Respuesta = {
  texto: string;
  avisos: Aviso[];
  /** Los precios de la zona que contestan la pregunta (agregados, nunca de un comercio puntual). */
  precios?: ProductoDeZona[];
  consulta?: string;
};

type Consulta = { texto: string; zona?: Zona; avisos: Aviso[] };

/** Un tema que Sami sabe contestar: lo reconoce por las palabras de la pregunta y arma la respuesta. */
type Tema = { patron: RegExp; responder: (consulta: Consulta) => Respuesta };

const PREGUNTA = new Set([
  'donde',
  'compro',
  'comprar',
  'compra',
  'mas',
  'barata',
  'barato',
  'baratos',
  'baratas',
  'precio',
  'precios',
  'conviene',
  'quien',
  'vende',
  'venden',
  'tiene',
  'tienen',
  'consigo',
  'cuesta',
  'sale',
  'cual',
  'que',
  'esta',
  'hay',
  'cerca',
  'mejor',
  'busco',
  'necesito',
  'cuanto',
  'cobro',
  'cobrar',
  'zona',
  'tengo',
  'estoy',
  'vendo',
  'vendes',
  'como',
  'cobran',
  'cobra',
  'cobras',
  'cuestan',
  'valen',
  'vale',
  'salen',
]);

const AVISOS_DEL_RESUMEN = 4;
const PRECIOS_A_MOSTRAR = 4;

const PREGUNTA_VACIA = 'Escribime una pregunta corta, por ejemplo «¿qué tengo que reponer?».';
const NO_SE =
  'Todavía no sé responder eso. Probá con «¿qué tengo que reponer?», «¿qué se vende cerca que no tengo?», «¿cuánto cobran la yerba en mi zona?» o «¿cómo vienen mis márgenes?».';

const avisosDeTipo = (avisos: Aviso[], tipos: TipoDeAviso[]) =>
  avisos.filter((aviso) => tipos.includes(aviso.tipo));

const conLista = (items: Aviso[], si: (cantidad: number) => string, no: string): Respuesta => ({
  texto: items.length ? si(items.length) : no,
  avisos: items,
});

/** Un tema que contesta con los avisos de ciertos tipos. */
const temaDeAvisos = (
  patron: RegExp,
  tipos: TipoDeAviso[],
  si: (cantidad: number) => string,
  no: string,
): Tema => ({ patron, responder: ({ avisos }) => conLista(avisosDeTipo(avisos, tipos), si, no) });

const responderPrecios = (consulta: Consulta): Respuesta => {
  const termino = palabras(consulta.texto).filter((palabra) => !PREGUNTA.has(palabra));
  if (termino.length) return preciosDeLaZona(termino, consulta.zona);
  return conLista(
    avisosDeTipo(consulta.avisos, ['precio_zona', 'muy_vendido', 'estancado']),
    (n) => `Encontré ${cuantosProductos(n)} para revisar el precio frente a tu zona.`,
    'Tus precios están alineados con los de tu zona. No veo nada para ajustar.',
  );
};

function preciosDeLaZona(termino: string[], zona: Zona | undefined): Respuesta {
  const consulta = termino.join(' ');
  const encontrados = productosQueNombran(termino, zona);
  const [primero] = encontrados;
  if (!primero) return sinDatosDe(consulta);
  return {
    texto: `En tu zona, «${consulta}» va de ${pesos(primero.precio.minimo)} a ${pesos(primero.precio.maximo)}, con un promedio de ${pesos(primero.precio.promedio)} (${primero.comercios} comercios).`,
    avisos: [],
    precios: encontrados.slice(0, PRECIOS_A_MOSTRAR),
    consulta,
  };
}

const productosQueNombran = (termino: string[], zona: Zona | undefined) =>
  (zona?.productos ?? [])
    .filter((producto) => {
      const propias = palabras(producto.nombre).join(' ');
      return termino.every((palabra) => propias.includes(palabra));
    })
    .sort((a, b) => b.ventas.comercios - a.ventas.comercios);

const sinDatosDe = (consulta: string): Respuesta => ({
  texto: `No tengo datos de «${consulta}» en tu zona: hace falta que lo vendan al menos 3 comercios para poder mostrarlo.`,
  avisos: [],
  precios: [],
  consulta,
});

const responderResumen = ({ avisos }: Consulta) =>
  conLista(
    avisos.slice(0, AVISOS_DEL_RESUMEN),
    () => `Lo más importante ahora (${avisos.length} avisos en total):`,
    'Todo en orden por ahora. Sigo mirando.',
  );

/** En orden: el primero que reconoce la pregunta es el que contesta. */
const TEMAS: Tema[] = [
  {
    patron: /precio|cobro|cobrar|cuesta|cuanto|barat|car[oa]\b|conviene|vendo/,
    responder: responderPrecios,
  },
  temaDeAvisos(
    /margen|gano|ganancia/,
    ['precio_zona', 'muy_vendido'],
    (n) => `Hay ${cuantosProductos(n)} donde podés ganar más sin pasarte del precio de tu zona.`,
    'No veo productos con poca ganancia frente a lo que cobran en tu zona.',
  ),
  temaDeAvisos(
    /repon|falta|stock|queda|agot|pedir/,
    ['stock'],
    (n) => `Tenés ${cuantosProductos(n)} para reponer.`,
    'Tu stock está por encima de los mínimos. No hace falta reponer nada por ahora.',
  ),
  temaDeAvisos(
    /no se vende|estanc|parado|sin ventas|no sale|no rota/,
    ['estancado'],
    (n) =>
      `${n === 1 ? 'Un producto' : `${n} productos`} no se mueve y está más caro que en tu zona.`,
    'No veo productos parados por precio.',
  ),
  temaDeAvisos(
    /combo|junto|pack|promo/,
    ['combo'],
    (n) => `Encontré ${n === 1 ? '1 combo posible' : `${n} combos posibles`}.`,
    'Todavía no veo productos que se vendan juntos con la frecuencia suficiente.',
  ),
  temaDeAvisos(
    /vende|venden|cerca|zona|consumo|popular|nuevo|oferta/,
    ['consumo_zona'],
    (n) => `Hay ${cuantosProductos(n)} que se venden bien cerca y vos no tenés.`,
    'No veo productos muy vendidos en tu zona que te falten.',
  ),
  {
    patron: /todo|resumen|como (estoy|voy|viene)|novedad|que hay|urgente/,
    responder: responderResumen,
  },
];

export function responder(
  pregunta: string,
  { zona, avisos = [] }: { zona?: Zona; avisos?: Aviso[] },
): Respuesta {
  const texto = normalize(pregunta);
  if (!texto) return { texto: PREGUNTA_VACIA, avisos: [] };
  const tema = TEMAS.find(({ patron }) => patron.test(texto));
  return tema ? tema.responder({ texto, zona, avisos }) : { texto: NO_SE, avisos: [] };
}
