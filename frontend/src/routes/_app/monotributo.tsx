import { createFileRoute } from '@tanstack/react-router';
import { MonotributoPage } from '@/pages/Monotributo/MonotributoPage';

export const Route = createFileRoute('/_app/monotributo')({
  component: MonotributoPage,
});
