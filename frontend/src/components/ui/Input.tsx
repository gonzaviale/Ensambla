import { useId, useState, type ComponentProps, type ReactNode } from 'react';
import { EyeIcon, EyeSlashIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

type InputFieldProps = ComponentProps<'input'> & {
  label: string;
  hint?: ReactNode;
  error?: string;
  trailing?: ReactNode;
};

const inputClasses =
  'w-full rounded-md border border-fg/16 bg-canvas px-2.5 py-2 text-sm text-fg caret-accent hover:border-fg/45 focus-visible:border-accent focus-visible:outline-none aria-invalid:border-bad';

export function InputField({ label, hint, error, trailing, className, ...props }: InputFieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-[12.5px] text-soft">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cn(inputClasses, trailing && 'pr-10', className)}
          {...props}
        />
        {trailing && <div className="absolute inset-y-0 right-1 flex items-center">{trailing}</div>}
      </div>
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
          className="flex size-8 cursor-pointer items-center justify-center rounded-md text-muted hover:text-fg focus-visible:outline-2 focus-visible:outline-accent"
        >
          <Icono aria-hidden size={16} />
        </button>
      }
    />
  );
}
