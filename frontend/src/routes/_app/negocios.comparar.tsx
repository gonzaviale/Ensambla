import { createFileRoute } from '@tanstack/react-router';
import { CompararNegociosPage } from '@/pages/Negocios/CompararNegociosPage';

export const Route = createFileRoute('/_app/negocios/comparar')({
  component: CompararNegociosPage,
});
