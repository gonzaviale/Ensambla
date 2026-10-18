import { createFileRoute } from '@tanstack/react-router';
import { InicioPage } from '@/pages/Inicio/InicioPage';

export const Route = createFileRoute('/_app/inicio')({
  component: InicioPage,
});
