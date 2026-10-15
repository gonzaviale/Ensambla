import { Link } from '@tanstack/react-router';
import { Button, Panel, buttonClasses } from '@/components/ui';
import { ComoFunciona } from './ComoFunciona';

type PasoPrimeraListaProps = {
  error?: string;
  /** Guarda que la bienvenida ya se hizo (se llama antes de ir a cualquiera de los tres caminos). */
  onTerminar: () => void;
  onUsarEjemplo: () => void;
  onVolver: () => void;
};

// Paso 3: «Cargá tu primera lista». Tres caminos claros; el principal es el de ejemplo.
export function PasoPrimeraLista({
  error,
  onTerminar,
  onUsarEjemplo,
  onVolver,
}: PasoPrimeraListaProps) {
  return (
    <div className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Cargá tu primera lista</h2>
      <p className="m-0 text-body text-soft">
        Elegí cómo querés empezar. Podés cambiar de idea cuando quieras.
      </p>

      <Panel className="flex flex-col gap-3">
        <h3 className="m-0 text-body font-bold">Usar la lista de ejemplo</h3>
        <p className="m-0 text-body text-soft">
          Te cargamos una lista de Distribuidora Sur para que veas cómo funciona. Es una
          demostración: no toca nada de tu comercio real.
        </p>
        <Button variant="solid" size="lg" fullWidth onClick={onUsarEjemplo}>
          Usar la lista de ejemplo de Distribuidora Sur
        </Button>
        {error && (
          <p role="alert" className="m-0 text-base font-semibold text-bad">
            {error}
          </p>
        )}
      </Panel>

      <Panel className="flex flex-col gap-3">
        <h3 className="m-0 text-body font-bold">Pasar mi cuaderno</h3>
        <p className="m-0 text-body text-soft">
          Sacale una foto a las hojas donde anotás tus precios y las pasamos al sistema.
        </p>
        <Link
          to="/cuaderno"
          onClick={onTerminar}
          className={buttonClasses({ variant: 'primary', size: 'lg', fullWidth: true })}
        >
          Sacarle foto a mi cuaderno
        </Link>
      </Panel>

      <Link
        to="/hoy"
        onClick={onTerminar}
        className={buttonClasses({ variant: 'secondary', size: 'lg', fullWidth: true })}
      >
        Lo hago después
      </Link>

      <ComoFunciona />

      <Button variant="ghost" onClick={onVolver} className="self-start">
        Volver
      </Button>
    </div>
  );
}
