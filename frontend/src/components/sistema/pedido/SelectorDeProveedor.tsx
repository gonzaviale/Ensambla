import type { GrupoDeReposicion } from '@/types/reposicion';
import { nombreDelGrupo } from '@/lib/sistema/pedidoProveedor';

type Props = {
  grupos: GrupoDeReposicion[];
  elegido: number;
  onElegir: (posicion: number) => void;
};

// Una solapa por proveedor, con cuántos productos hay que pedirle.
export function SelectorDeProveedor({ grupos, elegido, onElegir }: Props) {
  return (
    <div className="pedido-proveedores" role="tablist" aria-label="Proveedores">
      {grupos.map((grupo, posicion) => (
        <button
          key={grupo.proveedor ?? 'sin-proveedor'}
          type="button"
          role="tab"
          aria-selected={posicion === elegido}
          className={posicion === elegido ? 'activa' : ''}
          onClick={() => onElegir(posicion)}
        >
          {nombreDelGrupo(grupo)} ({grupo.productos.length})
        </button>
      ))}
    </div>
  );
}
