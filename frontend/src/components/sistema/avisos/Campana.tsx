import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';

// La campana de avisos del encabezado: el número y el color dicen cuántos hay y si alguno es urgente.
export function Campana() {
  const avisos = useSistemaStore((estado) => estado.avisos);
  const abrirPanel = useSistemaStore((estado) => estado.abrirPanel);
  const urgente = avisos.some((aviso) => aviso.prioridad >= 2);

  return (
    <button
      type="button"
      className={`alert-bell ${urgente ? 'urgente' : ''}`.trim()}
      aria-label={`Avisos de Sami: ${avisos.length}`}
      title="Avisos de Sami"
      onClick={() => abrirPanel()}
    >
      <Icon name="bell" />
      {avisos.length > 0 && (
        <span className="alert-badge">{avisos.length > 9 ? '9+' : avisos.length}</span>
      )}
    </button>
  );
}
