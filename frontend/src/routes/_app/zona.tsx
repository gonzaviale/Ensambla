import { createFileRoute } from '@tanstack/react-router';
import { ZonaLayout } from '@/layouts/ZonaLayout';

export const Route = createFileRoute('/_app/zona')({
  component: ZonaLayout,
});
