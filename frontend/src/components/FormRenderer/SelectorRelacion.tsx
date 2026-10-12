import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { ArrowsClockwiseIcon, MagnifyingGlassIcon, XIcon } from '@phosphor-icons/react';
import { controlOpcion, tarjetaOpcion } from '@/components/Constructor/opcionStyles';
import { Button, InputField, Skeleton } from '@/components/ui';
import { leerErrorServidor } from '@/hooks/errores';
import { useEtiquetaDeFila, useReferencias } from '@/hooks/registros/queries';
import { cn } from '@/lib/cn';
import type { Campo } from '@/types/proyecto';

type Props = {
  campo: Campo;
  /** Nombre de la tabla con la que se conecta la columna. */
  nombreTabla: string;
  /** Id de la fila elegida; vacío si todavía no se eligió. */
  valor: string;
  onChange: (valor: string) => void;
  error?: string;
};

const MAX_RESULTADOS = 20;
const FLECHAS = new Set(['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight']);

// Elige una fila de otra tabla: se busca por texto y se toca una tarjeta grande. Ya elegida, se ve
// su nombre con «Cambiar» y «Quitar». Con el teclado, las flechas recorren los resultados sin
// cerrar la lista (marcan la fila); Espacio o un toque la eligen y cierran la búsqueda.
export function SelectorRelacion({ campo, nombreTabla, valor, onChange, error }: Props) {
  const id = useId();
  const bloqueId = campo.bloqueRelacionadoId ?? '';
  const [buscando, setBuscando] = useState(false);
  const [texto, setTexto] = useState('');
  const [elegida, setElegida] = useState<{ id: string; etiqueta: string }>();
  const [enfocar, setEnfocar] = useState<'buscar' | 'cambiar'>();
  const conFlechas = useRef(false);
  const contenedor = useRef<HTMLFieldSetElement>(null);

  const abierto = buscando || !valor;
  const conocida = elegida?.id === valor ? elegida.etiqueta : undefined;
  // Si el valor llegó de afuera (no se eligió acá), se pide su etiqueta.
  const remota = useEtiquetaDeFila(bloqueId, valor && conocida === undefined ? valor : '');
  const etiquetaElegida =
    conocida ??
    (remota.isPending ? 'Cargando…' : remota.data === null ? 'Fila eliminada' : remota.data);

  useEffect(() => {
    if (!enfocar) return;
    const selector = enfocar === 'buscar' ? 'input[type="search"]' : '[data-cambiar]';
    contenedor.current?.querySelector<HTMLElement>(selector)?.focus();
    setEnfocar(undefined);
  }, [enfocar]);

  function elegir(fila: { id: string; etiqueta: string }) {
    setElegida(fila);
    onChange(fila.id);
    if (conFlechas.current) {
      conFlechas.current = false;
      return;
    }
    setBuscando(false);
    setTexto('');
    setEnfocar('cambiar');
  }

  function quitar() {
    onChange('');
    setElegida(undefined);
    setBuscando(true);
    setEnfocar('buscar');
  }

  const errorId = `${id}-error`;

  return (
    <fieldset
      ref={contenedor}
      className="m-0 flex min-w-0 flex-col gap-3 border-0 p-0"
      aria-describedby={error ? errorId : undefined}
    >
      <legend className="mb-1 flex flex-wrap items-baseline gap-x-2 p-0 text-base font-semibold">
        {campo.nombre}
        {campo.obligatorio && (
          <span aria-hidden className="text-hint font-normal text-muted">
            (obligatorio)
          </span>
        )}
      </legend>

      {valor && (
        <div
          className={cn(
            'flex flex-col gap-3 rounded-md border-2 p-3 sm:flex-row sm:items-center sm:justify-between',
            abierto ? 'border-line bg-surface' : 'border-accent-700 bg-accent-100',
          )}
        >
          <p className="m-0 flex min-w-0 flex-col">
            <span className="text-hint text-soft">Fila elegida de «{nombreTabla}»</span>
            <span className="text-body font-semibold break-words">{etiquetaElegida}</span>
          </p>
          <div className="flex shrink-0 flex-wrap gap-2">
            {abierto ? (
              <Button
                onClick={() => {
                  setBuscando(false);
                  setTexto('');
                  setEnfocar('cambiar');
                }}
              >
                Listo
              </Button>
            ) : (
              <Button
                name={campo.id}
                data-cambiar
                aria-label={`Cambiar la fila elegida en ${campo.nombre}`}
                onClick={() => {
                  setBuscando(true);
                  setEnfocar('buscar');
                }}
              >
                <ArrowsClockwiseIcon aria-hidden size={20} />
                Cambiar
              </Button>
            )}
            <Button
              variant="ghost"
              aria-label={`Quitar la fila elegida en ${campo.nombre}`}
              onClick={quitar}
            >
              <XIcon aria-hidden size={20} />
              Quitar
            </Button>
          </div>
        </div>
      )}

      {abierto && (
        <Buscador
          id={id}
          campo={campo}
          bloqueId={bloqueId}
          nombreTabla={nombreTabla}
          texto={texto}
          onTexto={setTexto}
          valor={valor}
          invalido={Boolean(error)}
          errorId={errorId}
          onTecla={(event) => {
            conFlechas.current = FLECHAS.has(event.key);
          }}
          onElegir={elegir}
        />
      )}

      {error && (
        <p id={errorId} className="m-0 text-hint font-semibold text-bad">
          {error}
        </p>
      )}
    </fieldset>
  );
}

