import { MetricasDelInicio } from '@/components/sistema/MetricasDelInicio';
import { AgregarMetrica } from '@/components/sistema/metricas/agregar/AgregarMetrica';
import { BarraDeSolapas } from '@/components/sistema/metricas/BarraDeSolapas';
import { SolapaDeMetrica } from '@/components/sistema/metricas/SolapaDeMetrica';
import { useMetricasAMedida } from '@/hooks/metricas/queries';
import { useSolapas, type Solapas } from '@/hooks/metricas/useSolapas';
import { colorDeLaMetrica, idDeLaSolapa, idDelCuerpo } from '@/lib/metricas/solapas';
import type { Sistema } from '@/lib/sistema/tipos';
import type { Metrica } from '@/types/metricas';

// «Tu negocio» en solapas: la primera, fija, son las ventas de siempre; cada métrica que la persona agrega es una
// solapa más, con su contenido en el mismo panel. Al final, un botón abre el panel del costado para agregar otra.
export function PanelDeTuNegocio({ sistema }: { sistema: Sistema }) {
  const { metricas = [], fallo } = useMetricasAMedida(sistema.id);
  const solapas = useSolapas(metricas);
  return (
    <div className="panel-negocio">
      <BarraDeSolapas metricas={metricas} solapas={solapas} onAgregar={solapas.abrirElPanel} />
      <div
        className="panel-negocio-cuerpo"
        role="tabpanel"
        id={idDelCuerpo(solapas.activa)}
        aria-labelledby={idDeLaSolapa(solapas.activa)}
      >
        <CuerpoDeLaSolapa sistema={sistema} metricas={metricas} solapas={solapas} />
      </div>
      {solapas.agregando && (
        <AgregarMetrica
          sistemaId={sistema.id}
          onCerrar={solapas.cerrarElPanel}
          onAgregada={solapas.abrirLaAgregada}
        />
      )}
      {fallo && (
        <p className="metricas-medida-aviso" role="status">
          No pudimos traer tus métricas ahora.
        </p>
      )}
    </div>
  );
}

type PropsDelCuerpo = { sistema: Sistema; metricas: Metrica[]; solapas: Solapas };

function CuerpoDeLaSolapa({ sistema, metricas, solapas }: PropsDelCuerpo) {
  if (!solapas.metricaActiva) return <MetricasDelInicio sistema={sistema} />;
  return (
    <SolapaAbierta
      sistema={sistema}
      metricas={metricas}
      solapas={solapas}
      metrica={solapas.metricaActiva}
    />
  );
}

function SolapaAbierta({
  sistema,
  metricas,
  solapas,
  metrica,
}: PropsDelCuerpo & { metrica: Metrica }) {
  const lugar = metricas.indexOf(metrica);
  return (
    <SolapaDeMetrica
      metrica={metrica}
      sistemaId={sistema.id}
      lugar={lugar}
      ultimoLugar={metricas.length - 1}
      color={colorDeLaMetrica(metrica.id, metricas)}
      onQuitada={solapas.volverALasVentas}
    />
  );
}
