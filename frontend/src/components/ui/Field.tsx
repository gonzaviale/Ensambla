import { useId, type ComponentProps, type ReactNode } from 'react';
import { cn } from '@/lib/cn';

type TextareaFieldProps = ComponentProps<'textarea'> & {
  label: string;
  hint?: ReactNode;
  error?: string;
};

export function TextareaField({ label, hint, error, className, ...props }: TextareaFieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-[12.5px] text-soft">
        {label}
      </label>
      <textarea
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cn(
          'min-h-36 w-full resize-y rounded-md border border-fg/16 bg-canvas px-2.5 py-2 text-sm text-fg caret-accent hover:border-fg/45 focus-visible:border-accent focus-visible:outline-none aria-invalid:border-bad',
          className,
        )}
        {...props}
      />
      {hint && (
        <p id={hintId} className="text-[11px] text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-[12.5px] text-bad">
          {error}
        </p>
      )}
    </div>
  );
}
