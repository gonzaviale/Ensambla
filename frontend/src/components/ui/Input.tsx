import { useState, type ComponentProps, type ReactNode } from 'react';
import { EyeIcon, EyeSlashIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { FieldShell } from './FieldShell';
import { fieldControlClasses } from './fieldStyles';

type InputFieldProps = ComponentProps<'input'> & {
  label: string;
  hint?: ReactNode;
  error?: string;
  trailing?: ReactNode;
};

export function InputField({
  label,
  hint,
  error,
  trailing,
  required,
  className,
  ...props
}: InputFieldProps) {
  return (
    <FieldShell label={label} hint={hint} error={error} required={required}>
      {({ id, describedBy }) => (
        <div className="relative">
          <input
            id={id}
            required={required}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            className={cn(fieldControlClasses, trailing && 'pr-14', className)}
            {...props}
          />
          {trailing && (
            <div className="absolute inset-y-0 right-0.5 flex items-center">{trailing}</div>
          )}
        </div>
      )}
    </FieldShell>
  );
}

type PasswordFieldProps = Omit<InputFieldProps, 'type' | 'trailing'>;

export function PasswordField(props: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const Icono = visible ? EyeSlashIcon : EyeIcon;

  return (
    <InputField
      {...props}
      type={visible ? 'text' : 'password'}
      trailing={
        <button
          type="button"
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          onClick={() => setVisible((actual) => !actual)}
          className="flex size-12 cursor-pointer items-center justify-center rounded-md text-soft hover:text-fg"
        >
          <Icono aria-hidden size={24} />
        </button>
      }
    />
  );
}
