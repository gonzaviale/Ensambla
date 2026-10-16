import type { DefinicionRespuesta, PiezaId, RespuestaId } from '@/types/negocio';

// Cada respuesta declara qué piezas necesita: sumar una pieza desbloquea las respuestas que la usan.

export const RESPUESTAS: DefinicionRespuesta[] = [
  {
    id: 'te-quedo',
    pregunta: '¿Cuánto te quedó este mes?',
    requiere: ['ventas', 'gastos'],
    ayuda: 'Lo que vendiste menos lo que gastaste, comparado con el mes anterior.',
    basica: true,
  },
  {
    id: 'vendiste',
    pregunta: '¿Cuánto vendiste este mes?',
    requiere: ['ventas'],
    ayuda: 'El total de tus ventas del mes y cuántas fueron.',
    basica: true,
  },
  {
    id: 'gastaste',
    pregunta: '¿Cuánto gastaste este mes?',
    requiere: ['gastos'],
    ayuda: 'El total de tus gastos del mes.',
    basica: true,
  },
  {
    id: 'margen-producto',
    pregunta: '¿Qué te deja más?',
    requiere: ['productos'],
    ayuda: 'Los productos con mejor ganancia entre lo que cuestan y lo que cobrás.',
    basica: true,
  },
  {
    id: 'producto-perdida',
    pregunta: '¿Qué vendés con pérdida?',
    requiere: ['productos'],
    ayuda: 'Los productos que cobrás por menos de lo que te cuestan.',
    basica: true,
  },
  {
    id: 'que-se-termina',
    pregunta: '¿Qué se te está terminando?',
    requiere: ['productos'],
    ayuda: 'Los productos con poco stock.',
    basica: false,
  },
  {
    id: 'quien-te-debe',
    pregunta: '¿Quién te debe?',
    requiere: ['fiados'],
    ayuda: 'Lo que te deben tus clientes y cuánto es en total.',
    basica: false,
  },
  {
    id: 'pedidos-pendientes',
    pregunta: '¿Qué pedidos tenés por entregar?',
    requiere: ['pedidos'],
    ayuda: 'Los pedidos sin entregar y cuánto falta cobrar.',
    basica: false,
  },
  {
    id: 'costo-plato',
    pregunta: '¿Cuánto te cuesta cada producto?',
    requiere: ['productos', 'insumos'],
    ayuda: 'El costo de cada producto según los insumos de su receta.',
    basica: false,
  },
];

export function definicion(id: RespuestaId): DefinicionRespuesta {
  const encontrada = RESPUESTAS.find((respuesta) => respuesta.id === id);
  if (!encontrada) throw new Error(`Respuesta desconocida: ${id}`);
  return encontrada;
}

/** Piezas que faltan para responder (vacío si ya se puede). */
export function faltan(requiere: PiezaId[], presentes: Iterable<PiezaId>): PiezaId[] {
  const tengo = new Set(presentes);
  return requiere.filter((id) => !tengo.has(id));
}

/** Respuestas que se desbloquean al sumar una pieza (las que dependían sólo de ella y de lo que ya hay). */
export function respuestasQueDesbloquea(
  pieza: PiezaId,
  presentes: Iterable<PiezaId>,
): RespuestaId[] {
  const antes = new Set(presentes);
  const despues = new Set(antes).add(pieza);
  return RESPUESTAS.filter(
    (respuesta) =>
      respuesta.requiere.includes(pieza) &&
      faltan(respuesta.requiere, antes).length > 0 &&
      faltan(respuesta.requiere, despues).length === 0,
  ).map((respuesta) => respuesta.id);
}
