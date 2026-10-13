import { useState } from 'react';
import { Link, useParams, useSearch } from '@tanstack/react-router';
import { ChartBarIcon } from '@phosphor-icons/react';
import { FiltrosReporte } from '@/components/Reportes/FiltrosReporte';
import { ResultadoReporte } from '@/components/Reportes/ResultadoReporte';
import { EmptyState, PageHeading, Panel, SelectField, buttonClasses } from '@/components/ui';
import { useProyecto } from '@/hooks/proyectos/queries';
import type { FiltroReporte } from '@/types/reporte';

const RUTA = '/_app/proyectos/$proyectoId/reportes';

// Reportes (T-076): se elige la tabla y los filtros, se ven los totales y las primeras filas y se descarga el
// mismo reporte en XLSX.
export function ReportesPage() {
  const { proyectoId } = useParams({ from: RUTA });
  const { bloque: bloqueInicial } = useSearch({ from: RUTA });
  const proyecto = useProyecto(proyectoId);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const [bloqueId, setBloqueId] = useState(bloqueInicial);
  const [filtro, setFiltro] = useState<FiltroReporte>({});
  const bloque = bloques.find((item) => item.id === bloqueId) ?? bloques[0];

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Reportes"
        title={proyecto.nombre}
        lead="Elegí una tabla y los filtros, mirá los totales y descargá el reporte en Excel."
      />

      {!bloque ? (
        <EmptyState
          icon={<ChartBarIcon aria-hidden size={32} />}
          title="Este sistema no tiene tablas"
        >
          <p className="m-0">Armá primero sus tablas y cargá filas para poder hacer reportes.</p>
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
                label="Tabla del reporte"
                value={bloque.id}
                onChange={(event) => {
                  setBloqueId(event.target.value);
                  setFiltro({});
                }}
              >
                {bloques.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nombre}
                  </option>
                ))}
              </SelectField>
            </div>
          )}

          <Panel aria-labelledby="reporte-filtros" className="flex flex-col gap-4">
            <h2 id="reporte-filtros" className="m-0 text-h3">
              Filtros
            </h2>
            <FiltrosReporte
              key={bloque.id}
              bloque={bloque}
              aplicado={filtro}
              onAplicar={setFiltro}
            />
          </Panel>

          <section aria-labelledby="reporte-resultado" className="flex flex-col gap-4">
            <h2 id="reporte-resultado" className="m-0 text-h3">
              Reporte
            </h2>
            <ResultadoReporte key={bloque.id} bloque={bloque} filtro={filtro} />
          </section>
        </>
      )}
    </div>
  );
}
