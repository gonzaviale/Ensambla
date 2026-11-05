import { createFileRoute } from '@tanstack/react-router';
import { CierrePage } from '@/pages/Cierre/CierrePage';

export const Route = createFileRoute('/_app/cierre')({
  component: CierrePage,
});
