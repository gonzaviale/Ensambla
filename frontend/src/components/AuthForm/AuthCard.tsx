import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { Panel } from '@/components/ui';

type AuthCardProps = {
  title: string;
  lead: string;
  children: ReactNode;
  footer: ReactNode;
};

export function AuthCard({ title, lead, children, footer }: AuthCardProps) {
  return (
    <div className="mx-auto flex w-full max-w-105 animate-fade-in flex-col gap-5 px-4 py-10 sm:py-16">
      <header className="flex flex-col gap-1.5">
        <h1 className="m-0 text-h2 sm:text-h1">{title}</h1>
        <p className="text-hint leading-relaxed text-soft">{lead}</p>
      </header>
      <Panel>{children}</Panel>
      <p className="text-center text-hint text-soft">{footer}</p>
    </div>
  );
}

export function EnlaceAuth({ to, children }: { to: '/login' | '/registro'; children: ReactNode }) {
  return <Link to={to}>{children}</Link>;
}
