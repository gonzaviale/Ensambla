import { createFileRoute } from '@tanstack/react-router';
import { plantillasQuery } from '@/hooks/plantillas/queries';
import { proyectosQuery } from '@/hooks/proyectos/queries';
import { ProyectosPage } from '@/pages/Proyectos/ProyectosPage';

export const Route = createFileRoute('/_app/proyectos/')({
  loader: ({ context }) =>
    Promise.all([
      context.queryClient.ensureQueryData(plantillasQuery()),
      context.queryClient.ensureQueryData(proyectosQuery()),
    ]),
  component: ProyectosPage,
});
