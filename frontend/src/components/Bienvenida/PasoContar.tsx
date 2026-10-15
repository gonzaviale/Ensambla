import { useRef, useState, type FormEvent } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { Button, InputField, TextareaField } from '@/components/ui';
import { enfocarPrimerError } from '@/components/Cuenta/enfocarPrimerError';
import { cn } from '@/lib/cn';
import { RUBROS } from '@/lib/red/relato';

export type DatosRelato = { relato: string; rubro: string; nombre: string; zona: string };

type PasoContarProps = {
  valores: DatosRelato;
  onCambiar: (cambio: Partial<DatosRelato>) => void;
  onSeguir: () => void;
};

const EJEMPLO =
  'Tengo un almacén, le compro a dos mayoristas y los precios los cambio a mano mirando el cuaderno.';

// Paso 1: «Contanos de tu comercio». El relato es opcional; el nombre del comercio no.
export function PasoContar({ valores, onCambiar, onSeguir }: PasoContarProps) {
  const formulario = useRef<HTMLFormElement>(null);
  const [errorNombre, setErrorNombre] = useState<string | undefined>();

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (!valores.nombre.trim()) {
      setErrorNombre('Escribí cómo se llama tu comercio.');
      enfocarPrimerError(formulario.current);
      return;
    }
    setErrorNombre(undefined);
    onSeguir();
  }

  return (
    <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Contanos de tu comercio</h2>
      <TextareaField
        label="¿Cómo trabajás hoy?"
        hint={`Con tus palabras, como se lo contarías a un conocido. Por ejemplo: «${EJEMPLO}»`}
        rows={5}
        maxLength={600}
        value={valores.relato}
        onChange={(evento) => onCambiar({ relato: evento.target.value })}
      />
      <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
        <legend className="mb-1.5 p-0 text-base font-semibold text-fg">¿Qué vendés?</legend>
        <div className="flex flex-wrap gap-2">
          {RUBROS.map((rubro) => {
            const elegido = valores.rubro === rubro;
            return (
              <button
                key={rubro}
                type="button"
                aria-pressed={elegido}
                onClick={() => onCambiar({ rubro: elegido ? '' : rubro })}
                className={cn(
                  'inline-flex min-h-12 cursor-pointer items-center gap-1.5 rounded-full border px-4 text-base font-semibold text-fg transition-colors',
                  elegido
                    ? 'border-accent bg-accent'
                    : 'border-fg/30 bg-surface hover:bg-fg/6 active:bg-fg/12',
                )}
              >
                {elegido && <CheckIcon aria-hidden size={18} weight="bold" />}
                {rubro}
              </button>
            );
          })}
        </div>
      </fieldset>
      <div className="grid gap-4 sm:grid-cols-2">
        <InputField
          label="Nombre del comercio"
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
        <InputField
          label="Zona o barrio"
          autoComplete="address-level2"
          maxLength={80}
          value={valores.zona}
          onChange={(evento) => onCambiar({ zona: evento.target.value })}
        />
      </div>
      <Button type="submit" variant="solid" size="lg" fullWidth>
        Seguir
      </Button>
    </form>
  );
}
