import { createFileRoute } from '@tanstack/react-router';
import { requerirUsuario } from '@/config/guards';
import { SistemaLayout } from '@/layouts/SistemaLayout';

export const Route = createFileRoute('/_app')({
  beforeLoad: requerirUsuario,
  component: SistemaLayout,
});
