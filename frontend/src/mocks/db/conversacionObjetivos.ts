import { lunesDe } from '@/lib/objetivosAutogestion';
import type {
  MiniManual,
  MiniMedido,
  MiniParaGuardar,
  PropuestaObjetivo,
  TipoMiniObjetivo,
  TipoMiniObjetivoPropuesto,
  TipoObjetivo,
} from '@/types/objetivos';

// Lo que hace el servidor cuando la persona le cuenta un objetivo a Sami, simulado para la demo y los tests: lee el
// texto por palabras clave (sin IA), elige métricas de un catálogo fijo, redacta acciones manuales y nunca inventa un
// número. Con datos, sin datos o mezclado: sale de los mini-objetivos que quedan.

export const MAXIMO_DE_MENSAJES = 6;
export const MAXIMO_DE_MINIS = 3;
const LIMITE_DE_MENSAJE = 500;

export const LIMITE_GRATUITO = 5;
export const LIMITE_PREMIUM = 20;

type Medida = Pick<
  MiniMedido,
  'tipo' | 'sentido' | 'titulo' | 'justificacion' | 'referencia' | 'registroId'
> & { valorInicial: number; meta: number; aporteEstimado: number };

/** El catálogo de métricas, con los números que calcula el servidor con los datos del negocio de ejemplo. */
const CATALOGO: Record<TipoMiniObjetivo, Medida> = {
  VENTAS_SEMANALES: {
    tipo: 'VENTAS_SEMANALES',
    sentido: 'SUBE',
    titulo: 'Vender un 10 % más por semana',
    justificacion:
      'En los últimos 7 días vendiste $182.000. Con $200.200 por semana sumás cerca de $31.200 por mes.',
    referencia: null,
    registroId: null,
    valorInicial: 182_000,
    meta: 200_200,
    aporteEstimado: 31_200,
  },
  PONER_A_LA_VISTA: {
    tipo: 'PONER_A_LA_VISTA',
    sentido: 'SUBE',
    titulo: 'Poné la yerba cerca de la caja',
    justificacion: 'Es lo que más sale y hoy está en el fondo: a la vista vendés más.',
    referencia: 'Yerba 1 kg',
    registroId: 'registro-yerba',
    valorInicial: 14,
    meta: 18,
    aporteEstimado: 6_800,
  },
  SUBIR_PRECIO: {
    tipo: 'SUBIR_PRECIO',
    sentido: 'SUBE',
    titulo: 'Subile $150 a Yerba 1 kg',
    justificacion: 'Te deja $900 por unidad y vendés cerca de 14 por semana.',
    referencia: 'Yerba 1 kg',
    registroId: 'registro-yerba',
    valorInicial: 12_600,
    meta: 12_750,
    aporteEstimado: 8_400,
  },
  MEJORAR_MARGEN: {
    tipo: 'MEJORAR_MARGEN',
    sentido: 'SUBE',
    titulo: 'Llevá tu ganancia por venta al 32 %',
    justificacion: 'Hoy ganás 28 de cada 100 que vendés; la zona anda por 33.',
    referencia: null,
    registroId: null,
    valorInicial: 28,
    meta: 32,
    aporteEstimado: 14_000,
  },
  RECORTAR_GASTO: {
    tipo: 'RECORTAR_GASTO',
    sentido: 'BAJA',
    titulo: 'Recortá $5.000 en luz',
    justificacion: 'Este mes la luz te salió $38.000: con $33.000 liberás plata cada mes.',
    referencia: 'Luz',
    registroId: 'registro-luz',
    valorInicial: 38_000,
    meta: 33_000,
    aporteEstimado: 5_000,
  },
  MOVER_PARADO: {
    tipo: 'MOVER_PARADO',
    sentido: 'SUBE',
    titulo: 'Mové 6 Fernet por semana',
    justificacion: 'Tenés 18 en el estante y hace 3 semanas que casi no sale.',
    referencia: 'Fernet 750 ml',
    registroId: 'registro-fernet',
    valorInicial: 1,
    meta: 6,
    aporteEstimado: 9_000,
  },
};

type Accion = { titulo: string; justificacion: string };

const MANUALES: Record<'conocer' | 'atender' | 'general', Accion[]> = {
  conocer: [
    {
      titulo: 'Pedile a 3 clientes que te recomienden con un vecino',
      justificacion:
        'Es la forma más barata de que te conozcan: la recomendación de alguien del barrio.',
    },
    {
      titulo: 'Poné un cartel en la vereda con lo que ofrecés',
      justificacion: 'Mucha gente pasa por la puerta y no sabe qué vendés.',
    },
  ],
  atender: [
    {
      titulo: 'Saludá por el nombre a los clientes de siempre',
      justificacion: 'Un saludo personal hace que vuelvan.',
    },
    {
      titulo: 'Preguntale a 5 clientes qué les falta en el local',
      justificacion: 'Te dicen qué agregar sin que tengas que adivinar.',
    },
  ],
  general: [
    {
      titulo: 'Escribí una cosa para mejorar cada día',
      justificacion: 'Con lo que anotes, el domingo armamos el siguiente paso.',
    },
    {
      titulo: 'Hablá con 3 clientes sobre lo que querés lograr',
      justificacion: 'Sus respuestas te dicen por dónde empezar.',
    },
  ],
};

