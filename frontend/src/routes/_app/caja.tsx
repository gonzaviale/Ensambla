import { createFileRoute } from '@tanstack/react-router';
import { CajaPage } from '@/pages/Caja/CajaPage';

export const Route = createFileRoute('/_app/caja')({ component: CajaPage });
