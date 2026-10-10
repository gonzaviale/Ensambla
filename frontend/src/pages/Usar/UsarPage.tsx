import { useState } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { TableIcon } from '@phosphor-icons/react';
import { FormularioRegistro } from '@/components/FormRenderer/FormularioRegistro';
import { FichaRegistro } from '@/components/Registros/FichaRegistro';
import { TablaRegistros } from '@/components/Registros/TablaRegistros';
import { EmptyState, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import { useProyecto } from '@/hooks/proyectos/queries';
import { cn } from '@/lib/cn';
import type { Registro } from '@/types/registro';

type Vista = 'registros' | 'cargar';

const VISTAS: { id: Vista; etiqueta: string }[] = [
  { id: 'registros', etiqueta: 'Ver registros' },
  { id: 'cargar', etiqueta: 'Cargar' },
];

// Tabla de registros por bloque (búsqueda, orden y ficha) y formulario de carga (ENS-6).
export function UsarPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/usar' });
  const proyecto = useProyecto(proyectoId);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const [bloqueId, setBloqueId] = useState<string>();
  const [seleccionado, setSeleccionado] = useState<Registro>();
  const [vista, setVista] = useState<Vista>('registros');
  // Los formularios quedan montados una vez abiertos para no perder los borradores.
  const [cargaAbierta, setCargaAbierta] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const bloque = bloques.find((item) => item.id === bloqueId) ?? bloques[0];

  const elegirVista = (siguiente: Vista) => {
    if (siguiente === 'cargar') setCargaAbierta(true);
    setVista(siguiente);
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo usar"
        title={proyecto.nombre}
        lead="Cargá datos en cada bloque y buscá, ordená y revisá lo que ya cargaste."
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
          <div className="flex flex-wrap items-end gap-4">
            <div className="min-w-48">
              <SelectField
                label="Bloque"
                value={bloque.id}
                disabled={enviando}
                onChange={(event) => {
                  setBloqueId(event.target.value);
                  setSeleccionado(undefined);
                }}
              >
                {bloques.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nombre}
                  </option>
                ))}
              </SelectField>
            </div>
            <div role="tablist" aria-label="Vista del bloque" className="flex gap-2">
              {VISTAS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  role="tab"
                  id={`vista-${item.id}`}
                  aria-selected={vista === item.id}
                  aria-controls={`panel-${item.id}`}
                  disabled={enviando}
                  onClick={() => elegirVista(item.id)}
                  className={cn(
                    buttonClasses({ size: 'sm' }),
                    vista === item.id && 'border-accent bg-accent/10',
                  )}
                >
                  {item.etiqueta}
                </button>
              ))}
            </div>
          </div>

          <div
            role="tabpanel"
            id="panel-registros"
            aria-labelledby="vista-registros"
            hidden={vista !== 'registros'}
          >
            {vista === 'registros' && (
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
            )}
          </div>

          <div
            role="tabpanel"
            id="panel-cargar"
            aria-labelledby="vista-cargar"
            hidden={vista !== 'cargar'}
          >
            {cargaAbierta &&
              bloques.map((item) => (
                <Panel
                  key={item.id}
                  className="max-w-2xl"
                  hidden={item.id !== bloque.id}
                  aria-labelledby={`bloque-${item.id}`}
                >
                  <p className="mt-0 mb-1 text-xs tracking-wide text-accent-800 uppercase">
                    Nuevo registro
                  </p>
                  <h2 id={`bloque-${item.id}`} className="mt-0 mb-5 text-xl">
                    {item.nombre}
                  </h2>
                  <FormularioRegistro bloque={item} onEnviandoChange={setEnviando} />
                </Panel>
              ))}
          </div>
        </>
      )}
    </div>
  );
}
