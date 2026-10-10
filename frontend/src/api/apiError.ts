export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;
  readonly traceId?: string;

  constructor(
    status: number,
    message: string,
    fieldErrors: Record<string, string> = {},
    traceId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
  }

  get isNotFound() {
    return this.status === 404;
  }

  get isClientError() {
    return this.status >= 400 && this.status < 500;
  }
}
