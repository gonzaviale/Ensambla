import { GraficoDeLaMetrica } from '@/components/sistema/metricas/GraficoDeLaMetrica';
import { graficoDeSiempre } from '@/lib/metricas/graficos';
import type { DefinicionDeMetrica, ResultadoDeMetrica } from '@/types/metricas';

type Props = { definicion: DefinicionDeMetrica; resultado: ResultadoDeMetrica; chica?: boolean };

/** La métrica dibujada con el mismo gráfico con el que va a quedar en su solapa; la chica sólo muestra el número. */
export function VistaPrevia({ definicion, resultado, chica = false }: Props) {
  return (
    <div className={chica ? 'vista-previa vista-previa-chica' : 'vista-previa'}>
      <GraficoDeLaMetrica
        definicion={definicion}
        resultado={resultado}
        grafico={chica ? 'NUMERO' : graficoDeSiempre(definicion.agrupar)}
      />
    </div>
  );
}
