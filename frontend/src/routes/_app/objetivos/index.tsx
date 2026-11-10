import { createFileRoute } from '@tanstack/react-router';
import { ObjetivosPage } from '@/pages/Objetivos/ObjetivosPage';

export const Route = createFileRoute('/_app/objetivos/')({ component: ObjetivosPage });
