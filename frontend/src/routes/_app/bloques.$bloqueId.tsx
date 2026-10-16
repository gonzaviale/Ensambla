import { createFileRoute } from '@tanstack/react-router';
import { BloquePage } from '@/pages/Bloques/BloquePage';

export const Route = createFileRoute('/_app/bloques/$bloqueId')({
  component: BloquePage,
});
