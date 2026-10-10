import { createFileRoute } from '@tanstack/react-router';
import { redirigirSiHaySesion } from '@/config/guards';
import { PublicLayout } from '@/layouts/PublicLayout';

export const Route = createFileRoute('/_publica')({
  beforeLoad: redirigirSiHaySesion,
  component: PublicLayout,
});
