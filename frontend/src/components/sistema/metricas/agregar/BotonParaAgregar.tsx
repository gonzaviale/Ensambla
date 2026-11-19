import { Icon } from '@/components/sistema/Icon';

type Props = {
  etiqueta?: string;
  yaLaTenes: boolean;
  guardando: boolean;
  onAgregar: () => void;
  texto?: string;
};

// Lo que ya está a la vista (guardado o en el panel de ventas) se muestra con un aviso en lugar del botón: no se
// agrega dos veces.
export function BotonParaAgregar({
  etiqueta,
  yaLaTenes,
  guardando,
  onAgregar,
  texto = 'Agregar',
}: Props) {
  if (yaLaTenes)
    return (
      <p className="aviso-ya-la-tenes" role="status">
        <Icon name="check-circle" />
        Ya la tenés en tu panel
      </p>
    );
  return (
    <button
      type="button"
      className="metricas-medida-agregar"
      disabled={guardando}
      aria-label={etiqueta}
      onClick={onAgregar}
    >
      {texto}
    </button>
  );
}
