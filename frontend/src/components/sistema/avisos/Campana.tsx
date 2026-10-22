import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';

// La campana de avisos del encabezado: el número y el color dicen cuántos hay y si alguno es urgente.
export function Campana() {
  const avisos = useSistemaStore((estado) => estado.avisos);
  const bloqueados = useSistemaStore((estado) => estado.accesoAvisos?.bloqueadas ?? 0);
  const total = avisos.length + bloqueados;
  const abrirPanel = useSistemaStore((estado) => estado.abrirPanel);
  const urgente = avisos.some((aviso) => aviso.prioridad >= 2);

  return (
    <button
      type="button"
      className={`alert-bell ${urgente ? 'urgente' : ''}`.trim()}
      aria-label={`Avisos de Sami: ${total}${bloqueados ? `, ${bloqueados} disponibles con Premium` : ''}`}
      title="Avisos de Sami"
      onClick={() => abrirPanel()}
    >
      <Icon name="bell" />
      {total > 0 && <span className="alert-badge">{total > 9 ? '9+' : total}</span>}
    </button>
  );
}