const SEGUIMIENTO_MANUAL = 'Cargá cómo te fue con tu objetivo';

const sinTildes = (texto: string) =>
  texto
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '');

/** Una meta en pesos por mes: va por el formulario de siempre, no con Sami. */
const ES_MONETARIO = /\$\s?\d|\b\d[\d.,]*\s?(mil|k|lucas|palos?|millon(es)?)\b/;

function medidosPara(texto: string): TipoMiniObjetivo[] {
  const elegidos: TipoMiniObjetivo[] = [];
  const quiere = (patron: RegExp) => patron.test(texto);
  if (quiere(/yerba/)) elegidos.push('PONER_A_LA_VISTA');
  if (quiere(/vend|venta/)) elegidos.push('VENTAS_SEMANALES');
  if (quiere(/gastar menos|gasto|luz/)) elegidos.push('RECORTAR_GASTO');
  if (quiere(/ganar mas|margen|precio/)) elegidos.push('SUBIR_PRECIO', 'MEJORAR_MARGEN');
  if (quiere(/parad|sin salida|stock/)) elegidos.push('MOVER_PARADO');
  return elegidos;
}

function manualesPara(texto: string): Accion[] {
  if (/conozc|barrio|gente|local|redes|cartel/.test(texto)) return MANUALES.conocer;
  if (/atender|cliente|servicio/.test(texto)) return MANUALES.atender;
  return [];
}

const comoMedido = (medida: Medida): TipoMiniObjetivoPropuesto => ({
  ...medida,
  modo: 'MEDIDO',
  textoConIa: false,
});

const comoManual = (accion: Accion): TipoMiniObjetivoPropuesto => ({
  modo: 'MANUAL',
  tipo: null,
  registroId: null,
  referencia: null,
  valorInicial: null,
  meta: null,
  aporteEstimado: null,
  textoConIa: true,
  ...accion,
});

/** El tipo que queda según los mini-objetivos: sólo medidos, sólo manuales o los dos. */
export function tipoDeObjetivo(minis: { modo: 'MEDIDO' | 'MANUAL' }[]): TipoObjetivo | null {
  const medidos = minis.some((mini) => mini.modo === 'MEDIDO');
  const manuales = minis.some((mini) => mini.modo === 'MANUAL');
  if (medidos && manuales) return 'HIBRIDO';
  if (medidos) return 'CON_DATOS';
  return manuales ? 'SIN_DATOS' : null;
}

const DATOS: PropuestaObjetivo['datos'] = [
  { fuente: 'VENTAS', hay: true },
  { fuente: 'MARGEN', hay: true },
  { fuente: 'INVENTARIO', hay: true },
  { fuente: 'GASTOS', hay: true },
  { fuente: 'ZONA', hay: false },
];

export type MensajeSimulado = { rol: 'PERSONA' | 'SAMI'; texto: string };

/** Lo que el servidor rechaza con un 400: sin mensajes, de más o vacíos. */
export class ConversacionInvalida extends Error {}

function mensajesDeLaPersona(mensajes: MensajeSimulado[]) {
  const personas = mensajes.filter((mensaje) => mensaje.rol === 'PERSONA');
  if (personas.length === 0) throw new ConversacionInvalida('Contale a Sami qué querés lograr.');
  if (personas.length > MAXIMO_DE_MENSAJES)
    throw new ConversacionInvalida(
      'Llegaste al máximo de mensajes de esta conversación. Guardá lo que armaste o empezá de nuevo.',
    );
  if (personas.some((mensaje) => !mensaje.texto.trim() || mensaje.texto.length > LIMITE_DE_MENSAJE))
    throw new ConversacionInvalida('Cada mensaje tiene que tener entre 1 y 500 caracteres.');
  return personas;
}

