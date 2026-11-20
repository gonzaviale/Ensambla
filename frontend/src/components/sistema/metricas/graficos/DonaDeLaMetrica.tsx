import { Cell, Pie, PieChart, Tooltip } from 'recharts';
import { LienzoDelGrafico } from '@/components/sistema/metricas/graficos/LienzoDelGrafico';
import { TarjetaDelDato } from '@/components/sistema/metricas/graficos/TarjetaDelDato';
import {
  datosDelGrafico,
  type DatoDelGrafico,
  type PropsDelGrafico,
} from '@/lib/metricas/graficos';

const LADO = 176;

// La torta como dona, a la izquierda, y al lado cada parte con su color, su valor y su porcentaje. Pasando el mouse por
// una porción se ve de quién es. Con muchas partes la lista crece sin deformar el dibujo.
export function DonaDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const datos = datosDelGrafico(partes, resultado.formato, (etiqueta) => etiqueta);
  return (
    <figure className="grafico-dona" aria-label={definicion.nombre}>
      <div className="grafico-dona-dibujo">
        <LienzoDelGrafico alto={LADO}>
          <PieChart>
            <Pie
              isAnimationActive={false}
              data={datos}
              dataKey="valor"
              nameKey="etiqueta"
              innerRadius="60%"
              outerRadius="96%"
              paddingAngle={2}
              stroke="#fff"
              strokeWidth={2}
            >
              {datos.map((dato) => (
                <Cell key={dato.etiqueta} fill={dato.color} />
              ))}
            </Pie>
            <Tooltip content={TarjetaDelDato} />
          </PieChart>
        </LienzoDelGrafico>
      </div>
      <ListaDeLasPorciones datos={datos} />
    </figure>
  );
}

function ListaDeLasPorciones({ datos }: { datos: DatoDelGrafico[] }) {
  return (
    <ul className="grafico-dona-lista">
      {datos.map((dato) => (
        <li key={dato.etiqueta}>
          <span
            className="grafico-dona-color"
            style={{ background: dato.color }}
            aria-hidden="true"
          />
          <span className="grafico-dona-etiqueta">{dato.etiqueta}</span>
          <strong>{dato.detalle}</strong>
          <span className="grafico-dona-porcentaje">{dato.porcentaje}%</span>
        </li>
      ))}
    </ul>
  );
}
