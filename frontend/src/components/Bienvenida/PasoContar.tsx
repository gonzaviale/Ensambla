import { useRef, useState, type FormEvent } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { Button, InputField, TextareaField } from '@/components/ui';
import { cn } from '@/lib/cn';
import { NOMBRE_RUBRO, RUBROS } from '@/lib/catalogo/piezas';
import type { Ensamblado, RubroId } from '@/types/negocio';

export type DatosNegocio = { nombre: string; relato: string; rubro: RubroId | null };

type PasoContarProps = {
  valores: DatosNegocio;
  entendido: Ensamblado;
  onCambiar: (cambio: Partial<DatosNegocio>) => void;
  onSeguir: () => void;
};

const EJEMPLO = 'Vendo viandas por encargo y cobro una seña';

// Paso 1: un solo dato grande, contar el negocio con palabras propias. Debajo, en vivo, lo que
// Ensambla va entendiendo (el mismo ensamblador que arma el sistema en el paso 2).
export function PasoContar({ valores, entendido, onCambiar, onSeguir }: PasoContarProps) {
  const nombre = useRef<HTMLInputElement>(null);
  const [errorNombre, setErrorNombre] = useState<string | undefined>();
  const hayAlgo = valores.relato.trim() !== '' || valores.rubro !== null;

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (!valores.nombre.trim()) {
      setErrorNombre('Escribí cómo se llama tu negocio.');
      nombre.current?.focus();
      return;
    }
    setErrorNombre(undefined);
    onSeguir();
  }

  return (
    <form noValidate onSubmit={enviar} className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Contanos de tu negocio</h2>
      <InputField
        ref={nombre}
        label="¿Cómo se llama tu negocio?"
        required
        autoComplete="organization"
        maxLength={120}
        value={valores.nombre}
        error={errorNombre}
        onChange={(evento) => {
          setErrorNombre(undefined);
          onCambiar({ nombre: evento.target.value });
        }}
      />
      <TextareaField
        label="¿Qué hacés?"
        hint={`Con tus palabras. Por ejemplo: «${EJEMPLO}».`}
        rows={4}
        maxLength={600}
        value={valores.relato}
        onChange={(evento) => onCambiar({ relato: evento.target.value })}
      />
      <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
        <legend className="mb-1.5 p-0 text-base font-semibold text-fg">
          ¿De qué rubro es? <span className="font-normal text-soft">(si querés)</span>
        </legend>
        <div className="flex flex-wrap gap-2">
          {RUBROS.map((rubro) => {
            const elegido = valores.rubro === rubro;
            return (
              <button
                key={rubro}
                type="button"
                aria-pressed={elegido}
                onClick={() => onCambiar({ rubro: elegido ? null : rubro })}
                className={cn(
                  'inline-flex min-h-12 cursor-pointer items-center gap-1.5 rounded-full border px-4 text-base font-semibold text-fg transition-colors',
                  elegido
                    ? 'border-accent bg-accent'
                    : 'border-fg/30 bg-surface hover:bg-fg/6 active:bg-fg/12',
                )}
              >
                {elegido && <CheckIcon aria-hidden size={18} weight="bold" />}
                {NOMBRE_RUBRO[rubro]}
              </button>
            );
          })}
        </div>
      </fieldset>

      <section
        aria-labelledby="entiendo-titulo"
        className="flex flex-col gap-2 rounded-lg border border-line bg-canvas p-4"
      >
        <h3 id="entiendo-titulo" className="m-0 text-body font-bold">
          Lo que voy entendiendo
        </h3>
        <div aria-live="polite" className="flex flex-col gap-2">
          {hayAlgo ? (
            <>
              <p className="m-0 text-body text-soft">
                Parece un negocio de <strong>{NOMBRE_RUBRO[entendido.rubro].toLowerCase()}</strong>.
                Armaría estos bloques:
              </p>
              <ul
                aria-label="Bloques que armaría"
                className="m-0 flex list-none flex-wrap gap-2 p-0"
              >
                {entendido.piezas.map((pieza) => (
                  <li
                    key={pieza.id}
                    className="rounded-full border border-fg/20 bg-surface px-3 py-1 text-hint font-semibold"
                  >
                    {pieza.nombre}
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="m-0 text-body text-soft">
              Contame qué hacés y a medida que escribís voy sumando bloques.
            </p>
          )}
        </div>
      </section>

      <Button type="submit" variant="solid" size="lg" fullWidth>
        Seguir
      </Button>
    </form>
  );
}
