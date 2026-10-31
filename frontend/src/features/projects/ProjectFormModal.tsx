import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/Button';
import { TextAreaField, TextField } from '../../components/FormFields';
import { Modal } from '../../components/Modal';
import { FormError } from '../../components/StateMessage';
import { useForm, type FieldErrors } from '../../hooks/useForm';
import { useCreateProject } from './projectQueries';

interface ProjectFormValues extends Record<string, string> {
  name: string;
  description: string;
}

const INITIAL_VALUES: ProjectFormValues = { name: '', description: '' };

function validate(values: ProjectFormValues): FieldErrors<ProjectFormValues> {
  const name = values.name.trim();
  return {
    name: !name ? 'Name is required' : name.length > 100 ? 'Name must be at most 100 characters' : undefined,
    description: values.description.length > 1000 ? 'Description must be at most 1000 characters' : undefined,
  };
}

export function ProjectFormModal({ onClose }: { onClose: () => void }) {
  const createProject = useCreateProject();
  const navigate = useNavigate();

  const form = useForm({
    initialValues: INITIAL_VALUES,
    validate,
    onSubmit: async (values) => {
      const project = await createProject.mutateAsync({
        name: values.name.trim(),
        description: values.description.trim() || null,
      });
      navigate(`/projects/${project.id}`);
    },
  });

  return (
    <Modal title="New project" onClose={onClose}>
      <form onSubmit={form.handleSubmit} noValidate>
        <TextField label="Name" data-autofocus {...form.field('name')} />
        <TextAreaField label="Description (optional)" rows={3} {...form.field('description')} />
        <FormError message={form.formError} />
        <div className="modal__actions">
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="primary" loading={form.submitting}>
            Create project
          </Button>
        </div>
      </form>
    </Modal>
  );
}
