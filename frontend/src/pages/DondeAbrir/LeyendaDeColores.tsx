import { ETIQUETA_DE_COINCIDENCIA, type Coincidencia } from '@/lib/dondeAbrir';

const ORDEN: Coincidencia[] = ['buena', 'intermedia', 'mala'];

// Qué quiere decir cada color de las zonas. Chica, para leerla de un vistazo.
export function LeyendaDeColores() {
  return (
    <ul className="donde-leyenda" aria-label="Qué significa cada color">
      {ORDEN.map((coincidencia) => (
        <li key={coincidencia} className={coincidencia}>
          <i aria-hidden />
          {ETIQUETA_DE_COINCIDENCIA[coincidencia]}
        </li>
      ))}
    </ul>
  );
}
