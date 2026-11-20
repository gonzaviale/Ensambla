import { MapPinAreaIcon } from '@phosphor-icons/react';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';

// La puerta de entrada desde Objetivos: «¿Evaluás abrir otro local?».
export function EntradaDondeAbrir() {
  const navegacion = useNavegacion();
  return (
    <section
      className="donde-entrada solido"
      data-tono="celeste"
      aria-labelledby="donde-entrada-titulo"
    >
      <span className="donde-entrada-icono" aria-hidden="true">
        <MapPinAreaIcon size={28} />
      </span>
      <div>
        <h2 id="donde-entrada-titulo">¿Evaluás abrir otro local?</h2>
        <p>Compará zonas con el mapa y los motivos de cada una.</p>
      </div>
      <button
        type="button"
        className="btn secondary"
        onClick={() => void navegacion.irADondeAbrir()}
      >
        Ver dónde conviene
      </button>
    </section>
  );
}
