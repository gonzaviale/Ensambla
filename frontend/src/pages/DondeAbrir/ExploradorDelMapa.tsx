import { useMemo } from 'react';
import type { Recorrido } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { filtrosActivos } from '@/lib/dondeAbrir';
import { CapasDelMapa } from './CapasDelMapa';
import { LeyendaDeColores } from './LeyendaDeColores';
import { MapaDeZonas } from './MapaDeZonas';
import { PanelDeFiltros } from './PanelDeFiltros';
import { RadioDeBusqueda } from './RadioDeBusqueda';

function MensajeDelPunto({ recorrido }: { recorrido: Recorrido }) {
  const { punto, consulta, quitarPunto } = recorrido;
  if (punto && consulta.isFetching)
    return <p>Estamos midiendo ese lugar con datos de OpenStreetMap…</p>;
  if (!punto || consulta.isError) {
    return <p>Tocá cualquier lugar del mapa para medirlo y compararlo con las zonas.</p>;
  }
  return (
    <p>
      Mediste un lugar a mano: se cuenta lo que hay a 500 m, ahora mismo.{' '}
      <button type="button" className="btn ghost small" onClick={quitarPunto}>
        Quitar el lugar
      </button>
    </p>
  );
}

function ControlesDelExplorador({ recorrido }: { recorrido: Recorrido }) {
  const { filtros, cambiarFiltros } = recorrido;
  const activos = useMemo(() => filtrosActivos(filtros), [filtros]);
  return (
    <aside className="donde-explorador-controles">
      <PanelDeFiltros filtros={filtros} activos={activos.length} onCambiar={cambiarFiltros} />
      <CapasDelMapa recorrido={recorrido} />
    </aside>
  );
}

function ColumnaDelMapa({ recorrido }: { recorrido: Recorrido }) {
  const { filtros, consulta, punto } = recorrido;
  return (
    <div className="donde-mapa-columna">
      <RadioDeBusqueda valor={filtros.limites.distanciaMaxKm} onCambiar={recorrido.cambiarRadio} />
      <MapaDeZonas
        ranking={recorrido.ranking}
        descartadas={recorrido.descartadas}
        seleccionadaId={recorrido.elegida?.zona.id ?? ''}
        puntos={recorrido.puntos}
        capas={recorrido.capas}
        onSeleccionar={recorrido.elegirZona}
        onElegirPunto={recorrido.elegirPunto}
        punto={punto}
        puntoElegidoId={recorrido.puntoElegidoId}
        localActual={consulta.data?.negocioActual}
      />
      <div className="donde-punto" role="status">
        <MensajeDelPunto recorrido={recorrido} />
      </div>
      <LeyendaDeColores capas={recorrido.capas} hayLugarElegido={Boolean(punto)} />
    </div>
  );
}

/** Los filtros y capas a la izquierda y el mapa con lo que hay alrededor de cada zona. */
export function ExploradorDelMapa({ recorrido }: { recorrido: Recorrido }) {
  return (
    <div className="donde-explorador">
      <ControlesDelExplorador recorrido={recorrido} />
      <ColumnaDelMapa recorrido={recorrido} />
    </div>
  );
}
