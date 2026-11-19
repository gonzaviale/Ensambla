import { Icon } from '@/components/sistema/Icon';
import { MINIMO_DE_COMERCIOS, frasePorComercios, type Zona } from '@/lib/sistema/zona';

export function SinDatosSuficientes({ zona }: { zona: Zona }) {
  return (
    <div className="empty">
      <Icon name="users" />
      <p>
        {frasePorComercios(zona.comercios)} Hacen falta al menos {MINIMO_DE_COMERCIOS} para mostrar
        datos, así nadie queda identificado.
      </p>
    </div>
  );
}
