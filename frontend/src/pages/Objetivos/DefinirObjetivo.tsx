import { useId, useState, type FormEvent } from 'react';
import { ChartLineUpIcon } from '@phosphor-icons/react';
import { leerErrorServidor } from '@/hooks/errores';
import { useDefinirObjetivo } from '@/hooks/objetivos/mutations';
import { useComoVieneTuNegocio } from '@/hooks/objetivos/useComoVieneTuNegocio';
import { alcanceDeLaMeta, leerObjetivo } from '@/lib/lecturaObjetivo';
import { EJEMPLOS_DE_OBJETIVO, objetivoDesdeServidor, type Objetivo } from '@/lib/objetivos';

type Props = {
  onDefinido: (objetivo: Objetivo) => void;
  onCancelar?: () => void;
};

const miles = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 0 });

/** Lo que la persona escribe en la meta, sin puntos ni «$»: «400.000» → 400000; vacío o cero → null. */
function metaEscritaDe(valor: string): number | null {
  const numero = Number(valor.replace(/\D/g, ''));
  return numero > 0 ? numero : null;
}

// «¿Qué querés lograr?»: la persona lo escribe con sus palabras, Sami le muestra qué entendió y la meta que va a
// medir, y la persona la confirma o la corrige antes de guardar.
export function DefinirObjetivo({ onDefinido, onCancelar }: Props) {
  const definir = useDefinirObjetivo();
  const { negocio } = useComoVieneTuNegocio();
  const [texto, setTexto] = useState('');
  // null mientras la persona no tocó la meta: se usa la que entendió Sami.
  const [metaEscrita, setMetaEscrita] = useState<string | null>(null);
  const idCampo = useId();
  const error = leerErrorServidor(definir.error);
  const lectura = texto.trim() ? leerObjetivo(texto, negocio) : null;
  const meta = metaEscrita === null ? (lectura?.meta ?? null) : metaEscritaDe(metaEscrita);
  const alcance = meta === null ? null : alcanceDeLaMeta(meta, negocio);
  const puedeGuardar = Boolean(texto.trim()) && meta !== null && !definir.isPending;

  const escribir = (nuevo: string) => {
    setTexto(nuevo);
    setMetaEscrita(null);
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!puedeGuardar || meta === null) return;
    definir.mutate(
      { texto, meta },
      {
        onSuccess: (guardado) => {
          escribir('');
          onDefinido(objetivoDesdeServidor(guardado));
        },
      },
    );
  };

  return (
    <section className="objetivos-definir" aria-labelledby={`${idCampo}-titulo`}>
      <form className="objetivos-formulario" onSubmit={enviar}>
        <h2 id={`${idCampo}-titulo`}>¿Qué querés lograr?</h2>
        <div className="field">
          <label htmlFor={idCampo}>Contale tu objetivo a Sami</label>
          <textarea
            id={idCampo}
            rows={4}
            value={texto}
            placeholder="Ej.: Quiero ganar $400.000 por mes sin subir demasiado los precios"
            onChange={(evento) => escribir(evento.target.value)}
          />
          <span className="input-hint">
            Sami busca cuánto querés ganar y te muestra qué entendió antes de guardar.
          </span>
        </div>
        {lectura && (
          <section
            className="objetivos-lectura"
            aria-live="polite"
            aria-label="Lo que entendió Sami"
          >
            <p className="objetivos-etiqueta">Lo que entendió Sami</p>
            <p>{lectura.explicacion}</p>
            {lectura.avisos.length > 0 && (
              <ul>
                {lectura.avisos.map((aviso) => (
                  <li key={aviso}>{aviso}</li>
                ))}
              </ul>
            )}
            <label className="objetivos-lectura-meta" htmlFor={`${idCampo}-meta`}>
              Tu meta: lo que querés que te quede por mes
              <span>
                <b aria-hidden>$</b>
                <input
                  id={`${idCampo}-meta`}
                  inputMode="numeric"
                  autoComplete="off"
                  value={metaEscrita ?? (lectura.meta === null ? '' : miles.format(lectura.meta))}
                  placeholder="Ej.: 400.000"
                  onChange={(evento) => {
                    const numero = metaEscritaDe(evento.target.value);
                    setMetaEscrita(numero === null ? '' : miles.format(numero));
                  }}
                />
              </span>
            </label>
            {alcance && (
              <div
                className={`objetivos-alcance objetivos-alcance-${alcance.nivel}`}
                role="note"
                aria-label={
                  alcance.nivel === 'imposible'
                    ? 'Esta meta no se alcanza con lo que vendés hoy'
                    : 'Esta meta está lejos de lo que ganás hoy'
                }
              >
                <p>{alcance.mensaje}</p>
                {alcance.sugerida !== null && (
                  <button
                    type="button"
                    className="btn secondary"
                    onClick={() => setMetaEscrita(miles.format(alcance.sugerida ?? 0))}
                  >
                    Empezar por ${miles.format(alcance.sugerida)}
                  </button>
                )}
              </div>
            )}
          </section>
        )}
        <div>
          <p className="objetivos-ejemplos-titulo">O empezá con uno de estos:</p>
          <div className="example-chips">
            {EJEMPLOS_DE_OBJETIVO.map((ejemplo) => (
              <button type="button" key={ejemplo} onClick={() => escribir(ejemplo)}>
                {ejemplo}
              </button>
            ))}
          </div>
        </div>
        {error && (
          <p className="input-hint" role="alert">
            {error.campos.texto ?? error.campos.meta ?? error.mensaje}
          </p>
        )}
        <div className="objetivos-formulario-acciones">
          {onCancelar && (
            <button type="button" className="btn secondary" onClick={onCancelar}>
              Cancelar
            </button>
          )}
          <button type="submit" className="btn primary" disabled={!puedeGuardar}>
            Definir objetivo
          </button>
        </div>
      </form>
      <aside className="objetivos-fundamento">
        <ChartLineUpIcon aria-hidden size={24} />
        <div>
          <strong>Cómo se mide</strong>
          <p>
            Ventas − costo de mercadería − gastos = resultado. Sami compara ese resultado con tu
            meta, y revisa margen, punto de equilibrio y ventas pendientes de cobro.
          </p>
        </div>
      </aside>
    </section>
  );
}