/** Sami responde con lo que entendió y propone hasta tres mini-objetivos; sin cuota no consulta a la IA. */
export function proponerSimulado(
  mensajes: MensajeSimulado[],
  cuota: { usadas: number; limite: number },
): PropuestaObjetivo {
  const personas = mensajesDeLaPersona(mensajes);
  const primero = personas[0]?.texto.trim() ?? '';
  const ultimo = personas.at(-1)?.texto ?? '';
  const mensajesRestantes = MAXIMO_DE_MENSAJES - personas.length;
  const cuotaActual = {
    usadas: cuota.usadas,
    limite: cuota.limite,
    restantes: Math.max(0, cuota.limite - cuota.usadas),
  };

  if (cuotaActual.restantes === 0) {
    return {
      respuesta: `Ya usaste los ${cuota.limite} objetivos con IA de este mes. Podés guardarlo igual como seguimiento manual.`,
      entendido: '',
      tipo: null,
      esMonetario: false,
      minis: [comoManual({ titulo: SEGUIMIENTO_MANUAL, justificacion: primero })],
      datos: [],
      origen: 'RESPALDO',
      mensajesRestantes,
      puedeGuardar: true,
      cuota: cuotaActual,
    };
  }

  if (ES_MONETARIO.test(sinTildes(ultimo))) {
    return {
      respuesta:
        'Una meta en pesos por mes se define con el formulario de siempre: allí Sami la mide con tu resultado.',
      entendido: '',
      tipo: null,
      esMonetario: true,
      minis: [],
      datos: [],
      origen: 'IA',
      mensajesRestantes,
      puedeGuardar: false,
      cuota: cuotaActual,
    };
  }

  const texto = sinTildes(personas.map((mensaje) => mensaje.texto).join(' '));
  const medidos = [...new Set(medidosPara(texto))].map((tipo) => comoMedido(CATALOGO[tipo]));
  const manuales = manualesPara(texto).map(comoManual);
  const sinPistas = medidos.length === 0 && manuales.length === 0;
  const minis = [
    ...medidos.slice(0, 2),
    ...(sinPistas ? MANUALES.general.map(comoManual) : manuales),
  ].slice(0, MAXIMO_DE_MINIS);
  const tipo = tipoDeObjetivo(minis);
  const lectura: Partial<Record<TipoObjetivo, string>> = {
    CON_DATOS: 'Lo puedo medir con los datos de tu negocio.',
    SIN_DATOS: 'De eso no hay datos: lo vas a ir contando vos cada semana.',
    HIBRIDO: 'Una parte la mido yo y otra me la vas a contar vos.',
  };
  return {
    respuesta: `Entendí: ${primero}. ${tipo ? (lectura[tipo] ?? '') : ''}`.trim(),
    entendido: primero.slice(0, 300),
    tipo,
    esMonetario: false,
    minis,
    datos: DATOS.map((dato) => ({ ...dato })),
    origen: 'IA',
    mensajesRestantes,
    puedeGuardar: minis.length > 0,
    cuota: cuotaActual,
  };
}

/** Lo que queda guardado de cada mini-objetivo aceptado; null si no está en el catálogo o no cumple las reglas. */
export function minisDeUnObjetivo(
  minis: MiniParaGuardar[],
  seguimientoManual: boolean,
  texto: string,
  objetivoId: string,
  hoy: string,
): (MiniMedido | MiniManual)[] | null {
  const lunes = lunesDe(hoy);
  const base = (indice: number, titulo: string, justificacion: string) => ({
    id: `${objetivoId}-mini-${indice + 1}`,
    posicion: indice + 1,
    estado: 'EN_CURSO' as const,
    titulo,
    justificacion,
    semanaInicio: lunes,
  });
  if (seguimientoManual)
    return [
      { ...base(0, SEGUIMIENTO_MANUAL, texto.slice(0, 280)), modo: 'MANUAL', textoConIa: false },
    ];
  if (minis.length === 0 || minis.length > MAXIMO_DE_MINIS) return null;
  const guardados: (MiniMedido | MiniManual)[] = [];
  for (const [indice, mini] of minis.entries()) {
    if (mini.modo === 'MEDIDO') {
      // De un medido sólo se sabe cuál es: los números los vuelve a calcular el servidor.
      const medida = CATALOGO[mini.tipo];
      guardados.push({
        ...base(indice, medida.titulo, medida.justificacion),
        modo: 'MEDIDO',
        tipo: medida.tipo,
        sentido: medida.sentido,
        referencia: medida.referencia,
        registroId: medida.registroId,
        valorInicial: medida.valorInicial,
        valorActual: medida.valorInicial,
        meta: medida.meta,
        avance: 0,
        aporteEstimado: medida.aporteEstimado,
        textoConIa: false,
      });
      continue;
    }
    const titulo = mini.titulo.trim();
    const justificacion = mini.justificacion.trim();
    // Una acción puede decir «hablá con 3 clientes», pero no afirmar una cifra de plata ni un porcentaje.
    if (!titulo || titulo.length > 80 || !justificacion || justificacion.length > 280) return null;
    if (/[$%]/.test(`${titulo} ${justificacion}`)) return null;
    guardados.push({ ...base(indice, titulo, justificacion), modo: 'MANUAL', textoConIa: false });
  }
  const tipos = guardados.flatMap((mini) => (mini.modo === 'MEDIDO' ? [mini.tipo] : []));
  return new Set(tipos).size === tipos.length ? guardados : null;
}
