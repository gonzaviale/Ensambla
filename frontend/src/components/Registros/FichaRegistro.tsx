import { useEffect, useRef, useState } from 'react';
import { ImageIcon, XIcon } from '@phosphor-icons/react';
import { VisorFoto } from '@/components/Cuaderno/Revision/VisorFoto';
import { Button, Panel } from '@/components/ui';
import { urlFotoOrigen } from '@/hooks/lotes/fotos';
import { useEliminarRegistro } from '@/hooks/registros/mutations';
import { formatearMomento, formatearValor } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

type FichaRegistroProps = {
  bloque: Bloque;
  registro: Registro;
  onCerrar: () => void;
};

export function FichaRegistro({ bloque, registro, onCerrar }: FichaRegistroProps) {
  const [confirmando, setConfirmando] = useState(false);
  const [viendoFoto, setViendoFoto] = useState(false);
  const titulo = useRef<HTMLHeadingElement>(null);
  const eliminar = useEliminarRegistro(bloque.id);
  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);

  useEffect(() => {
    titulo.current?.focus();
  }, [registro.id]);

  return (
    <aside aria-label="Ficha del registro">
      <Panel className="flex flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <h2 ref={titulo} tabIndex={-1} className="m-0 text-lg focus:outline-none">
            Ficha de {bloque.nombre}
          </h2>
          <Button variant="ghost" size="sm" aria-label="Cerrar ficha" onClick={onCerrar}>
            <XIcon aria-hidden size={16} />
          </Button>
        </div>

        <dl className="m-0 flex flex-col gap-3">
          {campos.map((campo) => (
            <div key={campo.id} className="flex flex-col gap-0.5 border-b border-line pb-2">
              <dt className="text-[12.5px] text-soft">{campo.nombre}</dt>
              <dd className="m-0 break-words">
                {formatearValor(campo.tipo, registro.valores[campo.id])}
              </dd>
            </div>
          ))}
          <div className="flex flex-col gap-0.5 text-[12.5px] text-muted">
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
              <ImageIcon aria-hidden size={16} />
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

        {confirmando ? (
          <div
            role="group"
            aria-label="Confirmar eliminación"
            className="rounded-md border border-bad/30 p-3"
          >
            <p className="mt-0">Se eliminará este registro. Esta acción no se puede deshacer.</p>
            <div className="flex flex-wrap gap-2">
              <Button
                disabled={eliminar.isPending}
                onClick={() => eliminar.mutate(registro.id, { onSuccess: onCerrar })}
              >
                Sí, eliminar registro
              </Button>
              <Button
                disabled={eliminar.isPending}
                onClick={() => {
                  setConfirmando(false);
                  eliminar.reset();
                }}
              >
                Conservar registro
              </Button>
            </div>
          </div>
        ) : (
          <Button variant="ghost" className="self-start" onClick={() => setConfirmando(true)}>
            Eliminar registro
          </Button>
        )}

        {eliminar.error && (
          <p role="alert" className="text-bad">
            {eliminar.error.message}
          </p>
        )}
      </Panel>
    </aside>
  );
}
