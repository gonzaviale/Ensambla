import { useCambiarGrafico } from '@/hooks/metricas/mutations';
import { graficoDeSiempre, graficosPosiblesPara } from '@/lib/metricas/graficos';
import type { GraficoDeMetrica, Metrica } from '@/types/metricas';

/**
 * Cómo se ve la métrica y cómo se cambia. Mientras el servidor guarda la elección ya se muestra la nueva; si el servidor
 * no manda el gráfico (uno anterior), se usa el de siempre para su reparto.
 */
export function useGraficoDeLaMetrica(metrica: Metrica, sistemaId: string) {
  const cambiar = useCambiarGrafico(sistemaId, metrica.id);
  const posibles = metrica.graficosPosibles ?? graficosPosiblesPara(metrica.definicion.agrupar);
  const guardado = elDeSiempreSiEraElNumero(
    metrica.grafico ?? graficoDeSiempre(metrica.definicion.agrupar),
    metrica,
    posibles,
  );
  return {
    grafico: cambiar.isPending ? cambiar.variables : guardado,
    posibles,
    elegir: (grafico: GraficoDeMetrica) => cambiar.mutate(grafico),
    fallo: cambiar.isError,
  };
}

// El número ya no se elige aparte: una métrica repartida que había quedado en «Número» vuelve al gráfico de siempre.
const elDeSiempreSiEraElNumero = (
  grafico: GraficoDeMetrica,
  metrica: Metrica,
  posibles: GraficoDeMetrica[],
) =>
  grafico === 'NUMERO' && posibles.length > 1
    ? graficoDeSiempre(metrica.definicion.agrupar)
    : grafico;
