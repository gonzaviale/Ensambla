import { createFileRoute } from '@tanstack/react-router';
import { requerirUsuario } from '@/config/guards';
import { AppLayout } from '@/layouts/AppLayout';

export const Route = createFileRoute('/_app')({
  beforeLoad: requerirUsuario,
  component: AppLayout,
});
