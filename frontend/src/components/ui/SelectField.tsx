import { useId, type ComponentProps } from 'react';
import { cn } from '@/lib/cn';

type SelectFieldProps = ComponentProps<'select'> & {
  label: string;
  error?: string;
};

export function SelectField({ label, error, className, children, ...props }: SelectFieldProps) {
  const id = useId();
  const errorId = `${id}-error`;
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-[12.5px] text-soft">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={cn(
          'w-full rounded-md border border-fg/16 bg-canvas px-2.5 py-2 text-sm text-fg focus-visible:border-accent focus-visible:outline-none aria-invalid:border-bad',
          className,
        )}
        {...props}
      >
        {children}
      </select>
      {error && (
        <p id={errorId} className="text-[12.5px] text-bad">
          {error}
        </p>
      )}
    </div>
  );
}
