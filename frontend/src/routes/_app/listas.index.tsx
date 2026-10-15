import { createFileRoute } from '@tanstack/react-router';
import { ListasPage } from '@/pages/Listas/ListasPage';

export const Route = createFileRoute('/_app/listas/')({
  component: ListasPage,
});
