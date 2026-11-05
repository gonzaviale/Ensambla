import { createFileRoute } from '@tanstack/react-router';
import { OfertasPage } from '@/pages/Ofertas/OfertasPage';

export const Route = createFileRoute('/_app/ofertas')({
  component: OfertasPage,
});
