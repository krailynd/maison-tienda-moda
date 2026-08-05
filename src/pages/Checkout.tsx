import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { Check, CreditCard, Truck, Zap, ShieldCheck, RotateCcw, ShoppingBag, Copy } from 'lucide-react';
import { useCartStore, useCartTotals } from '@/stores/cartStore';
import { formatPrice } from '@/lib/data';
import { pageEnter } from '@/lib/animations';

const STEPS = ['Envío', 'Método', 'Pago', 'Revisar', 'Confirmar'];

function Stepper({ current }: { current: number }) {
  return (
    <div className="mx-auto flex max-w-[600px] items-center justify-center">
      {STEPS.map((label, i) => (
        <div key={label} className="flex items-center">
          <div className="flex flex-col items-center">
            <motion.div
              initial={false}
              animate={i === current ? { scale: [1, 1.1, 1] } : { scale: 1 }}
              transition={{ type: 'spring', stiffness: 300, damping: 15 }}
              className={`flex h-10 w-10 items-center justify-center rounded-full border-2 text-sm font-medium transition-all duration-300 ${
                i < current
                  ? 'border-maison-ink bg-maison-ink text-white'
                  : i === current
                    ? 'border-maison-ink bg-maison-ink text-white shadow-[0_0_0_4px_rgba(15,15,15,0.1)]'
                    : 'border-maison-line bg-transparent text-maison-muted'
              }`}
            >
              {i < current ? <Check size={16} /> : i + 1}
            </motion.div>
            <span className={`mt-2 hidden text-xs sm:block ${i <= current ? 'text-maison-ink' : 'text-maison-warmgray'}`}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div className="relative mx-2 mb-0 h-0.5 w-8 sm:mb-6 sm:w-14 bg-maison-line overflow-hidden rounded-full">
              <motion.div
                className="absolute inset-y-0 left-0 bg-maison-ink"
                initial={false}
                animate={{ width: i < current ? '100%' : '0%' }}
                transition={{ duration: 0.5 }}
              />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function Field({
  label, icon: Icon, error, ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label: string; icon?: React.ElementType; error?: string }) {
  return (
    <div>
      <label className="label-micro mb-2 block text-maison-warmgray">{label}</label>
      <div className="relative">
        {Icon && <Icon size={20} strokeWidth={1.5} className="absolute left-4 top-1/2 -translate-y-1/2 text-maison-muted" />}
        <input
          {...props}
          className={`w-full rounded-2xl border bg-white py-3.5 text-sm text-maison-soft outline-none transition-all duration-200 ${
            Icon ? 'pl-12 pr-4' : 'px-4'
          } ${
            error
              ? 'border-maison-terracotta'
              : 'border-maison-line focus:border-maison-ink focus:shadow-[0_0_0_3px_rgba(15,15,15,0.08)]'
          }`}
        />
      </div>
      {error && <p className="mt-1.5 text-xs text-maison-terracotta">{error}</p>}
    </div>
  );
}

export default function Checkout() {
  const { items, clear } = useCartStore();
  const { subtotal } = useCartTotals();
  const [step, setStep] = useState(0);
  const [shipMethod, setShipMethod] = useState<'standard' | 'express'>('standard');
  const [payMethod, setPayMethod] = useState<'card' | 'yape'>('card');
  const [form, setForm] = useState({ nombre: '', email: '', direccion: '', ciudad: '', tarjeta: '', venc: '', cvv: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [copied, setCopied] = useState(false);
  const navigate = useNavigate();

  const shipping = shipMethod === 'express' ? 39 : subtotal >= 500 ? 0 : 25;
  const total = subtotal + shipping;
  const orderNumber = useMemo(() => `MA-${Math.floor(100000 + Math.random() * 900000)}`, []);

  const next = () => {
    if (step === 0) {
      const e: Record<string, string> = {};
      if (form.nombre.trim().length < 3) e.nombre = 'Ingresa tu nombre completo';
      if (!form.email.includes('@')) e.email = 'Ingresa un correo válido';
      if (form.direccion.trim().length < 5) e.direccion = 'Ingresa una dirección válida';
      if (!form.ciudad.trim()) e.ciudad = 'Ingresa tu ciudad';
      setErrors(e);
      if (Object.keys(e).length) return;
    }
    if (step === 2 && payMethod === 'card') {
      const e: Record<string, string> = {};
      if (form.tarjeta.replace(/\s/g, '').length < 16) e.tarjeta = 'Número de tarjeta incompleto';
      if (form.venc.length < 5) e.venc = 'MM/AA';
      if (form.cvv.length < 3) e.cvv = 'CVV inválido';
      setErrors(e);
      if (Object.keys(e).length) return;
    }
    setErrors({});
    if (step === 3) clear();
    setStep((s) => Math.min(4, s + 1));
  };

  if (items.length === 0 && step < 4) {
    return (
      <main className="flex min-h-screen flex-col items-center justify-center px-5 text-center">
        <ShoppingBag size={48} strokeWidth={1} className="text-maison-muted" />
        <p className="mt-4 font-serif-display text-2xl text-maison-ink">Tu bolsa está vacía</p>
        <Link to="/catalogo" className="mt-6 rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white">
          Ir a la tienda
        </Link>
      </main>
    );
  }

  const slide = {
    initial: { x: 30, opacity: 0 },
    animate: { x: 0, opacity: 1 },
    exit: { x: -30, opacity: 0 },
    transition: { duration: 0.35, ease: [0.4, 0, 0.2, 1] as const },
  };

  return (
    <motion.main variants={pageEnter} initial="hidden" animate="visible" exit="exit" className="mx-auto max-w-7xl px-5 pt-28 md:px-8 md:pt-32">
      {step < 4 && <Stepper current={step} />}

      <div className={`mt-12 ${step < 4 ? 'grid gap-10 lg:grid-cols-[60%_40%]' : ''}`}>
        <AnimatePresence mode="wait">
          {/* STEP 0: Envío */}
          {step === 0 && (
            <motion.div key="s0" {...slide}>
              <h1 className="font-serif-display text-3xl text-maison-ink">Datos de envío</h1>
              <div className="mt-8 grid gap-5 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <Field label="Nombre completo" placeholder="Ana María Torres" value={form.nombre} error={errors.nombre}
                    onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
                </div>
                <div className="sm:col-span-2">
                  <Field label="Correo electrónico" type="email" placeholder="ana@correo.com" value={form.email} error={errors.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </div>
                <div className="sm:col-span-2">
                  <Field label="Dirección" placeholder="Av. Ejemplo 123, Dpto 401" value={form.direccion} error={errors.direccion}
                    onChange={(e) => setForm({ ...form, direccion: e.target.value })} />
                </div>
                <Field label="Ciudad" placeholder="Lima" value={form.ciudad} error={errors.ciudad}
                  onChange={(e) => setForm({ ...form, ciudad: e.target.value })} />
                <Field label="Código postal (opcional)" placeholder="15074" />
              </div>
            </motion.div>
          )}

          {/* STEP 1: Método de envío */}
          {step === 1 && (
            <motion.div key="s1" {...slide}>
              <h1 className="font-serif-display text-3xl text-maison-ink">Método de envío</h1>
              <div className="mt-8 space-y-4">
                {([
                  { id: 'standard', icon: Truck, title: 'Envío estándar', desc: '2 a 4 días hábiles', price: subtotal >= 500 ? 0 : 25 },
                  { id: 'express', icon: Zap, title: 'Envío express', desc: 'Entrega en 24 horas', price: 39 },
                ] as const).map((m) => {
                  const active = shipMethod === m.id;
                  return (
                    <motion.button
                      key={m.id}
                      whileHover={{ y: -2 }}
                      onClick={() => setShipMethod(m.id)}
                      className={`relative flex w-full items-center gap-5 rounded-[20px] border-[1.5px] p-6 text-left transition-all duration-200 ${
                        active ? 'border-maison-ink bg-maison-warm' : 'border-maison-line hover:border-[#C4C4C4]'
                      }`}
                    >
                      <m.icon size={40} strokeWidth={1.2} className="text-maison-soft" />
                      <div className="flex-1">
                        <p className="font-medium text-maison-ink">{m.title}</p>
                        <p className="text-sm text-maison-warmgray">{m.desc}</p>
                      </div>
                      <span className="font-serif-display text-lg text-maison-ink">
                        {m.price === 0 ? 'Gratis' : formatPrice(m.price)}
                      </span>
                      <AnimatePresence>
                        {active && (
                          <motion.span
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                            exit={{ scale: 0 }}
                            transition={{ type: 'spring', stiffness: 300, damping: 15 }}
                            className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-maison-ink text-white"
                          >
                            <Check size={14} />
                          </motion.span>
                        )}
                      </AnimatePresence>
                    </motion.button>
                  );
                })}
              </div>
            </motion.div>
          )}

          {/* STEP 2: Pago */}
          {step === 2 && (
            <motion.div key="s2" {...slide}>
              <h1 className="font-serif-display text-3xl text-maison-ink">Método de pago</h1>
              <div className="mt-8 grid gap-4 sm:grid-cols-2">
                {([
                  { id: 'card', icon: CreditCard, label: 'Tarjeta' },
                  { id: 'yape', icon: Zap, label: 'Yape / Plin' },
                ] as const).map((m) => (
                  <button
                    key={m.id}
                    onClick={() => setPayMethod(m.id)}
                    className={`flex flex-col items-center gap-3 rounded-[20px] p-6 transition-all ${
                      payMethod === m.id
                        ? 'bg-white ring-2 ring-maison-ink'
                        : 'bg-gradient-to-br from-maison-cream to-maison-warm hover:from-maison-gray'
                    }`}
                  >
                    <m.icon size={40} strokeWidth={1.2} className="text-maison-soft" />
                    <span className="text-sm font-medium text-maison-ink">{m.label}</span>
                  </button>
                ))}
              </div>
              {payMethod === 'card' && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  className="mt-6 space-y-5 rounded-2xl bg-maison-warm p-6"
                >
                  <Field label="Número de tarjeta" placeholder="4242 4242 4242 4242" value={form.tarjeta} error={errors.tarjeta}
                    onChange={(e) => setForm({ ...form, tarjeta: e.target.value })} />
                  <div className="grid grid-cols-2 gap-5">
                    <Field label="Vencimiento" placeholder="MM/AA" value={form.venc} error={errors.venc}
                      onChange={(e) => setForm({ ...form, venc: e.target.value })} />
                    <Field label="CVV" placeholder="123" value={form.cvv} error={errors.cvv}
                      onChange={(e) => setForm({ ...form, cvv: e.target.value })} />
                  </div>
                </motion.div>
              )}
            </motion.div>
          )}

          {/* STEP 3: Revisar */}
          {step === 3 && (
            <motion.div key="s3" {...slide}>
              <h1 className="font-serif-display text-3xl text-maison-ink">Revisa tu pedido</h1>
              <div className="mt-8 space-y-6">
                <div className="rounded-3xl border border-maison-gray bg-white p-6">
                  <p className="label-micro text-maison-warmgray">Envío a</p>
                  <p className="mt-2 text-sm text-maison-soft">{form.nombre} — {form.direccion}, {form.ciudad}</p>
                  <p className="mt-1 text-sm text-maison-warmgray">
                    {shipMethod === 'express' ? 'Envío express (24 h)' : 'Envío estándar (2–4 días)'} · Pago con {payMethod === 'card' ? 'tarjeta' : 'Yape/Plin'}
                  </p>
                </div>
                <ul className="space-y-3">
                  {items.map((item) => (
                    <li key={`${item.product.id}-${item.color}-${item.size}`} className="flex items-center gap-4 rounded-2xl bg-white p-3 shadow-[0_2px_12px_rgba(0,0,0,0.03)]">
                      <img src={item.product.image} alt={item.product.name} className="h-20 w-[60px] rounded-xl object-cover" />
                      <div className="flex-1">
                        <p className="font-serif-display text-sm text-maison-ink">{item.product.name}</p>
                        <p className="text-xs text-maison-warmgray">{item.color} · {item.size} · ×{item.qty}</p>
                      </div>
                      <span className="text-sm text-maison-ink">{formatPrice(item.product.price * item.qty)}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </motion.div>
          )}

          {/* STEP 4: Confirmación */}
          {step === 4 && (
            <motion.div key="s4" {...slide} className="mx-auto max-w-lg text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: [0, 1.2, 1] }}
                transition={{ type: 'spring', stiffness: 200, damping: 14 }}
                className="relative mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-maison-sage"
              >
                <svg viewBox="0 0 24 24" className="h-10 w-10 text-white" fill="none" stroke="currentColor" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round">
                  <motion.path
                    d="M5 13l4 4L19 7"
                    initial={{ pathLength: 0 }}
                    animate={{ pathLength: 1 }}
                    transition={{ duration: 0.5, delay: 0.3 }}
                  />
                </svg>
                {/* Confetti */}
                {Array.from({ length: 16 }).map((_, i) => (
                  <motion.span
                    key={i}
                    initial={{ opacity: 1, x: 0, y: 0, scale: 0 }}
                    animate={{
                      opacity: 0,
                      x: Math.cos((i / 16) * Math.PI * 2) * (60 + (i % 3) * 20),
                      y: Math.sin((i / 16) * Math.PI * 2) * (60 + (i % 3) * 20) + 40,
                      scale: 1,
                    }}
                    transition={{ duration: 2, ease: 'easeOut', delay: 0.2 }}
                    className="absolute h-2.5 w-2.5 rounded-full"
                    style={{ background: i % 3 === 0 ? '#D4B896' : i % 3 === 1 ? '#F5E6D3' : '#E8D4C0' }}
                  />
                ))}
              </motion.div>
              <h1 className="mt-8 font-serif-display text-4xl text-maison-ink">¡Gracias por tu compra!</h1>
              <p className="mt-3 text-sm text-maison-warmgray">
                Hemos enviado la confirmación a tu correo. Tu pedido llegará pronto.
              </p>
              <button
                onClick={() => {
                  navigator.clipboard?.writeText(orderNumber);
                  setCopied(true);
                  setTimeout(() => setCopied(false), 1500);
                }}
                className="group mt-6 inline-flex items-center gap-2 rounded-full bg-maison-gray px-6 py-3 text-sm font-medium text-maison-ink transition-colors hover:bg-maison-line"
              >
                Pedido {orderNumber}
                {copied ? <Check size={16} className="text-maison-sage" /> : <Copy size={16} strokeWidth={1.5} />}
              </button>
              <div className="mt-10 flex flex-col justify-center gap-3 sm:flex-row">
                <button
                  onClick={() => navigate('/catalogo')}
                  className="rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
                >
                  Seguir comprando
                </button>
                <button className="rounded-full border-[1.5px] border-maison-ink px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-all hover:bg-maison-ink hover:text-white">
                  Ver mis órdenes
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Summary sidebar */}
        {step < 4 && (
          <aside className="h-fit rounded-3xl glass p-8 lg:sticky lg:top-28">
            <h2 className="font-serif-display text-xl text-maison-ink">Tu pedido</h2>
            <ul className="mt-5 space-y-4">
              {items.map((item) => (
                <li key={`${item.product.id}-${item.color}-${item.size}`} className="flex items-center gap-3">
                  <img src={item.product.image} alt={item.product.name} className="h-20 w-[60px] rounded-xl object-cover" />
                  <div className="flex-1">
                    <p className="font-serif-display text-sm text-maison-ink">{item.product.name}</p>
                    <p className="text-xs text-maison-warmgray">{item.color} · {item.size} · ×{item.qty}</p>
                  </div>
                  <span className="text-sm text-maison-ink">{formatPrice(item.product.price * item.qty)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-6 space-y-2.5 border-t border-maison-line pt-5 text-sm">
              <div className="flex justify-between">
                <span className="text-maison-warmgray">Subtotal</span>
                <span className="text-maison-ink">{formatPrice(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-maison-warmgray">Envío</span>
                <span className="text-maison-ink">{shipping === 0 ? 'Gratis' : formatPrice(shipping)}</span>
              </div>
              <div className="flex justify-between pt-2">
                <span className="font-medium text-maison-ink">Total</span>
                <span className="font-serif-display text-2xl text-maison-ink">{formatPrice(total)}</span>
              </div>
            </div>
            <div className="mt-6 flex items-center justify-between rounded-2xl bg-white/60 px-5 py-3.5">
              {[
                [ShieldCheck, 'Pago seguro'],
                [Truck, 'Envío gratis'],
                [RotateCcw, 'Devolución fácil'],
              ].map(([Icon, text]) => (
                <div key={text as string} className="flex items-center gap-1.5">
                  <Icon size={16} strokeWidth={1.5} className="text-maison-warmgray" />
                  <span className="text-xs text-maison-warmgray">{text as string}</span>
                </div>
              ))}
            </div>
          </aside>
        )}
      </div>

      {/* Nav buttons */}
      {step < 4 && (
        <div className="mx-auto mt-12 flex max-w-3xl justify-between pb-4">
          <button
            onClick={() => (step === 0 ? navigate('/carrito') : setStep((s) => s - 1))}
            className="rounded-full border-[1.5px] border-maison-line px-8 py-3.5 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-soft transition-all hover:border-maison-ink hover:text-maison-ink"
          >
            Volver
          </button>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={next}
            className="rounded-full bg-grad-midnight px-10 py-3.5 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
          >
            {step === 3 ? 'Confirmar pedido' : 'Continuar'}
          </motion.button>
        </div>
      )}
    </motion.main>
  );
}
