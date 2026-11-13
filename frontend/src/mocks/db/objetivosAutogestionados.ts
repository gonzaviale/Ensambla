import { lunesDe } from '@/lib/objetivosAutogestion';
import type {
  CandidatoMiniObjetivo,
  DecisionObjetivo,
  MiniObjetivo,
  ObjetivoDelServidor,
  SemanaObjetivo,
  TipoDecision,
} from '@/types/objetivos';

export { lunesDe };

// Lo que el backend guarda de un objetivo que se autogestiona, simulado para la demo y los tests: seis semanas de
// progreso, decisiones de la persona y dos reajustes de domingo. Los montos salen de la meta del objetivo.

export type DatosAutogestion = {
  minis: MiniObjetivo[];
  historial: MiniObjetivo[];
  progreso: { semana: string; valor: number; estimado: boolean }[];
  decisiones: DecisionObjetivo[];
  semanas: SemanaObjetivo[];
};

const DIA_MS = 86_400_000;

export function sumarSemanas(lunes: string, semanas: number): string {
  return new Date(new Date(`${lunes}T12:00:00Z`).getTime() + semanas * 7 * DIA_MS)
    .toISOString()
    .slice(0, 10);
}

const redondear = (valor: number) => Math.round(valor / 100) * 100;

function mini(
  id: string,
  posicion: number,
  datos: Pick<
    MiniObjetivo,
    'tipo' | 'titulo' | 'justificacion' | 'valorInicial' | 'valorActual' | 'meta'
  > &
    Partial<MiniObjetivo>,
): MiniObjetivo {
  const recorrido = datos.meta - datos.valorInicial;
  const avance =
    recorrido === 0
      ? 1
      : Math.min(1, Math.max(0, (datos.valorActual - datos.valorInicial) / recorrido));
  return {
    id,
    posicion,
    sentido: 'SUBE',
    estado: 'EN_CURSO',
    referencia: null,
    registroId: null,
    avance,
    aporteEstimado: 0,
    semanaInicio: '',
    textoConIa: false,
    ...datos,
  };
}

function decision(
  id: string,
  tipo: TipoDecision,
  semanaInicio: string,
  descripcion: string,
  miniObjetivoId: string | null,
  efecto: DecisionObjetivo['efecto'] = 'SIN_MEDICION',
  deltaIngresoSemanal: number | null = null,
): DecisionObjetivo {
  return {
    id,
    tipo,
    miniObjetivoId,
    avisoId: tipo.startsWith('AVISO') ? `aviso-${id}` : null,
    registroId: null,
    descripcion,
    // El miércoles de esa semana, a la tarde.
    tomadaEn: new Date(new Date(`${semanaInicio}T15:00:00Z`).getTime() + 2 * DIA_MS).toISOString(),
    semanaInicio,
    efecto,
    deltaIngresoSemanal,
  };
}

