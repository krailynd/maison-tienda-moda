import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router';
import { motion } from 'framer-motion';
import { AdminApiError, loginAdmin } from '@/lib/adminApi';

const loginSchema = z.object({
  email: z.string().email('Ingresa un correo válido'),
  password: z.string().min(1, 'La contraseña es obligatoria'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const inputClass =
  'w-full rounded-full border border-maison-line bg-white px-6 py-4 text-sm text-maison-soft outline-none transition-all focus:border-maison-ink focus:shadow-[0_0_0_3px_rgba(15,15,15,0.08)]';

export default function AdminLogin() {
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (values: LoginFormValues) => {
    setSubmitError(null);
    try {
      await loginAdmin(values.email, values.password);
      navigate('/admin/products', { replace: true });
    } catch (err) {
      if (err instanceof AdminApiError) {
        // Envelope message surfaced inline (e.g. INVALID_CREDENTIALS).
        setSubmitError(err.message);
      } else {
        setSubmitError('No se pudo iniciar sesión. Comprueba la conexión e inténtalo nuevamente.');
      }
    }
  };

  return (
    <div
      className="flex min-h-screen items-center justify-center px-4"
      style={{ background: 'radial-gradient(ellipse at top, #F5F0EB, #FAF9F6)' }}
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
        className="w-full max-w-md rounded-[32px] border border-maison-line bg-white p-8 shadow-soft md:p-10"
      >
        <h1 className="font-serif-display text-3xl text-maison-ink">MAISON Admin</h1>
        <p className="mt-2 text-sm text-maison-warmgray">Inicia sesión para gestionar la tienda.</p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-7 space-y-4" noValidate>
          <div>
            <label htmlFor="email" className="mb-2 block text-sm font-medium text-maison-ink">
              Correo electrónico
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="admin@fashion.com"
              className={inputClass}
              {...register('email')}
            />
            {errors.email && <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>}
          </div>

          <div>
            <label htmlFor="password" className="mb-2 block text-sm font-medium text-maison-ink">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className={inputClass}
              {...register('password')}
            />
            {errors.password && <p className="mt-1.5 text-xs text-red-600">{errors.password.message}</p>}
          </div>

          {submitError && (
            <div className="rounded-full bg-red-50 px-5 py-3 text-sm text-red-700" role="alert">
              {submitError}
            </div>
          )}

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-full bg-maison-ink py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? 'Iniciando sesión…' : 'Iniciar sesión'}
          </motion.button>
        </form>
      </motion.div>
    </div>
  );
}
