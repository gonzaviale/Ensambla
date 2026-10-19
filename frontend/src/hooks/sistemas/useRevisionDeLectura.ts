import { useCallback, useState } from 'react';
import {
  iniciarRevision,
  reducirRevision,
  type AccionDeRevision,
  type LecturaParaRevisar,
  type Revision,
} from '@/lib/sistema/revision';

const SIN_REVISION: Revision = { filas: [], camposNuevos: [] };

// El borrador de la revisión de una lectura: se carga al leer la foto y cada corrección lo cambia.
export function useRevisionDeLectura() {
  const [revision, setRevision] = useState<Revision>(SIN_REVISION);

  const cargar = useCallback(
    (lectura: LecturaParaRevisar) => setRevision(iniciarRevision(lectura)),
    [],
  );
  const actuar = useCallback(
    (accion: AccionDeRevision) => setRevision((actual) => reducirRevision(actual, accion)),
    [],
  );

  return { revision, cargar, actuar };
}
