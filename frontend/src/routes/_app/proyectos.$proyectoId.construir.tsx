import { createFileRoute } from '@tanstack/react-router';
import { ConstructorPage } from '@/pages/Constructor/ConstructorPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

// ?bloque=<id> recuerda el bloque abierto: sobrevive a la recarga y en el celular el botón
// "atrás" vuelve de la ficha del bloque a la lista.
type BusquedaConstructor = { bloque?: string };

export const Route = createFileRoute('/_app/proyectos/$proyectoId/construir')({
  validateSearch: (search: Record<string, unknown>): BusquedaConstructor =>
    typeof search.bloque === 'string' && search.bloque ? { bloque: search.bloque } : {},
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: ConstructorPage,
});
