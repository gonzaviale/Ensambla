import { useId, useRef, useState, type FormEvent } from 'react';
import { PlusIcon, XIcon } from '@phosphor-icons/react';
import { SelectorRelacion } from '@/components/FormRenderer/SelectorRelacion';
import { Button, ErrorState, InputField, SelectField, Skeleton } from '@/components/ui';
import { useCampos } from '@/hooks/bloques/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useGuardarReceta } from '@/hooks/receta/mutations';
import { useReceta } from '@/hooks/receta/queries';
import { cn } from '@/lib/cn';
import { formatearCantidad, formatearDinero } from '@/lib/valores';
import type { Campo } from '@/types/proyecto';
import type { Registro } from '@/types/registro';
import {
  MAX_LINEAS_RECETA,
  MAX_UNIDAD_RECETA,
  type LineaReceta,
  type Receta,
} from '@/types/receta';

type Tabla = { id: string; nombre: string };

type Props = {
  registro: Registro;
  /** Nombre con el que se llama a la fila en los textos («Pan», «Torta»). */
  producto: string;
  /** Las tablas del sistema: de una de ellas salen los insumos. */
  tablas: readonly Tabla[];
  /** La tabla del producto: es la que se propone para los insumos si no hay otra. */
  bloqueId: string;
};

type LineaBorrador = { clave: string; insumoId: string; cantidad: string; unidad: string };

type Borrador = { tablaId: string; campoPrecioId: string; lineas: LineaBorrador[] };

let siguienteClave = 0;
const nuevaClave = () => `linea-${++siguienteClave}`;

const textoDeCantidad = (cantidad: number) => String(cantidad).replace('.', ',');

function borradorDe(receta: Receta, tablas: readonly Tabla[], bloqueId: string): Borrador {
  if (receta.bloqueInsumosId) {
    return {
      tablaId: receta.bloqueInsumosId,
      campoPrecioId: receta.campoPrecioId ?? '',
      lineas: receta.lineas.map((linea) => ({
        clave: nuevaClave(),
        insumoId: linea.insumoId,
        cantidad: textoDeCantidad(linea.cantidad),
        unidad: linea.unidad ?? '',
      })),
    };
  }
  // Los insumos suelen estar en otra tabla que el producto, pero puede ser la misma.
  const otra = tablas.find((tabla) => tabla.id !== bloqueId);
  return { tablaId: otra?.id ?? bloqueId, campoPrecioId: '', lineas: [] };
}

const NUMERO = /^\d+([.,]\d+)?$/;

// Receta de un producto: qué insumos lleva y cuánto de cada uno. El costo lo calcula el servidor con el precio
// actual de cada insumo, por eso se muestra tal como vuelve de ahí y no se recalcula en pantalla.
export function SeccionReceta({ registro, producto, tablas, bloqueId }: Props) {
  const receta = useReceta(registro.id);

  if (receta.isPending) {
    return (
      <div role="status" aria-label="Cargando la receta…" className="flex flex-col gap-2">
        <Skeleton className="h-12 w-full" />
        <span className="sr-only">Cargando la receta…</span>
      </div>
    );
  }
  if (receta.isError) {
    return (
      <ErrorState
        title="No pudimos cargar la receta"
        message={receta.error.message}
        onRetry={() => void receta.refetch()}
      />
    );
  }
  return (
    <EditorReceta
      registro={registro}
      producto={producto}
      tablas={tablas}
      bloqueId={bloqueId}
      receta={receta.data}
    />
  );
}

