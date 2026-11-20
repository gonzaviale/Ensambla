import { GraficoDeLaMetrica } from '@/components/sistema/metricas/GraficoDeLaMetrica';
import { EscalaDelAlto } from '@/components/sistema/metricas/graficos/escalaDelAlto';
import { graficoDeSiempre } from '@/lib/metricas/graficos';
import type { DefinicionDeMetrica, ResultadoDeMetrica } from '@/types/metricas';

// En la ventana el gráfico ocupa menos alto, para ver todo sin scrollear.
const ESCALA_DE_LA_VISTA_PREVIA = 0.7;

type Props = { definicion: DefinicionDeMetrica; resultado: ResultadoDeMetrica; chica?: boolean };

/** La métrica dibujada con el mismo gráfico con el que va a quedar en su solapa; la chica sólo muestra el número. */
export function VistaPrevia({ definicion, resultado, chica = false }: Props) {
  return (
    <div className={chica ? 'vista-previa vista-previa-chica' : 'vista-previa'}>
      <EscalaDelAlto.Provider value={ESCALA_DE_LA_VISTA_PREVIA}>
        <GraficoDeLaMetrica
          definicion={definicion}
          resultado={resultado}
          grafico={chica ? 'NUMERO' : graficoDeSiempre(definicion.agrupar)}
        />
      </EscalaDelAlto.Provider>
    </div>
  );
}
