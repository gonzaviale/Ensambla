import type { AccionDelServidor, AvisoDelServidor } from '@/types/avisos';
import type { AccionDeAviso, Aviso, GrupoDeAviso, TipoDeAviso } from './alertas';

const PREFIJO = 'servidor:';
const SISTEMA_SIN_NOMBRE = 'Tu negocio';

// El aviso del servidor llega con ids del backend (proyecto, tabla, registro): el proyecto es el «sistema» de
// la pantalla. Aplicar un cambio de datos (editar un precio, crear un combo) lo hace el servidor; registrar y abrir sólo llevan a la tabla.
function aAccion(accion: AccionDelServidor, avisoId: string): AccionDeAviso {
  const sistemaId = accion.proyectoId ?? '';
  const bloqueId = accion.bloqueId ?? '';
  switch (accion.tipo) {
    case 'EDITAR_REGISTRO':
      return {
        tipo: 'servidor',
        texto: accion.texto ?? 'Aplicar el cambio',
        avisoId,
        confirmacion: 'Listo, aplicamos el cambio.',
      };
    case 'CREAR_REGISTRO':
      return {
        tipo: 'servidor',
        texto: accion.texto ?? 'Crear el combo',
        avisoId,
        confirmacion: 'Listo, creamos el combo.',
      };
    case 'REGISTRAR':
      return { tipo: 'registrar', texto: accion.texto ?? 'Registrar entrada', sistemaId, bloqueId };
    case 'ABRIR':
      return {
        tipo: 'abrir',
        texto: accion.texto ?? 'Ver producto',
        sistemaId,
        bloqueId,
        filaId: accion.registroId,
      };
  }
}

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
  };
}
