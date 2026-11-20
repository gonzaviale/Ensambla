import { Bar, BarChart, Cell, LabelList, Tooltip, XAxis, YAxis } from 'recharts';
import { LienzoDelGrafico } from '@/components/sistema/metricas/graficos/LienzoDelGrafico';
import { ListaParaElLector } from '@/components/sistema/metricas/graficos/ListaParaElLector';
import { TarjetaDelDato } from '@/components/sistema/metricas/graficos/TarjetaDelDato';
import { datosDelGrafico, type PropsDelGrafico } from '@/lib/metricas/graficos';

const ALTO = 220;
const COLOR_DEL_TEXTO = '#263443';
const LARGO_DE_LOS_DIAS = 3;
const PARTES_QUE_SE_LEEN_DE_FRENTE = 5;

// Barras paradas, una por parte y cada una de su color, con el valor arriba y el nombre abajo (inclinado si son
// muchas, para que no se encimen). «Otros» va en arena. Al pasar el mouse cada barra dice de quién es y cuánto vale.
export function BarrasDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const datos = datosDelGrafico(
    partes,
    resultado.formato,
    definicion.agrupar === 'DIA_DE_LA_SEMANA'
      ? (dia) => dia.slice(0, LARGO_DE_LOS_DIAS)
      : undefined,
  );
  const inclinado = datos.length > PARTES_QUE_SE_LEEN_DE_FRENTE;
  return (
    <figure className="grafico-recharts" aria-label={definicion.nombre}>
      <LienzoDelGrafico alto={ALTO}>
        <BarChart
          data={datos}
          margin={{ top: 28, right: 8, bottom: 4, left: 8 }}
          barCategoryGap="22%"
        >
          <XAxis
            dataKey="corta"
            interval={0}
            tickLine={false}
            axisLine={{ stroke: '#e6c9a8', strokeWidth: 2 }}
            angle={inclinado ? -35 : 0}
            textAnchor={inclinado ? 'end' : 'middle'}
            height={inclinado ? 64 : 30}
            tick={{ fill: COLOR_DEL_TEXTO, fontSize: 12, fontWeight: 700 }}
          />
          <YAxis hide domain={[0, 'auto']} />
          <Tooltip content={TarjetaDelDato} cursor={{ fill: '#f6ede1' }} />
          <Bar isAnimationActive={false} dataKey="valor" radius={[10, 10, 0, 0]} maxBarSize={48}>
            {datos.map((dato) => (
              <Cell key={dato.etiqueta} fill={dato.color} />
            ))}
            <LabelList
              dataKey="detalle"
              position="top"
              fill={COLOR_DEL_TEXTO}
              fontSize={12}
              fontWeight={800}
            />
          </Bar>
        </BarChart>
      </LienzoDelGrafico>
      <ListaParaElLector datos={datos} />
    </figure>
  );
}
