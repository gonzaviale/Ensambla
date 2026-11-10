import { createFileRoute } from '@tanstack/react-router';
import { DondeAbrirPage } from '@/pages/DondeAbrir/DondeAbrirPage';

export const Route = createFileRoute('/_app/donde-abrir')({ component: DondeAbrirPage });
