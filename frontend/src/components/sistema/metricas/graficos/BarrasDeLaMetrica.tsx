import type { ReactNode } from 'react';
import { GraficoDeBarras } from '@/components/sistema/metricas/GraficoDeBarras';
import type { PropsDelGrafico } from '@/lib/metricas/graficos';
import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { AgrupacionDeMetrica } from '@/types/metricas';

// Los días van como el gráfico de ventas del inicio (barras paradas, en orden); lo demás, acostado, para que entren
// los nombres de productos y clientes.
const BARRAS_PARA: Record<AgrupacionDeMetrica, (props: PropsDelGrafico) => ReactNode> = {
  NINGUNA: () => null,
  PRODUCTO: BarrasAcostadas,
  CLIENTE: BarrasAcostadas,
  CATEGORIA: BarrasAcostadas,
  DIA_DE_LA_SEMANA: BarrasParadas,
};

export function BarrasDeLaMetrica(props: PropsDelGrafico) {
  const Barras = BARRAS_PARA[props.definicion.agrupar];
  return <Barras {...props} />;
}

function BarrasParadas({ definicion, resultado, partes }: PropsDelGrafico) {
  const barras = partes.map((parte) => ({
    clave: parte.etiqueta,
    etiqueta: parte.etiqueta.slice(0, 3),
    nombre: parte.etiqueta,
    valor: parte.valor,
    detalle: formatoDelValor(parte.valor, resultado.formato),
  }));
  return <GraficoDeBarras barras={barras} titulo={definicion.nombre} variante="claro" />;
}

function BarrasAcostadas({ definicion, resultado, partes }: PropsDelGrafico) {
  // «Otros» junta muchas partes: la escala sale de las de verdad y su barra no pasa del ancho.
  const maximo = Math.max(0, ...partes.filter((parte) => !parte.otros).map((parte) => parte.valor));
  return (
    <ul className="metrica-medida-reparto" aria-label={definicion.nombre}>
      {partes.map((parte) => (
        <li key={parte.etiqueta} data-otros={parte.otros ? 'true' : undefined}>
          <span className="metrica-medida-etiqueta">{parte.etiqueta}</span>
          <span className="metrica-medida-barra" aria-hidden="true">
            <span style={{ width: anchoDeLaBarra(parte.valor, maximo) }} />
          </span>
          <span className="metrica-medida-numero">
            {formatoDelValor(parte.valor, resultado.formato)}
          </span>
        </li>
      ))}
    </ul>
  );
}

const anchoDeLaBarra = (valor: number, maximo: number) =>
  `${maximo > 0 ? Math.min(100, Math.max(4, (valor / maximo) * 100)) : 0}%`;
