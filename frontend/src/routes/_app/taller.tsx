import { createFileRoute } from '@tanstack/react-router';
import { TallerPage } from '@/pages/Taller/TallerPage';

export const Route = createFileRoute('/_app/taller')({
  component: TallerPage,
});
