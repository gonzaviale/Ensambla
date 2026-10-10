import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CameraIcon, SquaresFourIcon, PlusIcon } from '@phosphor-icons/react';
import { Button, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { CatalogoPlantillas } from '@/components/Proyectos/CatalogoPlantillas';
import { CrearProyectoForm } from '@/components/Proyectos/CrearProyectoForm';
import { ProyectoCard } from '@/components/Proyectos/ProyectoCard';
import { useProyectos } from '@/hooks/proyectos/queries';

export function ProyectosPage() {
  const proyectos = useProyectos();
  const [creando, setCreando] = useState(false);
  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Mis proyectos"
        title="¿Por dónde querés empezar?"
        lead="Pasá lo que tenés en el cuaderno a un sistema para tu negocio."
      />
      <section
        aria-labelledby="cuaderno-titulo"
        className="rounded-lg border border-accent-200 bg-accent-100 p-6 sm:p-8"
      >
        <CameraIcon size={36} aria-hidden className="mb-4 text-accent-800" />
        <h2 id="cuaderno-titulo" className="m-0 text-2xl">
          Tu cuaderno, más organizado
        </h2>
        <p className="max-w-xl text-soft">
          Una foto será el primer paso para ordenar tus productos, pedidos o clientes.
        </p>
        <Link to="/cuaderno" className={buttonClasses({ variant: 'solid' })}>
          Subir foto de mi cuaderno
        </Link>
      </section>
      <div className="flex flex-wrap gap-3" aria-label="Otras formas de empezar">
        <a href="#plantillas" className={buttonClasses()}>
          <SquaresFourIcon size={20} aria-hidden />
          Usar plantilla
        </a>
        <Button onClick={() => setCreando(true)}>
          <PlusIcon size={20} aria-hidden />
          Empezar en blanco
        </Button>
      </div>
      {creando && (
        <Panel>
          <CrearProyectoForm onCancelar={() => setCreando(false)} />
        </Panel>
      )}
      <section aria-labelledby="proyectos-titulo" className="flex flex-col gap-4">
        <h2 id="proyectos-titulo" className="m-0 text-xl">
          Tus proyectos
        </h2>
        {proyectos.length === 0 ? (
          <Panel>
            <p className="m-0 text-soft">
              Todavía no tenés proyectos. Elegí una de las opciones de arriba para empezar.
            </p>
          </Panel>
        ) : (
          <ul className="m-0 grid list-none gap-4 p-0 sm:grid-cols-2 lg:grid-cols-3">
            {proyectos.map((proyecto) => (
              <li key={proyecto.id}>
                <ProyectoCard proyecto={proyecto} />
              </li>
            ))}
          </ul>
        )}
      </section>
      <CatalogoPlantillas />
    </div>
  );
}
