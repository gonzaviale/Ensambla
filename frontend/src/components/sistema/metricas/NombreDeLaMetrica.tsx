import { Warning } from '@phosphor-icons/react';
import type { ResultadoDeMetrica } from '@/types/metricas';

type Props = { nombre: string; resultado: ResultadoDeMetrica };

/**
 * El nombre de la métrica y, al lado, el aviso de lo que falta cargar cuando no se puede calcular: es lo primero que
 * tiene que ver la persona, así que va resaltado junto al título y no perdido debajo.
 */
export function NombreDeLaMetrica({ nombre, resultado }: Props) {
  return (
    <div className="metrica-medida-titulo">
      <h3>{nombre}</h3>
      <AvisoSinDatos resultado={resultado} />
    </div>
  );
}

export function AvisoSinDatos({ resultado }: { resultado: ResultadoDeMetrica }) {
  if (resultado.valor != null) return null;
  return (
    <p className="metrica-medida-aviso" role="note">
      <Warning weight="fill" aria-hidden="true" />
      <span>{resultado.motivo ?? 'Todavía no se puede calcular.'}</span>
    </p>
  );
}
