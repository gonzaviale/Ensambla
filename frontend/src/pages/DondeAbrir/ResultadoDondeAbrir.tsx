import { CopyIcon } from '@phosphor-icons/react';
import type { Recorrido } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { ComparacionDeZonas } from './ComparacionDeZonas';
import { DetalleDeZona } from './DetalleDeZona';
import { EstadoDeLaConsulta } from './EstadoDeLaConsulta';
import { ExploradorDelMapa } from './ExploradorDelMapa';
import { ListaDeZonas } from './ListaDeZonas';
import { RecomendacionDeSami } from './RecomendacionDeSami';

function ComparacionDelPodio({ recorrido }: { recorrido: Recorrido }) {
  if (recorrido.mejores.length === 0) return null;
  return <ComparacionDeZonas zonas={recorrido.mejores.slice(0, 3)} filtros={recorrido.filtros} />;
}

function BarraDeControl({ recorrido }: { recorrido: Recorrido }) {
  return (
    <div className="donde-barra-de-control">
      <p>Explorá el mapa y ajustá solo lo que necesites.</p>
      <div className="donde-acciones">
        <button type="button" className="btn ghost small" onClick={recorrido.copiarResumen}>
          <CopyIcon aria-hidden size={16} /> Copiar resumen
        </button>
        <button type="button" className="btn ghost small" onClick={recorrido.irALasPreguntas}>
          Cambiar mis respuestas
        </button>
      </div>
    </div>
  );
}

function RankingCompleto({ recorrido }: { recorrido: Recorrido }) {
  return (
    <section className="donde-ranking-completo" aria-labelledby="donde-ranking-titulo">
      <div className="donde-seccion-cabeza">
        <div>
          <p className="objetivos-etiqueta">Ranking completo</p>
          <h2 id="donde-ranking-titulo">Todas las zonas analizadas</h2>
        </div>
        <p>Elegí una para ver el detalle y los trámites.</p>
      </div>
      <ListaDeZonas
        ranking={recorrido.ranking}
        descartadas={recorrido.descartadas}
        seleccionadaId={recorrido.elegida?.zona.id ?? ''}
        cambios={recorrido.cambios}
        onElegir={recorrido.elegirZona}
      />
    </section>
  );
}

function DetalleDeLaZonaElegida({ recorrido }: { recorrido: Recorrido }) {
  if (!recorrido.elegida) return null;
  return (
    <DetalleDeZona
      item={recorrido.elegida}
      filtros={recorrido.filtros}
      superficie={recorrido.superficie}
      onSuperficie={recorrido.cambiarSuperficie}
      calculoAbierto={recorrido.calculoAbierto}
      onAlternarCalculo={recorrido.alternarCalculo}
    />
  );
}

/** El último paso: ranking, mapa, detalle de la zona elegida y trámites. */
export function ResultadoDondeAbrir({ recorrido }: { recorrido: Recorrido }) {
  return (
    <div className="donde-resultado">
      <EstadoDeLaConsulta recorrido={recorrido} />
      <RecomendacionDeSami recorrido={recorrido} />
      <ComparacionDelPodio recorrido={recorrido} />
      <BarraDeControl recorrido={recorrido} />
      <ExploradorDelMapa recorrido={recorrido} />
      <RankingCompleto recorrido={recorrido} />
      <DetalleDeLaZonaElegida recorrido={recorrido} />
      <p className="donde-nota-chica">
        Es una estimación para decidir mejor, no una predicción de ventas.
      </p>
    </div>
  );
}
