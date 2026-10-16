import { createFileRoute } from '@tanstack/react-router';
import { BienvenidaPage } from '@/pages/Bienvenida/BienvenidaPage';

export const Route = createFileRoute('/_app/bienvenida')({
  component: BienvenidaPage,
});
