import { useState, type ReactNode } from 'react';
import { BarrasDeLaMetrica } from '@/components/sistema/metricas/graficos/BarrasDeLaMetrica';
import { DonaDeLaMetrica } from '@/components/sistema/metricas/graficos/DonaDeLaMetrica';
import { LineaDeLaMetrica } from '@/components/sistema/metricas/graficos/LineaDeLaMetrica';
import { hayMasPartes, partesParaMostrar, type PropsDelGrafico } from '@/lib/metricas/graficos';
import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { DefinicionDeMetrica, GraficoDeMetrica, ResultadoDeMetrica } from '@/types/metricas';

// Un dibujo por tipo de gráfico; el número grande va siempre arriba, así que «Número» no agrega nada más.
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
  const [todas, setTodas] = useState(false);
  if (resultado.valor == null) return <MotivoSinNumero resultado={resultado} />;
  const Dibujo = DIBUJOS[grafico];
  return (
    <div className="grafico-metrica">
      <p className="metrica-medida-valor">{formatoDelValor(resultado.valor, resultado.formato)}</p>
      <Dibujo
        definicion={definicion}
        resultado={resultado}
        partes={partesParaMostrar(definicion, resultado, todas)}
      />
      <VerTodo
        grafico={grafico}
        definicion={definicion}
        resultado={resultado}
        todas={todas}
        onCambiar={setTodas}
      />
    </div>
  );
}

function MotivoSinNumero({ resultado }: { resultado: ResultadoDeMetrica }) {
  return (
    <p className="metrica-medida-motivo" role="status">
      {resultado.motivo ?? 'Todavía no se puede calcular.'}
    </p>
  );
}

// La línea sigue el orden de los días y el número no tiene partes: sólo las listas largas se recortan.
const SE_RECORTA: Record<GraficoDeMetrica, boolean> = {
  NUMERO: false,
  BARRAS: true,
  DONA: true,
  LINEA: false,
};

type PropsDeVerTodo = {
  grafico: GraficoDeMetrica;
  definicion: DefinicionDeMetrica;
  resultado: ResultadoDeMetrica;
  todas: boolean;
  onCambiar: (todas: boolean) => void;
};

function VerTodo({ grafico, definicion, resultado, todas, onCambiar }: PropsDeVerTodo) {
  if (!SE_RECORTA[grafico] || !hayMasPartes(definicion, resultado)) return null;
  return (
    <button
      type="button"
      className="grafico-metrica-ver-todo"
      aria-expanded={todas}
      onClick={() => onCambiar(!todas)}
    >
      {todas ? 'Ver menos' : `Ver las ${resultado.filas.length}`}
    </button>
  );
}
