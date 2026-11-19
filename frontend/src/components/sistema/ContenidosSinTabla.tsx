import type { ContenidoDeBloqueProps } from './tiposDeContenido';
import { ResumenDeLaSeccion } from './ResumenDeLaSeccion';

export function ContenidoDeResumen({ sistema }: ContenidoDeBloqueProps) {
  return <ResumenDeLaSeccion sistema={sistema} />;
}

export function ContenidoDeNota({ bloque }: ContenidoDeBloqueProps) {
  return <div className="note-content">{bloque.content}</div>;
}
