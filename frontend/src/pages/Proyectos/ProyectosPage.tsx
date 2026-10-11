import { useMemo, useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CameraIcon, MagnifyingGlassIcon, PlusIcon, SquaresFourIcon } from '@phosphor-icons/react';
import {
  Button,
  InputField,
  Isotipo,
  PageHeading,
  Panel,
  Sheet,
  buttonClasses,
} from '@/components/ui';
import { CatalogoPlantillas } from '@/components/Proyectos/CatalogoPlantillas';
import { CrearProyectoForm } from '@/components/Proyectos/CrearProyectoForm';
import { ProyectoCard } from '@/components/Proyectos/ProyectoCard';
import { useEstructuras, useProyectos } from '@/hooks/proyectos/queries';
import type { Proyecto } from '@/types/proyecto';

// Con más sistemas que esto aparece el buscador.
const UMBRAL_BUSCADOR = 6;

// Quien vuelve ve primero sus sistemas; la bienvenida para empezar sólo encabeza la pantalla
// mientras la persona todavía no tiene ninguno. La acción principal (subir la foto del cuaderno,
// T-015) está siempre arriba y es la única en naranja.
export function ProyectosPage() {
  const proyectos = useProyectos();
  const [creando, setCreando] = useState(false);

  return (
    <div className="flex flex-col gap-10">
      {proyectos.length === 0 ? (
        <>
          <PageHeading
            kicker="Mis sistemas"
            title="¿Por dónde querés empezar?"
            lead="Pasá lo que tenés en el cuaderno a un sistema para tu negocio."
          />
          <section
            aria-labelledby="cuaderno-titulo"
            className="flex flex-col items-start gap-5 rounded-lg border border-accent-200 bg-accent-100 p-6 sm:flex-row sm:items-center sm:p-8"
          >
            <Isotipo className="size-20 shrink-0" />
            <div className="flex min-w-0 flex-col items-start gap-3">
              <h2 id="cuaderno-titulo" className="m-0 text-h2">
                Tu cuaderno, más organizado
              </h2>
              <p className="m-0 max-w-xl text-lead text-soft">
                Una foto es el primer paso para ordenar tus productos, pedidos o clientes.
              </p>
              <Link to="/cuaderno" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
                <CameraIcon size={24} aria-hidden />
                Subir foto de mi cuaderno
              </Link>
            </div>
          </section>
          <section aria-label="Otras formas de empezar" className="flex flex-col gap-4">
            <Panel flat>
              <p className="m-0 text-body text-soft">
                Todavía no tenés sistemas. También podés empezar con una plantilla o desde cero.
              </p>
            </Panel>
            <FormasDeEmpezar onEmpezarEnBlanco={() => setCreando(true)} />
          </section>
        </>
      ) : (
        <>
          <PageHeading
            kicker="Inicio"
            title="Mis sistemas"
            lead="Elegí un sistema para seguir cargando tus datos, o armá uno nuevo."
            actions={
              <Link to="/cuaderno" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
                <CameraIcon size={24} aria-hidden />
                Subir foto de mi cuaderno
              </Link>
            }
          />
          <ListaProyectos proyectos={proyectos} />
          <section aria-labelledby="nuevo-titulo" className="flex flex-col gap-4">
            <h2 id="nuevo-titulo" className="m-0 text-h3">
              Empezar otro sistema
            </h2>
            <FormasDeEmpezar onEmpezarEnBlanco={() => setCreando(true)} />
          </section>
        </>
      )}
      <CatalogoPlantillas />
      <Sheet
        open={creando}
        onClose={() => setCreando(false)}
        title="Tu sistema en blanco"
        description="Dale un nombre y después armás sus tablas."
      >
        <CrearProyectoForm onCancelar={() => setCreando(false)} />
      </Sheet>
    </div>
  );
}

function ListaProyectos({ proyectos }: { proyectos: Proyecto[] }) {
  const [busqueda, setBusqueda] = useState('');
  const estructuras = useEstructuras(proyectos.map((proyecto) => proyecto.id));
  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return texto
      ? proyectos.filter((proyecto) => proyecto.nombre.toLowerCase().includes(texto))
      : proyectos;
  }, [proyectos, busqueda]);

  return (
    <section aria-labelledby="lista-sistemas" className="flex flex-col gap-4">
      <h2 id="lista-sistemas" className="sr-only">
        Tus sistemas
      </h2>
      {proyectos.length > UMBRAL_BUSCADOR && (
        <div className="max-w-md">
          <InputField
            label="Buscar un sistema"
            type="search"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Escribí parte del nombre"
          />
        </div>
      )}
      {visibles.length === 0 ? (
        <Panel flat className="flex items-center gap-3">
          <MagnifyingGlassIcon aria-hidden size={24} />
          <p className="m-0">Ningún sistema se llama así. Probá con otra palabra.</p>
        </Panel>
      ) : (
        <ul className="m-0 grid list-none grid-cols-[repeat(auto-fill,minmax(min(100%,18rem),1fr))] gap-4 p-0">
          {visibles.map((proyecto) => {
            const estructura = estructuras[proyecto.id];
            return (
              <li key={proyecto.id} className="min-w-0">
                <ProyectoCard
                  proyecto={proyecto}
                  bloques={estructura?.bloques}
                  cargando={estructura?.cargando ?? true}
                />
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

// Plantilla o en blanco: apoyos de la acción principal, siempre en contorno.
function FormasDeEmpezar({ onEmpezarEnBlanco }: { onEmpezarEnBlanco: () => void }) {
  return (
    <div className="flex flex-wrap gap-3" aria-label="Formas de empezar">
      <a href="#plantillas" className={buttonClasses()}>
        <SquaresFourIcon size={22} aria-hidden />
        Usar plantilla
      </a>
      <Button onClick={onEmpezarEnBlanco}>
        <PlusIcon size={22} aria-hidden />
        Empezar en blanco
      </Button>
    </div>
  );
}
