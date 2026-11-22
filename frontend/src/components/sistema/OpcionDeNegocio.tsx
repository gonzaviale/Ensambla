import { InicialDelNegocio } from '@/components/negocios/InicialDelNegocio';
import type { NegocioDeLaCuenta } from '@/types/negocios';
import { Icon } from './Icon';

type Props = {
  negocio: NegocioDeLaCuenta;
  posicion: number;
  detalle?: string;
  sePuedeEliminar: boolean;
  onElegir: () => void;
  onEliminar: () => void;
};

const MOTIVO_DEL_UNICO = 'No podés eliminar el único negocio de tu cuenta';

// Una fila del menú de negocios: elegir el negocio y, al costado, eliminarlo.
export function OpcionDeNegocio({
  negocio,
  posicion,
  detalle,
  sePuedeEliminar,
  onElegir,
  onEliminar,
}: Props) {
  return (
    <div className="negocio-selector-fila">
      <button
        type="button"
        role="menuitem"
        className={negocio.activo ? 'negocio-selector-opcion activa' : 'negocio-selector-opcion'}
        aria-current={negocio.activo ? 'true' : undefined}
        onClick={onElegir}
      >
        <InicialDelNegocio
          className="negocio-selector-inicial"
          nombre={negocio.nombre}
          posicion={posicion}
        />
        <span>
          <strong>{negocio.nombre}</strong>
          <small>{detalle}</small>
        </span>
        {negocio.activo && <Icon name="check" />}
      </button>
      <button
        type="button"
        role="menuitem"
        className="negocio-selector-eliminar"
        aria-label={`Eliminar ${negocio.nombre}`}
        title={sePuedeEliminar ? 'Eliminar este negocio' : MOTIVO_DEL_UNICO}
        disabled={!sePuedeEliminar}
        onClick={onEliminar}
      >
        <Icon name="trash" />
      </button>
    </div>
  );
}
