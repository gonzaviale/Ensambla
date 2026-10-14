import { createFileRoute } from '@tanstack/react-router';
import { StockPage } from '@/pages/Stock/StockPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

// ?bloque=<id>: la tabla con los productos que se muestra al abrir.
type BusquedaStock = { bloque?: string };

export const Route = createFileRoute('/_app/proyectos/$proyectoId/stock')({
  validateSearch: (search: Record<string, unknown>): BusquedaStock =>
    typeof search.bloque === 'string' && search.bloque ? { bloque: search.bloque } : {},
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: StockPage,
});
