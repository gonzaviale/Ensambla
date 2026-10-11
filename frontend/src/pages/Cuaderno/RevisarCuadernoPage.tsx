import { useNavigate, useParams } from '@tanstack/react-router';
import { LecturaLote } from '@/components/Cuaderno/LecturaLote';

// T-100 y T-101: la lectura de un lote tiene su propia dirección, así que se puede recargar o volver
// a ella sin perder lo que se estaba revisando.
export function RevisarCuadernoPage() {
  const { loteId } = useParams({ from: '/_app/cuaderno/$loteId' });
  const navigate = useNavigate();

  return <LecturaLote loteId={loteId} onSubirMas={() => void navigate({ to: '/cuaderno' })} />;
}
