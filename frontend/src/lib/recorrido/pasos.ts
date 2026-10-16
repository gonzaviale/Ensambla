// Recorrido guiado de la app: la lista de pasos, en el orden en que se muestran. Es puro (sin React
// ni router): cada paso dice en qué pantalla está, qué elemento se resalta y qué se le cuenta a la
// persona. El elemento se marca en la pantalla real con `data-recorrido="<ancla>"`.

export type PasoRecorrido = {
  id: string;
  /** Pantalla (pathname) donde se muestra el paso. */
  ruta: string;
  /** Valor del atributo `data-recorrido` del elemento que se resalta. */
  ancla: string;
  /** Otra ancla de la misma pantalla por si la principal no está (por ejemplo, sin avisos). */
  alternativa?: string;
  titulo: string;
  /** Qué es, por qué importa y qué probar: una a tres frases, en voseo y sin jerga. */
  texto: string;
  /** Una acción concreta para probar, opcional. */
  sugerencia?: string;
};

export const PASOS_RECORRIDO: readonly PasoRecorrido[] = [
  {
    id: 'inicio-respuestas',
    ruta: '/inicio',
    ancla: 'inicio-respuestas',
    alternativa: 'inicio-checklist',
    titulo: 'Tus respuestas, a la vista',
    texto:
      'Acá ves lo que tu negocio ya sabe contestar: cuánto te quedó, quién te debe, qué te falta. Son pocas cifras, grandes y con plata.',
    sugerencia: 'Mirá cuáles ya están listas y cuáles esperan un bloque.',
  },
  {
    id: 'inicio-desbloquear',
    ruta: '/inicio',
    ancla: 'inicio-desbloquear',
    alternativa: 'inicio-respuestas',
    titulo: 'Cada bloque desbloquea una respuesta',
    texto:
      'Un bloque es una pieza de tu sistema, como «Ventas» o «Clientes». Cuando sumás una pieza, se desbloquea una respuesta nueva: el valor crece de a poco, sin armar nada complicado.',
    sugerencia: 'Tocá lo que falta para ver qué bloque te lo desbloquea.',
  },
  {
    id: 'subir-contar',
    ruta: '/subir',
    ancla: 'subir-contar',
    alternativa: 'subir-historia',
    titulo: 'Contá qué pasó',
    texto:
      'Escribí lo que pasó en tu negocio como si le mandaras un mensaje a alguien: «vendí 3 tortas a 4500». Ensambla lo entiende y lo acomoda en el bloque que corresponde. No hace falta sacar fotos.',
    sugerencia: 'Escribí una venta de ejemplo y mirá cómo la entiende.',
  },
  {
    id: 'subir-historia',
    ruta: '/subir',
    ancla: 'subir-historia',
    alternativa: 'subir-contar',
    titulo: 'Traé tu historia de una vez',
    texto:
      'Si tenés un cuaderno o una planilla, podés subirlos para empezar con datos de meses atrás. Es un atajo opcional: contar lo de cada día alcanza.',
  },
  {
    id: 'bloques-tarjetas',
    ruta: '/bloques',
    ancla: 'bloques-tarjetas',
    alternativa: 'bloques-sumar',
    sugerencia: 'Mirá qué respuesta te desbloquea cada bloque antes de sumarlo.',
    titulo: 'Tus bloques',
    texto:
      'Estas son las piezas que Ensambla armó para tu negocio. Entrás a una para ver lo que cargaste y, cuando querés saber algo nuevo, sumás la pieza que lo responde.',
  },
  {
    id: 'respuestas-lista',
    ruta: '/respuestas',
    ancla: 'respuestas-lista',
    titulo: 'Todas las respuestas',
    texto:
      'Acá están todas las preguntas que Ensambla sabe contestar. Las que ya se pueden contestar muestran la cifra; las otras te dicen qué bloque falta.',
  },
  {
    id: 'plan-planes',
    ruta: '/plan',
    ancla: 'plan-planes',
    titulo: 'Tu plan y la prueba de 7 días',
    texto:
      'Probás 7 días con todo desbloqueado. Si no te sirve, volvés al plan Gratis y no perdés nada de lo que cargaste: sólo se bloquean algunas funciones. El pago es simulado.',
  },
  {
    id: 'cuenta-demo',
    ruta: '/cuenta',
    ancla: 'cuenta-demo',
    titulo: 'El modo demostración',
    texto:
      'Los planes y pagos son una simulación: podés volver a empezar la prueba desde acá cuando quieras. Para repetir este recorrido, buscalo en «Ayuda», más arriba en esta pantalla.',
  },
];

export const TOTAL_PASOS = PASOS_RECORRIDO.length;

/** Selector CSS del elemento de una ancla. */
export const selectorDeAncla = (ancla: string) => `[data-recorrido="${ancla}"]`;

/** Normaliza un pathname para compararlo: sin barra final (salvo la raíz). */
export function normalizarRuta(ruta: string): string {
  return ruta.length > 1 ? ruta.replace(/\/+$/, '') : ruta;
}
