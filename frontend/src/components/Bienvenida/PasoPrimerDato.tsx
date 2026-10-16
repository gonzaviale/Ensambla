import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { BookOpenIcon, ChatCircleTextIcon, TableIcon } from '@phosphor-icons/react';
import { Button, Panel, buttonClasses } from '@/components/ui';

type PasoPrimerDatoProps = {
  cargandoEjemplo: boolean;
  /** Cuántas filas de ejemplo se cargaron (undefined = todavía no). */
  cargadas?: number;
  error?: string;
  /** Sin sistema leído todavía no se puede cargar el ejemplo. */
  puedeCargarEjemplo: boolean;
  onCargarEjemplo: () => void;
};

function Opcion({
  icono,
  titulo,
  texto,
  accion,
}: {
  icono: ReactNode;
  titulo: string;
  texto: string;
  accion: ReactNode;
}) {
  return (
    <li className="m-0 list-none">
      <Panel tight className="flex h-full flex-col gap-3">
        <span
          aria-hidden
          className="flex size-12 items-center justify-center rounded-lg bg-accent-100 text-accent-800"
        >
          {icono}
        </span>
        <h3 className="m-0 text-body font-bold">{titulo}</h3>
        <p className="m-0 flex-1 text-hint text-soft">{texto}</p>
        {accion}
      </Panel>
    </li>
  );
}

// Paso 3: tres caminos para el primer dato. La acción principal de la pantalla es ir al inicio.
export function PasoPrimerDato({
  cargandoEjemplo,
  cargadas,
  error,
  puedeCargarEjemplo,
  onCargarEjemplo,
}: PasoPrimerDatoProps) {
  return (
    <div className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Primer dato</h2>
      <p className="m-0 text-body text-soft">
        Tu sistema ya está armado. Elegí cómo cargar lo primero; con un dato ya empiezan las
        respuestas.
      </p>

      <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-3">
        <Opcion
          icono={<ChatCircleTextIcon size={28} weight="duotone" />}
          titulo="Contá qué pasó"
          texto="Escribí «vendí 3 yerbas a 3.600» y Ensambla lo ordena."
          accion={
            <Link to="/subir" className={buttonClasses({ variant: 'primary', fullWidth: true })}>
              Contá qué pasó
            </Link>
          }
        />
        <Opcion
          icono={<TableIcon size={28} weight="duotone" />}
          titulo="Cargar datos de ejemplo"
          texto="Unas filas de muestra para ver cómo se ve. Están rotuladas «ejemplo» y podés borrarlas."
          accion={
            <Button
              variant="primary"
              fullWidth
              loading={cargandoEjemplo}
              disabled={!puedeCargarEjemplo || cargadas !== undefined}
              onClick={onCargarEjemplo}
            >
              Cargar datos de ejemplo
            </Button>
          }
        />
        <Opcion
          icono={<BookOpenIcon size={28} weight="duotone" />}
          titulo="Traer mi historia"
          texto="Sacale una foto a tu cuaderno y Ensambla pasa lo que anotaste."
          accion={
            <Link to="/cuaderno" className={buttonClasses({ variant: 'primary', fullWidth: true })}>
              Traer mi historia
            </Link>
          }
        />
      </ul>

      {cargadas !== undefined && (
        <p role="status" className="m-0 rounded-lg border border-line bg-canvas p-4 text-body">
          Listo: cargamos {cargadas} filas de <strong>datos de ejemplo</strong>. Podés borrarlas
          cuando quieras.
        </p>
      )}
      {error && (
        <p role="alert" className="m-0 text-body font-semibold text-bad">
          {error}
        </p>
      )}

      <Link
        to="/inicio"
        className={buttonClasses({ variant: 'solid', size: 'lg', fullWidth: true })}
      >
        Ir a mi inicio
      </Link>
    </div>
  );
}
