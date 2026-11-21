import { COLOR_DE_PUNTO } from '@/hooks/dondeAbrir/useMapaDeZonas';
import type { Recorrido } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { CAPAS } from '@/lib/dondeAbrir';
import type { DondeMostrar } from '@/lib/dondeAbrirRecorrido';

const DONDE_MOSTRAR: { valor: DondeMostrar; etiqueta: string }[] = [
  { valor: 'elegida', etiqueta: 'Solo en la zona elegida' },
  { valor: 'todas', etiqueta: 'En todas las zonas' },
];

function SelectorDeCapas({ recorrido }: { recorrido: Recorrido }) {
  const { capas, alternarCapa } = recorrido;
  return (
    <div className="donde-capas-lista">
      {CAPAS.map(({ tipo, etiqueta }) => (
        <label key={tipo} className={capas[tipo] ? 'marcada' : ''}>
          <input type="checkbox" checked={capas[tipo]} onChange={() => alternarCapa(tipo)} />
          <i aria-hidden style={{ background: COLOR_DE_PUNTO[tipo] }} />
          {etiqueta}
        </label>
      ))}
    </div>
  );
}

function SelectorDeZonasConPuntos({ recorrido }: { recorrido: Recorrido }) {
  const { dondeMostrar, cambiarDondeMostrar } = recorrido;
  return (
    <div className="donde-donde" role="radiogroup" aria-label="Dónde mostrarlo">
      {DONDE_MOSTRAR.map(({ valor, etiqueta }) => (
        <label key={valor} className={dondeMostrar === valor ? 'marcada' : ''}>
          <input
            type="radio"
            name="donde-mostrar"
            checked={dondeMostrar === valor}
            onChange={() => cambiarDondeMostrar(valor)}
          />
          {etiqueta}
        </label>
      ))}
    </div>
  );
}

export function CapasDelMapa({ recorrido }: { recorrido: Recorrido }) {
  return (
    <details className="donde-capas">
      <summary>Qué mostrar en el mapa</summary>
      <SelectorDeCapas recorrido={recorrido} />
      <SelectorDeZonasConPuntos recorrido={recorrido} />
    </details>
  );
}
