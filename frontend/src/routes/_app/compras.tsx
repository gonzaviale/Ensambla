import { createFileRoute } from '@tanstack/react-router';
import { ComprasPage } from '@/pages/Compras/ComprasPage';

export const Route = createFileRoute('/_app/compras')({
  component: ComprasPage,
});
