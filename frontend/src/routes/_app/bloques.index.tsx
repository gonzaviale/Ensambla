import { createFileRoute } from '@tanstack/react-router';
import { BloquesPage } from '@/pages/Bloques/BloquesPage';

export const Route = createFileRoute('/_app/bloques/')({
  component: BloquesPage,
});
