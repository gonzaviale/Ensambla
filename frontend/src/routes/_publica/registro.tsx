import { createFileRoute } from '@tanstack/react-router';
import { RegistroPage } from '@/pages/Registro/RegistroPage';

export const Route = createFileRoute('/_publica/registro')({
  component: RegistroPage,
});
