import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Link, useNavigate, useParams, useSearch } from '@tanstack/react-router';
import { PlusCircleIcon, TableIcon } from '@phosphor-icons/react';
import { FormularioRegistro } from '@/components/FormRenderer/FormularioRegistro';
import { FichaRegistro } from '@/components/Registros/FichaRegistro';
import { SelectorBloque } from '@/components/Registros/SelectorBloque';
import { TablaRegistros } from '@/components/Registros/TablaRegistros';
import { Button, EmptyState, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import { useProyecto } from '@/hooks/proyectos/queries';
import { cn } from '@/lib/cn';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

const RUTA = '/_app/proyectos/$proyectoId/usar';

type Vista = 'registros' | 'cargar';

// "Cargar" va primero y es la vista por defecto: es lo que más se hace.
const VISTAS: { id: Vista; etiqueta: string; icono: typeof TableIcon }[] = [
  { id: 'cargar', etiqueta: 'Cargar', icono: PlusCircleIcon },
  { id: 'registros', etiqueta: 'Ver filas', icono: TableIcon },
];

// Filas por tabla (búsqueda, orden y ficha) y formulario de carga (ENS-6).
// Con pocas tablas se muestran todas como botones; con más, una lista desplegable.
const MAX_BLOQUES_EN_BOTONES = 5;

function IrAConstruir({ proyectoId, bloqueId }: { proyectoId: string; bloqueId: string }) {
  return (
    <Link
      to="/proyectos/$proyectoId/construir"
      params={{ proyectoId }}
      search={{ bloque: bloqueId }}
      className={buttonClasses({ variant: 'primary', className: 'mt-3' })}
    >
      Agregar columnas en Armar
    </Link>
  );
}

function BloqueSinDatos({ proyectoId, bloque }: { proyectoId: string; bloque: Bloque }) {
  return (
    <EmptyState
      icon={<TableIcon aria-hidden size={32} />}
      title={`${bloque.nombre} todavía no tiene columnas`}
    >
      <p className="m-0">
        Para cargar algo, primero elegí qué datos guarda, como Nombre o Precio. Cada dato es una
        columna de la tabla.
      </p>
      <IrAConstruir proyectoId={proyectoId} bloqueId={bloque.id} />
    </EmptyState>
  );
}

export function UsarPage() {
  const { proyectoId } = useParams({ from: RUTA });
  const { nuevo, bloque: bloqueInicial } = useSearch({ from: RUTA });
  const navigate = useNavigate();
  const proyecto = useProyecto(proyectoId);
  const panelCarga = useRef<HTMLDivElement>(null);
  const botonesVista = useRef<Partial<Record<Vista, HTMLButtonElement | null>>>({});
  const enfocarCarga = useRef(false);
  const bloques = [...proyecto.bloques].sort((a, b) => a.posicion - b.posicion);
  const [bloqueId, setBloqueId] = useState<string | undefined>(bloqueInicial);
  const [seleccionado, setSeleccionado] = useState<Registro>();
  // Quien llega de guardar un cuaderno (?bloque=) quiere ver las filas que se guardaron.
  const [vista, setVista] = useState<Vista>(bloqueInicial ? 'registros' : 'cargar');
  // Los formularios quedan montados una vez abiertos para no perder los borradores.
  const [cargaAbierta, setCargaAbierta] = useState(!bloqueInicial);
  const [enviando, setEnviando] = useState(false);
  const bloque = bloques.find((item) => item.id === bloqueId) ?? bloques[0];

  const elegirVista = (siguiente: Vista) => {
    if (siguiente === 'cargar') setCargaAbierta(true);
    setVista(siguiente);
  };

  const elegirBloque = (id: string) => {
    setBloqueId(id);
    setSeleccionado(undefined);
  };

  // "Cargar la primera fila": abre la carga y lleva el foco al primer campo del formulario.
  const abrirCarga = () => {
    enfocarCarga.current = true;
    elegirVista('cargar');
  };

  useEffect(() => {
    if (vista !== 'cargar' || !enfocarCarga.current) return;
    enfocarCarga.current = false;
    panelCarga.current
      ?.querySelector<HTMLElement>(
        'section:not([hidden]) :is(input, select, textarea):not([type="hidden"])',
      )
      ?.focus();
  }, [vista]);

  // Pestañas con flechas: izquierda/derecha cambian de vista, Inicio/Fin van a los extremos.
  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    const indice = VISTAS.findIndex((item) => item.id === vista);
    const destino =
      evento.key === 'ArrowRight'
        ? (VISTAS[(indice + 1) % VISTAS.length] ?? VISTAS[0])
        : evento.key === 'ArrowLeft'
          ? (VISTAS[(indice - 1 + VISTAS.length) % VISTAS.length] ?? VISTAS[0])
          : evento.key === 'Home'
            ? VISTAS[0]
            : evento.key === 'End'
              ? VISTAS[VISTAS.length - 1]
              : undefined;
    if (!destino || enviando) return;
    evento.preventDefault();
    elegirVista(destino.id);
    botonesVista.current[destino.id]?.focus();
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Cargar y ver"
        title={proyecto.nombre}
        lead="Cargá filas en cada tabla y buscá, ordená y revisá lo que ya cargaste."
      />

      {nuevo && (
        <Panel
          role="status"
          className="flex flex-col items-start gap-3 border-accent-200 bg-accent-100"
        >
          <p className="m-0 text-body">
            <strong>¡Listo! Tu sistema ya viene armado.</strong> Podés empezar a cargar. Si querés
            cambiar las tablas o sus columnas, podés hacerlo en Armar.
          </p>
          <div className="flex flex-wrap gap-3">
            <Link
              to="/proyectos/$proyectoId/construir"
              params={{ proyectoId }}
              className={buttonClasses()}
            >
              Armar mis tablas
            </Link>
            <Button
              variant="ghost"
              onClick={() =>
                void navigate({
                  to: '/proyectos/$proyectoId/usar',
                  params: { proyectoId },
                  search: {},
                  replace: true,
                })
              }
            >
              Entendido
            </Button>
          </div>
        </Panel>
      )}

      {!bloque ? (
        <EmptyState icon={<TableIcon aria-hidden size={32} />} title="Este sistema no tiene tablas">
          <p className="m-0">Armá primero sus tablas para poder cargar filas.</p>
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
          <div className="flex flex-col items-start gap-4">
            {bloques.length <= MAX_BLOQUES_EN_BOTONES ? (
              <SelectorBloque
                bloques={bloques}
                seleccionadoId={bloque.id}
                disabled={enviando}
                onElegir={elegirBloque}
              />
            ) : (
              <div className="w-full max-w-sm">
                <SelectField
                  label="Tabla"
                  value={bloque.id}
                  disabled={enviando}
                  onChange={(event) => elegirBloque(event.target.value)}
                >
                  {bloques.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.nombre}
                    </option>
                  ))}
                </SelectField>
              </div>
            )}
            <div
              role="tablist"
              aria-label="Qué querés hacer con la tabla"
              onKeyDown={alTeclear}
              className="flex w-full gap-1 rounded-lg bg-fg/8 p-1 sm:w-fit"
            >
              {VISTAS.map((item) => {
                const activa = vista === item.id;
                const Icono = item.icono;
                return (
                  <button
                    key={item.id}
                    ref={(el) => {
                      botonesVista.current[item.id] = el;
                    }}
                    type="button"
                    role="tab"
                    id={`vista-${item.id}`}
                    aria-selected={activa}
                    aria-controls={`panel-${item.id}`}
                    tabIndex={activa ? 0 : -1}
                    disabled={enviando}
                    onClick={() => elegirVista(item.id)}
                    className={cn(
                      'flex min-h-12 flex-1 cursor-pointer items-center justify-center gap-2 rounded-md px-5 text-base font-semibold sm:flex-none',
                      activa ? 'bg-surface text-fg shadow-sm' : 'text-soft hover:text-fg',
                      'disabled:cursor-not-allowed disabled:opacity-60',
                    )}
                  >
                    <Icono aria-hidden size={22} />
                    {item.etiqueta}
                  </button>
                );
              })}
            </div>
          </div>

          <div
            role="tabpanel"
            id="panel-registros"
            aria-labelledby="vista-registros"
            hidden={vista !== 'registros'}
          >
            {vista === 'registros' && bloque.campos.length === 0 && (
              <BloqueSinDatos proyectoId={proyectoId} bloque={bloque} />
            )}
            {vista === 'registros' && bloque.campos.length > 0 && (
              <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
                <TablaRegistros
                  key={bloque.id}
                  bloque={bloque}
                  seleccionadoId={seleccionado?.id}
                  onSeleccionar={setSeleccionado}
                  onCargar={abrirCarga}
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
            ref={panelCarga}
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
                  <p className="mt-0 mb-1 text-caption font-bold tracking-widest text-accent-800 uppercase">
                    Nueva fila
                  </p>
                  <h2 id={`bloque-${item.id}`} className="mt-0 mb-5 text-h3">
                    {item.nombre}
                  </h2>
                  <FormularioRegistro bloque={item} onEnviandoChange={setEnviando} />
                  {item.campos.length === 0 && (
                    <IrAConstruir proyectoId={proyectoId} bloqueId={item.id} />
                  )}
                </Panel>
              ))}
          </div>
        </>
      )}
    </div>
  );
}
