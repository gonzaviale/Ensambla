import { z } from 'zod';

export const errorResponseSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  error: z.string(),
  message: z.string(),
  path: z.string(),
  traceId: z.string(),
  errors: z.record(z.string()).optional(),
});

export type ErrorResponse = z.infer<typeof errorResponseSchema>;

export function pageSchema<T extends z.ZodTypeAny>(item: T) {
  return z.object({
    content: z.array(item),
    pageNumber: z.number(),
    pageSize: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
    first: z.boolean(),
    last: z.boolean(),
  });
}

export type Page<T> = {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
