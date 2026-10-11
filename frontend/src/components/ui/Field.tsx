import type { ComponentProps, ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { FieldShell } from './FieldShell';
import { fieldControlClasses } from './fieldStyles';

type TextareaFieldProps = ComponentProps<'textarea'> & {
  label: string;
  hint?: ReactNode;
  error?: string;
};

export function TextareaField({
  label,
  hint,
  error,
  required,
  className,
  ...props
}: TextareaFieldProps) {
  return (
    <FieldShell label={label} hint={hint} error={error} required={required}>
      {({ id, describedBy }) => (
        <textarea
          id={id}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cn(fieldControlClasses, 'min-h-32 resize-y', className)}
          {...props}
        />
      )}
    </FieldShell>
  );
}
