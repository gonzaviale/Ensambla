import { createFileRoute } from '@tanstack/react-router';
import { UsarPage } from '@/pages/Usar/UsarPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

// ?nuevo=true: el proyecto se acaba de crear desde una plantilla; se muestra un aviso de bienvenida.
// ?bloque=<id>: abre ese bloque (por ejemplo, el que se acaba de guardar desde el cuaderno).
type BusquedaUsar = { nuevo?: true; bloque?: string };

export const Route = createFileRoute('/_app/proyectos/$proyectoId/usar')({
  validateSearch: (search: Record<string, unknown>): BusquedaUsar =>
    ({
      ...(search.nuevo === true || search.nuevo === 'true' ? { nuevo: true as const } : {}),
      ...(typeof search.bloque === 'string' && search.bloque ? { bloque: search.bloque } : {}),
    }),
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: UsarPage,
});
