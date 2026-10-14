import { createFileRoute } from '@tanstack/react-router';
import { ReportesPage } from '@/pages/Reportes/ReportesPage';
import { proyectoQuery } from '@/hooks/proyectos/queries';

// ?bloque=<id>: la tabla del reporte que se muestra al abrir.
type BusquedaReportes = { bloque?: string };

export const Route = createFileRoute('/_app/proyectos/$proyectoId/reportes')({
  validateSearch: (search: Record<string, unknown>): BusquedaReportes =>
    typeof search.bloque === 'string' && search.bloque ? { bloque: search.bloque } : {},
  loader: ({ context, params }) =>
    context.queryClient.ensureQueryData(proyectoQuery(params.proyectoId)),
  component: ReportesPage,
});
