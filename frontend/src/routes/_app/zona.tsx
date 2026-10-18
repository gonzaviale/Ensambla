import { createFileRoute } from '@tanstack/react-router';
import { ZonaPage } from '@/pages/Zona/ZonaPage';

export const Route = createFileRoute('/_app/zona')({
  component: ZonaPage,
});
