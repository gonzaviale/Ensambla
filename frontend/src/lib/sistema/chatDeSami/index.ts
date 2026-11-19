import { normalize } from '../core';
import { avisosDelSistema, deZona, stockBajo } from './avisosYStock';
import {
  SUGERENCIAS_INICIALES,
  destinoDe,
  numero,
  plural,
  productosDe,
  tiene,
  type ContextoDeChat,
  type Producto,
  type Respuesta,
} from './datos';
import { deUnProducto } from './deUnProducto';
import { productosNombrados } from './productosNombrados';
import { masVendidos, ventasGenerales } from './ventas';

// El chat de Sami: contesta preguntas de stock, ventas y avisos con los datos de la sección abierta. No usa un
// modelo de lenguaje: reconoce de qué se habla por palabras clave y arma la respuesta con cuentas simples, así
// anda sin conexión y siempre dice de dónde sale cada número.

export {
  SUGERENCIAS_INICIALES,
  type ContextoDeChat,
  type ItemDeChat,
  type Respuesta,
} from './datos';

type Tema = {
  palabras: string[];
  responder: (pregunta: string, contexto: ContextoDeChat) => Respuesta;
};

const MAXIMO_DE_PARECIDOS = 4;
const AVISOS_DE_INICIO = 3;

const noEntendi = (): Respuesta => ({
  texto:
    'No estoy segura de haber entendido. Puedo contarte del stock, las ventas, los avisos o de un producto en particular.',
  sugerencias: SUGERENCIAS_INICIALES,
});

export function ayuda(): Respuesta {
  return {
    texto:
      'Puedo contarte cómo viene tu sección. Probá preguntarme por el stock, las ventas, los avisos o un producto, por ejemplo «¿cuánta yerba me queda?».',
    sugerencias: SUGERENCIAS_INICIALES,
  };
}

/** En orden: el primero que reconoce la pregunta es el que contesta. */
const TEMAS: Tema[] = [
  {
    palabras: ['mas vendido', 'mas vende', 'mejor', 'estrella', 'top ', 'lo que mas'],
    responder: (_, contexto) => masVendidos(contexto),
  },
  {
    palabras: [
      'aviso',
      'alerta',
      'notific',
      'novedad',
      'pendiente',
      'urgente',
      'importante',
      'resumen',
      'que pasa',
      'que hay',
      'revisar',
    ],
    responder: (_, contexto) => avisosDelSistema(contexto),
  },
  {
    palabras: ['stock', 'falta', 'reponer', 'pedir', 'agot', 'quedan', 'queda'],
    responder: (_, contexto) => stockBajo(contexto),
  },
  {
    palabras: ['vend', 'venta', 'factur', 'ingres', 'caja', 'recaud', 'cuanto gane', 'ganancia'],
    responder: ventasGenerales,
  },
  {
    palabras: ['zona', 'competencia', 'caro', 'barato', 'precios'],
    responder: (_, contexto) => deZona(contexto),
  },
  {
    palabras: ['hola', 'buenas', 'ayuda', 'que podes', 'que sabes', 'como funciona'],
    responder: () => ayuda(),
  },
];

export function saludo(contexto: ContextoDeChat): Respuesta {
  const cantidad = contexto.avisos.length;
  const sistema = contexto.sistema.name;
  return {
    texto: cantidad
      ? `¡Hola! Estuve mirando «${sistema}» y encontré ${plural(cantidad, 'cosa', 'cosas')} para mirar. ¿Qué querés ver?`
      : `¡Hola! Estuve mirando «${sistema}» y por ahora está todo en orden. ¿Qué querés ver?`,
    sugerencias: SUGERENCIAS_INICIALES,
  };
}

/** Lo primero que dice Sami al abrir el chat: saluda y, si hay avisos, muestra los más importantes. */
export function inicio(contexto: ContextoDeChat): Respuesta {
  const bienvenida = saludo(contexto);
  if (!contexto.avisos.length) return bienvenida;
  const avisos = avisosDelSistema(contexto);
  const total = contexto.avisos.length;
  return {
    ...avisos,
    texto: bienvenida.texto,
    items: avisos.items?.slice(0, AVISOS_DE_INICIO),
    verTodos: total > AVISOS_DE_INICIO ? total : undefined,
  };
}

export function responder(pregunta: string, contexto: ContextoDeChat): Respuesta {
  const nombrados = productosNombrados(pregunta, productosDe(contexto.sistema));
  const [unico] = nombrados;
  if (nombrados.length === 1 && unico) return deUnProducto(pregunta, unico, contexto);
  if (nombrados.length > 1 && nombrados.length <= MAXIMO_DE_PARECIDOS)
    return cualDeEstos(nombrados, contexto);
  const texto = normalize(pregunta).replace(/[^a-z0-9 ]+/g, ' ');
  const tema = TEMAS.find(({ palabras }) => tiene(texto, palabras));
  return tema ? tema.responder(pregunta, contexto) : noEntendi();
}

const cualDeEstos = (nombrados: Producto[], contexto: ContextoDeChat): Respuesta => ({
  texto: `Encontré ${nombrados.length} productos que se parecen. ¿De cuál hablás?`,
  items: nombrados.map((producto) => ({
    id: producto.fila.id,
    titulo: producto.nombre,
    detalle: `Quedan ${numero(producto.fila.quantity)}`,
    destino: destinoDe(producto, contexto.sistema),
  })),
});
