import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { PlanesCuenta } from './PlanesCuenta';
import { PlanesPublicos } from './PlanesPublicos';

export function PlanesPage() {
  const usuario = useUsuarioOpcional();
  return usuario ? <PlanesCuenta /> : <PlanesPublicos />;
}
