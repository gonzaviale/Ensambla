import { useState, type ComponentType } from 'react';
import {
  HandCoinsIcon,
  ReceiptIcon,
  ShoppingBagOpenIcon,
  WalletIcon,
  type IconProps,
} from '@phosphor-icons/react';
import { InicialDelNegocio } from '@/components/negocios/InicialDelNegocio';
import {
  METRICAS,
  ORDEN_INICIAL,
  ordenar,
  siguienteOrden,
  valorDeMetrica,
  type MetricaComparable,
  type Orden,
} from '@/lib/negocios/comparar';
import { pesos } from '@/lib/sistema/panelDelNegocio';
import { tonoDe, type ConceptoDeColor } from '@/lib/sistema/tonos';
import type { ResumenConjunto } from '@/types/negocios';

type Props = {
  resumen: ResumenConjunto;
  /** Los ids en el orden de la cuenta, para que cada negocio tenga el mismo color que en el menú. */
  ordenDeLaCuenta?: string[];
};

type TotalDeLaCuenta = {
  clave: MetricaComparable;
  titulo: string;
  concepto: ConceptoDeColor;
  Icono: ComponentType<IconProps>;
};

const TOTALES: ReadonlyArray<TotalDeLaCuenta> = [
  { clave: 'vendiste', titulo: 'Vendiste', concepto: 'ventas', Icono: ShoppingBagOpenIcon },
  { clave: 'gastos', titulo: 'Gastaste', concepto: 'gastos', Icono: ReceiptIcon },
  { clave: 'queda', titulo: 'Te queda', concepto: 'ganancia', Icono: WalletIcon },
  { clave: 'teDeben', titulo: 'Te deben', concepto: 'fiado', Icono: HandCoinsIcon },
];

function TarjetaDeTotal({
  total,
  resumen,
}: {
  total: TotalDeLaCuenta;
  resumen: ResumenConjunto['total'];
}) {
  const { clave, titulo, concepto, Icono } = total;
  const quedaEnRojo = clave === 'queda' && resumen.queda < 0;
  return (
    <div className="comparar-total solido" data-tono={tonoDe(concepto)}>
      <Icono aria-hidden size={26} weight="regular" className="comparar-total-icono" />
      <dt>{titulo}</dt>
      <dd data-negativo={quedaEnRojo || undefined}>{pesos(resumen[clave])}</dd>
    </div>
  );
}

const SENTIDO_ARIA = { desc: 'descending', asc: 'ascending' } as const;

// Los totales de la cuenta y una tabla con un negocio por fila (T-205). Tocar el encabezado de una métrica ordena
// por ella; lo que no tiene valor queda al final. No sabe de consultas: recibe el resumen ya cargado.
export function CompararNegocios({ resumen, ordenDeLaCuenta }: Props) {
  const [orden, setOrden] = useState<Orden>(ORDEN_INICIAL);
  const { total } = resumen;
  const filas = ordenar(resumen.negocios, orden);
  const posicionDe = (id: string) => {
    const enLaCuenta = ordenDeLaCuenta?.indexOf(id) ?? -1;
    return enLaCuenta >= 0 ? enLaCuenta : resumen.negocios.findIndex((n) => n.id === id);
  };
  // La barrita de cada fila: qué parte de lo que vendió la cuenta vendió ese negocio.
  const parteDeLasVentas = (vendiste: number | null) =>
    vendiste === null || total.vendiste <= 0
      ? 0
      : Math.round((Math.max(vendiste, 0) / total.vendiste) * 100);

  return (
    <>
      <section className="comparar-totales" aria-labelledby="comparar-totales-titulo">
        <div className="comparar-totales-cabecera">
          <h2 id="comparar-totales-titulo">Todos tus negocios juntos</h2>
          <span>{total.negocios === 1 ? '1 negocio' : `${total.negocios} negocios`}</span>
        </div>
        <dl>
          {TOTALES.map((t) => (
            <TarjetaDeTotal key={t.clave} total={t} resumen={total} />
          ))}
        </dl>
      </section>

      <section className="comparar-tabla" aria-labelledby="comparar-tabla-titulo">
        <h2 id="comparar-tabla-titulo">Negocio por negocio</h2>
        <p id="comparar-tabla-ayuda" className="comparar-ayuda">
          Tocá un título para ordenar por eso.
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
                  <span className="comparar-negocio">
                    <InicialDelNegocio
                      nombre={negocio.nombre}
                      posicion={posicionDe(negocio.id)}
                      className="negocio-selector-inicial"
                    />
                    <span>
                      <span className="comparar-nombre">{negocio.nombre}</span>
                      {!negocio.hayDatos && (
                        <span className="comparar-detalle">Sin ventas en estos 30 días</span>
                      )}
                    </span>
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
                  >
                    {valorDeMetrica(negocio, clave)}
                    {clave === 'vendiste' && negocio.hayDatos && (
                      <span className="comparar-parte" aria-hidden="true">
                        <span style={{ width: `${parteDeLasVentas(negocio.vendiste)}%` }} />
                      </span>
                    )}
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
