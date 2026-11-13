import { zonaConAcceso, type TipoDeCupo } from '../db/contenidoPlan';
import { delay, http, HttpResponse } from 'msw';
import { productosPropios } from '@/lib/sistema/zona';
import { pedidoDeAnalisisSchema } from '@/types/zona';
import { sistemasDe } from '../db/estado';
import {
  ordenadaParaVista,
  zonaDeLaPersona,
  zonaParaPunto,
  zonaParaTexto,
  type VistaDeLaZona,
} from '../db/zona';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';
import {
  FILTROS_INICIALES,
  ZONAS,
  distanciaEnKm,
  explorar,
  puntosDeLaZona,
  puntuar,
  type DatosDeZona,
  type FactorClave,
  type Filtros,
  type Rubro,
} from '@/lib/dondeAbrir';

const ID_DEL_PUNTO_ELEGIDO = '00000000-0000-4000-8000-0000000000ff';
const DEMORA_DEL_ANALISIS_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// Cada pantalla de Mi zona ordena los productos a su manera y gasta su propio cupo gratuito.
const VISTAS: Record<string, { vista: VistaDeLaZona; cupo: TipoDeCupo }> = {
  precios: { vista: 'precios', cupo: 'ZONA' },
  cerca: { vista: 'cerca', cupo: 'ZONA_CERCA' },
};

