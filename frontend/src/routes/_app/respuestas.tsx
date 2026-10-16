import { createFileRoute } from '@tanstack/react-router';
import { RespuestasPage } from '@/pages/Respuestas/RespuestasPage';

export const Route = createFileRoute('/_app/respuestas')({
  component: RespuestasPage,
});
