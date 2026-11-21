import type { AccionDelServidor, AvisoDelServidor } from '@/types/avisos';
import type { AccionDeAviso, Aviso, GrupoDeAviso, TipoDeAviso } from './alertas';

const PREFIJO = 'servidor:';
const SISTEMA_SIN_NOMBRE = 'Tu negocio';

type FabricaDeAccion = (accion: AccionDelServidor, avisoId: string) => AccionDeAviso;

const sistemaDe = (accion: AccionDelServidor) => accion.proyectoId ?? '';
const bloqueDe = (accion: AccionDelServidor) => accion.bloqueId ?? '';

const editarRegistro: FabricaDeAccion = (accion, avisoId) => ({
  tipo: 'servidor',
  texto: accion.texto ?? 'Aplicar el cambio',
  avisoId,
  confirmacion: 'Listo, aplicamos el cambio.',
  ...(accion.bloqueId
    ? { sistemaId: sistemaDe(accion), bloqueId: bloqueDe(accion), filaId: accion.registroId }
    : {}),
});

const crearRegistro: FabricaDeAccion = (accion, avisoId) => ({
  tipo: 'servidor',
  texto: accion.texto ?? 'Crear la fila',
  avisoId,
  confirmacion: 'Listo, lo creamos.',
});

const verZona: FabricaDeAccion = (accion) => ({
  tipo: 'zona',
  texto: accion.texto ?? 'Ver en Mi zona',
  producto: accion.producto,
});

const registrar: FabricaDeAccion = (accion, avisoId) => ({
  tipo: 'registrar',
  texto: accion.texto ?? 'Cargar entrada',
  sistemaId: sistemaDe(accion),
  bloqueId: bloqueDe(accion),
  avisoId,
});

const abrir: FabricaDeAccion = (accion) => ({
  tipo: 'abrir',
  texto: accion.texto ?? 'Ver producto',
  sistemaId: sistemaDe(accion),
  bloqueId: bloqueDe(accion),
  filaId: accion.registroId,
});

// El aviso del servidor llega con ids del backend (proyecto, tabla, registro): el proyecto es el «sistema» de
// la pantalla. Aplicar un cambio de datos lo hace el servidor; registrar y abrir sólo llevan a la tabla.
const FABRICAS_DE_ACCION: Record<AccionDelServidor['tipo'], FabricaDeAccion> = {
  EDITAR_REGISTRO: editarRegistro,
  CREAR_REGISTRO: crearRegistro,
  VER_ZONA: verZona,
  REGISTRAR: registrar,
  ABRIR: abrir,
};

const aAccion: FabricaDeAccion = (accion, avisoId) =>
  FABRICAS_DE_ACCION[accion.tipo](accion, avisoId);

/** El aviso del servidor con la forma que ya saben mostrar el panel, la campana y las tarjetas. */
export function aAvisoDeSami(
  aviso: AvisoDelServidor,
  nombresDeSistemas: ReadonlyMap<string, string>,
): Aviso {
  return {
    id: `${PREFIJO}${aviso.id}`,
    servidorId: aviso.id,
    tipo: aviso.tipo.toLowerCase() as TipoDeAviso,
    grupo: aviso.grupo.toLowerCase() as GrupoDeAviso,
    prioridad: aviso.prioridad as Aviso['prioridad'],
    sistemaId: aviso.proyectoId ?? '',
    sistema: nombresDeSistemas.get(aviso.proyectoId ?? '') ?? SISTEMA_SIN_NOMBRE,
    titulo: aviso.titulo,
    detalle: aviso.detalle,
    porque: aviso.motivos,
    acciones: aviso.acciones.map((accion) => aAccion(accion, aviso.id)),
    datos: aviso.datos,
    creadoEn: aviso.creadoEn,
  };
}

/** Tipos de aviso que genera el motor del servidor. Con el backend real, el navegador no los calcula por su cuenta. */
export const TIPOS_DEL_MOTOR: ReadonlySet<TipoDeAviso> = new Set<TipoDeAviso>([
  'stock',
  'estancado',
  'precio_zona',
  'muy_vendido',
  'revisar_precio',
  'combo',
  'consumo_zona',
]);

/**
 * Tipos de aviso cuya copia del navegador no se muestra. Siempre los que el servidor ya trajo; y, si su motor está
 * activo y respondió, también todos los que él genera aunque hoy no tenga ninguno pendiente: un aviso aplicado o
 * descartado deja de estar en la lista, y sin esto la copia del navegador lo volvería a mostrar.
 */
export function tiposCubiertosPorElServidor(
  delServidor: readonly Aviso[],
  motorActivo: boolean,
): ReadonlySet<TipoDeAviso> {
  const tipos = new Set<TipoDeAviso>(delServidor.map((aviso) => aviso.tipo));
  if (motorActivo) TIPOS_DEL_MOTOR.forEach((tipo) => tipos.add(tipo));
  return tipos;
}
