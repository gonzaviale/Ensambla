import { hoy } from '@/lib/sistema/formato';
import type {
  ListaDeNegocios,
  NegocioDeLaCuenta,
  ResumenConjunto,
  ResumenDeNegocio,
} from '@/types/negocios';
import { estado, sistemasDe, type UsuarioMock } from './estado';
import { panelDe } from './panel';
import { planesDe } from './planes';

// Los negocios de cada cuenta. Los sistemas, los avisos y el resto de los datos siguen colgando de la cuenta y
// se atribuyen al primer negocio (el que se crea al registrarse); los que se agregan después arrancan vacíos.
// Cuando cada pantalla trabaje con el negocio activo, los datos pasarán a colgar de cada negocio.

export type NegocioMock = Omit<NegocioDeLaCuenta, 'activo'>;

export type NegociosDeLaCuenta = { activoId: string; negocios: NegocioMock[] };

/** Lo que se muestra de la cuenta: el negocio con que se registró, hasta que se agregue otro. */
function negocioInicial(usuario: UsuarioMock): NegocioMock {
  const configuracion = estado.negocios.get(usuario.email);
  return {
    id: crypto.randomUUID(),
    nombre: configuracion?.nombre ?? `Negocio de ${usuario.nombre}`,
    rubro: configuracion?.rubro ?? 'OTRO',
    creadoEn: new Date().toISOString(),
  };
}

/** Los negocios de la cuenta; la primera vez que se piden aparece el negocio con que se registró. */
export function negociosDe(usuario: UsuarioMock): NegociosDeLaCuenta {
  const existentes = estado.negociosDeLaCuenta.get(usuario.email);
  if (existentes) return existentes;
  const inicial = negocioInicial(usuario);
  const cuenta = { activoId: inicial.id, negocios: [inicial] };
  estado.negociosDeLaCuenta.set(usuario.email, cuenta);
  return cuenta;
}

export function guardarNegociosDe(email: string, cuenta: NegociosDeLaCuenta) {
  estado.negociosDeLaCuenta.set(email, cuenta);
  estado.alCambiar?.();
}

/** Cuántos negocios permite el plan vigente de la cuenta (Gratuito 1; Premium el propio y 5 adicionales). */
export function limiteDeNegociosDe(email: string) {
  const { planes, suscripcion } = planesDe(email);
  const actual = planes.find((plan) => plan.codigo === suscripcion.codigoPlanActual);
  return actual?.limiteNegocios ?? 1;
}

export const aNegocioDeLaCuenta = (negocio: NegocioMock, activoId: string): NegocioDeLaCuenta => ({
  ...negocio,
  activo: negocio.id === activoId,
});

export function listaDeNegociosDe(usuario: UsuarioMock): ListaDeNegocios {
  const { activoId, negocios } = negociosDe(usuario);
  const limite = limiteDeNegociosDe(usuario.email);
  return {
    activoId,
    limite,
    puedeCrear: negocios.length < limite,
    negocios: negocios.map((negocio) => aNegocioDeLaCuenta(negocio, activoId)),
  };
}

const suma = (valores: Array<number | null | undefined>) =>
  valores.reduce<number>((total, valor) => total + (valor ?? 0), 0);

/** Lo mismo que el panel de un solo negocio, sumado sobre todos los sistemas del negocio. */
function resumenDelPrimerNegocio(
  usuario: UsuarioMock,
  negocio: NegocioMock,
  activoId: string,
  dias: number,
): ResumenDeNegocio {
  const paneles = sistemasDe(usuario.email)
    .map((sistema) => panelDe(sistema, hoy(), dias))
    .filter((panel) => panel.hayDatos && panel.metricas);
  const avisosPendientes = (estado.avisosDelServidor.get(usuario.email) ?? []).filter(
    (aviso) => aviso.estado === 'PENDIENTE',
  ).length;
  const metricas = paneles.map((panel) => panel.metricas);
  return {
    ...aNegocioDeLaCuenta(negocio, activoId),
    hayDatos: paneles.length > 0,
    vendiste: paneles.length > 0 ? suma(metricas.map((dato) => dato?.vendiste)) : null,
    gastos: paneles.length > 0 ? suma(metricas.map((dato) => dato?.gastos)) : null,
    queda: paneles.length > 0 ? suma(metricas.map((dato) => dato?.queda)) : null,
    teDeben: paneles.length > 0 ? suma(metricas.map((dato) => dato?.teDeben)) : null,
    avisosPendientes,
  };
}

const sinDatos = (negocio: NegocioMock, activoId: string): ResumenDeNegocio => ({
  ...aNegocioDeLaCuenta(negocio, activoId),
  hayDatos: false,
  vendiste: null,
  gastos: null,
  queda: null,
  teDeben: null,
  avisosPendientes: 0,
});

const DIA_MS = 86_400_000;

export function resumenConjuntoDe(usuario: UsuarioMock, dias: number): ResumenConjunto {
  const { activoId, negocios } = negociosDe(usuario);
  const resumenes = negocios.map((negocio, indice) =>
    indice === 0
      ? resumenDelPrimerNegocio(usuario, negocio, activoId, dias)
      : sinDatos(negocio, activoId),
  );
  const fin = Date.parse(`${hoy()}T12:00:00Z`);
  return {
    desde: new Date(fin - (dias - 1) * DIA_MS).toISOString().slice(0, 10),
    hasta: new Date(fin).toISOString().slice(0, 10),
    dias,
    total: {
      negocios: resumenes.length,
      vendiste: suma(resumenes.map((resumen) => resumen.vendiste)),
      gastos: suma(resumenes.map((resumen) => resumen.gastos)),
      queda: suma(resumenes.map((resumen) => resumen.queda)),
      teDeben: suma(resumenes.map((resumen) => resumen.teDeben)),
      avisosPendientes: suma(resumenes.map((resumen) => resumen.avisosPendientes)),
    },
    negocios: resumenes,
  };
}
