import { createFileRoute } from '@tanstack/react-router';
import { MostradorPage } from '@/pages/Listas/MostradorPage';

export const Route = createFileRoute('/_app/mostrador')({
  component: MostradorPage,
});
