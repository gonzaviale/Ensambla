import { useEffect, useRef, useState } from 'react';
import { ImageIcon, TrashIcon, XIcon } from '@phosphor-icons/react';
import { VisorFoto } from '@/components/Cuaderno/Revision/VisorFoto';
import { Button, ConfirmDialog, Panel, Sheet } from '@/components/ui';
import { urlFotoOrigen } from '@/hooks/lotes/fotos';
import { useEliminarRegistro } from '@/hooks/registros/mutations';
import { useEsEscritorio } from '@/hooks/useMediaQuery';
import { formatearMomento, formatearValor } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

type FichaRegistroProps = {
  bloque: Bloque;
  registro: Registro;
  onCerrar: () => void;
};

// Ficha de una fila: panel lateral fijo en escritorio, hoja desde abajo en el celular.
// Editar una fila existente necesita un endpoint de modificación que el backend todavía no expone.
export function FichaRegistro({ bloque, registro, onCerrar }: FichaRegistroProps) {
  const esEscritorio = useEsEscritorio();
  const [confirmando, setConfirmando] = useState(false);
  const [viendoFoto, setViendoFoto] = useState(false);
  const titulo = useRef<HTMLHeadingElement>(null);
  const eliminar = useEliminarRegistro(bloque.id);
  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);

  useEffect(() => {
    titulo.current?.focus();
  }, [registro.id, esEscritorio]);

  const cuerpo = (
    <div className="flex flex-col gap-4">
      <dl className="m-0 flex flex-col gap-3">
        {campos.map((campo) => (
          <div key={campo.id} className="flex flex-col gap-0.5 border-b border-line pb-2">
            <dt className="text-hint text-soft">{campo.nombre}</dt>
            <dd className="m-0 text-body break-words">
              {formatearValor(campo.tipo, registro.valores[campo.id])}
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

      <Button variant="danger" className="self-start" onClick={() => setConfirmando(true)}>
        <TrashIcon aria-hidden size={20} />
        Eliminar fila
      </Button>

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