/** Seis semanas cerradas hasta la semana pasada y la de hoy en curso. */
export function datosDeEjemplo(objetivo: ObjetivoDelServidor, hoy: string): DatosAutogestion {
  const actual = lunesDe(hoy);
  const semana = (atras: number) => sumarSemanas(actual, -atras);
  const meta = objetivo.meta;
  const resultados = [0.3, 0.31, 0.34, 0.36, 0.35, 0.41, 0.45].map((parte) =>
    redondear(meta * parte),
  );
  const id = (sufijo: string) => `${objetivo.id}-${sufijo}`;

  const yerbaVieja = mini(id('mini-yerba-1'), 1, {
    tipo: 'SUBIR_PRECIO',
    estado: 'LOGRADO',
    titulo: 'Subile $150 a Yerba 1 kg',
    justificacion: 'Te deja $900 por unidad y vendés cerca de 14 por semana.',
    referencia: 'Yerba 1 kg',
    valorInicial: 12_600,
    valorActual: 14_400,
    meta: 14_200,
    semanaInicio: semana(6),
  });
  const gastos = mini(id('mini-gastos'), 3, {
    tipo: 'RECORTAR_GASTO',
    sentido: 'BAJA',
    estado: 'REEMPLAZADO',
    titulo: 'Recortá $20.000 en gastos del mes',
    justificacion: 'Los gastos extraordinarios subieron en las últimas semanas.',
    valorInicial: 85_000,
    valorActual: 85_000,
    meta: 65_000,
    avance: 0,
    semanaInicio: semana(6),
  });
  const yerba = mini(id('mini-yerba-2'), 1, {
    tipo: 'SUBIR_PRECIO',
    titulo: 'Subile $250 a Yerba 1 kg',
    justificacion: 'Ya llegaste a la meta anterior y en la zona se vende más cara.',
    referencia: 'Yerba 1 kg',
    valorInicial: 14_400,
    valorActual: 15_100,
    meta: 16_000,
    aporteEstimado: 6_800,
    semanaInicio: semana(2),
  });
  const ventas = mini(id('mini-ventas'), 2, {
    tipo: 'VENTAS_SEMANALES',
    titulo: 'Vendé $90.000 por semana',
    justificacion: 'Hoy vendés unos $78.000 por semana: con 15 % más te acercás a la meta.',
    valorInicial: 78_000,
    valorActual: 84_000,
    meta: 90_000,
    aporteEstimado: 12_000,
    semanaInicio: semana(6),
  });
  const fernet = mini(id('mini-fernet'), 3, {
    tipo: 'MOVER_PARADO',
    titulo: 'Mové 6 Fernet por semana',
    justificacion: 'Tenés 18 en el estante y hace 3 semanas que casi no sale.',
    referencia: 'Fernet 750 ml',
    valorInicial: 1,
    valorActual: 3,
    meta: 6,
    aporteEstimado: 9_000,
    semanaInicio: semana(1),
  });

  const decisiones = [
    decision(id('d1'), 'PASO_ACEPTADO', semana(5), 'Subile $150 a Yerba 1 kg', yerbaVieja.id),
    decision(
      id('d2'),
      'AVISO_APLICADO',
      semana(4),
      'Subí el precio de Yerba 1 kg',
      yerbaVieja.id,
      'MEDIDO',
      4_500,
    ),
    decision(id('d3'), 'PASO_IGNORADO', semana(3), 'Recortá $20.000 en gastos del mes', gastos.id),
    decision(
      id('d4'),
      'AVISO_DESCARTADO',
      semana(2),
      'Poné en oferta el alfajor triple',
      ventas.id,
    ),
    decision(id('d5'), 'PASO_ACEPTADO', semana(1), 'Mové 6 Fernet por semana', fernet.id),
    decision(
      id('d6'),
      'AVISO_APLICADO',
      semana(1),
      'Poné el Fernet a la vista',
      fernet.id,
      'MIDIENDO',
    ),
  ];

  const conteo = (lunes: string) => {
    const deLaSemana = decisiones.filter((item) => item.semanaInicio === lunes);
    const cuantas = (tipo: TipoDecision) => deLaSemana.filter((item) => item.tipo === tipo).length;
    return {
      aplicadas: cuantas('AVISO_APLICADO'),
      descartadas: cuantas('AVISO_DESCARTADO'),
      aceptadas: cuantas('PASO_ACEPTADO'),
      ignoradas: cuantas('PASO_IGNORADO'),
    };
  };
  const pesos = (valor: number) => `$${new Intl.NumberFormat('es-AR').format(valor)}`;
  const faltan = (resultado: number) => `Te faltan ${pesos(meta - resultado)} para tu meta.`;

  const cierres: SemanaObjetivo[] = [6, 5, 4, 3, 2, 1].map((atras) => {
    const resultado = resultados[6 - atras] ?? 0;
    const anterior = resultados[5 - atras];
    const lunes = semana(atras);
    const variacion =
      anterior === undefined
        ? `Al cerrar la semana, lo que te queda de los últimos 30 días es ${pesos(resultado)}.`
        : resultado >= anterior
          ? `Esta semana tu resultado subió ${pesos(resultado - anterior)}: de ${pesos(anterior)} a ${pesos(resultado)}.`
          : `Esta semana tu resultado bajó ${pesos(anterior - resultado)}: de ${pesos(anterior)} a ${pesos(resultado)}.`;
    return {
      semana: lunes,
      resultado,
      avance: Math.min(1, resultado / meta),
      decisiones: conteo(lunes),
      queFunciono: `${variacion} ${faltan(resultado)}`,
      textoConIa: false,
      reajustes: [],
    };
  });
  const cierreDe = (atras: number) => cierres.find((cierre) => cierre.semana === semana(atras));
  cierreDe(3)?.reajustes.push({
    tipo: 'SUBIR_META',
    motivo: 'Lo lograste: llegaste a $14.400 por semana. Ahora el objetivo es $16.000 por semana.',
    de: { titulo: yerbaVieja.titulo, meta: yerbaVieja.meta },
    a: { titulo: yerba.titulo, meta: yerba.meta },
  });
  cierreDe(2)?.reajustes.push({
    tipo: 'REEMPLAZAR',
    motivo: `Hace 2 semanas que «${gastos.titulo}» no avanza. Lo cambiamos por «${fernet.titulo}».`,
    de: { titulo: gastos.titulo, meta: gastos.meta },
    a: { titulo: fernet.titulo, meta: fernet.meta },
  });

  return {
    minis: [yerba, ventas, fernet],
    historial: [gastos, yerbaVieja],
    progreso: resultados.map((valor, indice) => ({
      semana: semana(6 - indice),
      valor,
      estimado: indice < 2,
    })),
    decisiones,
    semanas: cierres.reverse(),
  };
}

