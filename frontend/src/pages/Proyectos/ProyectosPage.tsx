import { useMemo, useState } from 'react';
import { Link } from '@tanstack/react-router';
import {
  ArrowRightIcon,
  CameraIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  SquaresFourIcon,
  TableIcon,
  UsersIcon,
  PackageIcon,
} from '@phosphor-icons/react';
import { Button, InputField, PageHeading, Panel, Sheet, buttonClasses } from '@/components/ui';
import { CatalogoPlantillas } from '@/components/Proyectos/CatalogoPlantillas';
import { CrearProyectoForm } from '@/components/Proyectos/CrearProyectoForm';
import { ProyectoCard } from '@/components/Proyectos/ProyectoCard';
import { ProyectosBienvenida } from '@/components/Proyectos/ProyectosBienvenida';
import '@/components/Proyectos/proyectos.css';
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
    <div className="proyectos-home">
      {proyectos.length === 0 ? (
        <>
          <ProyectosBienvenida />
          <div className="proyectos-section-heading">
            <h2>Tres caminos. Tu próximo sistema.</h2>
            <span />
          </div>
          <p className="proyectos-empty-note">
            Todavía no tenés sistemas. Elegí cómo querés empezar.
          </p>
          <FormasDeEmpezar onEmpezarEnBlanco={() => setCreando(true)} />
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
            <FormasDeEmpezar onEmpezarEnBlanco={() => setCreando(true)} mostrarFoto={false} />
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
        <ul className="proyectos-card-grid">
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
function FormasDeEmpezar({
  onEmpezarEnBlanco,
  mostrarFoto = true,
}: {
  onEmpezarEnBlanco: () => void;
  mostrarFoto?: boolean;
}) {
  return (
    <div
      className={`proyectos-start-grid ${!mostrarFoto ? 'proyectos-start-grid-two' : ''}`}
      aria-label="Formas de empezar"
    >
      {mostrarFoto && (
        <article className="proyectos-start-card proyectos-photo-card">
          <span className="proyectos-card-tag">Desde tu cuaderno</span>
          <img className="proyectos-card-sami" src="/mockup/sami-armando-sin-fondo.png" alt="" />
          <h3>
            Tu cuaderno,
            <br />
            más organizado.
          </h3>
          <p>Una foto es el primer paso. Revisá lo que leímos y convertí tus apuntes en tablas.</p>
          <Link
            to="/cuaderno"
            className={buttonClasses({ variant: 'solid', className: 'proyectos-route-button' })}
          >
            <CameraIcon size={20} aria-hidden />
            Subir foto de mi cuaderno
            <ArrowRightIcon size={18} aria-hidden />
          </Link>
        </article>
      )}
      <article className="proyectos-start-card proyectos-template-card">
        <div className="proyectos-route-blocks" aria-hidden="true">
          <i>
            <TableIcon size={23} />
          </i>
          <i>
            <UsersIcon size={23} />
          </i>
          <i>
            <PackageIcon size={23} />
          </i>
        </div>
        <h3>
          Un conjunto
          <br />
          que ya funciona.
        </h3>
        <p>
          Elegí una base ya armada. Después ajustá las tablas y columnas a tu manera de trabajar.
        </p>
        <a href="#plantillas" className={buttonClasses({ className: 'proyectos-route-button' })}>
          <SquaresFourIcon size={20} aria-hidden />
          Usar plantilla
          <ArrowRightIcon size={18} aria-hidden />
        </a>
      </article>
      <article className="proyectos-start-card proyectos-blank-card">
        <div className="proyectos-free-arrangement" aria-hidden="true">
          <i />
          <i />
          <i>
            <PlusIcon size={18} />
          </i>
        </div>
        <h3>
          Tu lienzo.
          <br />
          Tus reglas.
        </h3>
        <p>Empezá por un nombre y armá las tablas que necesitás, columna por columna.</p>
        <Button onClick={onEmpezarEnBlanco} className="proyectos-route-button">
          <PlusIcon size={20} aria-hidden />
          Empezar en blanco
          <ArrowRightIcon size={18} aria-hidden />
        </Button>
      </article>
    </div>
  );
}
