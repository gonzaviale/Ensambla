import type { ReactNode } from 'react';
import { BarrasDeLaMetrica } from '@/components/sistema/metricas/graficos/BarrasDeLaMetrica';
import { DonaDeLaMetrica } from '@/components/sistema/metricas/graficos/DonaDeLaMetrica';
import { LineaDeLaMetrica } from '@/components/sistema/metricas/graficos/LineaDeLaMetrica';
import {
  elPrimero,
  partesParaMostrar,
  queEsElNumero,
  type PropsDelGrafico,
} from '@/lib/metricas/graficos';
import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { DefinicionDeMetrica, GraficoDeMetrica, ResultadoDeMetrica } from '@/types/metricas';

// Un dibujo por tipo de gráfico; el número grande va a la izquierda del gráfico, así que «Número» no agrega nada más.
const DIBUJOS: Record<GraficoDeMetrica, (props: PropsDelGrafico) => ReactNode> = {
  NUMERO: () => null,
  BARRAS: BarrasDeLaMetrica,
  DONA: DonaDeLaMetrica,
  LINEA: LineaDeLaMetrica,
};

type Props = {
  definicion: DefinicionDeMetrica;
  resultado: ResultadoDeMetrica;
  grafico: GraficoDeMetrica;
};

/**
 * El número que da la métrica y, si se reparte, su gráfico. Si no se pudo calcular no muestra nada (nunca un cero): el
 * motivo va resaltado junto al nombre, en {@link NombreDeLaMetrica}.
 */
export function GraficoDeLaMetrica({ definicion, resultado, grafico }: Props) {
  if (resultado.valor == null) return null;
  const Dibujo = DIBUJOS[grafico];
  return (
    <div className="grafico-metrica" data-grafico={grafico}>
      <div className="grafico-metrica-numero">
        <p className="metrica-medida-valor">
          {formatoDelValor(resultado.valor, resultado.formato)}
        </p>
        <NotaDelNumero definicion={definicion} resultado={resultado} />
      </div>
      {grafico !== 'NUMERO' && (
        <div className="grafico-metrica-dibujo">
          {sinPartes(grafico, resultado) ? (
            <SinPartesParaDibujar />
          ) : (
            <Dibujo
              definicion={definicion}
              resultado={resultado}
              partes={partesParaMostrar(definicion, resultado)}
            />
          )}
        </div>
      )}
    </div>
  );
}

// Dice qué es el número grande y, si se reparte, quién lo encabeza: así no hay que adivinar de dónde sale.
function NotaDelNumero({ definicion, resultado }: Pick<Props, 'definicion' | 'resultado'>) {
  if (definicion.agrupar === 'NINGUNA') return null;
  const primero = elPrimero(definicion, resultado);
  return (
    <p className="grafico-metrica-nota">
      {queEsElNumero(definicion)}
      {primero && (
        <>
          {' · El primero: '}
          <strong>{primero}</strong>
        </>
      )}
    </p>
  );
}

// Una métrica repartida que dio número pero no tiene partes (por ejemplo, sin movimientos en el período) no dibuja un
// gráfico vacío: lo dice.
const sinPartes = (grafico: GraficoDeMetrica, resultado: ResultadoDeMetrica) =>
  grafico !== 'NUMERO' && resultado.filas.length === 0;

function SinPartesParaDibujar() {
  return (
    <p className="metrica-medida-motivo" role="status">
      Todavía no hay datos para esta métrica en este período.
    </p>
  );
}