/** Hasta tres alternativas para el lugar de un mini-objetivo: nunca el mismo tipo ni uno que ya está en curso. */
export function candidatosPara(
  datos: DatosAutogestion,
  mini: MiniObjetivo,
): CandidatoMiniObjetivo[] {
  const enCurso = new Set(datos.minis.map((item) => item.titulo));
  return CANDIDATOS.filter(
    (candidato) => candidato.tipo !== mini.tipo && !enCurso.has(candidato.titulo),
  ).slice(0, 3);
}

const CANDIDATOS: CandidatoMiniObjetivo[] = [
  {
    tipo: 'PONER_A_LA_VISTA',
    sentido: 'SUBE',
    titulo: 'Poné las galletitas al lado de la caja',
    justificacion: 'Salen junto con el alfajor y hoy están en el fondo.',
    referencia: 'Galletitas surtidas',
    registroId: 'registro-galletitas',
    valorInicial: 8,
    meta: 12,
    aporteEstimado: 5_200,
  },
  {
    tipo: 'MEJORAR_MARGEN',
    sentido: 'SUBE',
    titulo: 'Llevá tu margen al 32 %',
    justificacion: 'Hoy ganás 28 de cada 100 que vendés; la zona anda por 33.',
    referencia: null,
    registroId: null,
    valorInicial: 28,
    meta: 32,
    aporteEstimado: 14_000,
  },
  {
    tipo: 'RECORTAR_GASTO',
    sentido: 'BAJA',
    titulo: 'Recortá $10.000 en gastos del mes',
    justificacion: 'Con la mitad de lo que te propusimos antes ya sumás.',
    referencia: null,
    registroId: null,
    valorInicial: 85_000,
    meta: 75_000,
    aporteEstimado: 10_000,
  },
  {
    tipo: 'MOVER_PARADO',
    sentido: 'SUBE',
    titulo: 'Mové 4 aceites por semana',
    justificacion: 'Tenés 12 en el depósito y se vencen en dos meses.',
    referencia: 'Aceite 1,5 l',
    registroId: 'registro-aceite',
    valorInicial: 1,
    meta: 4,
    aporteEstimado: 7_500,
  },
];
