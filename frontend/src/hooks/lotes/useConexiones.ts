import { useCampos } from '@/hooks/bloques/queries';
import { useEstructuras, useProyectosDisponibles } from '@/hooks/proyectos/queries';
import type { ColumnaRevision, Destino } from '@/lib/revisionLectura';
import type { Campo } from '@/types/campo';

/** A qué tabla apunta una columna de conexión de la revisión, y cómo se llama esa tabla. */
export type Conexion = { campo: Campo; nombreTabla: string };

const TABLA_SIN_NOMBRE = 'la tabla conectada';

// Las columnas de conexión sólo existen al sumar las filas a una tabla que ya las tiene: el campo del
// bloque dice a qué tabla apuntan. Sin columnas de conexión no se pide nada.
export function useConexiones(
  columnas: ColumnaRevision[],
  destino: Destino,
): Record<string, Conexion> {
  const hayConexiones = columnas.some((columna) => columna.tipo === 'RELACION');
  const bloqueId = hayConexiones && destino.modo === 'bloque' ? destino.bloqueId : '';
  const campos = useCampos(bloqueId);
  const proyectos = useProyectosDisponibles();
  const estructuras = useEstructuras(hayConexiones ? (proyectos.data ?? []).map((p) => p.id) : []);
  const tablas = Object.values(estructuras).flatMap((estructura) => estructura.bloques ?? []);

  const conexiones: Record<string, Conexion> = {};
  for (const columna of columnas) {
    if (columna.tipo !== 'RELACION') continue;
    const campo = campos.data?.find((item) => item.id === columna.campoExistenteId);
    if (!campo?.bloqueRelacionadoId) continue;
    const tabla = tablas.find((item) => item.id === campo.bloqueRelacionadoId);
    conexiones[columna.clave] = { campo, nombreTabla: tabla?.nombre ?? TABLA_SIN_NOMBRE };
  }
  return conexiones;
}
