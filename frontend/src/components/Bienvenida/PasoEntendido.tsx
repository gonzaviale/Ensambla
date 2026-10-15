import { SparkleIcon } from '@phosphor-icons/react';
import { Button, Panel } from '@/components/ui';
import { BLOQUES, ORDEN_BLOQUES, unirFrases, type BloqueId, type Relato } from '@/lib/red/relato';

type PasoEntendidoProps = {
  relato: Relato;
  elegidos: ReadonlySet<BloqueId>;
  onAlternar: (id: BloqueId) => void;
  onVolver: () => void;
  onSeguir: () => void;
};

function Bloque({
  id,
  motivo,
  marcado,
  onAlternar,
}: {
  id: BloqueId;
  motivo?: string;
  marcado: boolean;
  onAlternar: (id: BloqueId) => void;
}) {
  const bloque = BLOQUES[id];
  return (
    <li>
      <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-fg/20 bg-surface p-4 has-checked:border-accent-700 has-checked:bg-accent-100/40">
        <input
          type="checkbox"
          checked={marcado}
          onChange={() => onAlternar(id)}
          className="mt-0.5 size-6 shrink-0 cursor-pointer accent-accent-700"
        />
        <span className="flex min-w-0 flex-col gap-0.5">
          <span className="text-body font-semibold">{bloque.nombre}</span>
          <span className="text-hint text-soft">{bloque.descripcion}</span>
          {motivo && <span className="text-hint text-muted">{motivo}</span>}
        </span>
      </label>
    </li>
  );
}

// Paso 1 (segunda parte): la respuesta del sistema al relato, con los bloques que se prendieron.
// Todo se puede destildar: nada queda prendido sin que la persona lo vea.
export function PasoEntendido({
  relato,
  elegidos,
  onAlternar,
  onVolver,
  onSeguir,
}: PasoEntendidoProps) {
  const resto = ORDEN_BLOQUES.filter((id) => !relato.bloques.includes(id));
  const frase = unirFrases(relato.entendido);

  return (
    <div className="flex flex-col gap-5">
      <h2 className="m-0 text-h3">Esto es lo que entendimos</h2>
      <Panel flat className="flex items-start gap-3">
        <SparkleIcon
          aria-hidden
          size={28}
          weight="fill"
          className="mt-0.5 shrink-0 text-amber-700"
        />
        <p className="m-0 text-body">
          <strong>Entendí esto:</strong>{' '}
          {frase
            ? `${frase}.`
            : 'todavía no me contaste mucho, así que prendí lo básico. Después lo podés cambiar.'}
        </p>
      </Panel>

      <fieldset className="m-0 flex min-w-0 flex-col gap-3 border-0 p-0">
        <legend className="mb-1 p-0 text-base font-semibold text-fg">
          Prendimos esto para vos (destildá lo que no quieras)
        </legend>
        <ul className="m-0 flex list-none flex-col gap-2 p-0">
          {relato.bloques.map((id) => (
            <Bloque
              key={id}
              id={id}
              motivo={relato.motivos[id]}
              marcado={elegidos.has(id)}
              onAlternar={onAlternar}
            />
          ))}
        </ul>
      </fieldset>

      {resto.length > 0 && (
        <details className="rounded-lg border border-line/60 bg-surface px-4 py-3">
          <summary className="min-h-8 cursor-pointer text-body font-semibold">
            ¿Querés sumar algo más?
          </summary>
          <fieldset className="m-0 mt-3 flex min-w-0 flex-col gap-3 border-0 p-0">
            <legend className="sr-only">Otros bloques que podés prender</legend>
            <ul className="m-0 flex list-none flex-col gap-2 p-0">
              {resto.map((id) => (
                <Bloque key={id} id={id} marcado={elegidos.has(id)} onAlternar={onAlternar} />
              ))}
            </ul>
          </fieldset>
        </details>
      )}

      <div className="flex flex-col gap-3">
        <Button variant="solid" size="lg" fullWidth onClick={onSeguir}>
          Está bien, seguir
        </Button>
        <Button variant="ghost" onClick={onVolver}>
          Quiero contarlo de nuevo
        </Button>
      </div>
    </div>
  );
}
