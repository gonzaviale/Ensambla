import { createFileRoute } from '@tanstack/react-router';
import { AvisosPage } from '@/pages/Avisos/AvisosPage';

export const Route = createFileRoute('/_app/avisos')({
  component: AvisosPage,
});
