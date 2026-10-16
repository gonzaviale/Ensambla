import { Link } from '@tanstack/react-router';
import { CaretRightIcon } from '@phosphor-icons/react';
import { Badge, Panel } from '@/components/ui';
import { useRegistros } from '@/hooks/registros/queries';
import { plural } from '@/lib/formato';
import { RESPUESTAS } from '@/lib/respuestas/definiciones';
import type { Pieza } from '@/types/negocio';
import type { Bloque } from '@/types/proyecto';

const MAX_PREGUNTAS = 3;

/** Preguntas cortas que esta pieza ayuda a responder. */
function preguntasDe(pieza: Pieza) {
  return RESPUESTAS.filter((respuesta) => respuesta.requiere.includes(pieza.id))
    .slice(0, MAX_PREGUNTAS)
    .map((respuesta) => respuesta.pregunta);
}

/** Un bloque del sistema: un solo número (cuántos registros tiene) y lo que desbloquea. */
export function TarjetaBloque({ bloque, pieza }: { bloque: Bloque; pieza: Pieza }) {
  const registros = useRegistros(bloque.id, { tamanio: 1 });
  const cantidad = registros.data?.totales.cantidad;
  const preguntas = preguntasDe(pieza);

  return (
    <li className="min-w-0">
      <Panel interactive className="relative flex h-full flex-col gap-3 focus-within:ring-2">
        <h3 className="m-0 flex items-center justify-between gap-2 text-h3">
          <Link
            to="/bloques/$bloqueId"
            params={{ bloqueId: bloque.id }}
            className="text-fg no-underline after:absolute after:inset-0 after:content-['']"
          >
            {bloque.nombre}
          </Link>
          <CaretRightIcon aria-hidden size={22} className="shrink-0 text-soft" />
        </h3>
        <p className="m-0 text-body text-soft">{pieza.descripcion}</p>
        <p className="m-0 text-body font-bold">
          {cantidad === undefined
            ? 'Contando…'
            : cantidad === 0
              ? 'Todavía sin datos'
              : plural(cantidad, 'registro', 'registros')}
        </p>
        {preguntas.length > 0 && (
          <div className="mt-auto flex flex-col gap-2">
            <p className="m-0 text-hint font-semibold text-soft">Desbloquea:</p>
            <ul className="m-0 flex list-none flex-wrap gap-2 p-0">
              {preguntas.map((pregunta) => (
                <li key={pregunta}>
                  <Badge tone="accent">{pregunta}</Badge>
                </li>
              ))}
            </ul>
          </div>
        )}
      </Panel>
    </li>
  );
}
