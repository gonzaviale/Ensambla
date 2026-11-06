import type { CSSProperties, ReactNode } from 'react';

export type ColumnaDeTabla = { titulo: string };

export type FilaDeTabla = {
  clave: string;
  /** Una celda por columna; la primera es el nombre. */
  celdas: ReactNode[];
  /** De 0 a 1: el largo de la barrita bajo el nombre, para comparar de un vistazo. Sin valor, no hay barra. */
  barra?: number;
};

// El detalle de cada tema, con las mismas columnas que el resumen en PDF: el nombre a la izquierda y los números
// a la derecha. La barrita bajo el nombre muestra qué fila pesa más sin tener que comparar cifras.
export function TablaDeDetalle({
  titulo,
  columnas,
  filas,
}: {
  titulo: string;
  columnas: ColumnaDeTabla[];
  filas: FilaDeTabla[];
}) {
  return (
    <div className="metricas-tabla-caja">
      <table className="metricas-tabla" aria-label={titulo}>
        <thead>
          <tr>
            {columnas.map((columna) => (
              <th key={columna.titulo} scope="col">
                {columna.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => (
            <tr key={fila.clave}>
              {fila.celdas.map((celda, columna) =>
                columna === 0 ? (
                  <th key={columna} scope="row">
                    <span className="metricas-tabla-nombre">{celda}</span>
                    {fila.barra !== undefined && (
                      <span
                        className="metricas-tabla-barra"
                        aria-hidden="true"
                        style={
                          {
                            '--ancho': `${Math.max(2, fila.barra * 100)}%`,
                            '--i': indice,
                          } as CSSProperties
                        }
                      />
                    )}
                  </th>
                ) : (
                  <td key={columna}>{celda}</td>
                ),
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
