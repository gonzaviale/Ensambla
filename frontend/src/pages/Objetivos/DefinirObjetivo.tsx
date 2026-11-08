import { useId, useState, type FormEvent } from 'react';
import { ChartLineUpIcon } from '@phosphor-icons/react';
import { EJEMPLOS_DE_OBJETIVO, type Objetivo } from '@/lib/objetivos';
import { useObjetivosStore } from '@/stores/objetivosStore';

type Props = {
  onDefinido: (objetivo: Objetivo) => void;
  onCancelar?: () => void;
};

// «¿Qué querés lograr?»: la persona lo escribe con sus palabras y Sami lo convierte en un objetivo medible.
export function DefinirObjetivo({ onDefinido, onCancelar }: Props) {
  const definir = useObjetivosStore((estado) => estado.definir);
  const [texto, setTexto] = useState('');
  const idCampo = useId();

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!texto.trim()) return;
    onDefinido(definir(texto));
    setTexto('');
  };

  return (
    <section aria-labelledby={`${idCampo}-titulo`}>
      <form className="objetivos-meta" onSubmit={enviar}>
        <h2 id={`${idCampo}-titulo`} className="objetivos-meta-titulo">
          ¿Qué querés lograr?
        </h2>
        <label htmlFor={idCampo}>Contale tu objetivo a Sami</label>
        <textarea
          id={idCampo}
          value={texto}
          placeholder="Ej.: Quiero ganar $400.000 por mes sin subir demasiado los precios"
          onChange={(evento) => setTexto(evento.target.value)}
        />
        <div className="objetivos-ejemplos">
          {EJEMPLOS_DE_OBJETIVO.map((ejemplo) => (
            <button type="button" key={ejemplo} onClick={() => setTexto(ejemplo)}>
              {ejemplo}
            </button>
          ))}
        </div>
        <div className="objetivos-meta-pie">
          <span>Sami lo transforma en una meta medible y la va siguiendo semana a semana.</span>
          <div className="objetivos-meta-acciones">
            {onCancelar && (
              <button type="button" className="btn secondary" onClick={onCancelar}>
                Cancelar
              </button>
            )}
            <button type="submit" className="btn primary" disabled={!texto.trim()}>
              Definir objetivo
            </button>
          </div>
        </div>
      </form>
      <aside className="objetivos-fundamento">
        <ChartLineUpIcon aria-hidden size={28} />
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
