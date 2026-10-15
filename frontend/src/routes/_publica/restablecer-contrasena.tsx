import { createFileRoute } from '@tanstack/react-router';
import { RestablecerClavePage } from '@/pages/RecuperarClave/RestablecerClavePage';

export const Route = createFileRoute('/_publica/restablecer-contrasena')({
  component: RestablecerClavePage,
});
