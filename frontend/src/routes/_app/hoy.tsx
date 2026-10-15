import { createFileRoute } from '@tanstack/react-router';
import { HoyPage } from '@/pages/Hoy/HoyPage';

export const Route = createFileRoute('/_app/hoy')({
  component: HoyPage,
});
