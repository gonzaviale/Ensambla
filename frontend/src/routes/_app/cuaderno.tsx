import { createFileRoute } from '@tanstack/react-router';
import { SubirCuadernoPage } from '@/pages/Cuaderno/SubirCuadernoPage';

export const Route = createFileRoute('/_app/cuaderno')({
  component: SubirCuadernoPage,
});
