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

/** El número que da la métrica y, si se reparte, su gráfico; si no se pudo calcular, el motivo (nunca un cero). */
export function GraficoDeLaMetrica({ definicion, resultado, grafico }: Props) {
  if (resultado.valor == null) return <MotivoSinNumero resultado={resultado} />;
  const Dibujo = DIBUJOS[grafico];
  return (
    <div className="grafico-metrica" data-grafico={grafico}>
      <div className="grafico-metrica-numero">
        <p className="metrica-medida-valor">
          {formatoDelValor(resultado.valor, resultado.formato)}
        </p>
        <NotaDelNumero definicion={definicion} resultado={resultado} />
      </div>
      <div className="grafico-metrica-dibujo">
        <Dibujo
          definicion={definicion}
          resultado={resultado}
          partes={partesParaMostrar(definicion, resultado)}
        />
      </div>
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

function MotivoSinNumero({ resultado }: { resultado: ResultadoDeMetrica }) {
  return (
    <p className="metrica-medida-motivo" role="status">
      {resultado.motivo ?? 'Todavía no se puede calcular.'}
    </p>
  );
}
