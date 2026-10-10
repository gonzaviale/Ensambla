import { createFileRoute } from '@tanstack/react-router';
import { UsarPage } from '@/pages/Usar/UsarPage';

export const Route = createFileRoute('/_app/proyectos/$proyectoId/usar')({
  component: UsarPage,
});
