import { createFileRoute } from '@tanstack/react-router';
import { ProyeccionPage } from '@/pages/Proyeccion/ProyeccionPage';

export const Route = createFileRoute('/_app/proyeccion')({
  component: ProyeccionPage,
});
