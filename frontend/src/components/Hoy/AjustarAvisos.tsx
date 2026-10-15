import { useState, type FormEvent } from 'react';
import { EtiquetaPlan } from '@/components/Plan/PlanGate';
import { Button, InputField, Sheet } from '@/components/ui';
import { useAccionesRed, usePlanActual, useReglas, useUmbrales } from '@/hooks/red/useRed';
import { FUNCION_DE_ALERTA } from '@/lib/red/planes';
import type { TipoAlerta } from '@/types/red';
import { META_AVISO, ORDEN_TIPOS, type MensajeHoy } from './avisoMeta';

type AjustarAvisosProps = {
  open: boolean;
  onClose: () => void;
  avisar: (mensaje: MensajeHoy) => void;
};

type Rango = { min: number; max: number };

const RANGOS = {
  margenMinimo: { min: 0, max: 100 },
  diasDeStock: { min: 1, max: 30 },
  aumentoRaro: { min: 5, max: 100 },
  diferenciaCompra: { min: 1, max: 50 },
} satisfies Record<string, Rango>;

type Campo = keyof typeof RANGOS;

function validar(texto: string, rango: Rango): string | undefined {
  const valor = Number(texto.replace(',', '.'));
  if (texto.trim() === '' || Number.isNaN(valor)) return 'Escribí un número';
  if (valor < rango.min || valor > rango.max) {
    return `Tiene que estar entre ${rango.min} y ${rango.max}`;
  }
  return undefined;
}

// «Ajustar avisos» (doc §2): el comercio decide qué avisos recibe y a partir de cuándo. Las
// reglas de precio (margen mínimo) y los umbrales se guardan juntos, al confirmar.
export function AjustarAvisos({ open, onClose, avisar }: AjustarAvisosProps) {
  if (!open) return null;
  return <FormularioAjustes onClose={onClose} avisar={avisar} />;
}

function FormularioAjustes({ onClose, avisar }: Omit<AjustarAvisosProps, 'open'>) {
  const reglas = useReglas();
  const umbrales = useUmbrales();
  const plan = usePlanActual();
  const { guardarReglas, guardarUmbrales } = useAccionesRed();

  const [activas, setActivas] = useState(umbrales.activas);
  const [numeros, setNumeros] = useState<Record<Campo, string>>({
    margenMinimo: String(reglas.margenMinimo),
    diasDeStock: String(umbrales.diasDeStock),
    aumentoRaro: String(umbrales.aumentoRaro),
    diferenciaCompra: String(umbrales.diferenciaCompra),
  });
  const [mostrarErrores, setMostrarErrores] = useState(false);

  const errores: Partial<Record<Campo, string>> = {};
  for (const campo of Object.keys(RANGOS) as Campo[]) {
    const error = validar(numeros[campo], RANGOS[campo]);
    if (error) errores[campo] = error;
  }

  function guardar(event: FormEvent) {
    event.preventDefault();
    if (Object.keys(errores).length > 0) {
      setMostrarErrores(true);
      return;
    }
    const numero = (campo: Campo) => Number(numeros[campo].replace(',', '.'));
    guardarReglas({ ...reglas, margenMinimo: numero('margenMinimo') });
    guardarUmbrales({
      ...umbrales,
      diasDeStock: numero('diasDeStock'),
      aumentoRaro: numero('aumentoRaro'),
      diferenciaCompra: numero('diferenciaCompra'),
      activas,
    });
    avisar({ tipo: 'exito', mensaje: 'Guardamos tus ajustes. Los avisos ya se recalcularon.' });
    onClose();
  }

  const campoNumero = (campo: Campo, label: string, hint: string, sufijo: string) => (
    <InputField
      label={label}
      hint={hint}
      type="number"
      inputMode="decimal"
      min={RANGOS[campo].min}
      max={RANGOS[campo].max}
      value={numeros[campo]}
      error={mostrarErrores ? errores[campo] : undefined}
      onChange={(event) => setNumeros((actual) => ({ ...actual, [campo]: event.target.value }))}
      trailing={<span className="pr-4 text-base font-semibold text-muted">{sufijo}</span>}
    />
  );

  return (
    <Sheet
      open
      onClose={onClose}
      title="Ajustar avisos"
      description="Elegí qué avisos querés recibir y a partir de cuándo te avisamos."
      footer={
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="solid" type="submit" form="ajustar-avisos">
            Guardar ajustes
          </Button>
        </>
      }
    >
      <form id="ajustar-avisos" onSubmit={guardar} noValidate className="flex flex-col gap-6">
        <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
          <legend className="mb-2 p-0 text-base font-semibold">Qué avisos querés recibir</legend>
          {ORDEN_TIPOS.map((tipo: TipoAlerta) => {
            const meta = META_AVISO[tipo];
            const funcion = FUNCION_DE_ALERTA[tipo];
            return (
              <label
                key={tipo}
                className="flex min-h-12 cursor-pointer items-start gap-3 rounded-md border border-line px-3 py-3 hover:bg-canvas"
              >
                <input
                  type="checkbox"
                  checked={activas[tipo]}
                  onChange={(event) =>
                    setActivas((actual) => ({ ...actual, [tipo]: event.target.checked }))
                  }
                  className="mt-0.5 size-6 shrink-0 cursor-pointer accent-accent-700"
                />
                <span className="flex min-w-0 flex-col gap-0.5">
                  <span className="text-body font-semibold">{meta.etiqueta}</span>
                  <span className="text-hint text-soft">{meta.descripcion}</span>
                  {!plan.incluye(funcion) && (
                    <span className="mt-1">
                      <EtiquetaPlan funcion={funcion} />
                    </span>
                  )}
                </span>
              </label>
            );
          })}
          <p className="m-0 text-hint text-muted">
            Si un tipo de aviso lo ignorás siempre, lo apagamos solo o te preguntamos si lo querés
            seguir recibiendo. No queremos llenarte de ruido.
          </p>
        </fieldset>

        <fieldset className="m-0 flex min-w-0 flex-col gap-4 border-0 p-0">
          <legend className="mb-2 p-0 text-base font-semibold">A partir de cuándo avisamos</legend>
          {campoNumero(
            'margenMinimo',
            'Ganancia mínima sobre el costo',
            'Si un producto queda con menos que esto, te avisamos «precio en riesgo».',
            '%',
          )}
          {campoNumero(
            'diasDeStock',
            'Días de stock',
            'Te avisamos cuando lo que tenés alcanza para menos de estos días de ventas.',
            'días',
          )}
          {campoNumero(
            'aumentoRaro',
            'Aumento que te parece raro',
            'Un costo que sube más que esto de golpe se marca como «cambio raro».',
            '%',
          )}
          {campoNumero(
            'diferenciaCompra',
            'Diferencia para comprar mejor',
            'Te avisamos si otro proveedor o tu zona tienen el producto al menos tanto más barato.',
            '%',
          )}
        </fieldset>
      </form>
    </Sheet>
  );
}
