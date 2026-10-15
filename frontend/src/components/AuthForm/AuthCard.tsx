import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { PageHeading, Panel } from '@/components/ui';

type AuthCardProps = {
  title: string;
  lead: string;
  children: ReactNode;
  footer: ReactNode;
};

export function AuthCard({ title, lead, children, footer }: AuthCardProps) {
  return (
    <div className="mx-auto flex w-full max-w-lg animate-fade-in flex-col gap-6 px-4 py-10 sm:py-16">
      <PageHeading title={title} lead={lead} />
      <Panel className="p-5 sm:p-6">{children}</Panel>
      <p className="m-0 text-center text-body text-soft">{footer}</p>
    </div>
  );
}

type DestinoAuth = '/login' | '/registro' | '/olvide-contrasena';

export function EnlaceAuth({ to, children }: { to: DestinoAuth; children: ReactNode }) {
  return (
    <Link to={to} className="font-semibold">
      {children}
    </Link>
  );
}
