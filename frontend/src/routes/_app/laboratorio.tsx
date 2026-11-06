import { createFileRoute } from '@tanstack/react-router';
import { LaboratorioPage } from '@/pages/Laboratorio/LaboratorioPage';

export const Route = createFileRoute('/_app/laboratorio')({ component: LaboratorioPage });
