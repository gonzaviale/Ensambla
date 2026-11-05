import type { Canal } from '@/types/promociones';

type Props = {
  canales: Canal[];
  elegido: string | null;
  deshabilitado: boolean;
  onElegir: (clave: string) => void;
};

// Dónde se va a compartir. Sami escribe el texto con el tono de cada lugar; la persona lo revisa antes de usarlo.
export function ElegirCanal({ canales, elegido, deshabilitado, onElegir }: Props) {
  return (
    <div className="promociones-canales" role="group" aria-label="Dónde lo vas a compartir">
      {canales.map((canal) => (
        <button
          key={canal.clave}
          type="button"
          className={canal.clave === elegido ? 'btn primary' : 'btn secondary'}
          aria-pressed={canal.clave === elegido}
          disabled={deshabilitado}
          onClick={() => onElegir(canal.clave)}
        >
          {canal.nombre}
        </button>
      ))}
    </div>
  );
}
