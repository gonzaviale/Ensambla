import { createFileRoute } from '@tanstack/react-router';
import { SamiProyectoPage } from '@/pages/SamiProyecto/SamiProyectoPage';

export const Route = createFileRoute('/_app/sami')({
  component: SamiProyectoPage,
});
