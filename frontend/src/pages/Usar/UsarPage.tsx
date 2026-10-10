import { useState } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { PageHeading, Panel, buttonClasses } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import { FormularioRegistro } from '@/components/FormRenderer/FormularioRegistro';
import { useProyecto } from '@/hooks/proyectos/queries';

export function UsarPage() {
  const { proyectoId } = useParams({ from: '/_app/proyectos/$proyectoId/usar' });
  const proyecto = useProyecto(proyectoId);
  const [seleccion, setSeleccion] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const bloqueActivo = bloques.find((bloque) => bloque.id === seleccion) ?? bloques[0];

  return (
    <div className="flex flex-col gap-6">
      <Link
        to="/proyectos/$proyectoId/construir"
        params={{ proyectoId }}
        className={buttonClasses({ variant: 'ghost', className: 'self-start' })}
      >
        ← Volver al proyecto
      </Link>
      <PageHeading
        kicker="Modo usar"
        title={proyecto.nombre}
        lead="Elegí un bloque y completá sus datos para guardar un nuevo registro."
      />
      {!bloqueActivo ? (
        <Panel className="max-w-2xl">
          <h2 className="mt-0 text-xl">Todavía no hay bloques</h2>
          <p className="mb-0 text-soft">
            Este proyecto necesita un bloque con campos para cargar datos. Podés empezar con una
            plantilla desde Mis proyectos.
          </p>
          <Link
            to="/proyectos"
            className={buttonClasses({ variant: 'secondary', className: 'mt-4' })}
          >
            Ir a mis proyectos
          </Link>
        </Panel>
      ) : (
        <div className="grid items-start gap-6 lg:grid-cols-[16rem_minmax(0,36rem)]">
          <Panel>
            <h2 className="mt-0 text-lg">¿Dónde querés cargar?</h2>
            <SelectField
              label="Bloque"
              value={bloqueActivo.id}
              disabled={enviando}
              onChange={(event) => setSeleccion(event.target.value)}
            >
              {bloques.map((bloque) => (
                <option key={bloque.id} value={bloque.id}>
                  {bloque.nombre}
                </option>
              ))}
            </SelectField>
            <p className="mb-0 text-sm text-soft">Cada bloque tiene su propio formulario.</p>
          </Panel>
          <div>
            {bloques.map((bloque) => (
              <Panel
                key={bloque.id}
                hidden={bloque.id !== bloqueActivo.id}
                aria-labelledby={`bloque-${bloque.id}`}
              >
                <p className="mt-0 mb-1 text-xs tracking-wide text-accent-800 uppercase">
                  Nuevo registro
                </p>
                <h2 id={`bloque-${bloque.id}`} className="mt-0 mb-5 text-xl">
                  {bloque.nombre}
                </h2>
                <FormularioRegistro bloque={bloque} onEnviandoChange={setEnviando} />
              </Panel>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
