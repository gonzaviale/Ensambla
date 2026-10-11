import { useState } from 'react';
import { CameraIcon, SquaresFourIcon, PlusIcon } from '@phosphor-icons/react';
import { Button, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { CatalogoPlantillas } from '@/components/Proyectos/CatalogoPlantillas';
import { CrearProyectoForm } from '@/components/Proyectos/CrearProyectoForm';
import { ProyectoCard } from '@/components/Proyectos/ProyectoCard';
import { useProyectos } from '@/hooks/proyectos/queries';
import type { Proyecto } from '@/types/proyecto';

// Quien vuelve ve primero sus proyectos; la bienvenida para empezar sólo encabeza la pantalla
// mientras la persona todavía no tiene ninguno.
export function ProyectosPage() {
  const proyectos = useProyectos();
  return (
    <div className="flex flex-col gap-8">
      {proyectos.length === 0 ? (
        <>
          <PageHeading
            kicker="Mis proyectos"
            title="¿Por dónde querés empezar?"
            lead="Pasá lo que tenés en el cuaderno a un sistema para tu negocio."
          />
          <FormasDeEmpezar destacarFoto />
          <Panel>
            <p className="m-0 text-soft">
              Todavía no tenés proyectos. Elegí una de las opciones de arriba para empezar.
            </p>
          </Panel>
        </>
      ) : (
        <>
          <PageHeading
            kicker="Inicio"
            title="Mis proyectos"
            lead="Elegí un proyecto para seguir cargando tus datos, o armá uno nuevo."
          />
          <ListaProyectos proyectos={proyectos} />
          <section aria-labelledby="nuevo-titulo" className="flex flex-col gap-4">
            <h2 id="nuevo-titulo" className="m-0 text-xl">
              Empezar otro proyecto
            </h2>
            <FormasDeEmpezar />
          </section>
        </>
      )}
      <CatalogoPlantillas />
    </div>
  );
}

function ListaProyectos({ proyectos }: { proyectos: Proyecto[] }) {
  return (
    <section aria-label="Tus proyectos">
      <ul className="m-0 grid list-none gap-4 p-0 sm:grid-cols-2 lg:grid-cols-3">
        {proyectos.map((proyecto) => (
          <li key={proyecto.id}>
            <ProyectoCard proyecto={proyecto} />
          </li>
        ))}
      </ul>
    </section>
  );
}

// Foto del cuaderno, plantilla o en blanco. Para quien empieza la foto va destacada;
// para quien ya tiene proyectos las tres opciones van compactas, debajo de la lista.
function FormasDeEmpezar({ destacarFoto = false }: { destacarFoto?: boolean }) {
  const [creando, setCreando] = useState(false);
  const [avisoFoto, setAvisoFoto] = useState(false);
  const aviso = avisoFoto && (
    <p role="status" className="mb-0 max-w-xl text-soft">
      La carga de fotos estará disponible próximamente. Mientras tanto, podés usar una plantilla o
      empezar en blanco.
    </p>
  );
  return (
    <>
      {destacarFoto && (
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
          <Button variant="solid" onClick={() => setAvisoFoto(true)}>
            Subir foto de mi cuaderno
          </Button>
          {aviso}
        </section>
      )}
      <div className="flex flex-wrap gap-3" aria-label="Formas de empezar">
        {!destacarFoto && (
          <Button onClick={() => setAvisoFoto(true)}>
            <CameraIcon size={20} aria-hidden />
            Subir foto de mi cuaderno
          </Button>
        )}
        <a href="#plantillas" className={buttonClasses()}>
          <SquaresFourIcon size={20} aria-hidden />
          Usar plantilla
        </a>
        <Button onClick={() => setCreando(true)}>
          <PlusIcon size={20} aria-hidden />
          Empezar en blanco
        </Button>
      </div>
      {!destacarFoto && aviso}
      {creando && (
        <Panel>
          <CrearProyectoForm onCancelar={() => setCreando(false)} />
        </Panel>
      )}
    </>
  );
}
