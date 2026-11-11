import { useId, useState, type CSSProperties } from 'react';
import {
  PALANCAS,
  aportes,
  brecha,
  formatoPesos,
  objetivoDeEjemplo,
  resultadoProyectado,
  ultimoValor,
  valoresRecomendados,
  type Objetivo,
  type Palanca,
  type Valores,
} from '@/lib/objetivos';
import { Explicacion } from '@/pages/Objetivos/piezas';

type Props = {
  objetivos: Objetivo[];
  objetivoId?: string;
  onElegir: (objetivoId: string) => void;
  onDefinir: () => void;
  onPlan: (objetivoId: string) => void;
};

// «Probá un escenario»: Sami explica cómo funciona y la persona lo prueba con uno de sus objetivos.
export function LeccionEscenario({ objetivos, objetivoId, onElegir, onDefinir, onPlan }: Props) {
  const idSelector = useId();
  const propio = objetivos.find((objetivo) => objetivo.id === objetivoId) ?? objetivos[0];
  const objetivo = propio ?? objetivoDeEjemplo();

  return (
    <div className="ensenanza-leccion">
      {propio ? (
        objetivos.length > 1 && (
          <div className="ensenanza-selector">
            <label htmlFor={idSelector}>Objetivo a probar</label>
            <select
              id={idSelector}
              value={propio.id}
              onChange={(evento) => onElegir(evento.target.value)}
            >
              {objetivos.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.texto}
                </option>
              ))}
            </select>
          </div>
        )
      ) : (
        <div className="ensenanza-aviso">
          <p>
            Todavía no definiste objetivos, así que probamos con un ejemplo: «{objetivo.texto}».
          </p>
          <button type="button" className="btn secondary" onClick={onDefinir}>
            Definir mi objetivo
          </button>
        </div>
      )}

      {brecha(objetivo) === 0 ? (
        <Explicacion>
          Este objetivo ya está cumplido: tu resultado de hoy llega a la meta. Probá con otro más
          alto.
        </Explicacion>
      ) : (
        <Simulador key={objetivo.id} objetivo={objetivo} />
      )}

      {propio && (
        <button type="button" className="btn primary" onClick={() => onPlan(propio.id)}>
          Ver el plan de este objetivo
        </button>
      )}
    </div>
  );
}

function Simulador({ objetivo }: { objetivo: Objetivo }) {
  const recomendados = valoresRecomendados(objetivo);
  const [valores, setValores] = useState<Valores>(recomendados);
  const proyectado = resultadoProyectado(objetivo, valores);
  const falta = Math.max(0, objetivo.meta - proyectado);
  const aporte = aportes(objetivo, valores);
  const hoy = ultimoValor(objetivo);
  const maximo = Math.max(objetivo.meta, proyectado, hoy);

  const barras = [
    { clase: 'hoy', nombre: 'Hoy', valor: hoy },
    { clase: 'escenario', nombre: 'Con tu escenario', valor: proyectado },
    { clase: 'meta', nombre: 'Tu objetivo', valor: objetivo.meta },
  ];

  return (
    <>
      <div className="ensenanza-simulador">
        <section className="ensenanza-panel" aria-labelledby="palancas-titulo">
          <h3 id="palancas-titulo">Palancas</h3>
          {PALANCAS.map((palanca) => (
            <Control
              key={palanca.clave}
              palanca={palanca}
              valor={valores[palanca.clave]}
              maximo={Math.max(palanca.paso, recomendados[palanca.clave] * 2)}
              onCambio={(valor) =>
                setValores((actuales) => ({ ...actuales, [palanca.clave]: valor }))
              }
            />
          ))}
          <p className="ensenanza-nota">Arrancan en lo que recomienda Sami.</p>
        </section>

        <section className="ensenanza-panel ensenanza-resultado" aria-labelledby="resultado-titulo">
          <h3 id="resultado-titulo">Resultado</h3>
          <p className="ensenanza-resultado-etiqueta">Resultado proyectado</p>
          <p className="ensenanza-resultado-valor">{formatoPesos(proyectado)}</p>
          <dl>
            <div>
              <dt>Meta mensual</dt>
              <dd>{formatoPesos(objetivo.meta)}</dd>
            </div>
            <div data-alerta={falta > 0 ? 'true' : undefined}>
              <dt>{falta === 0 ? 'Meta alcanzada' : 'Te faltaría'}</dt>
              <dd>{falta === 0 ? '✓' : formatoPesos(falta)}</dd>
            </div>
          </dl>
        </section>

        <section className="ensenanza-panel" aria-labelledby="comparacion-titulo">
          <h3 id="comparacion-titulo">Comparación</h3>
          <ul className="ensenanza-barras">
            {barras.map((barra) => (
              <li key={barra.nombre} data-clase={barra.clase}>
                <span>{barra.nombre}</span>
                <strong>{formatoPesos(barra.valor)}</strong>
                <i style={{ width: `${Math.max(4, (barra.valor / maximo) * 100)}%` }} />
              </li>
            ))}
          </ul>
        </section>
      </div>
      <Explicacion>
        {falta === 0
          ? `Llegás a la meta: ventas aportan ${formatoPesos(aporte.ventas)}, margen ${formatoPesos(aporte.margen)} y gastos ${formatoPesos(aporte.gastos)} por mes.`
          : `Te faltarían ${formatoPesos(falta)} por mes. Probá subir una palanca o combinar las tres.`}
      </Explicacion>
    </>
  );
}

function Control({
  palanca,
  valor,
  maximo,
  onCambio,
}: {
  palanca: Palanca;
  valor: number;
  maximo: number;
  onCambio: (valor: number) => void;
}) {
  return (
    <label className="ensenanza-control">
      <span>
        {palanca.etiqueta}
        <strong>{palanca.formato(valor)}</strong>
      </span>
      <input
        type="range"
        aria-label={palanca.etiqueta}
        min={0}
        max={maximo}
        step={palanca.paso}
        value={valor}
        style={{ '--lleno': `${maximo > 0 ? (valor / maximo) * 100 : 0}%` } as CSSProperties}
        onChange={(evento) => onCambio(Number(evento.target.value))}
      />
    </label>
  );
}
