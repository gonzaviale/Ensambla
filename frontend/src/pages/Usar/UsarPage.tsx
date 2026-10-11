import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearch } from '@tanstack/react-router';
import { TableIcon } from '@phosphor-icons/react';
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

const VISTAS: { id: Vista; etiqueta: string }[] = [
  { id: 'registros', etiqueta: 'Ver registros' },
  { id: 'cargar', etiqueta: 'Cargar' },
];

// Tabla de registros por bloque (búsqueda, orden y ficha) y formulario de carga (ENS-6).
// Con pocos bloques se muestran todos como botones; con más, una lista desplegable.
const MAX_BLOQUES_EN_BOTONES = 5;

function IrAConstruir({ proyectoId, bloqueId }: { proyectoId: string; bloqueId: string }) {
  return (
    <Link
      to="/proyectos/$proyectoId/construir"
      params={{ proyectoId }}
      search={{ bloque: bloqueId }}
      className={buttonClasses({ variant: 'primary', className: 'mt-3' })}
    >
      Agregar datos en Construir
    </Link>
  );
}

function BloqueSinDatos({ proyectoId, bloque }: { proyectoId: string; bloque: Bloque }) {
  return (
    <EmptyState
      icon={<TableIcon aria-hidden size={32} />}
      title={`${bloque.nombre} todavía no tiene datos`}
    >
      <p className="m-0 text-[15px]">
        Para cargar algo, primero elegí qué datos guarda, como Nombre o Precio. Cada dato es una
        columna de la tabla.
      </p>
      <IrAConstruir proyectoId={proyectoId} bloqueId={bloque.id} />
    </EmptyState>
  );
}

export function UsarPage() {
  const { proyectoId } = useParams({ from: RUTA });
  const { nuevo } = useSearch({ from: RUTA });
  const navigate = useNavigate();
  const proyecto = useProyecto(proyectoId);
  const panelCarga = useRef<HTMLDivElement>(null);
  const enfocarCarga = useRef(false);
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

  const elegirBloque = (id: string) => {
    setBloqueId(id);
    setSeleccionado(undefined);
  };

  // "Cargar el primer registro": abre la carga y lleva el foco al primer dato del formulario.
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

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker="Modo usar"
        title={proyecto.nombre}
        lead="Cargá datos en cada bloque y buscá, ordená y revisá lo que ya cargaste."
      />

      {nuevo && (
        <Panel
          role="status"
          className="flex flex-col items-start gap-3 border-accent-200 bg-accent-100"
        >
          <p className="m-0">
            <strong>¡Listo! Tu proyecto ya viene armado.</strong> Podés empezar a cargar. Si querés
            cambiar los bloques o sus datos, podés hacerlo en Construir.
          </p>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/proyectos/$proyectoId/construir"
              params={{ proyectoId }}
              className={buttonClasses({ size: 'sm' })}
            >
              Cambiar la estructura
            </Link>
            <Button
              size="sm"
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
          <div className="flex flex-col items-start gap-4">
            {bloques.length <= MAX_BLOQUES_EN_BOTONES ? (
              <SelectorBloque
                bloques={bloques}
                seleccionadoId={bloque.id}
                disabled={enviando}
                onElegir={elegirBloque}
              />
            ) : (
              <div className="min-w-48">
                <SelectField
                  label="Bloque"
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
            {vista === 'registros' && bloque.campos.length === 0 && (
              <BloqueSinDatos proyectoId={proyectoId} bloque={bloque} />
            )}
            {vista === 'registros' && bloque.campos.length > 0 && (
              <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
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
                  <p className="mt-0 mb-1 text-xs tracking-wide text-accent-800 uppercase">
                    Nuevo registro
                  </p>
                  <h2 id={`bloque-${item.id}`} className="mt-0 mb-5 text-xl">
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
