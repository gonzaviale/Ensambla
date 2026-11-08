import { createFileRoute } from '@tanstack/react-router';
import { ObjetivoDetallePage } from '@/pages/Objetivos/ObjetivoDetallePage';

export const Route = createFileRoute('/_app/objetivos/$objetivoId')({
  component: function ObjetivoRoute() {
    const { objetivoId } = Route.useParams();
    return <ObjetivoDetallePage objetivoId={objetivoId} />;
  },
});
