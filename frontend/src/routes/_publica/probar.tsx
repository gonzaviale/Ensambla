import { createFileRoute } from '@tanstack/react-router';
import { ProbarPage } from '@/pages/Probar/ProbarPage';

export const Route = createFileRoute('/_publica/probar')({
  component: ProbarPage,
});
