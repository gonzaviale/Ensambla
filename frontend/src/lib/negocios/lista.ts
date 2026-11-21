import type { Sistema } from '@/lib/sistema/tipos';
import type { PanelDelNegocio } from '@/types/panel';
import type {
  ListaDeNegocios,
  NegocioDeLaCuenta,
  ResumenConjunto,
  ResumenDeNegocio,
} from '@/types/negocios';

// Cada negocio de la cuenta es un cuaderno. El servidor no sabe cuál se está usando, así que el activo se recuerda en
// este navegador: si el guardado ya no existe (se eliminó, o es de otra cuenta) vale el primero.

const CLAVE_DEL_ACTIVO = 'ensambla.negocio.activo';

/** Cuántos días mira el resumen conjunto. */
export const DIAS_DEL_RESUMEN_CONJUNTO = 30;

export function leerNegocioGuardado(): string | null {
  try {
    return window.localStorage.getItem(CLAVE_DEL_ACTIVO);
  } catch {
    return null;
  }
}

export function guardarNegocioActivo(id: string) {
  try {
    window.localStorage.setItem(CLAVE_DEL_ACTIVO, id);
  } catch {
    // Sin almacenamiento funciona igual: sólo no se acuerda del negocio al volver a entrar.
  }
}

/** El cuaderno que hace de negocio activo: el pedido si existe y, si no, el primero. */
export const negocioActivoDe = (sistemas: Sistema[], pedidoId: string | null) =>
  sistemas.find((sistema) => sistema.id === pedidoId) ?? sistemas[0];

const aNegocio = (sistema: Sistema, activoId: string | null): NegocioDeLaCuenta => ({
  id: sistema.id,
  nombre: sistema.name,
  activo: sistema.id === activoId,
  creadoEn: sistema.createdAt,
});

export function listaDeNegocios(
  sistemas: Sistema[],
  pedidoId: string | null,
  limite: number | null,
): ListaDeNegocios {
  const activoId = negocioActivoDe(sistemas, pedidoId)?.id ?? null;
  return {
    activoId,
    limite,
    puedeCrear: limite === null || sistemas.length < limite,
    negocios: sistemas.map((sistema) => aNegocio(sistema, activoId)),
  };
}

const resumenDe = (negocio: NegocioDeLaCuenta, panel: PanelDelNegocio): ResumenDeNegocio => {
  const metricas = panel.hayDatos ? panel.metricas : null;
  return {
    id: negocio.id,
    nombre: negocio.nombre,
    activo: negocio.activo,
    hayDatos: metricas !== null,
    vendiste: metricas?.vendiste ?? null,
    gastos: metricas?.gastos ?? null,
    queda: metricas?.queda ?? null,
    teDeben: metricas?.teDeben ?? null,
    avisosPendientes: null,
  };
};

const suma = (valores: Array<number | null>) =>
  valores.reduce<number>((total, valor) => total + (valor ?? 0), 0);

/** Junta el panel de cada negocio (en el mismo orden que la lista) en el resumen de toda la cuenta. */
export function resumenConjuntoDe(
  negocios: NegocioDeLaCuenta[],
  paneles: PanelDelNegocio[],
  dias = DIAS_DEL_RESUMEN_CONJUNTO,
): ResumenConjunto {
  const resumenes = negocios.flatMap((negocio, indice) => {
    const panel = paneles[indice];
    return panel ? [resumenDe(negocio, panel)] : [];
  });
  return {
    dias,
    total: {
      negocios: resumenes.length,
      vendiste: suma(resumenes.map((resumen) => resumen.vendiste)),
      gastos: suma(resumenes.map((resumen) => resumen.gastos)),
      queda: suma(resumenes.map((resumen) => resumen.queda)),
      teDeben: suma(resumenes.map((resumen) => resumen.teDeben)),
    },
    negocios: resumenes,
  };
}