type BuscadorProps = {
  id: string;
  campo: Campo;
  bloqueId: string;
  nombreTabla: string;
  texto: string;
  onTexto: (texto: string) => void;
  valor: string;
  invalido: boolean;
  errorId: string;
  onTecla: (event: KeyboardEvent<HTMLInputElement>) => void;
  onElegir: (fila: { id: string; etiqueta: string }) => void;
};

function Buscador({
  id,
  campo,
  bloqueId,
  nombreTabla,
  texto,
  onTexto,
  valor,
  invalido,
  errorId,
  onTecla,
  onElegir,
}: BuscadorProps) {
  const referencias = useReferencias(bloqueId, texto);
  const { busqueda, data, isPending, isError, isFetching } = referencias;

  return (
    <div className="flex flex-col gap-3">
      <InputField
        label={`Buscá en «${nombreTabla}»`}
        hint="Escribí una parte del nombre y tocá la fila que querés."
        type="search"
        // Con este nombre lo encuentra el formulario para llevar el foco al error.
        name={campo.id}
        value={texto}
        onChange={(event) => onTexto(event.target.value)}
        // Enter busca (ya se busca al escribir): no guarda la fila a medio completar.
        onKeyDown={(event) => {
          if (event.key === 'Enter') event.preventDefault();
        }}
        aria-invalid={invalido || undefined}
        aria-errormessage={invalido ? errorId : undefined}
        autoComplete="off"
        trailing={<MagnifyingGlassIcon aria-hidden size={22} className="mr-3 text-muted" />}
      />

      {isPending ? (
        <div role="status" className="flex flex-col gap-2">
          <span className="sr-only">Buscando filas…</span>
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      ) : isError ? (
        <div
          role="alert"
          className="flex flex-col items-start gap-2 rounded-md border border-bad/30 p-3"
        >
          <p className="m-0">No pudimos traer las filas de «{nombreTabla}».</p>
          <p className="m-0 text-hint text-soft">{leerErrorServidor(referencias.error)?.mensaje}</p>
          <Button onClick={() => void referencias.refetch()}>Reintentar</Button>
        </div>
      ) : data.length === 0 ? (
        <p
          role="status"
          className="m-0 rounded-md border border-dashed border-fg/30 p-4 text-body text-soft"
        >
          {busqueda
            ? `Ninguna fila de «${nombreTabla}» coincide con «${busqueda}».`
            : `Todavía no hay filas en «${nombreTabla}». Cargá alguna en esa tabla para poder elegirla.`}
        </p>
      ) : (
        <fieldset
          className={cn('m-0 flex min-w-0 flex-col gap-2 border-0 p-0', isFetching && 'opacity-60')}
        >
          <legend className="sr-only">Filas de «{nombreTabla}»</legend>
          {data.map((fila) => (
            <label key={fila.id} className={cn(tarjetaOpcion, 'min-h-14 items-center')}>
              <input
                type="radio"
                name={`${id}-fila`}
                value={fila.id}
                checked={valor === fila.id}
                onKeyDown={onTecla}
                onChange={() => onElegir(fila)}
                className={cn(controlOpcion, 'mt-0')}
              />
              <span className="min-w-0 font-semibold break-words">{fila.etiqueta}</span>
            </label>
          ))}
          {data.length >= MAX_RESULTADOS && (
            <p className="m-0 text-hint text-muted">
              Se ven las {MAX_RESULTADOS} filas más nuevas. Escribí para encontrar otras.
            </p>
          )}
        </fieldset>
      )}
    </div>
  );
}
