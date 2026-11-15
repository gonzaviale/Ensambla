import { hoy } from '@/lib/sistema/formato';
import type {
  ListaDeNegocios,
  NegocioDeLaCuenta,
  ResumenConjunto,
  ResumenDeNegocio,
} from '@/types/negocios';
import type { ConfiguracionNegocio } from '@/types/negocio';
import { claveDeSistemas, estado, sistemasDe, type UsuarioMock } from './estado';
import { panelDe } from './panel';
import { planesDe } from './planes';

// Los negocios de cada cuenta. Cada negocio tiene su configuración (la del activo se refleja en `estado.negocios`) y
// sus secciones (ver `claveDeSistemas`): las pantallas trabajan con las del negocio activo y uno nuevo arranca vacío.
// Los avisos y los objetivos siguen colgando de la cuenta y se atribuyen al primer negocio (el que se crea al
// registrarse).

export type NegocioMock = Omit<NegocioDeLaCuenta, 'activo'> & {
  configuracion: ConfiguracionNegocio;
};

export type NegociosDeLaCuenta = { activoId: string; negocios: NegocioMock[] };

/** Lo que se muestra de la cuenta: el negocio con que se registró, hasta que se agregue otro. */
function negocioInicial(usuario: UsuarioMock): NegocioMock {
  const configuracion = estado.negocios.get(usuario.email);
  return {
    id: crypto.randomUUID(),
    nombre: configuracion?.nombre ?? `Negocio de ${usuario.nombre}`,
    rubro: configuracion?.rubro ?? 'OTRO',
    creadoEn: new Date().toISOString(),
    configuracion: configuracion ?? {
      nombre: `Negocio de ${usuario.nombre}`,
      rubro: 'OTRO',
      ubicacion: null,
      comparteDatosZona: true,
      zona: null,
    },
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
  id: negocio.id,
  nombre: negocio.nombre,
  rubro: negocio.rubro,
  creadoEn: negocio.creadoEn,
  activo: negocio.id === activoId,
});

export function reflejarNegocioActivo(email: string) {
  const cuenta = estado.negociosDeLaCuenta.get(email);
  const activo = cuenta?.negocios.find((negocio) => negocio.id === cuenta.activoId);
  if (activo) estado.negocios.set(email, structuredClone(activo.configuracion));
}

export function actualizarConfiguracionActiva(email: string, configuracion: ConfiguracionNegocio) {
  const cuenta = estado.negociosDeLaCuenta.get(email);
  const activo = cuenta?.negocios.find((negocio) => negocio.id === cuenta.activoId);
  if (activo) {
    activo.nombre = configuracion.nombre;
    activo.rubro = configuracion.rubro;
    activo.configuracion = structuredClone(configuracion);
  }
}

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

/** Lo mismo que el panel de un solo negocio, sumado sobre todas las secciones del negocio. */
function resumenDelNegocio(
  usuario: UsuarioMock,
  negocio: NegocioMock,
  activoId: string,
  dias: number,
  esElPrimero: boolean,
): ResumenDeNegocio {
  const paneles = sistemasDe(usuario.email, negocio.id)
    .map((sistema) => panelDe(sistema, hoy(), dias))
    .filter((panel) => panel.hayDatos && panel.metricas);
  const avisosPendientes = esElPrimero
    ? (estado.avisosDelServidor.get(usuario.email) ?? []).filter(
        (aviso) => aviso.estado === 'PENDIENTE',
      ).length
    : 0;
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

const DIA_MS = 86_400_000;

export function resumenConjuntoDe(usuario: UsuarioMock, dias: number): ResumenConjunto {
  const { activoId, negocios } = negociosDe(usuario);
  const resumenes = negocios.map((negocio, indice) =>
    resumenDelNegocio(usuario, negocio, activoId, dias, indice === 0),
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

/**
 * Saca de la cuenta el negocio con sus secciones. Si era el primero, el que pasa a serlo se queda con la clave del
 * email (ver `claveDeSistemas`), así sus secciones no se pierden.
 */
export function eliminarNegocioDe(email: string, negocioId: string) {
  const cuenta = estado.negociosDeLaCuenta.get(email);
  if (!cuenta) return;
  const restantes = cuenta.negocios.filter((negocio) => negocio.id !== negocioId);
  const deCadaNegocio = restantes.map(
    (negocio) => [negocio.id, estado.sistemas.get(claveDeSistemas(email, negocio.id))] as const,
  );
  for (const negocio of cuenta.negocios) estado.sistemas.delete(claveDeSistemas(email, negocio.id));
  // Si se elimina el activo, pasa a serlo el más antiguo de los que quedan.
  const activoId =
    cuenta.activoId === negocioId ? (restantes[0]?.id ?? cuenta.activoId) : cuenta.activoId;
  estado.negociosDeLaCuenta.set(email, { activoId, negocios: restantes });
  for (const [id, sistemas] of deCadaNegocio) {
    if (sistemas) estado.sistemas.set(claveDeSistemas(email, id), sistemas);
  }
  reflejarNegocioActivo(email);
  estado.alCambiar?.();
}
