import { BotonParaAgregar } from '@/components/sistema/metricas/agregar/BotonParaAgregar';
import type { PropsDeLaPestana } from '@/components/sistema/metricas/agregar/tipos';
import { VistaPrevia } from '@/components/sistema/metricas/agregar/VistaPrevia';
import { NombreDeLaMetrica } from '@/components/sistema/metricas/NombreDeLaMetrica';
import { useMetricasSugeridas } from '@/hooks/metricas/queries';
import { resumenDeLaReceta } from '@/lib/metricas/metricasAMedida';
import type { SugeridaDelRubro } from '@/types/metricas';

/** Las métricas que el catálogo propone para el rubro, cada una con cómo se vería hoy. */
export function Sugeridas({ sistemaId, guardando, onAgregar }: PropsDeLaPestana) {
  const { sugeridas, cargando, fallo, reintentar } = useMetricasSugeridas(sistemaId, true);
  if (fallo) return <NoSePudieronTraer onReintentar={() => void reintentar()} />;
  if (cargando || !sugeridas)
    return (
      <p className="metricas-medida-aviso" role="status">
        Buscando métricas para tu negocio…
      </p>
    );
  return (
    <ul className="panel-agregar-sugeridas">
      {sugeridas.map((sugerida) => (
        <TarjetaSugerida
          key={`${sugerida.definicion.nombre}-${resumenDeLaReceta(sugerida.definicion)}`}
          sugerida={sugerida}
          guardando={guardando}
          onAgregar={() => onAgregar(sugerida.definicion, 'SUGERIDA')}
        />
      ))}
    </ul>
  );
}

function NoSePudieronTraer({ onReintentar }: { onReintentar: () => void }) {
  return (
    <p className="metricas-medida-aviso" role="status">
      No pudimos traer las sugeridas.{' '}
      <button type="button" onClick={onReintentar}>
        Probar de nuevo
      </button>
    </p>
  );
}

type PropsDeLaSugerida = { sugerida: SugeridaDelRubro; guardando: boolean; onAgregar: () => void };

function TarjetaSugerida({ sugerida, guardando, onAgregar }: PropsDeLaSugerida) {
  const { definicion, resultado, yaLaTenes } = sugerida;
  return (
    <li className="panel-agregar-sugerida">
      <NombreDeLaMetrica nombre={definicion.nombre} resultado={resultado} />
      <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
      <VistaPrevia definicion={definicion} resultado={resultado} chica />
      <BotonParaAgregar
        etiqueta={`Agregar ${definicion.nombre}`}
        yaLaTenes={yaLaTenes}
        guardando={guardando}
        onAgregar={onAgregar}
      />
    </li>
  );
}
