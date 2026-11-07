import { createFileRoute } from '@tanstack/react-router';
import { MetricasPage } from '@/pages/Metricas/MetricasPage';

export const Route = createFileRoute('/_app/metricas')({
  component: MetricasPage,
});
