import { createFileRoute } from '@tanstack/react-router';
import { CartelesPage } from '@/pages/Carteles/CartelesPage';

export const Route = createFileRoute('/_app/carteles')({ component: CartelesPage });
