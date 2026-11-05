import { createFileRoute } from '@tanstack/react-router';
import { PromocionesPage } from '@/pages/Promociones/PromocionesPage';

export const Route = createFileRoute('/_app/promociones')({
  component: PromocionesPage,
});
