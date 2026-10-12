import { useId, useRef, useState, type FormEvent } from 'react';
import { Button, InputField, SelectField } from '@/components/ui';
import { useCrearCampo, useEditarCampo } from '@/hooks/bloques/mutations';
import { useBloques } from '@/hooks/bloques/queries';
import { leerErrorServidor, ubicarErrores } from '@/hooks/errores';
import { prepararCampo, type BorradorCampo, type ErroresCampo } from '@/lib/campos';
import { cn } from '@/lib/cn';
import { MAX_NOMBRE_CAMPO, type Campo, type TipoCampo } from '@/types/campo';
import { controlOpcion, tarjetaOpcion } from './opcionStyles';
import { OpcionesEditor } from './OpcionesEditor';
import { TipoDeDatoCards } from './TipoDeDatoCards';

type Props = {
  proyectoId: string;
  bloqueId: string;
  /** Si viene, se edita; si no, se crea una columna nueva al final de la tabla. */
  campo?: Campo;
  onListo: () => void;
  onCancelar: () => void;
};

const INPUTS = ['nombre', 'tipo', 'opciones', 'bloqueRelacionadoId', 'valorInicial'] as const;

function borradorDe(campo?: Campo): BorradorCampo {
  if (!campo) {
    return {
      nombre: '',
      tipo: 'TEXTO',
      obligatorio: false,
      valorInicial: '',
      opciones: [],
      bloqueRelacionadoId: '',
    };
  }
  const valor = campo.valorInicial ?? '';
  return {
    nombre: campo.nombre,
    tipo: campo.tipo,
    obligatorio: campo.obligatorio,
    valorInicial: campo.tipo === 'NUMERO' ? valor.replace('.', ',') : valor,
    opciones: campo.opciones.length > 0 ? [...campo.opciones] : [],
    bloqueRelacionadoId: campo.bloqueRelacionadoId ?? '',
  };
}

// Formulario de una columna. Vive dentro de una hoja (Sheet) abierta desde la lista de columnas.
export function CampoForm({ proyectoId, bloqueId, campo, onListo, onCancelar }: Props) {
  const id = useId();
  const formulario = useRef<HTMLFormElement>(null);
  const [borrador, setBorrador] = useState(() => borradorDe(campo));
  const [errores, setErrores] = useState<ErroresCampo>({});
  const crear = useCrearCampo(proyectoId, bloqueId);
  const editar = useEditarCampo(proyectoId, bloqueId);
  // Las tablas del sistema, para elegir con cuál se conecta una columna (la lista ya está en caché).
  const tablas = useBloques(proyectoId);
  const guardado = campo ? editar : crear;
  const ocupado = guardado.isPending;
  const servidor = ubicarErrores(leerErrorServidor(guardado.error), INPUTS);
  const error = (input: (typeof INPUTS)[number]) => errores[input] ?? servidor.porInput[input];

  const cambiar = (cambios: Partial<BorradorCampo>) =>
    setBorrador((actual) => ({ ...actual, ...cambios }));

  function cambiarTipo(tipo: TipoCampo) {
    // El valor inicial y la tabla conectada dependen del tipo: al cambiarlo se vacían para no
    // guardar algo inválido.
    cambiar({
      tipo,
      valorInicial: '',
      bloqueRelacionadoId: '',
      ...(tipo === 'OPCION' && borrador.opciones.length === 0 ? { opciones: ['', ''] } : {}),
    });
    setErrores((actual) => ({
      ...actual,
      valorInicial: undefined,
      opciones: undefined,
      bloqueRelacionadoId: undefined,
    }));
  }

  function enfocarPrimerError() {
    requestAnimationFrame(() =>
      formulario.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
    );
  }

  function enviar(event: FormEvent) {
    event.preventDefault();
    const preparado = prepararCampo(borrador);
    if (!preparado.ok) {
      setErrores(preparado.errores);
      enfocarPrimerError();
      return;
    }
    setErrores({});
    const opciones = { onSuccess: onListo, onError: enfocarPrimerError };
    if (campo) editar.mutate({ campoId: campo.id, datos: preparado.datos }, opciones);
    else crear.mutate(preparado.datos, opciones);
  }

  const opcionesElegibles = [
    ...new Set(borrador.opciones.map((opcion) => opcion.trim()).filter(Boolean)),
  ];
  const titulo = campo ? `Cambiar la columna «${campo.nombre}»` : 'Nueva columna';

  return (
    <form
      ref={formulario}
      onSubmit={enviar}
      noValidate
      aria-label={titulo}
      className="flex flex-col gap-5"
    >
      <InputField
        label="Nombre de la columna"
        hint="Es el título de la columna, por ejemplo Precio, Teléfono o Fecha de entrega."
        value={borrador.nombre}
        onChange={(event) => cambiar({ nombre: event.target.value })}
        error={error('nombre')}
        maxLength={MAX_NOMBRE_CAMPO}
        disabled={ocupado}
        autoComplete="off"
        autoFocus
        data-autofocus
      />

      <TipoDeDatoCards
        valor={borrador.tipo}
        onCambiar={cambiarTipo}
        disabled={ocupado}
        error={error('tipo')}
        unaColumna
      />

      {borrador.tipo === 'OPCION' && (
        <OpcionesEditor
          opciones={borrador.opciones}
          onCambiar={(opciones) => cambiar({ opciones })}
          disabled={ocupado}
          error={error('opciones')}
        />
      )}

      {borrador.tipo === 'RELACION' ? (
        <SelectField
          label="¿Con qué tabla se conecta?"
          hint={
            tablas.isError
              ? 'No pudimos cargar las tablas de este sistema. Cerrá y probá de nuevo.'
              : 'Cada fila va a elegir una fila de esa tabla.'
          }
          value={borrador.bloqueRelacionadoId}
          onChange={(event) => cambiar({ bloqueRelacionadoId: event.target.value })}
          error={error('bloqueRelacionadoId')}
          disabled={ocupado || !tablas.isSuccess}
        >
          <option value="">{tablas.isPending ? 'Cargando las tablas…' : 'Elegí una tabla'}</option>
          {[...(tablas.data ?? [])]
            .sort((a, b) => a.posicion - b.posicion)
            .map((tabla) => (
              <option key={tabla.id} value={tabla.id}>
                {tabla.id === bloqueId ? `${tabla.nombre} (esta misma tabla)` : tabla.nombre}
              </option>
            ))}
        </SelectField>
      ) : (
        <ValorInicial
          id={id}
          tipo={borrador.tipo}
          valor={borrador.valorInicial}
          opciones={opcionesElegibles}
          error={error('valorInicial')}
          ocupado={ocupado}
          onCambiar={(valorInicial) => cambiar({ valorInicial })}
        />
      )}

      <label className={tarjetaOpcion}>
        <input
          type="checkbox"
          checked={borrador.obligatorio}
          onChange={(event) => cambiar({ obligatorio: event.target.checked })}
          disabled={ocupado}
          aria-labelledby={`${id}-obligatorio`}
          aria-describedby={`${id}-obligatorio-ayuda`}
          className={controlOpcion}
        />
        <span className="flex flex-col">
          <span id={`${id}-obligatorio`} className="font-semibold">
            Es obligatorio
          </span>
          <span id={`${id}-obligatorio-ayuda`} className="text-hint text-soft">
            No se va a poder guardar una fila sin completar esta columna.
          </span>
        </span>
      </label>

      {servidor.general && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {servidor.general}
        </p>
      )}

      <div className="sticky bottom-0 -mx-5 -mb-4 flex flex-wrap justify-end gap-3 border-t border-line bg-surface px-5 py-3">
        <Button onClick={onCancelar} disabled={ocupado}>
          Cancelar
        </Button>
        <Button type="submit" variant="solid" loading={ocupado}>
          {ocupado ? 'Guardando…' : 'Guardar columna'}
        </Button>
      </div>
    </form>
  );
}

