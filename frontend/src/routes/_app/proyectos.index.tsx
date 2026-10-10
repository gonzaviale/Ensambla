import { createFileRoute } from '@tanstack/react-router';
import { plantillasQuery } from '@/hooks/plantillas/queries';
import { ProyectosPage } from '@/pages/Proyectos/ProyectosPage';

export const Route = createFileRoute('/_app/proyectos/')({
  loader: ({ context }) => context.queryClient.ensureQueryData(plantillasQuery()),
  component: ProyectosPage,
});
