import { useEffect, useRef, useState } from 'react';
import { ImageIcon, PencilSimpleIcon, TrashIcon, XIcon } from '@phosphor-icons/react';
import { VisorFoto } from '@/components/Cuaderno/Revision/VisorFoto';
import { FormRenderer } from '@/components/FormRenderer/FormRenderer';
import { Button, ConfirmDialog, Panel, Sheet } from '@/components/ui';
import { urlFotoOrigen } from '@/hooks/lotes/fotos';
import { useEliminarRegistro } from '@/hooks/registros/mutations';
import { useEtiquetas } from '@/hooks/registros/queries';
import { useEditarRegistro } from '@/hooks/registros/useEditarRegistro';
import { useEsEscritorio } from '@/hooks/useMediaQuery';
import { cambiosParaEditar, valoresDeRegistro } from '@/lib/formularioRegistro';
import { formatearMomento, formatearValor } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { Registro, ResultadoEdicion, ValoresRegistro } from '@/types/registro';

type FichaRegistroProps = {
  bloque: Bloque;
  registro: Registro;
  onCerrar: () => void;
  /** Avisa con la fila ya guardada, para que quien la mostró no quede con los datos viejos. */
  onActualizado: (registro: Registro) => void;
  /** Tablas del sistema: al editar, las columnas que se conectan con otra tabla la nombran. */
  tablas?: readonly { id: string; nombre: string }[];
};

// Ficha de una fila: panel lateral fijo en escritorio, hoja desde abajo en el celular.
// Conviene montarla con `key={registro.id}`: al elegir otra fila arranca de cero, sin edición.
export function FichaRegistro({
  bloque,
  registro,
  onCerrar,
  onActualizado,
  tablas,
}: FichaRegistroProps) {
  const esEscritorio = useEsEscritorio();
  const [confirmando, setConfirmando] = useState(false);
  const [viendoFoto, setViendoFoto] = useState(false);
  const [editando, setEditando] = useState(false);
  const [guardado, setGuardado] = useState(false);
  const titulo = useRef<HTMLHeadingElement>(null);
  const cuerpoRef = useRef<HTMLDivElement>(null);
  const botonEditar = useRef<HTMLButtonElement>(null);
  const foco = useRef<'formulario' | 'editar' | null>(null);
  const eliminar = useEliminarRegistro(bloque.id);
  const editar = useEditarRegistro(bloque.id);
  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);
  const etiquetaDe = useEtiquetas(campos, [registro]);

  useEffect(() => {
    titulo.current?.focus();
  }, [registro.id, esEscritorio]);

  // Al entrar o salir de la edición el botón que tenía el foco desaparece: se lo lleva a donde sigue.
  useEffect(() => {
    if (foco.current === 'formulario') {
      cuerpoRef.current
        ?.querySelector<HTMLElement>('input:not([type="hidden"]), select, textarea')
        ?.focus();
    } else if (foco.current === 'editar') {
      botonEditar.current?.focus();
    }
    foco.current = null;
  }, [editando]);

  function empezarEdicion() {
    foco.current = 'formulario';
    setGuardado(false);
    setEditando(true);
  }

  function terminarEdicion() {
    foco.current = 'editar';
    setEditando(false);
  }

  async function guardar(cargados: ValoresRegistro): Promise<ResultadoEdicion> {
    const resultado = await editar(registro.id, cambiosParaEditar(campos, cargados));
    if (resultado.exito) {
      onActualizado(resultado.registro);
      setGuardado(true);
      terminarEdicion();
    }
    return resultado;
  }

  const datos = (
    <>
      {guardado && (
        <p role="status" className="m-0 text-base font-semibold text-ok">
          Cambios guardados.
        </p>
      )}
      <dl className="m-0 flex flex-col gap-3">
        {campos.map((campo) => (
          <div key={campo.id} className="flex flex-col gap-0.5 border-b border-line pb-2">
            <dt className="text-hint text-soft">{campo.nombre}</dt>
            <dd className="m-0 text-body break-words">
              {formatearValor(
                campo.tipo,
                registro.valores[campo.id],
                etiquetaDe(campo, registro.valores[campo.id]),
              )}
            </dd>
          </div>
        ))}
        <div className="flex flex-col gap-0.5 text-hint text-muted">
          <span>Cargado: {formatearMomento(registro.creadoEn)}</span>
          <span>Última modificación: {formatearMomento(registro.actualizadoEn)}</span>
        </div>
      </dl>

      {registro.fotoOrigenId && (
        <div className="flex flex-col gap-2">
          <Button
            className="self-start"
            aria-expanded={viendoFoto}
            onClick={() => setViendoFoto((abierta) => !abierta)}
          >
            <ImageIcon aria-hidden size={20} />
            {viendoFoto ? 'Ocultar el cuaderno' : 'Ver en el cuaderno'}
          </Button>
          {viendoFoto && (
            <VisorFoto
              paginas={[{ numero: 1, url: urlFotoOrigen(registro.id) }]}
              actual={1}
              onElegir={() => undefined}
            />
          )}
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Button ref={botonEditar} variant="solid" onClick={empezarEdicion}>
          <PencilSimpleIcon aria-hidden size={20} />
          Editar fila
        </Button>
        <Button variant="danger" onClick={() => setConfirmando(true)}>
          <TrashIcon aria-hidden size={20} />
          Eliminar fila
        </Button>
      </div>

      <ConfirmDialog
        open={confirmando}
        titulo="¿Eliminar esta fila?"
        confirmar="Sí, eliminar fila"
        conservar="Conservar fila"
        ocupado={eliminar.isPending}
        error={eliminar.error?.message}
        onConfirmar={() => eliminar.mutate(registro.id, { onSuccess: onCerrar })}
        onConservar={() => {
          setConfirmando(false);
          eliminar.reset();
        }}
      >
        <p className="m-0">Se eliminará esta fila. Esta acción no se puede deshacer.</p>
      </ConfirmDialog>
    </>
  );

  const cuerpo = (
    <div ref={cuerpoRef} className="flex flex-col gap-4">
      {editando ? (
        <FormRenderer
          modo="editar"
          campos={bloque.campos}
          valores={valoresDeRegistro(bloque.campos, registro.valores)}
          tablas={tablas}
          onSubmit={guardar}
          onCancelar={terminarEdicion}
        />
      ) : (
        datos
      )}
    </div>
  );

  if (!esEscritorio) {
    return (
      <Sheet open onClose={onCerrar} title={`Ficha de ${bloque.nombre}`}>
        {cuerpo}
      </Sheet>
    );
  }

  return (
    <aside aria-label="Ficha de la fila" className="lg:sticky lg:top-20 lg:self-start">
      <Panel className="flex flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <h2 ref={titulo} tabIndex={-1} className="m-0 text-h3 break-words focus:outline-none">
            Ficha de {bloque.nombre}
          </h2>
          <Button variant="ghost" iconOnly aria-label="Cerrar ficha" onClick={onCerrar}>
            <XIcon aria-hidden size={22} />
          </Button>
        </div>
        {cuerpo}
      </Panel>
    </aside>
  );
}
