import { useId, useState, type FormEvent } from 'react';
import { ChartLineUpIcon } from '@phosphor-icons/react';
import { leerErrorServidor } from '@/hooks/errores';
import { useDefinirObjetivo } from '@/hooks/objetivos/mutations';
import { EJEMPLOS_DE_OBJETIVO, objetivoDesdeServidor, type Objetivo } from '@/lib/objetivos';

type Props = {
  onDefinido: (objetivo: Objetivo) => void;
  onCancelar?: () => void;
};

// «¿Qué querés lograr?»: la persona lo escribe con sus palabras y Sami lo convierte en un objetivo medible.
export function DefinirObjetivo({ onDefinido, onCancelar }: Props) {
  const definir = useDefinirObjetivo();
  const [texto, setTexto] = useState('');
  const idCampo = useId();
  const error = leerErrorServidor(definir.error);

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!texto.trim() || definir.isPending) return;
    definir.mutate(texto, {
      onSuccess: (guardado) => {
        setTexto('');
        onDefinido(objetivoDesdeServidor(guardado));
      },
    });
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
            onChange={(evento) => setTexto(evento.target.value)}
          />
          <span className="input-hint">
            Sami lo transforma en una meta medible y la sigue semana a semana.
          </span>
        </div>
        <div>
          <p className="objetivos-ejemplos-titulo">O empezá con uno de estos:</p>
          <div className="example-chips">
            {EJEMPLOS_DE_OBJETIVO.map((ejemplo) => (
              <button type="button" key={ejemplo} onClick={() => setTexto(ejemplo)}>
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
          <button
            type="submit"
            className="btn primary"
            disabled={!texto.trim() || definir.isPending}
          >
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
