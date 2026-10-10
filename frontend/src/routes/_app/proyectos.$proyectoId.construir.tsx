import { createFileRoute } from '@tanstack/react-router';
import { ConstructorPage } from '@/pages/Constructor/ConstructorPage';

export const Route = createFileRoute('/_app/proyectos/$proyectoId/construir')({
  component: ConstructorPage,
});
