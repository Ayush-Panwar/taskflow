import { useState, type ChangeEvent, type FormEvent } from 'react';
import { toApiError } from '../api/client';

export type FieldErrors<T> = Partial<Record<keyof T, string>>;

interface UseFormOptions<T> {
  initialValues: T;
  validate: (values: T) => FieldErrors<T>;
  onSubmit: (values: T) => Promise<unknown>;
}

type ChangeableElement = HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

/**
 * Holds a form's values, runs validation before submitting, and shows server errors in the right
 * place: a field-level error next to its input, anything else as a message for the whole form.
 */
export function useForm<T extends Record<string, string>>({ initialValues, validate, onSubmit }: UseFormOptions<T>) {
  const [values, setValues] = useState<T>(initialValues);
  const [errors, setErrors] = useState<FieldErrors<T>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function field<K extends keyof T & string>(name: K) {
    return {
      name,
      value: values[name],
      error: errors[name],
      onChange: (event: ChangeEvent<ChangeableElement>) => {
        const { value } = event.target;
        setValues((current) => ({ ...current, [name]: value }));
        setErrors((current) => ({ ...current, [name]: undefined }));
      },
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const validationErrors = validate(values);
    setErrors(validationErrors);
    if (Object.values(validationErrors).some(Boolean)) {
      return;
    }

    setSubmitting(true);
    setFormError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const apiError = toApiError(error);
      const fieldNames = Object.keys(values);
      const fieldEntries = Object.entries(apiError.fieldErrors).filter(([name]) => fieldNames.includes(name));
      setErrors(Object.fromEntries(fieldEntries) as FieldErrors<T>);
      if (fieldEntries.length === 0) {
        setFormError(apiError.message);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return { values, errors, formError, submitting, field, handleSubmit };
}
