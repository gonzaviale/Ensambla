import { useState } from 'react';
import { Link, useParams, useSearch } from '@tanstack/react-router';
import { PackageIcon } from '@phosphor-icons/react';
import { FormularioMovimiento } from '@/components/Stock/FormularioMovimiento';
import { ListaMovimientos } from '@/components/Stock/ListaMovimientos';
import { TablaStock } from '@/components/Stock/TablaStock';
import { EmptyState, PageHeading, Panel, SelectField, buttonClasses } from '@/components/ui';
import { useProyecto } from '@/hooks/proyectos/queries';

const RUTA = '/_app/proyectos/$proyectoId/stock';

// Inventario por movimientos (T-074): se elige la tabla que tiene los productos, se registran
// entradas y salidas y se ve cuánto queda de cada uno.
export function StockPage() {
  const { proyectoId } = useParams({ from: RUTA });
  const { bloque: bloqueInicial } = useSearch({ from: RUTA });
  const proyecto = useProyecto(proyectoId);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const [bloqueId, setBloqueId] = useState(bloqueInicial);
  const bloque = bloques.find((item) => item.id === bloqueId) ?? bloques[0];

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Stock"
        title={proyecto.nombre}
        lead="Registrá las entradas y las salidas de tus productos y mirá cuánto queda de cada uno."
      />

      {!bloque ? (
        <EmptyState
          icon={<PackageIcon aria-hidden size={32} />}
          title="Este sistema no tiene tablas"
        >
          <p className="m-0">Armá primero una tabla con tus productos para llevar su stock.</p>
          <Link
            to="/proyectos/$proyectoId/construir"
            params={{ proyectoId }}
            className={buttonClasses({ variant: 'solid', className: 'mt-3' })}
          >
            Armar mis tablas
          </Link>
        </EmptyState>
      ) : (
        <>
          {bloques.length > 1 && (
            <div className="w-full max-w-sm">
              <SelectField
                label="¿Qué tabla tiene tus productos?"
                hint="El stock se lleva fila por fila de la tabla que elijas."
                value={bloque.id}
                onChange={(event) => setBloqueId(event.target.value)}
              >
                {bloques.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nombre}
                  </option>
                ))}
              </SelectField>
            </div>
          )}

          <Panel aria-labelledby="stock-registrar" className="flex flex-col gap-4">
            <h2 id="stock-registrar" className="m-0 text-h3">
              Registrar un movimiento
            </h2>
            <FormularioMovimiento key={bloque.id} bloque={bloque} />
          </Panel>

          <section aria-labelledby="stock-actual" className="flex flex-col gap-4">
            <h2 id="stock-actual" className="m-0 text-h3">
              Stock actual
            </h2>
            <TablaStock key={bloque.id} bloque={bloque} />
          </section>

          <section aria-labelledby="stock-movimientos" className="flex flex-col gap-4">
            <h2 id="stock-movimientos" className="m-0 text-h3">
              Últimos movimientos
            </h2>
            <ListaMovimientos key={bloque.id} bloque={bloque} />
          </section>
        </>
      )}
    </div>
  );
}
