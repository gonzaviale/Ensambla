import { createFileRoute } from '@tanstack/react-router';
import { CuadernosPage } from '@/pages/Cuadernos/CuadernosPage';

export const Route = createFileRoute('/_app/cuadernos')({
  component: CuadernosPage,
});
