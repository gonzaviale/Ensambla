import { useState } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { TableIcon } from '@phosphor-icons/react';
import { FichaRegistro } from '@/components/Registros/FichaRegistro';
import { TablaRegistros } from '@/components/Registros/TablaRegistros';
import { EmptyState, PageHeading, buttonClasses } from '@/components/ui';
import { useProyecto } from '@/hooks/proyectos/queries';
import { cn } from '@/lib/cn';
import type { Registro } from '@/types/registro';

// T-028 a T-030: tabla de registros por bloque con búsqueda, orden y ficha. T-031 suma la carga de datos.
export function UsarPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/usar' });
  const proyecto = useProyecto(proyectoId);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const [bloqueId, setBloqueId] = useState<string>();
  const [seleccionado, setSeleccionado] = useState<Registro>();
  const bloque = bloques.find((item) => item.id === bloqueId) ?? bloques[0];

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo usar"
        title={proyecto.nombre}
        lead="Buscá, ordená y revisá los datos de cada bloque."
      />

      {!bloque ? (
        <EmptyState
          icon={<TableIcon aria-hidden size={32} />}
          title="Este proyecto no tiene bloques"
        >
          <p>Armá primero su estructura en el modo construir.</p>
          <Link
            to="/proyectos/$proyectoId/construir"
            params={{ proyectoId }}
            className={buttonClasses({ variant: 'primary', className: 'mt-3' })}
          >
            Ir al constructor
          </Link>
        </EmptyState>
      ) : (
        <>
          <nav aria-label="Bloques del proyecto" className="flex flex-wrap gap-2">
            {bloques.map((item) => (
              <button
                key={item.id}
                type="button"
                aria-pressed={item.id === bloque.id}
                onClick={() => {
                  setBloqueId(item.id);
                  setSeleccionado(undefined);
                }}
                className={cn(
                  buttonClasses({ size: 'sm' }),
                  item.id === bloque.id && 'border-accent bg-accent/10',
                )}
              >
                {item.nombre}
              </button>
            ))}
          </nav>

          <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
            <TablaRegistros
              key={bloque.id}
              bloque={bloque}
              seleccionadoId={seleccionado?.id}
              onSeleccionar={setSeleccionado}
            />
            {seleccionado?.bloqueId === bloque.id && (
              <FichaRegistro
                bloque={bloque}
                registro={seleccionado}
                onCerrar={() => setSeleccionado(undefined)}
              />
            )}
          </div>
        </>
      )}
    </div>
  );
}
