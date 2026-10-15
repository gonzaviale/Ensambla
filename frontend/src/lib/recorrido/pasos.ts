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
    id: 'hoy-valor',
    ruta: '/hoy',
    ancla: 'hoy-valor',
    titulo: 'El valor que aporta Ensambla',
    texto:
      'Acá ves cuánto te sirve Ensambla: precios corregidos a tiempo, plata que dejaste de perder y horas que te ahorraste. Son cuentas con tus propios datos.',
    sugerencia: 'Abrí «¿Cómo lo calculamos?» para ver de dónde sale cada cifra.',
  },
  {
    id: 'hoy-avisos',
    ruta: '/hoy',
    ancla: 'hoy-aviso',
    alternativa: 'hoy-avisos',
    titulo: 'Los avisos y el botón de un toque',
    texto:
      'Ensambla te avisa lo que importa: un precio en riesgo, una lista nueva, algo que se termina. Cada aviso trae un botón que lo resuelve de un toque. Si no te interesa, tocás «Ignorar».',
    sugerencia:
      'Probá el botón naranja de un aviso. Es una demostración: podés volver a los datos de ejemplo desde Mi cuenta.',
  },
  {
    id: 'hoy-pedido',
    ruta: '/hoy',
    ancla: 'hoy-pedido',
    titulo: 'Tu pedido',
    texto:
      'Lo que te conviene comprar se junta acá, ordenado por proveedor y con el costo estimado. Cuando está listo, lo compartís por WhatsApp.',
  },
  {
    id: 'hoy-resumen',
    ruta: '/hoy',
    ancla: 'hoy-resumen',
    titulo: 'El resumen del día',
    texto:
      'Cómo viene la semana en plata y en ganancia, sin tablas ni planillas. Te marca el producto que anda más cerca del costo.',
  },
  {
    id: 'hoy-ajustar',
    ruta: '/hoy',
    ancla: 'hoy-ajustar',
    titulo: 'Ajustá tus avisos',
    texto:
      'Vos decidís de qué te avisamos y desde cuándo: tu ganancia mínima, qué aumento te parece raro y cuántos días de stock cuidar.',
    sugerencia: 'Tocá «Ajustar avisos» para ver las opciones.',
  },
  {
    id: 'listas-cargar',
    ruta: '/listas',
    ancla: 'listas-cargar',
    alternativa: 'listas-por-revisar',
    titulo: 'Cargá la lista de tu proveedor',
    texto:
      'Cuando tu proveedor te manda su lista (WhatsApp, PDF, Excel o foto), la cargás acá y en un minuto ves qué precios cambiar. Las que esperan revisión quedan en «Por revisar».',
    sugerencia: 'Probá «Cargar una lista» con la lista de ejemplo de un proveedor.',
  },
  {
    id: 'lista-pasos',
    ruta: '/listas/lista-sur-oct',
    ancla: 'lista-pasos',
    titulo: 'Revisás la lista en cuatro pasos',
    texto:
      'Ves qué cambió, resolvés las dudas, revisás tus precios y confirmás. Nada se aplica hasta el último paso, y sólo si vos lo pedís.',
  },
  {
    id: 'lista-contenido',
    ruta: '/listas/lista-sur-oct',
    ancla: 'lista-contenido',
    titulo: 'Dudas y precios según tus reglas',
    texto:
      'En el paso 2 resolvés las dudas, por ejemplo si un número es el precio de una unidad o de un bulto cerrado. En el paso 3 ves el precio que sale de tus reglas y lo podés cambiar.',
    sugerencia: 'Tocá «Seguir» para recorrerlos: no se cambia nada hasta confirmar.',
  },
  {
    id: 'mostrador-lista',
    ruta: '/mostrador',
    ancla: 'mostrador-lista',
    alternativa: 'mostrador-etiquetas',
    titulo: 'Pasá los cambios al mostrador',
    texto:
      'Cuando aplicás una lista, los precios nuevos aparecen acá. Mientras cambiás cada etiqueta en el local, la marcás y queda anotado qué falta.',
    sugerencia: 'Marcá un precio como cambiado. Si te equivocás, lo desmarcás.',
  },
  {
    id: 'mostrador-etiquetas',
    ruta: '/mostrador',
    ancla: 'mostrador-etiquetas',
    titulo: 'Etiquetas listas para imprimir',
    texto:
      'Imprimí las etiquetas con el precio nuevo o copiá la lista de cambios. Es parte del plan Comercio.',
  },
  {
    id: 'precios-productos',
    ruta: '/precios',
    ancla: 'precios-productos',
    alternativa: 'precios-pestana-productos',
    titulo: 'Tu catálogo y cuánto ganás',
    texto:
      'Cada producto con su costo, su precio y su ganancia sobre el costo. Los que ganan menos que tu mínimo se marcan para que los revises.',
    sugerencia: 'Tocá el nombre de un producto: ves de dónde sale su costo y cómo fue cambiando.',
  },
  {
    id: 'precios-reglas',
    ruta: '/precios',
    ancla: 'precios-pestana-reglas',
    titulo: 'Tus reglas de precio',
    texto:
      'Definís cuánto querés ganar en general y por categoría. Con esas reglas Ensambla te propone el precio cuando llega una lista nueva, y vos confirmás.',
    sugerencia: 'Tocá «Reglas de precio» para verlas.',
  },
  {
    id: 'red-como-funciona',
    ruta: '/red',
    ancla: 'red-como-funciona',
    titulo: 'Cómo funciona la red',
    texto:
      'Los proveedores suben su lista una vez y Ensambla la cruza con la de muchos comercios de tu zona. Así te avisa lo que te conviene sin que busques.',
  },
  {
    id: 'red-ofertas',
    ruta: '/red',
    ancla: 'red-ofertas',
    titulo: 'Las ofertas que te sirven',
    texto:
      'Sólo ves ofertas de productos que realmente vendés, con cuánto ahorrarías y hasta cuándo valen. Las demás quedan aparte.',
  },
  {
    id: 'red-comparar',
    ruta: '/red',
    ancla: 'red-comparar',
    titulo: 'Comparar proveedores',
    texto:
      'Para un mismo producto ves lo que cobra cada proveedor y quién te sale más barato, aunque cada uno lo escriba distinto.',
  },
  {
    id: 'red-caro',
    ruta: '/red',
    ancla: 'red-caro',
    titulo: '¿Comprás caro?',
    texto:
      'Te mostramos si pagás más que otros comercios de tu zona por el mismo producto. Son promedios anónimos: nunca se ve qué compra otro comercio.',
  },
  {
    id: 'red-neutralidad',
    ruta: '/red',
    ancla: 'red-neutralidad',
    titulo: 'Privacidad y neutralidad',
    texto:
      'Tus datos son tuyos: las comparaciones son anónimas y sólo aparecen con 5 comercios o más de tu zona. Y ningún mayorista puede pagar para esconder que otro vende más barato.',
    sugerencia: 'Tocá «Ver cómo lo ve un mayorista» para conocer el otro lado de la red.',
  },
  {
    id: 'mayorista-oferta',
    ruta: '/red/mayorista',
    ancla: 'mayorista-oferta',
    titulo: 'La vista del mayorista',
    texto:
      'Así trabaja un mayorista: sube su lista una vez y publica una oferta, que le llega sólo a los comercios que venden ese producto.',
    sugerencia: 'Probá publicar una oferta con un costo menor al habitual y mirá a cuántos llega.',
  },
  {
    id: 'plan-estado',
    ruta: '/plan',
    ancla: 'plan-estado',
    titulo: 'Tu plan y la prueba de 7 días',
    texto:
      'Probás 7 días todo Ensambla, sin cargar tarjeta. Si no te sirve, cancelás cuando quieras: volvés al plan Gratis y no perdés nada de lo que cargaste.',
  },
  {
    id: 'plan-comparar',
    ruta: '/plan',
    ancla: 'plan-comparar',
    titulo: 'Comparar planes y pagar',
    texto:
      'Compará lo que incluye cada plan y elegí pagar por mes o por año. El pago es simulado: acá no se cobra nada de verdad.',
    sugerencia: 'Elegí un plan para ver cómo es el pago simulado.',
  },
  {
    id: 'cuenta-datos',
    ruta: '/cuenta',
    ancla: 'cuenta-datos',
    titulo: 'Los datos de tu comercio',
    texto:
      'El nombre y el contacto que usamos en tus avisos y etiquetas. Los podés cambiar cuando quieras.',
  },
  {
    id: 'cuenta-demo',
    ruta: '/cuenta',
    ancla: 'cuenta-demo',
    titulo: 'El modo demostración',
    texto:
      'Todo lo que viste usa datos de ejemplo: podés probar sin miedo y volver a empezar desde acá. Para repetir este recorrido, buscalo en «Ayuda», más arriba en esta pantalla.',
  },
];

export const TOTAL_PASOS = PASOS_RECORRIDO.length;

/** Selector CSS del elemento de una ancla. */
export const selectorDeAncla = (ancla: string) => `[data-recorrido="${ancla}"]`;

/** Normaliza un pathname para compararlo: sin barra final (salvo la raíz). */
export function normalizarRuta(ruta: string): string {
  return ruta.length > 1 ? ruta.replace(/\/+$/, '') : ruta;
}
