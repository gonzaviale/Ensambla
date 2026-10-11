import { createFileRoute } from '@tanstack/react-router';
import { UsarPage } from '@/pages/Usar/UsarPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

// ?nuevo=true: el proyecto se acaba de crear desde una plantilla; se muestra un aviso de bienvenida.
type BusquedaUsar = { nuevo?: true };

export const Route = createFileRoute('/_app/proyectos/$proyectoId/usar')({
  validateSearch: (search: Record<string, unknown>): BusquedaUsar =>
    search.nuevo === true || search.nuevo === 'true' ? { nuevo: true } : {},
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: UsarPage,
});
