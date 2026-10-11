import { useId, useRef, useState, type FormEvent } from 'react';
import { PlusIcon, XIcon } from '@phosphor-icons/react';
import { Button, InputField, SelectField } from '@/components/ui';
import { useCrearCampo, useEditarCampo } from '@/hooks/bloques/mutations';
import { leerErrorServidor, ubicarErrores } from '@/hooks/errores';
import { prepararCampo, type BorradorCampo, type ErroresCampo } from '@/lib/campos';
import { cn } from '@/lib/cn';
import { MAX_NOMBRE_CAMPO, TIPOS_CAMPO, type Campo, type TipoCampo } from '@/types/campo';
import { TIPOS_DE_DATO } from './tiposDeDato';

type Props = {
  proyectoId: string;
  bloqueId: string;
  /** Si viene, se edita; si no, se crea un dato nuevo al final del bloque. */
  campo?: Campo;
  onListo: () => void;
  onCancelar: () => void;
};

const INPUTS = ['nombre', 'tipo', 'opciones', 'valorInicial'] as const;

function borradorDe(campo?: Campo): BorradorCampo {
  if (!campo) {
    return { nombre: '', tipo: 'TEXTO', obligatorio: false, valorInicial: '', opciones: [] };
  }
  const valor = campo.valorInicial ?? '';
  return {
    nombre: campo.nombre,
    tipo: campo.tipo,
    obligatorio: campo.obligatorio,
    valorInicial: campo.tipo === 'NUMERO' ? valor.replace('.', ',') : valor,
    opciones: campo.opciones.length > 0 ? [...campo.opciones] : [],
  };
}

const tarjeta =
  'flex cursor-pointer items-start gap-3 rounded-md border-2 border-line bg-surface p-3 hover:border-accent-700 has-checked:border-accent-700 has-checked:bg-accent-100 has-focus-visible:outline-2 has-focus-visible:outline-accent-700';
const control = 'mt-0.5 size-5 min-h-0 shrink-0 cursor-pointer accent-accent-700';

