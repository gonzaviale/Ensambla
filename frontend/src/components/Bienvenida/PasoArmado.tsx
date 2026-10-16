import { ArrowCounterClockwiseIcon, XIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { definicion, respuestasQueDesbloquea } from '@/lib/respuestas/definiciones';
import type { PiezaEnsamblada, PiezaId } from '@/types/negocio';
import { TarjetaBloque } from './TarjetaBloque';

type PasoArmadoProps = {
  /** Las piezas que quedan (ya sin las que la persona sacó). */
  piezas: PiezaEnsamblada[];
  /** La última pieza que sacó, para ofrecer «Deshacer». */
  ultimaSacada?: PiezaEnsamblada;
  /** Cuántos bloques entran en el plan (undefined = sin tope). */
  tope?: number;
  armando: boolean;
  error?: string;
  onSacar: (id: PiezaId) => void;
  onDeshacer: () => void;
  onVolver: () => void;
  onArmar: () => void;
};

// Paso 2: las piezas ensambladas, cada una con su motivo y lo que desbloquea. Se puede sacar una
// (con «Deshacer»). Si el plan Gratis no alcanza para todas, las que sobran quedan «para después».
export function PasoArmado({
  piezas,
  ultimaSacada,
  tope,
  armando,
  error,
  onSacar,
  onDeshacer,
  onVolver,
  onArmar,
}: PasoArmadoProps) {
  // Qué respuestas abre cada bloque, sumando de a uno en el orden en que se arman.
  const desbloqueos = new Map<PiezaId, string[]>();
  const acumuladas: PiezaId[] = [];
  for (const { id } of piezas) {
    desbloqueos.set(
      id,
      respuestasQueDesbloquea(id, acumuladas).map((r) => definicion(r).pregunta),
    );
    acumuladas.push(id);
  }
  const sobran = tope !== undefined && piezas.length > tope ? piezas.length - tope : 0;

  return (
    <div className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Esto armé para vos</h2>
      <p className="m-0 text-body text-soft">
        Cada bloque te desbloquea respuestas. Sacá los que no te sirvan; después podés sumar más.
      </p>

      {piezas.length === 0 ? (
        <p role="status" className="m-0 rounded-lg border border-line bg-canvas p-4 text-body">
          No quedó ningún bloque. Deshacé lo que sacaste o volvé para contarnos más de tu negocio.
        </p>
      ) : (
        <ul
          aria-label="Bloques de tu sistema"
          className="m-0 grid list-none gap-3 p-0 sm:grid-cols-2"
        >
          {piezas.map((pieza, i) => {
            const preguntas = desbloqueos.get(pieza.id) ?? [];
            const despues = tope !== undefined && i >= tope;
            return (
              <TarjetaBloque
                key={pieza.id}
                id={pieza.id}
                nombre={pieza.nombre}
                motivo={pieza.motivo}
                orden={i}
                apagada={despues}
                detalle={
                  despues
                    ? 'Queda para después: el plan Gratis incluye 3 bloques.'
                    : preguntas.length > 0
                      ? `Te responde: ${preguntas.join(' · ')}`
                      : 'Suma datos a las otras respuestas.'
                }
                accion={
                  <Button
                    variant="ghost"
                    size="compact"
                    aria-label={`Sacar ${pieza.nombre}`}
                    onClick={() => onSacar(pieza.id)}
                  >
                    <XIcon aria-hidden size={18} />
                    Sacar
                  </Button>
                }
              />
            );
          })}
        </ul>
      )}

      {ultimaSacada && (
        <p
          role="status"
          className="m-0 flex flex-wrap items-center gap-2 rounded-lg border border-line bg-canvas px-4 py-2 text-body"
        >
          <span>
            Sacaste <strong>{ultimaSacada.nombre}</strong>.
          </span>
          <Button variant="primary" size="compact" onClick={onDeshacer}>
            <ArrowCounterClockwiseIcon aria-hidden size={18} />
            Deshacer
          </Button>
        </p>
      )}

      {sobran > 0 && (
        <p
          role="note"
          className="m-0 rounded-lg border border-amber-700/40 bg-amber-100 p-4 text-body"
        >
          Tu plan Gratis incluye 3 bloques: armamos los 3 primeros y{' '}
          {sobran === 1 ? 'el otro queda' : 'los otros quedan'} para después. No perdés nada: lo
          sumás cuando quieras desde un plan.
        </p>
      )}

      {error && (
        <p role="alert" className="m-0 text-body font-semibold text-bad">
          {error}
        </p>
      )}

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <Button variant="secondary" size="lg" onClick={onVolver} disabled={armando}>
          Volver
        </Button>
        <Button
          variant="solid"
          size="lg"
          loading={armando}
          disabled={piezas.length === 0}
          onClick={onArmar}
        >
          Armar mi sistema
        </Button>
      </div>
    </div>
  );
}