// La zona llega siempre agregada: el mínimo de tres comercios se aplica acá, antes de entregar nada.
export const handlersZona = [
  http.get(api('/zona/agregados'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const pedida = VISTAS[new URL(request.url).searchParams.get('vista') ?? 'precios'];
    if (!pedida) {
      return errorResponse(400, 'La vista tiene que ser «precios» o «cerca»', pathDe(request));
    }
    const zona = ordenadaParaVista(
      zonaDeLaPersona(),
      pedida.vista,
      productosPropios(sistemasDe(usuario.email)),
    );
    return HttpResponse.json(zonaConAcceso(usuario.email, zona, pedida.cupo));
  }),

  http.post(api('/zona/analisis'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DEL_ANALISIS_MS);
    const pedido = pedidoDeAnalisisSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(400, 'Indicá un lugar o un punto del mapa', pathDe(request));
    }
    const otraZona = (zona: ReturnType<typeof zonaParaTexto>) =>
      HttpResponse.json(
        zonaConAcceso(usuario.email, ordenadaParaVista(zona, 'otra', []), 'ZONA_OTRA'),
      );
    if ('lugar' in pedido.data) return otraZona(zonaParaTexto(pedido.data.lugar));
    const zona = zonaParaPunto(pedido.data);
    return zona
      ? otraZona(zona)
      : errorResponse(404, 'Todavía no tenemos datos de esa zona', pathDe(request));
  }),

  http.post(api('/zona/donde-abrir'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    const body = (await request.json()) as {
      rubro?: string;
      publico?: string;
      importancia?: Record<string, 0 | 1 | 2>;
      limites?: Partial<Filtros['limites']>;
      punto?: { latitud?: number; longitud?: number };
    };
    const rubro = ({ ALMACEN: 'almacen', PAPELERIA: 'papeleria', KIOSCO: 'kiosco' } as const)[
      body.rubro as 'ALMACEN'
    ];
    if (!rubro) return errorResponse(400, 'Elegí un rubro válido', pathDe(request));
    const publico = (
      { VECINOS: 'vecinos', ESTUDIANTES: 'estudiantes', OFICINAS: 'oficinas' } as const
    )[body.publico as 'VECINOS'];
    if (!publico) return errorResponse(400, 'Elegí el público principal', pathDe(request));
    const factorDesdeApi: Record<string, FactorClave> = {
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
    const importancia = { ...FILTROS_INICIALES.importancia };
    Object.entries(body.importancia ?? {}).forEach(([factor, valor]) => {
      const clave = factorDesdeApi[factor];
      if (clave) importancia[clave] = valor;
    });
    const filtros: Filtros = {
      rubro,
      publico,
      importancia,
      limites: {
        ...FILTROS_INICIALES.limites,
        distanciaMaxKm: body.limites?.distanciaMaxKm ?? 6,
        competenciaMax: body.limites?.competenciaMax ?? 15,
        escuelasMin: body.limites?.escuelasMin ?? 0,
        parquesMin: body.limites?.parquesMin ?? 0,
        transporteMin: body.limites?.transporteMin ?? 0,
      },
    };
    const resultado = explorar(filtros);
    // Modo demo: un lugar tocado en el mapa se simula con los números de la zona precargada más cercana.
    // El servidor real lo mide en vivo con OpenStreetMap.
    let zonaDelPunto: DatosDeZona | null = null;
    if (body.punto) {
      const { latitud, longitud } = body.punto;
      if (typeof latitud !== 'number' || typeof longitud !== 'number')
        return errorResponse(
          400,
          'El punto elegido tiene que tener latitud y longitud válidas',
          pathDe(request),
        );
      const cercana = ZONAS.map((zona) => ({
        zona,
        km: distanciaEnKm({ latitud, longitud }, zona),
      })).sort((a, b) => a.km - b.km)[0];
      if (!cercana || cercana.km > 8)
        return errorResponse(
          400,
          'Por ahora medimos puntos de CABA y La Matanza: elegí un lugar dentro de esas zonas',
          pathDe(request),
        );
      zonaDelPunto = {
        ...cercana.zona,
        id: 'punto-elegido',
        nombre: `Punto elegido · cerca de ${cercana.zona.nombre}`,
        latitud,
        longitud,
      };
    }
    const puntoPuntuado = zonaDelPunto ? puntuar(zonaDelPunto, filtros) : null;
    const rankingConPunto = puntoPuntuado
      ? [...resultado.ranking, puntoPuntuado].sort(
          (a, b) => b.puntaje - a.puntaje || a.distanciaKm - b.distanciaKm,
        )
      : resultado.ranking;
    const idDe = (id: string) => {
      if (id === 'punto-elegido') return ID_DEL_PUNTO_ELEGIDO;
      const indice = Math.max(
        0,
        ZONAS.findIndex((zona) => zona.id === id),
      );
      return `00000000-0000-4000-8000-${String(indice + 1).padStart(12, '0')}`;
    };
    const fuente = {
      codigo: 'OSM',
      nombre: 'OpenStreetMap',
      tipo: 'COMUNITARIA',
      url: 'https://www.openstreetmap.org/',
      licencia: 'ODbL 1.0',
    };
    const factorApi: Record<FactorClave, string> = Object.fromEntries(
      Object.entries(factorDesdeApi).map(([api, front]) => [front, api]),
    ) as Record<FactorClave, string>;
    const valor = (zona: (typeof ZONAS)[number], factor: FactorClave, rubroActual: Rubro) => {
      if (factor === 'competencia') return zona.competencia[rubroActual];
      if (factor === 'vecinos') return zona.densidad;
      if (factor === 'demanda') return zona.demanda ?? 0;
      return zona[factor];
    };
    const tipoPunto = {
      competencia: 'COMPETENCIA',
      escuela: 'ESCUELA',
      oficina: 'OFICINA',
      transporte: 'TRANSPORTE',
      parque: 'PARQUE',
      salud: 'SALUD',
      complementario: 'COMPLEMENTARIO',
    } as const;
    const zonaBase = (zona: (typeof ZONAS)[number], distanciaKm: number) => ({
      id: idDe(zona.id),
      nombre: zona.nombre,
      localidad: zona.jurisdiccion === 'caba' ? 'Ciudad Autónoma de Buenos Aires' : zona.nombre,
      jurisdiccion: zona.jurisdiccion === 'caba' ? 'CABA' : 'La Matanza',
      latitud: zona.latitud,
      longitud: zona.longitud,
      distanciaKm,
    });
    const ranking = rankingConPunto.map((item) => ({
      ...zonaBase(item.zona, item.distanciaKm),
      puntaje: item.puntaje,
      coincidencia: item.coincidencia.toUpperCase(),
      aportes: item.aportes
        .filter((aporte) => aporte.clave !== 'demanda')
        .map((aporte) => ({
          factor: factorApi[aporte.clave],
          etiqueta: aporte.etiqueta,
          valor: valor(item.zona, aporte.clave, filtros.rubro),
          unidad: 'estimacion',
          peso: aporte.peso,
          puntaje: aporte.puntaje,
          aporte: aporte.aporte,
          fuente,
          observadaEn: '2026-11-10',
        })),
      porQue: item.porQue,
      riesgos: item.riesgos,
      puntos: puntosDeLaZona(item.zona, filtros.rubro).map((punto, indice) => ({
        clave: `mock:${item.zona.id}:${indice}`,
        tipo: tipoPunto[punto.tipo],
        rubro: punto.tipo === 'competencia' ? body.rubro : null,
        nombre: punto.nombre,
        latitud: punto.latitud,
        longitud: punto.longitud,
        fuente,
        observadaEn: '2026-11-10',
      })),
    }));
    return HttpResponse.json({
      negocioActual: {
        nombre: `Negocio de ${usuario.nombre}`,
        rubro: 'ALMACEN',
        direccion: 'Av. Corrientes 5000',
        latitud: -34.5996,
        longitud: -58.4403,
        zonaId: null,
      },
      evaluadas: resultado.evaluadas + (puntoPuntuado ? 1 : 0),
      ranking,
      descartadas: resultado.descartadas.map((item) => ({
        ...zonaBase(item.zona, item.distanciaKm),
        motivos: item.motivos,
      })),
      avisos: puntoPuntuado
        ? ['Modo demo: el lugar que elegiste se simula con los números de la zona más cercana']
        : [],
      puntoElegidoId: puntoPuntuado ? ID_DEL_PUNTO_ELEGIDO : null,
    });
  }),
];
