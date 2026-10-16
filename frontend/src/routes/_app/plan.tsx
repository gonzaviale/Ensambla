import { createFileRoute } from '@tanstack/react-router';
import { PlanPage } from '@/pages/Plan/PlanPage';

export const Route = createFileRoute('/_app/plan')({
  component: PlanPage,
});
