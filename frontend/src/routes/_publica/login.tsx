import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { LoginPage } from '@/pages/Login/LoginPage';

const searchSchema = z.object({ redirect: z.string().optional() });

export const Route = createFileRoute('/_publica/login')({
  validateSearch: searchSchema,
  component: LoginPage,
});
