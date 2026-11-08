import { useId, useState } from 'react';
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
import { Explicacion, Metrica } from '@/pages/Objetivos/piezas';

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
    <div className="academy-leccion">
      <ol className="academy-pasos">
        <li>
          <strong>Elegís un objetivo.</strong>
          <span>El escenario parte de tu resultado de hoy y de la meta que te pusiste.</span>
        </li>
        <li>
          <strong>Movés las tres palancas.</strong>
          <span>Ventas, margen y gastos. Cada una cierra una parte de lo que te falta.</span>
        </li>
        <li>
          <strong>Ves qué pasaría.</strong>
          <span>Es una simulación: no cambia tus datos, tus precios ni tus registros.</span>
        </li>
      </ol>

      {propio ? (
        objetivos.length > 1 && (
          <div className="academy-selector">
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
        <div className="academy-aviso">
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
    { clase: 'coral', nombre: 'Hoy', valor: hoy },
    { clase: 'naranja', nombre: 'Con tu escenario', valor: proyectado },
    { clase: 'celeste', nombre: 'Tu objetivo', valor: objetivo.meta },
  ];

  return (
    <>
      <div className="objetivos-simulador">
        <div className="objetivos-controles">
          <h3>Cambios a probar</h3>
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
          <p>
            <strong>Supuesto:</strong> la cantidad vendida no cae al mover precios. Los controles
            arrancan en lo que recomienda Sami para llegar a tu meta.
          </p>
        </div>
        <div className="objetivos-resultados">
          <Metrica etiqueta="Resultado proyectado" valor={formatoPesos(proyectado)} />
          <Metrica
            etiqueta={falta === 0 ? 'Meta alcanzada' : 'Te faltaría'}
            valor={falta === 0 ? formatoPesos(objetivo.meta) : formatoPesos(falta)}
            alerta={falta > 0}
          />
          <Metrica etiqueta="Meta mensual" valor={formatoPesos(objetivo.meta)} />
          <div className="objetivos-grafico">
            <h3>Comparación</h3>
            <div>
              {barras.map((barra) => (
                <figure key={barra.nombre}>
                  <div
                    className={`objetivos-barra ${barra.clase}`}
                    style={{ height: `${Math.max(6, (barra.valor / maximo) * 100)}%` }}
                  />
                  <figcaption>
                    {barra.nombre}
                    <strong>{formatoPesos(barra.valor)}</strong>
                  </figcaption>
                </figure>
              ))}
            </div>
          </div>
        </div>
      </div>
      <Explicacion>
        {falta === 0
          ? `Con estos cambios llegarías a la meta: ventas aportan ${formatoPesos(aporte.ventas)}, margen ${formatoPesos(aporte.margen)} y gastos ${formatoPesos(aporte.gastos)} por mes. Si una palanca rinde menos de lo supuesto, el resultado quedaría por debajo.`
          : `Con estos cambios llegarías a ${formatoPesos(proyectado)} por mes y te faltarían ${formatoPesos(falta)}. Probá subir una palanca o combinar las tres. Es una estimación de demostración.`}
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
    <label className="objetivos-control">
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
        onChange={(evento) => onCambio(Number(evento.target.value))}
      />
    </label>
  );
}
