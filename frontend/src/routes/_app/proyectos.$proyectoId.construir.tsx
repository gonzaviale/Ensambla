import { createFileRoute } from '@tanstack/react-router';
import { ConstructorPage } from '@/pages/Constructor/ConstructorPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

export const Route = createFileRoute('/_app/proyectos/$proyectoId/construir')({
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: ConstructorPage,
});
