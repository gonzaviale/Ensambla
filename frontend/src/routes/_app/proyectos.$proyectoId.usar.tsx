import { createFileRoute } from '@tanstack/react-router';
import { UsarPage } from '@/pages/Usar/UsarPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

export const Route = createFileRoute('/_app/proyectos/$proyectoId/usar')({
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: UsarPage,
});
