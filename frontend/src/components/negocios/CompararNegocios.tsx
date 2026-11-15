import { useState } from 'react';
import {
  METRICAS,
  ORDEN_INICIAL,
  ordenar,
  siguienteOrden,
  valorDeMetrica,
  type Orden,
} from '@/lib/negocios/comparar';
import { rubroLegible } from '@/lib/negocios/presentacion';
import { pesos } from '@/lib/sistema/panelDelNegocio';
import type { ResumenConjunto } from '@/types/negocios';

type Props = {
  resumen: ResumenConjunto;
  /** El nombre de cada rubro según el servidor; sin él se muestra el código legible. */
  nombreDelRubro?: (codigo: string) => string | undefined;
};

const SENTIDO_ARIA = { desc: 'descending', asc: 'ascending' } as const;

// Los totales de la cuenta y una tabla con un negocio por fila (T-205). Tocar el encabezado de una métrica ordena
// por ella; lo que no tiene valor queda al final. No sabe de consultas: recibe el resumen ya cargado.
export function CompararNegocios({ resumen, nombreDelRubro }: Props) {
  const [orden, setOrden] = useState<Orden>(ORDEN_INICIAL);
  const { total } = resumen;
  const filas = ordenar(resumen.negocios, orden);

  return (
    <>
      <section className="comparar-totales" aria-labelledby="comparar-totales-titulo">
        <div className="comparar-totales-cabecera">
          <h2 id="comparar-totales-titulo">Toda la cuenta</h2>
          <span>{total.negocios === 1 ? '1 negocio' : `${total.negocios} negocios`}</span>
        </div>
        <dl>
          <div>
            <dt>Vendiste</dt>
            <dd>{pesos(total.vendiste)}</dd>
          </div>
          <div>
            <dt>Gastos</dt>
            <dd>{pesos(total.gastos)}</dd>
          </div>
          <div>
            <dt>Te queda</dt>
            <dd data-negativo={total.queda < 0 || undefined}>{pesos(total.queda)}</dd>
          </div>
          <div>
            <dt>Te deben</dt>
            <dd>{pesos(total.teDeben)}</dd>
          </div>
          <div>
            <dt>Avisos pendientes</dt>
            <dd data-hay={total.avisosPendientes > 0 || undefined}>{total.avisosPendientes}</dd>
          </div>
        </dl>
      </section>

      <section className="comparar-tabla" aria-labelledby="comparar-tabla-titulo">
        <h2 id="comparar-tabla-titulo">Negocio por negocio</h2>
        <p id="comparar-tabla-ayuda" className="comparar-ayuda">
          Tocá una columna para ordenar por ella.
        </p>
        <table aria-labelledby="comparar-tabla-titulo" aria-describedby="comparar-tabla-ayuda">
          <thead>
            <tr>
              <th scope="col">Negocio</th>
              {METRICAS.map(({ clave, titulo }) => {
                const activa = orden.metrica === clave;
                return (
                  <th
                    key={clave}
                    scope="col"
                    aria-sort={activa ? SENTIDO_ARIA[orden.sentido] : undefined}
                  >
                    <button type="button" onClick={() => setOrden(siguienteOrden(orden, clave))}>
                      {titulo}
                      <span className="comparar-flecha" aria-hidden="true">
                        {activa ? (orden.sentido === 'desc' ? '▼' : '▲') : ''}
                      </span>
                    </button>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {filas.map((negocio) => (
              <tr key={negocio.id} data-sin-datos={!negocio.hayDatos || undefined}>
                <th scope="row">
                  <span className="comparar-nombre">{negocio.nombre}</span>
                  <span className="comparar-detalle">
                    {nombreDelRubro?.(negocio.rubro) ?? rubroLegible(negocio.rubro)}
                    {!negocio.hayDatos && ' · Sin ventas en el período'}
                  </span>
                </th>
                {METRICAS.map(({ clave, titulo }) => (
                  <td
                    key={clave}
                    data-titulo={titulo}
                    data-negativo={
                      (clave === 'queda' && negocio.queda !== null && negocio.queda < 0) ||
                      undefined
                    }
                    data-hay={
                      (clave === 'avisosPendientes' && negocio.avisosPendientes > 0) || undefined
                    }
                  >
                    {valorDeMetrica(negocio, clave)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </>
  );
}
