import type { Bloque } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';

type BarraDelBloqueProps = {
  bloque: Bloque;
  consulta: string;
  onConsulta: (consulta: string) => void;
  onEscanear?: () => void;
};

// Buscar, escanear, ver a pantalla completa y registrar: lo que se hace sobre la tabla de un bloque.
export function BarraDelBloque({ bloque, consulta, onConsulta, onEscanear }: BarraDelBloqueProps) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const verTablaCompleta = useSistemaStore((estado) => estado.verTablaCompleta);
  return (
    <div className={`block-bar ${onEscanear ? 'con-escaneo' : ''}`.trim()}>
      <label className="searchbox">
        <Icon name="search" />
        <input
          className="input"
          placeholder={`Buscar en ${bloque.name.toLowerCase()}`}
          aria-label={`Buscar en ${bloque.name}`}
          data-filter-block={bloque.id}
          value={consulta}
          onChange={(evento) => onConsulta(evento.target.value)}
        />
      </label>
      <span className="record-count">{bloque.rows.length} registros</span>
      {onEscanear && (
        <Btn cls="primary small" onClick={onEscanear}>
          <Icon name="camera" /> Escanear productos
        </Btn>
      )}
      <IconBtn
        name="expand"
        label={`Ver ${bloque.name} a pantalla completa`}
        data-accion="table-fullscreen"
        onClick={() => verTablaCompleta(bloque.id)}
      />
      <Btn
        cls="soft small"
        data-accion="add-record"
        data-tour="openRecord"
        data-tour-id={bloque.id}
        onClick={() => abrirModal({ tipo: 'registro', bloqueId: bloque.id })}
      >
        <Icon name="plus" /> Registrar
      </Btn>
    </div>
  );
}
