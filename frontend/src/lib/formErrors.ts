// Los validadores de TanStack Form (zod vía Standard Schema) devuelven issues con `message`.
export function primerError(errors: readonly unknown[]): string | undefined {
  for (const error of errors) {
    if (typeof error === 'string') return error;
    if (error && typeof error === 'object' && 'message' in error) {
      const { message } = error as { message: unknown };
      if (typeof message === 'string') return message;
    }
  }
  return undefined;
}