type ValorInicialProps = {
  id: string;
  tipo: Exclude<TipoCampo, 'RELACION'>;
  valor: string;
  opciones: string[];
  error?: string;
  ocupado: boolean;
  onCambiar: (valor: string) => void;
};

const AYUDA_VALOR = 'Opcional. Se completa solo en cada fila nueva y se puede cambiar.';

// El input del valor inicial cambia según el tipo de dato elegido.
function ValorInicial({ id, tipo, valor, opciones, error, ocupado, onCambiar }: ValorInicialProps) {
  const etiqueta = 'Valor inicial (opcional)';

  if (tipo === 'SI_NO') {
    const elecciones = [
      { valor: '', texto: 'Sin valor inicial' },
      { valor: 'true', texto: 'Sí' },
      { valor: 'false', texto: 'No' },
    ];
    return (
      <fieldset
        className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0"
        aria-describedby={cn(`${id}-valor-ayuda`, error && `${id}-valor-error`)}
      >
        <legend className="mb-1 p-0 text-base font-semibold">{etiqueta}</legend>
        <div className="flex flex-wrap gap-2">
          {elecciones.map((eleccion) => (
            <label key={eleccion.valor} className={cn(tarjetaOpcion, 'items-center py-2')}>
              <input
                type="radio"
                name={`${id}-valor`}
                value={eleccion.valor}
                checked={valor === eleccion.valor}
                onChange={() => onCambiar(eleccion.valor)}
                disabled={ocupado}
                className={cn(controlOpcion, 'mt-0')}
              />
              {eleccion.texto}
            </label>
          ))}
        </div>
        <p id={`${id}-valor-ayuda`} className="m-0 text-hint text-muted">
          {AYUDA_VALOR}
        </p>
        {error && (
          <p id={`${id}-valor-error`} className="m-0 text-hint font-semibold text-bad">
            {error}
          </p>
        )}
      </fieldset>
    );
  }

  if (tipo === 'OPCION') {
    return (
      <SelectField
        label={etiqueta}
        hint={opciones.length === 0 ? 'Primero escribí las opciones de la lista.' : AYUDA_VALOR}
        value={opciones.includes(valor) ? valor : ''}
        onChange={(event) => onCambiar(event.target.value)}
        error={error}
        disabled={ocupado || opciones.length === 0}
      >
        <option value="">Sin valor inicial</option>
        {opciones.map((opcion) => (
          <option key={opcion} value={opcion}>
            {opcion}
          </option>
        ))}
      </SelectField>
    );
  }

  const porTipo = {
    TEXTO: { type: 'text', hint: AYUDA_VALOR },
    NUMERO: {
      type: 'text',
      inputMode: 'decimal' as const,
      hint: 'Opcional. Podés usar coma o punto para los decimales, por ejemplo 12,5.',
    },
    FECHA: { type: 'date', hint: AYUDA_VALOR },
  }[tipo];

  return (
    <InputField
      label={etiqueta}
      value={valor}
      onChange={(event) => onCambiar(event.target.value)}
      error={error}
      disabled={ocupado}
      autoComplete="off"
      {...porTipo}
    />
  );
}
