import {
  ID_OBJETIVO_DE_EJEMPLO,
  META_DEL_EJEMPLO,
  TEXTO_OBJETIVO_DE_EJEMPLO,
} from '@/lib/objetivos';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { PREFERENCIAS_INICIALES } from '@/types/preferencias';
import { crearKioscoCargado, crearPuestoDeFeria } from './demoNegocios';
import {
  estado,
  guardarObjetivosDe,
  guardarPreferenciasDe,
  guardarSistemasDe,
  objetivosDe,
  sistemasDe,
} from './estado';
import {
  datosSinDatosDeEjemplo,
  ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
  TEXTO_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
} from './objetivosAutogestionados';

const DIA_MS = 24 * 60 * 60 * 1000;

/** Los tres negocios de la demo, cada uno en su cuaderno: el almacén es el primero. */
const NEGOCIOS_DE_LA_DEMO: ReadonlyArray<{
  id: string;
  nombre: string;
  hace: number;
  crear: (hoy: string) => Sistema;
}> = [
  { id: 'demo-almacen', nombre: 'Almacén de barrio', hace: 120, crear: crearAlmacenCargado },
  { id: 'demo-kiosco', nombre: 'Kiosco de la estación', hace: 75, crear: crearKioscoCargado },
  { id: 'demo-feria', nombre: 'Puesto de la feria', hace: 70, crear: crearPuestoDeFeria },
];

/**
 * Deja la cuenta de demostración lista para mostrar: tres negocios (el almacén con muchos datos, un kiosco que este mes
 * viene en rojo y un puesto de feria sin ventas en el último mes), con las fechas contadas desde hoy, los avisos sin
 * descartar y Premium por un año. Se corre en cada arranque de `npm run dev:demo`, así la demo siempre empieza igual:
 * vuelven los tres negocios de ejemplo y se conservan los que armó la persona a mano.
 */
export function sembrarDemoCargada(email: string, ahora = new Date()) {
  const propios = sistemasDe(email).filter((sistema) => !sistema.demoAlmacen);
  guardarSistemasDe(email, [...negociosDeLaDemo(ahora), ...propios]);
  guardarPreferenciasDe(email, PREFERENCIAS_INICIALES);
  // El objetivo de ejemplo vuelve a su lugar (el primero) en cada arranque; los que la persona definió se conservan.
  guardarObjetivosDe(email, [
    {
      id: ID_OBJETIVO_DE_EJEMPLO,
      texto: TEXTO_OBJETIVO_DE_EJEMPLO,
      tipo: 'MONETARIO',
      meta: META_DEL_EJEMPLO,
      armadoConSami: false,
      creadoEn: new Date(ahora.getTime() - 35 * DIA_MS).toISOString(),
    },
    {
      id: ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
      texto: TEXTO_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
      tipo: 'SIN_DATOS',
      meta: null,
      armadoConSami: true,
      creadoEn: new Date(ahora.getTime() - 9 * DIA_MS).toISOString(),
    },
    ...objetivosDe(email).filter(
      (objetivo) =>
        objetivo.id !== ID_OBJETIVO_DE_EJEMPLO && objetivo.id !== ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
    ),
  ]);
  // Un objetivo sin datos con manuales para contar y un resumen del domingo para leer.
  const { respondido, ...sinDatos } = datosSinDatosDeEjemplo(
    ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO,
    hoy(),
  );
  estado.autogestion.set(ID_OBJETIVO_SIN_DATOS_DE_EJEMPLO, sinDatos);
  estado.respuestasDeMinis.set(respondido, {
    id: `${respondido}-respuesta`,
    miniObjetivoId: respondido,
    hizo: 'SI',
    quePaso: 'FUNCIONO',
    comentario: null,
    semanaInicio: hoy(),
    respondidaEn: ahora.toISOString(),
  });
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(ahora.getTime() - DIA_MS).toISOString(),
    finEn: new Date(ahora.getTime() + 365 * DIA_MS).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  estado.alCambiar?.();
}

/** Cada negocio con un id y un nombre fijos, así el elegido en el navegador se conserva entre arranques. */
function negociosDeLaDemo(ahora: Date): Sistema[] {
  const dia = hoy();
  return NEGOCIOS_DE_LA_DEMO.map(({ id, nombre, hace, crear }) => ({
    ...crear(dia),
    id,
    name: nombre,
    createdAt: new Date(ahora.getTime() - hace * DIA_MS).toISOString(),
  }));
}
