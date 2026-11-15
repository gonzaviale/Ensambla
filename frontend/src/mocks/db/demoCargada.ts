import {
  ID_OBJETIVO_DE_EJEMPLO,
  META_POR_DEFECTO,
  TEXTO_OBJETIVO_DE_EJEMPLO,
} from '@/lib/objetivos';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { PREFERENCIAS_INICIALES } from '@/types/preferencias';
import { crearKioscoCargado, crearPuestoDeFeria } from './demoNegocios';
import {
  claveDelNegocio,
  estado,
  guardarObjetivosDe,
  guardarPreferenciasDe,
  guardarSistemasDe,
  objetivosDe,
  sistemasDe,
} from './estado';
import type { NegocioMock } from './negocios';

const DIA_MS = 24 * 60 * 60 * 1000;

/** Los tres negocios de la demo: el almacén es el primero (el de la cuenta) y tiene los avisos y los objetivos. */
const NEGOCIOS_DE_LA_DEMO = [
  { id: 'demo-almacen', nombre: 'Almacén de barrio', rubro: 'ALMACEN', hace: 120 },
  { id: 'demo-kiosco', nombre: 'Kiosco de la estación', rubro: 'KIOSCO', hace: 75 },
  { id: 'demo-feria', nombre: 'Puesto de la feria', rubro: 'OTRO', hace: 70 },
] as const;

const SECCIONES_DE_LA_DEMO: Record<
  (typeof NEGOCIOS_DE_LA_DEMO)[number]['id'],
  (hoy: string) => Sistema
> = {
  'demo-almacen': crearAlmacenCargado,
  'demo-kiosco': crearKioscoCargado,
  'demo-feria': crearPuestoDeFeria,
};

/**
 * Deja la cuenta de demostración lista para mostrar: tres negocios (el almacén con muchos datos, un kiosco que este mes
 * viene en rojo y un puesto de feria sin ventas en el último mes), con las fechas contadas desde hoy, los avisos sin
 * descartar y Premium por un año. Se corre en cada arranque de `npm run dev:demo`, así la demo siempre empieza igual:
 * vuelven los tres negocios y sus secciones de ejemplo, y se conservan las secciones que armó la persona a mano.
 */
export function sembrarDemoCargada(email: string, ahora = new Date()) {
  sembrarNegocios(email, ahora);
  guardarPreferenciasDe(email, PREFERENCIAS_INICIALES);
  // El objetivo de ejemplo vuelve a su lugar (el primero) en cada arranque; los que la persona definió se conservan.
  guardarObjetivosDe(email, [
    {
      id: ID_OBJETIVO_DE_EJEMPLO,
      texto: TEXTO_OBJETIVO_DE_EJEMPLO,
      meta: META_POR_DEFECTO,
      creadoEn: new Date(ahora.getTime() - 35 * DIA_MS).toISOString(),
    },
    ...objetivosDe(email).filter((objetivo) => objetivo.id !== ID_OBJETIVO_DE_EJEMPLO),
  ]);
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(ahora.getTime() - DIA_MS).toISOString(),
    finEn: new Date(ahora.getTime() + 365 * DIA_MS).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  estado.alCambiar?.();
}

function sembrarNegocios(email: string, ahora: Date) {
  const anterior = estado.negociosDeLaCuenta.get(email);
  // Las secciones propias se leen antes de cambiar la lista, con la clave que tenían.
  const propias = new Map(
    NEGOCIOS_DE_LA_DEMO.map(({ id }) => [
      id,
      anterior?.negocios.some((negocio) => negocio.id === id)
        ? sistemasDe(email, id).filter((sistema) => !sistema.demoAlmacen)
        : [],
    ]),
  );
  // Antes de que hubiera varios negocios, las secciones de la cuenta eran las del almacén.
  if (!anterior?.negocios.some((negocio) => negocio.id === 'demo-almacen')) {
    propias.set(
      'demo-almacen',
      (estado.sistemas.get(email) ?? []).filter((sistema) => !sistema.demoAlmacen),
    );
  }

  const negocios: NegocioMock[] = NEGOCIOS_DE_LA_DEMO.map(({ id, nombre, rubro, hace }) => ({
    id,
    nombre,
    rubro,
    creadoEn: new Date(ahora.getTime() - hace * DIA_MS).toISOString(),
  }));
  const activoId = negocios.some((negocio) => negocio.id === anterior?.activoId)
    ? (anterior?.activoId ?? 'demo-almacen')
    : 'demo-almacen';
  estado.negociosDeLaCuenta.set(email, { activoId, negocios });

  const dia = hoy();
  for (const { id } of NEGOCIOS_DE_LA_DEMO) {
    guardarSistemasDe(email, [SECCIONES_DE_LA_DEMO[id](dia), ...(propias.get(id) ?? [])], id);
  }
  // El almacén se llama como en la lista aunque se haya guardado otro nombre; la ubicación se conserva.
  const configuracion = estado.negocios.get(claveDelNegocio(email, 'demo-almacen'));
  if (configuracion) {
    estado.negocios.set(claveDelNegocio(email, 'demo-almacen'), {
      ...configuracion,
      nombre: 'Almacén de barrio',
      rubro: 'ALMACEN',
    });
  }
}
