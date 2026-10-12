import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { X } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useUIStore } from '@/stores/uiStore';
import { useAuthStore, type AdminUser } from '@/stores/authStore';
import { AdminApiError, loginAdmin, logoutAdmin, registerCustomer, type AuthResponse } from '@/lib/adminApi';

const inputClass =
  'w-full rounded-full border border-maison-line bg-white px-6 py-4 text-sm text-maison-soft outline-none transition-all focus:border-maison-ink focus:shadow-[0_0_0_3px_rgba(15,15,15,0.08)]';

const loginSchema = z.object({
  email: z.string().email('Ingresá un correo válido'),
  password: z.string().min(1, 'Ingresá tu contraseña'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const registerSchema = z
  .object({
    firstName: z.string().min(1, 'Ingresá tu nombre'),
    lastName: z.string().min(1, 'Ingresá tu apellido'),
    email: z.string().email('Ingresá un correo válido'),
    password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres'),
    confirmPassword: z.string().min(1, 'Confirmá tu contraseña'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

function LoginForm({ onAuthenticated }: { onAuthenticated: (data: AuthResponse) => void }) {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (values: LoginFormValues) => {
    setSubmitError(null);
    try {
      const data = await loginAdmin(values.email, values.password);
      onAuthenticated(data);
    } catch (err) {
      if (err instanceof AdminApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError('No se pudo iniciar sesión. Revisá la conexión e intentá de nuevo.');
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <div>
        <label htmlFor="auth-email" className="mb-2 block text-sm font-medium text-maison-ink">
          Email
        </label>
        <input
          id="auth-email"
          type="email"
          autoComplete="email"
          placeholder="tu@correo.com"
          className={inputClass}
          {...register('email')}
        />
        {errors.email && <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>}
      </div>

      <div>
        <label htmlFor="auth-password" className="mb-2 block text-sm font-medium text-maison-ink">
          Contraseña
        </label>
        <input
          id="auth-password"
          type="password"
          autoComplete="current-password"
          placeholder="••••••••"
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
        {isSubmitting ? 'Ingresando…' : 'Iniciar sesión'}
      </motion.button>
    </form>
  );
}

function RegisterForm({ onAuthenticated }: { onAuthenticated: (data: AuthResponse) => void }) {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = async (values: RegisterFormValues) => {
    setSubmitError(null);
    try {
      const data = await registerCustomer(values.firstName, values.lastName, values.email, values.password);
      onAuthenticated(data);
    } catch (err) {
      if (err instanceof AdminApiError) {
        setSubmitError(err.message);
      } else {
        setSubmitError('No se pudo crear la cuenta. Revisá la conexión e intentá de nuevo.');
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="auth-first-name" className="mb-2 block text-sm font-medium text-maison-ink">
            Nombre
          </label>
          <input
            id="auth-first-name"
            type="text"
            autoComplete="given-name"
            placeholder="María"
            className={inputClass}
            {...register('firstName')}
          />
          {errors.firstName && <p className="mt-1.5 text-xs text-red-600">{errors.firstName.message}</p>}
        </div>
        <div>
          <label htmlFor="auth-last-name" className="mb-2 block text-sm font-medium text-maison-ink">
            Apellido
          </label>
          <input
            id="auth-last-name"
            type="text"
            autoComplete="family-name"
            placeholder="Gómez"
            className={inputClass}
            {...register('lastName')}
          />
          {errors.lastName && <p className="mt-1.5 text-xs text-red-600">{errors.lastName.message}</p>}
        </div>
      </div>

      <div>
        <label htmlFor="auth-register-email" className="mb-2 block text-sm font-medium text-maison-ink">
          Email
        </label>
        <input
          id="auth-register-email"
          type="email"
          autoComplete="email"
          placeholder="tu@correo.com"
          className={inputClass}
          {...register('email')}
        />
        {errors.email && <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>}
      </div>

      <div>
        <label htmlFor="auth-register-password" className="mb-2 block text-sm font-medium text-maison-ink">
          Contraseña
        </label>
        <input
          id="auth-register-password"
          type="password"
          autoComplete="new-password"
          placeholder="Mínimo 8 caracteres"
          className={inputClass}
          {...register('password')}
        />
        {errors.password && <p className="mt-1.5 text-xs text-red-600">{errors.password.message}</p>}
      </div>

      <div>
        <label htmlFor="auth-confirm-password" className="mb-2 block text-sm font-medium text-maison-ink">
          Confirmar contraseña
        </label>
        <input
          id="auth-confirm-password"
          type="password"
          autoComplete="new-password"
          placeholder="Repetí tu contraseña"
          className={inputClass}
          {...register('confirmPassword')}
        />
        {errors.confirmPassword && (
          <p className="mt-1.5 text-xs text-red-600">{errors.confirmPassword.message}</p>
        )}
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
        {isSubmitting ? 'Creando cuenta…' : 'Crear cuenta'}
      </motion.button>
    </form>
  );
}

function AccountPanel({ user, onClose }: { user: AdminUser; onClose: () => void }) {
  const isAdmin = user.roles.includes('ADMIN');

  const handleLogout = async () => {
    await logoutAdmin();
    onClose();
  };

  return (
    <div className="flex flex-col items-center text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-maison-ink font-serif-display text-2xl text-white">
        {user.firstName.charAt(0).toUpperCase()}
      </div>
      <h2 className="mt-4 font-serif-display text-2xl text-maison-ink">
        {user.firstName} {user.lastName}
      </h2>
      <p className="mt-1 text-sm text-maison-warmgray">{user.email}</p>
      <span className="mt-3 inline-flex items-center rounded-full bg-maison-warm px-3 py-1 text-xs font-medium uppercase tracking-[0.06em] text-maison-soft">
        {isAdmin ? 'Administrador' : 'Cliente'}
      </span>

      {isAdmin && (
        <Link
          to="/admin/products"
          onClick={onClose}
          className="mt-6 w-full rounded-full bg-maison-ink py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-colors hover:bg-maison-soft"
        >
          Panel de administración
        </Link>
      )}

      <button
        type="button"
        onClick={handleLogout}
        className="mt-3 w-full rounded-full border border-maison-line py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-colors hover:border-maison-ink"
      >
        Cerrar sesión
      </button>
    </div>
  );
}

export default function AuthModal() {
  const { authOpen, authOrigin, setAuthOpen, pushToast } = useUIStore();
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');

  const originX = authOrigin?.x ?? (typeof window !== 'undefined' ? window.innerWidth / 2 : 0);
  const originY = authOrigin?.y ?? (typeof window !== 'undefined' ? window.innerHeight / 2 : 0);
  const radius = typeof window !== 'undefined' ? Math.hypot(window.innerWidth, window.innerHeight) : 0;

  const isAuthenticated = Boolean(accessToken && user);

  useEffect(() => {
    if (!authOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setAuthOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [authOpen, setAuthOpen]);

  useEffect(() => {
    if (!authOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [authOpen]);

  const handleAuthenticated = (data: AuthResponse) => {
    if (data.user.roles.includes('ADMIN')) {
      setAuthOpen(false);
      pushToast('success', 'Sesión iniciada');
      navigate('/admin/products', { replace: true });
    } else {
      setAuthOpen(false);
      pushToast('success', `Bienvenido(a), ${data.user.firstName}!`);
    }
  };

  return (
    <AnimatePresence>
      {authOpen && (
        <motion.div
          key="auth-modal"
          initial={{ clipPath: `circle(0px at ${originX}px ${originY}px)` }}
          animate={{ clipPath: `circle(${radius}px at ${originX}px ${originY}px)` }}
          exit={{ clipPath: `circle(0px at ${originX}px ${originY}px)` }}
          transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
          className="fixed inset-0 z-[80]"
        >
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            onClick={() => setAuthOpen(false)}
            className="absolute inset-0 bg-black/40 backdrop-blur-md"
          />

          <div className="pointer-events-none absolute inset-0 flex items-center justify-center overflow-y-auto p-4">
            <motion.div
              role="dialog"
              aria-modal="true"
              aria-label="Cuenta"
              initial={{ opacity: 0, y: 16, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 16, scale: 0.98 }}
              transition={{ delay: 0.12, duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
              onClick={(e) => e.stopPropagation()}
              className="glass-strong pointer-events-auto relative my-auto w-full max-w-md overflow-hidden rounded-[32px] p-8 shadow-soft md:p-10"
            >
              <button
                aria-label="Cerrar"
                onClick={() => setAuthOpen(false)}
                className="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full text-maison-soft transition-all hover:rotate-90 hover:bg-white/60 hover:text-maison-ink"
              >
                <X size={20} strokeWidth={1.5} />
              </button>

              {isAuthenticated ? (
                <AccountPanel user={user!} onClose={() => setAuthOpen(false)} />
              ) : (
                <>
                  <div className="mb-7 pr-10">
                    <h2 className="font-serif-display text-3xl text-maison-ink">
                      {mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}
                    </h2>
                    <p className="mt-2 text-sm text-maison-warmgray">
                      {mode === 'login'
                        ? 'Ingresá para acceder a tu cuenta y a tu bolsa.'
                        : 'Unite a MAISON y descubrí lo nuevo antes que nadie.'}
                    </p>
                  </div>

                  <AnimatePresence mode="wait">
                    <motion.div
                      key={mode}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -12 }}
                      transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
                    >
                      {mode === 'login' ? (
                        <LoginForm onAuthenticated={handleAuthenticated} />
                      ) : (
                        <RegisterForm onAuthenticated={handleAuthenticated} />
                      )}
                    </motion.div>
                  </AnimatePresence>

                  <p className="mt-6 text-center text-sm text-maison-warmgray">
                    {mode === 'login' ? (
                      <>
                        ¿No tenés cuenta?{' '}
                        <button
                          type="button"
                          onClick={() => setMode('register')}
                          className="font-medium text-maison-ink underline-offset-4 transition-colors hover:underline"
                        >
                          Crear cuenta
                        </button>
                      </>
                    ) : (
                      <>
                        ¿Ya tenés cuenta?{' '}
                        <button
                          type="button"
                          onClick={() => setMode('login')}
                          className="font-medium text-maison-ink underline-offset-4 transition-colors hover:underline"
                        >
                          Iniciar sesión
                        </button>
                      </>
                    )}
                  </p>
                </>
              )}
            </motion.div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
