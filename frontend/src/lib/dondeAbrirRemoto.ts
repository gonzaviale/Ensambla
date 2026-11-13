import type {
  Aporte,
  Coincidencia,
  DatosDeZona,
  Descartada,
  Exploracion,
  FactorClave,
  Filtros,
  Punto,
  TipoDePunto,
  ZonaPuntuada,
} from '@/lib/dondeAbrir';
import type { DondeAbrirResponse } from '@/types/zona';

const FACTOR: Record<
  DondeAbrirResponse['ranking'][number]['aportes'][number]['factor'],
  FactorClave
> = {
  COMPETENCIA: 'competencia',
  ACTIVIDAD_ACCESO: 'movimiento',
  DENSIDAD: 'vecinos',
  ESCUELAS: 'escuelas',
  OFICINAS: 'oficinas',
  TRANSPORTE: 'transporte',
  PARQUES: 'parques',
  SALUD: 'salud',
  COMPLEMENTARIOS: 'complementarios',
  PERFIL_SOCIOECONOMICO: 'poder',
  DEMANDA_ENSAMBLA: 'demanda',
  ALQUILER: 'alquiler',
};

const PUNTO: Record<DondeAbrirResponse['ranking'][number]['puntos'][number]['tipo'], TipoDePunto> =
  {
    COMPETENCIA: 'competencia',
    ESCUELA: 'escuela',
    OFICINA: 'oficina',
    TRANSPORTE: 'transporte',
    PARQUE: 'parque',
    SALUD: 'salud',
    COMPLEMENTARIO: 'complementario',
  };

const COINCIDENCIA: Record<DondeAbrirResponse['ranking'][number]['coincidencia'], Coincidencia> = {
  BUENA: 'buena',
  INTERMEDIA: 'intermedia',
  MALA: 'mala',
};

const jurisdiccion = (valor: 'CABA' | 'La Matanza') =>
  valor === 'CABA' ? ('caba' as const) : ('la-matanza' as const);

function datosDeZona(item: DondeAbrirResponse['ranking'][number], filtros: Filtros): DatosDeZona {
  const valor = (factor: keyof typeof FACTOR) =>
    item.aportes.find((aporte) => aporte.factor === factor)?.valor ?? 0;
  return {
    id: item.id,
    jurisdiccion: jurisdiccion(item.jurisdiccion),
    nombre: item.nombre,
    latitud: item.latitud,
    longitud: item.longitud,
    densidad: valor('DENSIDAD'),
    movimiento: valor('ACTIVIDAD_ACCESO'),
    escuelas: valor('ESCUELAS'),
    oficinas: valor('OFICINAS'),
    transporte: valor('TRANSPORTE'),
    parques: valor('PARQUES'),
    salud: valor('SALUD'),
    complementarios: valor('COMPLEMENTARIOS'),
    demanda: valor('DEMANDA_ENSAMBLA'),
    poder: valor('PERFIL_SOCIOECONOMICO'),
    alquiler: valor('ALQUILER'),
    competencia: {
      almacen: filtros.rubro === 'almacen' ? valor('COMPETENCIA') : 0,
      papeleria: filtros.rubro === 'papeleria' ? valor('COMPETENCIA') : 0,
      kiosco: filtros.rubro === 'kiosco' ? valor('COMPETENCIA') : 0,
    },
  };
}

const aporte = (item: DondeAbrirResponse['ranking'][number]['aportes'][number]): Aporte => ({
  clave: FACTOR[item.factor],
  etiqueta: item.etiqueta,
  peso: item.peso,
  puntaje: item.puntaje,
  aporte: item.aporte,
  valor: item.valor,
  unidad: item.unidad,
  fuente: item.fuente,
  observadaEn: item.observadaEn,
});

const punto = (item: DondeAbrirResponse['ranking'][number]['puntos'][number]): Punto => ({
  tipo: PUNTO[item.tipo],
  nombre: item.nombre,
  latitud: item.latitud,
  longitud: item.longitud,
});

export type ExploracionRemota = Exploracion & {
  negocioActual: DondeAbrirResponse['negocioActual'];
  avisos: string[];
  /** Id del lugar medido en vivo, si la persona tocó uno en el mapa. */
  puntoElegidoId: string | null;
};

export function adaptarDondeAbrir(
  respuesta: DondeAbrirResponse,
  filtros: Filtros,
): ExploracionRemota {
  const ranking: ZonaPuntuada[] = respuesta.ranking.map((item) => ({
    zona: datosDeZona(item, filtros),
    distanciaKm: item.distanciaKm ?? 0,
    puntaje: item.puntaje,
    coincidencia: COINCIDENCIA[item.coincidencia],
    aportes: item.aportes.map(aporte),
    porQue: item.porQue,
    riesgos: item.riesgos,
    puntos: item.puntos.map(punto),
  }));
  const descartadas: Descartada[] = respuesta.descartadas.map((item) => ({
    zona: {
      id: item.id,
      jurisdiccion: jurisdiccion(item.jurisdiccion),
      nombre: item.nombre,
      latitud: item.latitud,
      longitud: item.longitud,
      densidad: 0,
      movimiento: 0,
      escuelas: 0,
      oficinas: 0,
      transporte: 0,
      parques: 0,
      salud: 0,
      complementarios: 0,
      poder: 0,
      alquiler: 0,
      competencia: { almacen: 0, papeleria: 0, kiosco: 0 },
    },
    distanciaKm: item.distanciaKm ?? 0,
    motivos: item.motivos,
  }));
  return {
    ranking,
    descartadas,
    evaluadas: respuesta.evaluadas,
    negocioActual: respuesta.negocioActual,
    avisos: respuesta.avisos,
    puntoElegidoId: respuesta.puntoElegidoId ?? null,
  };
}
