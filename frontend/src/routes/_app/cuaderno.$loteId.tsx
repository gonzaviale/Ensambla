import { createFileRoute } from '@tanstack/react-router';
import { RevisarCuadernoPage } from '@/pages/Cuaderno/RevisarCuadernoPage';

export const Route = createFileRoute('/_app/cuaderno/$loteId')({
  component: RevisarCuadernoPage,
});
