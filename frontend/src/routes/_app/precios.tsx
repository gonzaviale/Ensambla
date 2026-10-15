import { createFileRoute } from '@tanstack/react-router';
import { PreciosPage } from '@/pages/Precios/PreciosPage';

export const Route = createFileRoute('/_app/precios')({
  component: PreciosPage,
});
