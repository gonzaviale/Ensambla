import { createFileRoute } from '@tanstack/react-router';
import { ListaPage } from '@/pages/Listas/ListaPage';

export const Route = createFileRoute('/_app/listas/$listaId')({
  component: ListaPage,
});
