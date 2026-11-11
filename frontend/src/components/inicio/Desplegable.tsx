import type { ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';

type Props = {
  titulo: string;
  /** Lo principal de la sección, escrito en el encabezado para leerlo sin abrirla. */
  resumen: string;
  children: ReactNode;
};

// Una sección de la home que arranca cerrada: el encabezado dice qué es y su dato clave, y al abrirla aparece el
// detalle. Es un <details> nativo, así que se maneja con el teclado y los lectores de pantalla la anuncian sola.
export function Desplegable({ titulo, resumen, children }: Props) {
  return (
    <details className="panel-desplegable">
      <summary>
        <h3>{titulo}</h3>
        <span className="panel-desplegable-resumen">{resumen}</span>
        <Icon name="down" />
      </summary>
      <div className="panel-desplegable-cuerpo">{children}</div>
    </details>
  );
}
