import { Line, LineChart, Tooltip, XAxis, YAxis } from 'recharts';
import { LienzoDelGrafico } from '@/components/sistema/metricas/graficos/LienzoDelGrafico';
import { ListaParaElLector } from '@/components/sistema/metricas/graficos/ListaParaElLector';
import { TarjetaDelDato } from '@/components/sistema/metricas/graficos/TarjetaDelDato';
import { datosDelGrafico, type PropsDelGrafico } from '@/lib/metricas/graficos';

const ALTO = 240;
const LARGO_DE_LOS_DIAS = 3;

// La línea une los días de la semana en su orden, con un punto por día; al pasar el mouse el punto dice su valor.
export function LineaDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const datos = datosDelGrafico(partes, resultado.formato, (dia) =>
    dia.slice(0, LARGO_DE_LOS_DIAS),
  );
  return (
    <figure className="grafico-recharts" aria-label={definicion.nombre}>
      <LienzoDelGrafico alto={ALTO}>
        <LineChart data={datos} margin={{ top: 16, right: 20, bottom: 4, left: 20 }}>
          <XAxis
            dataKey="corta"
            tickLine={false}
            axisLine={{ stroke: '#00506a', strokeWidth: 2 }}
            tick={{ fill: '#263443', fontSize: 12, fontWeight: 700 }}
          />
          <YAxis hide domain={[0, 'auto']} />
          <Tooltip
            content={TarjetaDelDato}
            cursor={{ stroke: '#00506a', strokeDasharray: '4 4' }}
          />
          <Line
            type="monotone"
            dataKey="valor"
            stroke="#00506a"
            strokeWidth={4}
            dot={{ r: 6, fill: '#ee8000', stroke: '#fff', strokeWidth: 3 }}
            activeDot={{ r: 8, fill: '#f85f5a', stroke: '#fff', strokeWidth: 3 }}
          />
        </LineChart>
      </LienzoDelGrafico>
      <ListaParaElLector datos={datos} />
    </figure>
  );
}
