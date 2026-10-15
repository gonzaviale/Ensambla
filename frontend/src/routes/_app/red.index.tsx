import { createFileRoute } from '@tanstack/react-router';
import { RedPage } from '@/pages/Red/RedPage';

export const Route = createFileRoute('/_app/red/')({
  component: RedPage,
});
