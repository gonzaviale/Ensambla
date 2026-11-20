import { useId } from 'react';
import { formatoPesos } from '@/lib/objetivos';
import { SENSIBILIDADES, type Paso, type Sensibilidad } from '@/lib/objetivosPasos';

type Props = {
  paso: Paso;
  valor: number;
  sensibilidad: Sensibilidad;
  onCambiar: (valor: number) => void;
};

// El monto de un paso lo elige la persona. Si el paso cambia un precio, una curva muestra cuánto ganaría con cada
// monto: sube un tramo y después baja, porque con un precio muy alto se vende demasiado poco.
export function AjusteDePaso({ paso, valor, sensibilidad, onCambiar }: Props) {
  const idCampo = useId();
  const { ajuste, curva } = paso;
  if (!ajuste) return null;
  const { verbo, desde, hasta, paso: salto, sugerido } = ajuste;
  const recomendado = sugerido(sensibilidad);

  const montos: number[] = [];
  const cada = Math.max(1, Math.ceil(((hasta - desde) / salto + 1) / 12));
  for (let monto = desde; monto <= hasta; monto += salto * cada) montos.push(monto);
  const ganancias = curva ? montos.map((monto) => curva(monto, sensibilidad)) : [];
  const techo = Math.max(1, ...ganancias.map(Math.abs));
  const elegida = montos.reduce(
    (cercano, monto, indice) =>
      Math.abs(monto - valor) < Math.abs((montos[cercano] ?? 0) - valor) ? indice : cercano,
    0,
  );

  return (
    <div className="objetivos-ajuste">
      <label htmlFor={idCampo}>
        {verbo} <strong>{formatoPesos(valor)}</strong>
        {valor === recomendado && <span> · el que más te conviene</span>}
      </label>
      <input
        id={idCampo}
        type="range"
        min={desde}
        max={hasta}
        step={salto}
        value={valor}
        aria-valuetext={formatoPesos(valor)}
        onChange={(evento) => onCambiar(Number(evento.target.value))}
      />
      <div className="objetivos-ajuste-extremos">
        <span>{formatoPesos(desde)}</span>
        <span>{formatoPesos(hasta)}</span>
      </div>
      {curva && (
        <div
          className="objetivos-curva"
          role="img"
          aria-label={`Lo que ganarías de más por mes con cada monto: ${montos
            .map(
              (monto, indice) => `${formatoPesos(monto)} ${formatoPesos(ganancias[indice] ?? 0)}`,
            )
            .join(', ')}`}
        >
          {montos.map((monto, indice) => {
            const ganancia = ganancias[indice] ?? 0;
            return (
              <i
                key={monto}
                className={`${ganancia < 0 ? 'negativa' : ''} ${indice === elegida ? 'elegida' : ''}`.trim()}
                style={{ height: `${Math.max(6, (Math.abs(ganancia) / techo) * 100)}%` }}
              />
            );
          })}
        </div>
      )}
      {curva && (
        <p className="objetivos-curva-nota">
          Cada barra es lo que ganarías de más por mes con ese monto. Si subís demasiado, se vende
          tan poco que se gana menos (barras rojas).
        </p>
      )}
    </div>
  );
}

// Cómo reaccionan los clientes cuando sube un precio: de eso depende cuánto se deja de vender.
export function Sensibilidades({
  sensibilidad,
  onCambiar,
}: {
  sensibilidad: Sensibilidad;
  onCambiar: (sensibilidad: Sensibilidad) => void;
}) {
  const nombre = useId();
  return (
    <fieldset className="objetivos-sensibilidad">
      <legend>Si subís un precio, tus clientes…</legend>
      {SENSIBILIDADES.map((opcion) => (
        <label key={opcion.clave}>
          <input
            type="radio"
            name={nombre}
            checked={sensibilidad === opcion.clave}
            onChange={() => onCambiar(opcion.clave)}
          />
          {opcion.etiqueta}
        </label>
      ))}
    </fieldset>
  );
}
