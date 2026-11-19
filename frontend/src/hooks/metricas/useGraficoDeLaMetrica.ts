import { useCambiarGrafico } from '@/hooks/metricas/mutations';
import { graficoDeSiempre, graficosPosiblesPara } from '@/lib/metricas/graficos';
import type { GraficoDeMetrica, Metrica } from '@/types/metricas';

/**
 * Cómo se ve la métrica y cómo se cambia. Mientras el servidor guarda la elección ya se muestra la nueva; si el servidor
 * no manda el gráfico (uno anterior), se usa el de siempre para su reparto.
 */
export function useGraficoDeLaMetrica(metrica: Metrica, sistemaId: string) {
  const cambiar = useCambiarGrafico(sistemaId, metrica.id);
  const guardado = metrica.grafico ?? graficoDeSiempre(metrica.definicion.agrupar);
  return {
    grafico: cambiar.isPending ? cambiar.variables : guardado,
    posibles: metrica.graficosPosibles ?? graficosPosiblesPara(metrica.definicion.agrupar),
    elegir: (grafico: GraficoDeMetrica) => cambiar.mutate(grafico),
    fallo: cambiar.isError,
  };
}
