import { createFileRoute } from '@tanstack/react-router';
import { OlvideClavePage } from '@/pages/RecuperarClave/OlvideClavePage';

export const Route = createFileRoute('/_publica/olvide-contrasena')({
  component: OlvideClavePage,
});
