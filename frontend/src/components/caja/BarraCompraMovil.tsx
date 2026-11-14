import { Icon } from '@/components/sistema/Icon';
import { dineroCaja } from '@/lib/caja';

export function BarraCompraMovil({
  total,
  articulos,
  revisando,
  bloqueada,
  pendiente,
  onRevisar,
  onCobrar,
  onEscanear,
}: {
  total: number;
  articulos: number;
  revisando: boolean;
  bloqueada: boolean;
  pendiente: boolean;
  onRevisar: () => void;
  onCobrar: () => void;
  onEscanear: () => void;
}) {
  return (
    <div className="caja-barra-movil" aria-label="Acciones de la compra">
      <div className="caja-barra-total">
        <span role={pendiente ? 'status' : undefined}>
          {pendiente
            ? 'Guardando inventario…'
            : `${articulos} ${articulos === 1 ? 'artículo' : 'artículos'}`}
        </span>
        <strong>{dineroCaja(total)}</strong>
      </div>
      {revisando && (
        <button
          type="button"
          className="caja-atajo-escanear"
          aria-label="Volver a escanear"
          disabled={bloqueada}
          onClick={onEscanear}
        >
          <Icon name="camera" />
          <span>Escanear</span>
        </button>
      )}
      <button
        type="button"
        className="btn primary"
        aria-label={revisando ? 'Continuar al cobro' : 'Revisar compra'}
        disabled={bloqueada || (revisando && (!articulos || pendiente))}
        onClick={revisando ? onCobrar : onRevisar}
      >
        {revisando ? 'Cobrar' : 'Ver compra'}
        <Icon name="arrow" />
      </button>
    </div>
  );
}
