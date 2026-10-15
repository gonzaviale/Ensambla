import { useRef, useState, type FormEvent } from 'react';
import { Button, InputField, SelectField } from '@/components/ui';
import { enfocarPrimerError } from '@/components/Cuenta/enfocarPrimerError';
import { precioSegunRegla } from '@/lib/red/calculos';
import { pesos } from '@/lib/red/formato';

export type DatosReglas = { margen: number; redondeo: number; margenMinimo: number };

type PasoReglasProps = {
  rubro: string | null;
  /** Punto de partida: lo sugerido para el rubro. */
  inicial: DatosReglas;
  sugerido: DatosReglas;
  guardando?: boolean;
  onVolver: () => void;
  onSeguir: (reglas: DatosReglas) => void;
};

const COSTO_DE_EJEMPLO = 1000;
const OPCIONES_REDONDEO = [
  { valor: 1, texto: 'No redondear' },
  { valor: 10, texto: 'A los $ 10' },
  { valor: 50, texto: 'A los $ 50' },
  { valor: 100, texto: 'A los $ 100' },
];

function numero(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.');
  if (limpio === '') return null;
  const valor = Number(limpio);
  return Number.isFinite(valor) ? valor : null;
}

// Paso 2: «Tus reglas de precio». Un punto de partida por rubro, explicado en una línea cada uno.
export function PasoReglas({
  rubro,
  inicial,
  sugerido,
  guardando,
  onVolver,
  onSeguir,
}: PasoReglasProps) {
  const formulario = useRef<HTMLFormElement>(null);
  const [margen, setMargen] = useState(String(inicial.margen));
  const [redondeo, setRedondeo] = useState(String(inicial.redondeo));
  const [minimo, setMinimo] = useState(String(inicial.margenMinimo));
  const [errores, setErrores] = useState<{ margen?: string; minimo?: string }>({});

  const margenNumero = numero(margen);
  const ejemplo =
    margenNumero !== null && margenNumero >= 0
      ? precioSegunRegla(COSTO_DE_EJEMPLO, {
          margen: margenNumero,
          redondeo: Number(redondeo) || 1,
        })
      : null;

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const m = numero(margen);
    const min = numero(minimo);
    const nuevos: typeof errores = {};
    if (m === null || m < 1 || m > 300) nuevos.margen = 'Poné un número entre 1 y 300.';
    if (min === null || min < 0 || min > 300) nuevos.minimo = 'Poné un número entre 0 y 300.';
    else if (m !== null && min > m) nuevos.minimo = 'No puede ser mayor que tu ganancia general.';
    setErrores(nuevos);
    if (nuevos.margen || nuevos.minimo || m === null || min === null) {
      enfocarPrimerError(formulario.current);
      return;
    }
    onSeguir({ margen: m, redondeo: Number(redondeo) || 1, margenMinimo: min });
  }

  return (
    <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Tus reglas de precio</h2>
      <p className="m-0 text-body text-soft">
        Con esto armamos el precio nuevo cuando sube un costo. Nunca cambia nada sin que vos
        confirmes.
      </p>
      <InputField
        label="Ganancia general (%)"
        type="number"
        inputMode="decimal"
        min={1}
        max={300}
        value={margen}
        hint={
          <>
            Lo que sumás al costo para llegar al precio.{' '}
            {rubro
              ? `Para ${rubro === 'Otro' ? 'tu rubro' : `un comercio de ${rubro.toLowerCase()}`} solemos sugerir ${sugerido.margen} %.`
              : `Solemos sugerir ${sugerido.margen} %.`}
            {ejemplo !== null && (
              <>
                {' '}
                Ejemplo: algo que te cuesta {pesos(COSTO_DE_EJEMPLO)} sale {pesos(ejemplo)}.
              </>
            )}
          </>
        }
        error={errores.margen}
        onChange={(evento) => {
          setErrores((actuales) => ({ ...actuales, margen: undefined }));
          setMargen(evento.target.value);
        }}
      />
      <SelectField
        label="Redondeo del precio"
        hint="Para que el precio quede redondo y fácil de dar el vuelto."
        value={redondeo}
        onChange={(evento) => setRedondeo(evento.target.value)}
      >
        {OPCIONES_REDONDEO.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.texto}
          </option>
        ))}
      </SelectField>
      <InputField
        label="Ganancia mínima (%)"
        type="number"
        inputMode="decimal"
        min={0}
        max={300}
        value={minimo}
        hint={`Si un precio te deja menos que esto, te avisamos. Sugerido: ${sugerido.margenMinimo} %.`}
        error={errores.minimo}
        onChange={(evento) => {
          setErrores((actuales) => ({ ...actuales, minimo: undefined }));
          setMinimo(evento.target.value);
        }}
      />
      <div className="flex flex-col gap-3">
        <Button type="submit" variant="solid" size="lg" fullWidth loading={guardando}>
          Guardar y seguir
        </Button>
        <Button variant="ghost" onClick={onVolver} disabled={guardando}>
          Volver
        </Button>
      </div>
    </form>
  );
}