export function CampoForm({ proyectoId, bloqueId, campo, onListo, onCancelar }: Props) {
  const id = useId();
  const formulario = useRef<HTMLFormElement>(null);
  const [borrador, setBorrador] = useState(() => borradorDe(campo));
  const [errores, setErrores] = useState<ErroresCampo>({});
  const crear = useCrearCampo(proyectoId, bloqueId);
  const editar = useEditarCampo(proyectoId, bloqueId);
  const guardado = campo ? editar : crear;
  const ocupado = guardado.isPending;
  const servidor = ubicarErrores(leerErrorServidor(guardado.error), INPUTS);
  const error = (input: (typeof INPUTS)[number]) => errores[input] ?? servidor.porInput[input];

  const cambiar = (cambios: Partial<BorradorCampo>) =>
    setBorrador((actual) => ({ ...actual, ...cambios }));

  function cambiarTipo(tipo: TipoCampo) {
    // El valor inicial depende del tipo: al cambiarlo se vacía para no guardar algo inválido.
    cambiar({
      tipo,
      valorInicial: '',
      ...(tipo === 'OPCION' && borrador.opciones.length === 0 ? { opciones: ['', ''] } : {}),
    });
    setErrores((actual) => ({ ...actual, valorInicial: undefined, opciones: undefined }));
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
  const titulo = campo ? `Cambiar el dato «${campo.nombre}»` : 'Nuevo dato';

  return (
    <form
      ref={formulario}
      onSubmit={enviar}
      noValidate
      aria-labelledby={`${id}-titulo`}
      className="flex flex-col gap-5 rounded-lg border-2 border-accent-200 bg-surface p-4 sm:p-5"
      onKeyDown={(event) => {
        if (event.key === 'Escape' && !ocupado) onCancelar();
      }}
    >
      <h3 id={`${id}-titulo`} className="m-0 text-lg">
        {titulo}
      </h3>

      <InputField
        label="Nombre del dato"
        hint="Es el título de la columna, por ejemplo Precio, Teléfono o Fecha de entrega."
        value={borrador.nombre}
        onChange={(event) => cambiar({ nombre: event.target.value })}
        error={error('nombre')}
        maxLength={MAX_NOMBRE_CAMPO}
        disabled={ocupado}
        autoComplete="off"
        autoFocus
        className="text-base"
      />

      <fieldset
        className="m-0 flex flex-col gap-2 border-0 p-0"
        aria-describedby={error('tipo') ? `${id}-tipo-error` : undefined}
      >
        <legend className="mb-2 p-0 font-semibold">¿Qué tipo de dato es?</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {TIPOS_CAMPO.map((tipo) => (
            <label key={tipo} className={tarjeta}>
              <input
                type="radio"
                name={`${id}-tipo`}
                value={tipo}
                checked={borrador.tipo === tipo}
                onChange={() => cambiarTipo(tipo)}
                disabled={ocupado}
                aria-labelledby={`${id}-${tipo}`}
                aria-describedby={`${id}-${tipo}-ayuda`}
                className={control}
              />
              <span className="flex flex-col">
                <span id={`${id}-${tipo}`} className="font-semibold">
                  {TIPOS_DE_DATO[tipo].nombre}
                </span>
                <span id={`${id}-${tipo}-ayuda`} className="text-hint text-soft">
                  {TIPOS_DE_DATO[tipo].ayuda}
                </span>
              </span>
            </label>
          ))}
        </div>
        {error('tipo') && (
          <p id={`${id}-tipo-error`} className="m-0 text-hint text-bad">
            {error('tipo')}
          </p>
        )}
      </fieldset>

      {borrador.tipo === 'OPCION' && (
        <fieldset
          className="m-0 flex flex-col gap-3 rounded-md border border-line p-3"
          aria-describedby={cn(`${id}-opciones-ayuda`, error('opciones') && `${id}-opciones-error`)}
        >
          <legend className="px-1 font-semibold">Opciones de la lista</legend>
          <p id={`${id}-opciones-ayuda`} className="m-0 text-hint text-soft">
            Escribí una opción en cada renglón. Los renglones vacíos no se guardan.
          </p>
          {borrador.opciones.map((opcion, indice) => (
            <div key={indice} className="flex items-end gap-2">
              <div className="min-w-0 flex-1">
                <InputField
                  label={`Opción ${indice + 1}`}
                  value={opcion}
                  onChange={(event) =>
                    cambiar({
                      opciones: borrador.opciones.map((actual, i) =>
                        i === indice ? event.target.value : actual,
                      ),
                    })
                  }
                  aria-invalid={error('opciones') ? true : undefined}
                  disabled={ocupado}
                  autoComplete="off"
                />
              </div>
              <Button
                variant="ghost"
                aria-label={`Quitar opción ${indice + 1}`}
                disabled={ocupado || borrador.opciones.length === 1}
                onClick={() =>
                  cambiar({ opciones: borrador.opciones.filter((_, i) => i !== indice) })
                }
              >
                <XIcon aria-hidden size={18} />
              </Button>
            </div>
          ))}
          <Button
            className="self-start"
            disabled={ocupado}
            onClick={() => cambiar({ opciones: [...borrador.opciones, ''] })}
          >
            <PlusIcon aria-hidden size={18} />
            Agregar otra opción
          </Button>
          {error('opciones') && (
            <p id={`${id}-opciones-error`} className="m-0 text-hint text-bad">
              {error('opciones')}
            </p>
          )}
        </fieldset>
      )}

      <ValorInicial
        id={id}
        tipo={borrador.tipo}
        valor={borrador.valorInicial}
        opciones={opcionesElegibles}
        error={error('valorInicial')}
        ocupado={ocupado}
        onCambiar={(valorInicial) => cambiar({ valorInicial })}
      />

      <label className={tarjeta}>
        <input
          type="checkbox"
          checked={borrador.obligatorio}
          onChange={(event) => cambiar({ obligatorio: event.target.checked })}
          disabled={ocupado}
          aria-labelledby={`${id}-obligatorio`}
          aria-describedby={`${id}-obligatorio-ayuda`}
          className={control}
        />
        <span className="flex flex-col">
          <span id={`${id}-obligatorio`} className="font-semibold">
            Es obligatorio
          </span>
          <span id={`${id}-obligatorio-ayuda`} className="text-hint text-soft">
            No se va a poder guardar un registro sin completar este dato.
          </span>
        </span>
      </label>

      {servidor.general && (
        <p role="alert" className="m-0 text-bad">
          {servidor.general}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <Button type="submit" variant="solid" className="text-base" disabled={ocupado}>
          {ocupado ? 'Guardando…' : 'Guardar dato'}
        </Button>
        <Button className="text-base" onClick={onCancelar} disabled={ocupado}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}

type ValorInicialProps = {
  id: string;
  tipo: TipoCampo;
  valor: string;
  opciones: string[];
  error?: string;
  ocupado: boolean;
  onCambiar: (valor: string) => void;
};

const AYUDA_VALOR = 'Opcional. Se completa solo en cada registro nuevo y se puede cambiar.';

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
        className="m-0 flex flex-col gap-2 border-0 p-0"
        aria-describedby={cn(`${id}-valor-ayuda`, error && `${id}-valor-error`)}
      >
        <legend className="mb-1 p-0 text-hint text-soft">{etiqueta}</legend>
        <div className="flex flex-wrap gap-2">
          {elecciones.map((eleccion) => (
            <label key={eleccion.valor} className={cn(tarjeta, 'items-center py-2')}>
              <input
                type="radio"
                name={`${id}-valor`}
                value={eleccion.valor}
                checked={valor === eleccion.valor}
                onChange={() => onCambiar(eleccion.valor)}
                disabled={ocupado}
                className={cn(control, 'mt-0')}
              />
              {eleccion.texto}
            </label>
          ))}
        </div>
        <p id={`${id}-valor-ayuda`} className="m-0 text-caption text-muted">
          {AYUDA_VALOR}
        </p>
        {error && (
          <p id={`${id}-valor-error`} className="m-0 text-hint text-bad">
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
      hint: 'Opcional. Usá coma para los decimales, por ejemplo 12,5.',
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
