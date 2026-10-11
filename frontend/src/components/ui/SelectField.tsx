import type { ComponentProps, ReactNode } from 'react';
import { CaretDownIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { FieldShell } from './FieldShell';
import { fieldControlClasses } from './fieldStyles';

type SelectFieldProps = ComponentProps<'select'> & {
  label: string;
  hint?: ReactNode;
  error?: string;
};

export function SelectField({
  label,
  hint,
  error,
  required,
  className,
  children,
  ...props
}: SelectFieldProps) {
  return (
    <FieldShell label={label} hint={hint} error={error} required={required}>
      {({ id, describedBy }) => (
        <div className="relative">
          <select
            id={id}
            required={required}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            className={cn(fieldControlClasses, 'cursor-pointer appearance-none pr-11', className)}
            {...props}
          >
            {children}
          </select>
          <CaretDownIcon
            aria-hidden
            size={20}
            weight="bold"
            className="pointer-events-none absolute top-1/2 right-3.5 -translate-y-1/2 text-soft"
          />
        </div>
      )}
    </FieldShell>
  );
}
