import { useId, useRef, useState, type FormEvent } from 'react';
import { ArrowDownIcon, ArrowUpIcon } from '@phosphor-icons/react';
import { controlOpcion, tarjetaOpcion } from '@/components/Constructor/opcionStyles';
import { SelectorRelacion } from '@/components/FormRenderer/SelectorRelacion';
import { Button, InputField, Toast } from '@/components/ui';
import { leerErrorServidor, ubicarErrores } from '@/hooks/errores';
import { useRegistrarMovimiento } from '@/hooks/stock/mutations';
import { cn } from '@/lib/cn';
import { formatearCantidad } from '@/lib/valores';
import type { Bloque, Campo } from '@/types/proyecto';
import { MAX_NOTA_MOVIMIENTO, type TipoMovimiento } from '@/types/stock';

type Props = {
  bloque: Bloque;
};

const INPUTS = ['registroId', 'tipo', 'cantidad', 'fecha', 'nota'] as const;
type Input = (typeof INPUTS)[number];

const TIPOS: { tipo: TipoMovimiento; texto: string; ayuda: string; icono: typeof ArrowUpIcon }[] = [
  {
    tipo: 'ENTRADA',
    texto: 'Entrada',
    ayuda: 'Suma al stock: una compra o una producción.',
    icono: ArrowDownIcon,
  },
  {
    tipo: 'SALIDA',
    texto: 'Salida',
    ayuda: 'Resta del stock: una venta o un consumo.',
    icono: ArrowUpIcon,
  },
];

const hoy = () => {
  const ahora = new Date();
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${String(ahora.getDate()).padStart(2, '0')}`;
};

const NUMERO = /^\d+([.,]\d+)?$/;

// Formulario simple de entrada o salida de un producto (una fila de la tabla elegida). El producto se
// busca y se elige como en una columna conectada; el servidor valida que la salida no deje stock negativo.
export function FormularioMovimiento({ bloque }: Props) {
  const id = useId();
  const formulario = useRef<HTMLFormElement>(null);
  const [registroId, setRegistroId] = useState('');
  const [tipo, setTipo] = useState<TipoMovimiento>('ENTRADA');
  const [cantidad, setCantidad] = useState('');
  const [fecha, setFecha] = useState(hoy);
  const [nota, setNota] = useState('');
  const [errores, setErrores] = useState<Partial<Record<Input, string>>>({});
  const [aviso, setAviso] = useState<string>();
  const registrar = useRegistrarMovimiento(bloque.id);
  const servidor = ubicarErrores(leerErrorServidor(registrar.error), INPUTS);
  const error = (input: Input) => errores[input] ?? servidor.porInput[input];

  // El producto se elige con el mismo buscador que una columna que se conecta con esta tabla.
  const columnaProducto: Campo = {
    id: `${id}-producto`,
    nombre: 'Producto',
    tipo: 'RELACION',
    obligatorio: true,
    valorInicial: null,
    opciones: [],
    bloqueRelacionadoId: bloque.id,
    formula: null,
    posicion: 0,
  };

  function enfocarPrimerError() {
    requestAnimationFrame(() =>
      formulario.current
        ?.querySelector<HTMLElement>('[aria-invalid="true"], [aria-describedby*="-error"] input')
        ?.focus(),
    );
  }

  function enviar(event: FormEvent) {
    event.preventDefault();
    const nuevos: Partial<Record<Input, string>> = {};
    if (!registroId) nuevos.registroId = 'Elegí el producto.';
    if (!NUMERO.test(cantidad.trim()))
      nuevos.cantidad = 'Escribí la cantidad, por ejemplo 5 o 2,5.';
    else if (Number(cantidad.replace(',', '.')) <= 0)
      nuevos.cantidad = 'Tiene que ser mayor a cero.';
    if (nota.length > MAX_NOTA_MOVIMIENTO)
      nuevos.nota = `Usá hasta ${MAX_NOTA_MOVIMIENTO} caracteres.`;
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) {
      enfocarPrimerError();
      return;
    }
    registrar.mutate(
      {
        registroId,
        tipo,
        cantidad: Number(cantidad.trim().replace(',', '.')),
        fecha,
        nota: nota.trim() || null,
      },
      {
        onSuccess: ({ movimiento, stockActual }) => {
          setAviso(
            `${movimiento.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'} registrada: ${movimiento.producto}. ` +
              `Ahora hay ${formatearCantidad(stockActual)}.`,
          );
          setRegistroId('');
          setCantidad('');
          setNota('');
        },
        onError: enfocarPrimerError,
      },
    );
  }

  return (
    <form
      ref={formulario}
      onSubmit={enviar}
      noValidate
      aria-label="Registrar un movimiento"
      className="flex flex-col gap-5"
    >
      <SelectorRelacion
        campo={columnaProducto}
        nombreTabla={bloque.nombre}
        valor={registroId}
        onChange={(valor) => {
          setRegistroId(valor);
          setAviso(undefined);
        }}
        error={error('registroId')}
      />

      <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
        <legend className="mb-1 p-0 text-base font-semibold">¿Qué pasó?</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {TIPOS.map((opcion) => {
            const Icono = opcion.icono;
            return (
              <label key={opcion.tipo} className={cn(tarjetaOpcion, 'items-center')}>
                <input
                  type="radio"
                  name={`${id}-tipo`}
                  value={opcion.tipo}
                  checked={tipo === opcion.tipo}
                  onChange={() => setTipo(opcion.tipo)}
                  disabled={registrar.isPending}
                  className={cn(controlOpcion, 'mt-0')}
                />
                <span className="flex min-w-0 flex-col">
                  <span className="flex items-center gap-2 font-semibold">
                    <Icono aria-hidden size={20} />
                    {opcion.texto}
                  </span>
                  <span className="text-hint text-soft">{opcion.ayuda}</span>
                </span>
              </label>
            );
          })}
        </div>
        {error('tipo') && <p className="m-0 text-hint font-semibold text-bad">{error('tipo')}</p>}
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-2">
        <InputField
          label="Cantidad"
          hint="Podés usar coma o punto para los decimales, por ejemplo 2,5."
          value={cantidad}
          onChange={(event) => setCantidad(event.target.value)}
          error={error('cantidad')}
          inputMode="decimal"
          autoComplete="off"
          disabled={registrar.isPending}
        />
        <InputField
          label="Fecha"
          type="date"
          value={fecha}
          max={hoy()}
          onChange={(event) => setFecha(event.target.value)}
          error={error('fecha')}
          disabled={registrar.isPending}
        />
      </div>

      <InputField
        label="Nota (opcional)"
        hint="Por ejemplo, el proveedor o el motivo."
        value={nota}
        onChange={(event) => setNota(event.target.value)}
        error={error('nota')}
        maxLength={MAX_NOTA_MOVIMIENTO}
        autoComplete="off"
        disabled={registrar.isPending}
      />

      {servidor.general && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {servidor.general}
        </p>
      )}

      <div className="flex justify-end">
        <Button type="submit" variant="solid" size="lg" loading={registrar.isPending}>
          {registrar.isPending ? 'Guardando…' : 'Registrar movimiento'}
        </Button>
      </div>
      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(undefined)} />}
    </form>
  );
}