function EditorReceta({
  registro,
  producto,
  tablas,
  bloqueId,
  receta,
}: Props & { receta: Receta }) {
  const id = useId();
  const formulario = useRef<HTMLFormElement>(null);
  const [borrador, setBorrador] = useState(() => borradorDe(receta, tablas, bloqueId));
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [sinGuardar, setSinGuardar] = useState(false);
  const [guardada, setGuardada] = useState(false);
  const guardar = useGuardarReceta(registro.id);
  const columnas = useCampos(borrador.tablaId);
  const servidor = leerErrorServidor(guardar.error);

  const numericas = (columnas.data ?? []).filter((columna) => columna.tipo === 'NUMERO');
  const sugerida =
    numericas.find((columna) => /precio|costo/i.test(columna.nombre)) ?? numericas[0];
  const campoPrecioId = numericas.some((columna) => columna.id === borrador.campoPrecioId)
    ? borrador.campoPrecioId
    : (sugerida?.id ?? '');
  const tabla = tablas.find((item) => item.id === borrador.tablaId);

  const error = (clave: string) => errores[clave] ?? servidor?.campos[clave];
  const cambiar = (cambios: Partial<Borrador>) => {
    setBorrador((actual) => ({ ...actual, ...cambios }));
    setSinGuardar(true);
    setGuardada(false);
  };
  const cambiarLinea = (clave: string, cambios: Partial<LineaBorrador>) =>
    cambiar({
      lineas: borrador.lineas.map((linea) =>
        linea.clave === clave ? { ...linea, ...cambios } : linea,
      ),
    });

  function enfocarPrimerError() {
    requestAnimationFrame(() =>
      formulario.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
    );
  }

  function enviar(event: FormEvent) {
    event.preventDefault();
    const nuevos: Record<string, string> = {};
    if (borrador.lineas.length > 0 && !campoPrecioId)
      nuevos.campoPrecioId = 'Elegí la columna con el precio de los insumos.';
    borrador.lineas.forEach((linea, indice) => {
      const clave = `lineas[${indice}]`;
      if (!linea.insumoId) nuevos[clave] = 'Elegí el insumo.';
      else if (!NUMERO.test(linea.cantidad.trim()))
        nuevos[clave] = 'Escribí la cantidad, por ejemplo 2 o 0,5.';
      else if (Number(linea.cantidad.replace(',', '.')) <= 0)
        nuevos[clave] = 'La cantidad tiene que ser mayor a cero.';
      else if (linea.unidad.trim().length > MAX_UNIDAD_RECETA)
        nuevos[clave] = `Usá hasta ${MAX_UNIDAD_RECETA} caracteres en la unidad.`;
    });
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) {
      enfocarPrimerError();
      return;
    }
    guardar.mutate(
      {
        campoPrecioId: borrador.lineas.length > 0 ? campoPrecioId : null,
        lineas: borrador.lineas.map((linea) => ({
          insumoId: linea.insumoId,
          cantidad: Number(linea.cantidad.trim().replace(',', '.')),
          unidad: linea.unidad.trim() || null,
        })),
      },
      {
        onSuccess: () => {
          setSinGuardar(false);
          setGuardada(true);
        },
        onError: enfocarPrimerError,
      },
    );
  }

  const guardadas = new Map(receta.lineas.map((linea) => [linea.insumoId, linea]));

  return (
    <form
      ref={formulario}
      onSubmit={enviar}
      noValidate
      aria-label={`Receta de ${producto}`}
      className="flex flex-col gap-4"
    >
      <p className="m-0 text-hint text-soft">
        Elegí los insumos que lleva «{producto}» y cuánto de cada uno. El costo se calcula con el
        precio actual de cada insumo.
      </p>

      <SelectField
        label="¿En qué tabla están los insumos?"
        value={borrador.tablaId}
        onChange={(event) =>
          cambiar({
            tablaId: event.target.value,
            campoPrecioId: '',
            lineas: borrador.lineas.map((linea) => ({ ...linea, insumoId: '' })),
          })
        }
        disabled={guardar.isPending}
      >
        {tablas.map((item) => (
          <option key={item.id} value={item.id}>
            {item.nombre}
          </option>
        ))}
      </SelectField>

      <SelectField
        label="¿Qué columna tiene el precio?"
        hint={
          columnas.isSuccess && numericas.length === 0
            ? 'Esa tabla no tiene columnas numéricas. Agregá una con el precio en Armar.'
            : 'El precio de cada insumo sale de esta columna.'
        }
        value={campoPrecioId}
        onChange={(event) => cambiar({ campoPrecioId: event.target.value })}
        error={error('campoPrecioId')}
        disabled={guardar.isPending || !columnas.isSuccess || numericas.length === 0}
      >
        <option value="">
          {columnas.isPending ? 'Cargando las columnas…' : 'Elegí una columna'}
        </option>
        {numericas.map((columna) => (
          <option key={columna.id} value={columna.id}>
            {columna.nombre}
          </option>
        ))}
      </SelectField>

      <ul aria-label="Insumos de la receta" className="m-0 flex list-none flex-col gap-4 p-0">
        {borrador.lineas.map((linea, indice) => (
          <li
            key={linea.clave}
            className="flex flex-col gap-3 rounded-lg border border-line bg-canvas p-3"
          >
            <SelectorRelacion
              campo={columnaInsumo(indice, borrador.tablaId)}
              nombreTabla={tabla?.nombre ?? 'la tabla'}
              valor={linea.insumoId}
              onChange={(insumoId) => cambiarLinea(linea.clave, { insumoId })}
              error={error(`lineas[${indice}]`)}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <InputField
                label={`Cantidad del insumo ${indice + 1}`}
                value={linea.cantidad}
                onChange={(event) => cambiarLinea(linea.clave, { cantidad: event.target.value })}
                inputMode="decimal"
                autoComplete="off"
                disabled={guardar.isPending}
              />
              <InputField
                label={`Unidad del insumo ${indice + 1} (opcional)`}
                placeholder="kg, lt, u"
                value={linea.unidad}
                onChange={(event) => cambiarLinea(linea.clave, { unidad: event.target.value })}
                maxLength={MAX_UNIDAD_RECETA}
                autoComplete="off"
                disabled={guardar.isPending}
              />
            </div>
            <ResumenLinea linea={guardadas.get(linea.insumoId)} />
            <Button
              className="self-start"
              variant="ghost"
              aria-label={`Quitar el insumo ${indice + 1}`}
              disabled={guardar.isPending}
              onClick={() =>
                cambiar({ lineas: borrador.lineas.filter((item) => item.clave !== linea.clave) })
              }
            >
              <XIcon aria-hidden size={20} />
              Quitar
            </Button>
          </li>
        ))}
      </ul>

      <Button
        className="self-start"
        disabled={guardar.isPending || borrador.lineas.length >= MAX_LINEAS_RECETA}
        onClick={() =>
          cambiar({
            lineas: [
              ...borrador.lineas,
              { clave: nuevaClave(), insumoId: '', cantidad: '', unidad: '' },
            ],
          })
        }
      >
        <PlusIcon aria-hidden size={20} />
        Agregar insumo
      </Button>

      {servidor && Object.keys(servidor.campos).length === 0 && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {servidor.mensaje}
        </p>
      )}

      <div
        aria-labelledby={`${id}-costo`}
        className={cn(
          'flex flex-col gap-1 rounded-lg border p-4',
          receta.incompleta ? 'border-bad/40 bg-coral-100' : 'border-line bg-accent-100',
        )}
      >
        <span id={`${id}-costo`} className="text-hint text-soft">
          Costo de la receta
        </span>
        <span className="text-h2 font-bold">{formatearDinero(receta.costo)}</span>
        {receta.lineas.length === 0 && (
          <span className="text-hint text-soft">Todavía no hay una receta guardada.</span>
        )}
        {receta.incompleta && (
          <span role="alert" className="text-hint font-semibold text-bad">
            Falta el precio de algún insumo (o ya no existe): este costo no está completo.
          </span>
        )}
        {sinGuardar && (
          <span className="text-hint text-soft">
            Hay cambios sin guardar: el costo es el de la receta guardada.
          </span>
        )}
      </div>

      {guardada && (
        <p role="status" className="m-0 font-semibold text-ok">
          Receta guardada.
        </p>
      )}

      <Button type="submit" variant="solid" loading={guardar.isPending} className="self-start">
        {guardar.isPending ? 'Guardando…' : 'Guardar receta'}
      </Button>
    </form>
  );
}

// El insumo se elige con el mismo buscador que una columna que se conecta con la tabla de los insumos.
function columnaInsumo(indice: number, tablaId: string): Campo {
  return {
    id: `insumo-${indice}`,
    nombre: `Insumo ${indice + 1}`,
    tipo: 'RELACION',
    obligatorio: true,
    valorInicial: null,
    opciones: [],
    bloqueRelacionadoId: tablaId,
    formula: null,
    posicion: indice,
  };
}

function ResumenLinea({ linea }: { linea: LineaReceta | undefined }) {
  if (!linea) return null;
  if (linea.precioUnitario === null || linea.subtotal === null) {
    return (
      <p className="m-0 text-hint font-semibold text-bad">
        {linea.insumo === 'Insumo eliminado'
          ? 'Este insumo ya no existe.'
          : `A «${linea.insumo}» le falta el precio.`}
      </p>
    );
  }
  const unidad = linea.unidad ? ` ${linea.unidad}` : '';
  return (
    <p className="m-0 text-hint text-soft">
      {formatearCantidad(linea.cantidad)}
      {unidad} × {formatearDinero(linea.precioUnitario)} ={' '}
      <strong className="text-fg">{formatearDinero(linea.subtotal)}</strong>
    </p>
  );
}
