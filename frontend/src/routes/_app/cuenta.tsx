import { createFileRoute } from '@tanstack/react-router';
import { CuentaPage } from '@/pages/Cuenta/CuentaPage';

export const Route = createFileRoute('/_app/cuenta')({
  component: CuentaPage,
});
