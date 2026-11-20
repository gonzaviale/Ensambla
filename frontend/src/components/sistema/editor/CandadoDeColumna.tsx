import { esColumnaFija } from '@/lib/sistema/estructuraFija';
import type { Campo } from '@/lib/sistema/tipos';
import { Icon } from '../Icon';

const EXPLICACION = 'La usa Ensambla para funcionar. Podés cambiarle el nombre, pero no quitarla.';

export function CandadoDeColumna({ campo }: { campo: Campo }) {
  if (!esColumnaFija(campo)) return null;
  return (
    <span className="columna-fija" role="img" title={EXPLICACION} aria-label={EXPLICACION}>
      <Icon name="lock" />
    </span>
  );
}
