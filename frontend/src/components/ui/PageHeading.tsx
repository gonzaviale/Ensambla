import type { ReactNode } from 'react';

type PageHeadingProps = { kicker?: ReactNode; title: string; lead?: ReactNode };

export function PageHeading({ kicker, title, lead }: PageHeadingProps) {
  return (
    <header className="flex flex-col gap-1.5">
      {kicker && <p className="text-[11px] tracking-widest text-accent-800 uppercase">{kicker}</p>}
      <h1 className="m-0 text-[23px] sm:text-[28px]">{title}</h1>
      {lead && <p className="text-[15px] leading-relaxed text-soft">{lead}</p>}
    </header>
  );
}
