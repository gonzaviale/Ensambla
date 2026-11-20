import { Bar, BarChart, Cell, LabelList, Tooltip, XAxis, YAxis } from 'recharts';
import { LienzoDelGrafico } from '@/components/sistema/metricas/graficos/LienzoDelGrafico';
import { ListaParaElLector } from '@/components/sistema/metricas/graficos/ListaParaElLector';
import { TarjetaDelDato } from '@/components/sistema/metricas/graficos/TarjetaDelDato';
import { datosDelGrafico, type PropsDelGrafico } from '@/lib/metricas/graficos';

const ALTO = 260;
const COLOR_DEL_TEXTO = '#263443';
const MAS_PARTES_QUE_NO_ENTRAN_ESCRITAS = 7;
const LARGO_DE_LOS_DIAS = 3;

// Barras paradas, una por parte, del color de la solapa; «Otros» en arena. Al pasar el mouse cada barra dice de quién
// es y cuánto vale. Los días van con tres letras; los nombres largos se recortan y se leen completos en la tarjeta.
export function BarrasDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const datos = datosDelGrafico(
    partes,
    resultado.formato,
    definicion.agrupar === 'DIA_DE_LA_SEMANA'
      ? (dia) => dia.slice(0, LARGO_DE_LOS_DIAS)
      : undefined,
  );
  return (
    <figure className="grafico-recharts" aria-label={definicion.nombre}>
      <LienzoDelGrafico alto={ALTO}>
        <BarChart data={datos} margin={{ top: 26, right: 8, bottom: 4, left: 8 }}>
          <XAxis
            dataKey="corta"
            interval={0}
            tickLine={false}
            axisLine={{ stroke: '#00506a', strokeWidth: 2 }}
            tick={{ fill: COLOR_DEL_TEXTO, fontSize: 12, fontWeight: 700 }}
          />
          <YAxis hide domain={[0, 'auto']} />
          <Tooltip content={TarjetaDelDato} cursor={{ fill: '#e6f3f7' }} />
          <Bar dataKey="valor" radius={[8, 8, 0, 0]} maxBarSize={64}>
            {datos.map((dato) => (
              <Cell
                key={dato.etiqueta}
                fill={dato.otros ? dato.color : 'var(--color-solapa, #00506a)'}
              />
            ))}
            {datos.length <= MAS_PARTES_QUE_NO_ENTRAN_ESCRITAS && (
              <LabelList
                dataKey="detalle"
                position="top"
                fill={COLOR_DEL_TEXTO}
                fontSize={12}
                fontWeight={700}
              />
            )}
          </Bar>
        </BarChart>
      </LienzoDelGrafico>
      <ListaParaElLector datos={datos} />
    </figure>
  );
}
