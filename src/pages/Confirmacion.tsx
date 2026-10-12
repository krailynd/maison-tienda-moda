import { Link, useSearchParams } from 'react-router';
import { motion } from 'framer-motion';
import { ShoppingBag, Truck, ShieldCheck, RotateCcw } from 'lucide-react';
import { formatPrice } from '@/lib/data';
import { useOrder } from '@/hooks/useApi';
import { pageEnter } from '@/lib/animations';

export default function Confirmacion() {
  const [params] = useSearchParams();
  const orderNumber = params.get('order') ?? '';
  const { order, loading, error } = useOrder(orderNumber);

  const statusPaid = order?.paymentStatus === 'PAID' || order?.status === 'PAID';

  return (
    <motion.main
      variants={pageEnter}
      initial="hidden"
      animate="visible"
      exit="exit"
      className="mx-auto max-w-3xl px-5 pt-28 pb-16 md:px-8 md:pt-36"
    >
      {!orderNumber ? (
        <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
          <ShoppingBag size={48} strokeWidth={1} className="text-maison-muted" />
          <p className="mt-4 font-serif-display text-2xl text-maison-ink">No encontramos tu pedido</p>
          <Link to="/catalogo" className="mt-6 rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white">
            Ir a la tienda
          </Link>
        </div>
      ) : loading ? (
        <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
          <svg className="h-12 w-12 animate-spin text-maison-ink" viewBox="0 0 24 24" fill="none" aria-label="Cargando">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" strokeDasharray="32" strokeDashoffset="10" strokeLinecap="round" />
          </svg>
          <p className="mt-6 font-serif-display text-2xl text-maison-ink">Cargando tu pedido…</p>
        </div>
      ) : error || !order ? (
        <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
          <ShoppingBag size={48} strokeWidth={1} className="text-maison-muted" />
          <p className="mt-4 font-serif-display text-2xl text-maison-ink">No pudimos cargar tu pedido</p>
          {error && <p className="mt-3 text-sm text-maison-warmgray">{error}</p>}
          <Link to="/catalogo" className="mt-6 rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white">
            Ir a la tienda
          </Link>
        </div>
      ) : (
        <div className="text-center">
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

          <div className="mx-auto mt-8 flex max-w-md flex-col items-center justify-center gap-3 sm:flex-row">
            <span className="rounded-full bg-maison-gray px-6 py-3 text-sm font-medium text-maison-ink">
              Pedido {order.orderNumber}
            </span>
            <span
              className={`rounded-full px-6 py-3 text-sm font-medium ${
                statusPaid ? 'bg-maison-sage text-white' : 'bg-maison-terracotta/90 text-white'
              }`}
            >
              {statusPaid ? 'Pagado' : order.status}
            </span>
          </div>

          <div className="mx-auto mt-10 max-w-md rounded-3xl border border-maison-gray bg-white p-8 text-left">
            <h2 className="font-serif-display text-xl text-maison-ink">Resumen</h2>
            <ul className="mt-5 space-y-4">
              {(order.items ?? []).map((item, i) => (
                <li key={i} className="flex items-center gap-4">
                  {item.imageUrl && (
                    <img src={item.imageUrl} alt={item.productName} className="h-16 w-14 rounded-xl object-cover" />
                  )}
                  <div className="flex-1">
                    <p className="font-serif-display text-sm text-maison-ink">{item.productName}</p>
                    <p className="text-xs text-maison-warmgray">
                      {item.variantName ?? `${item.color ?? ''} ${item.size ?? ''}`.trim()} · ×{item.quantity}
                    </p>
                  </div>
                  <span className="text-sm text-maison-ink">{formatPrice(item.totalPrice)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-6 space-y-2.5 border-t border-maison-line pt-5 text-sm">
              <div className="flex justify-between">
                <span className="text-maison-warmgray">Subtotal</span>
                <span className="text-maison-ink">{formatPrice(order.subtotal ?? order.totalAmount ?? order.total ?? 0)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-maison-warmgray">Envío</span>
                <span className="text-maison-ink">{formatPrice(order.shippingCost ?? order.shippingAmount ?? 0)}</span>
              </div>
              <div className="flex justify-between pt-2">
                <span className="font-medium text-maison-ink">Total</span>
                <span className="font-serif-display text-2xl text-maison-ink">{formatPrice(order.total ?? order.totalAmount ?? 0)}</span>
              </div>
            </div>
          </div>

          <div className="mx-auto mt-6 flex max-w-md items-center justify-between rounded-2xl bg-white/60 px-5 py-3.5">
            {[
              [ShieldCheck, 'Pago seguro'],
              [Truck, 'Envío seguro'],
              [RotateCcw, 'Devolución fácil'],
            ].map(([Icon, text]) => (
              <div key={text as string} className="flex items-center gap-1.5">
                <Icon size={16} strokeWidth={1.5} className="text-maison-warmgray" />
                <span className="text-xs text-maison-warmgray">{text as string}</span>
              </div>
            ))}
          </div>

          <div className="mt-10 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              to="/catalogo"
              className="rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
            >
              Seguir comprando
            </Link>
            <Link
              to="/catalogo"
              className="rounded-full border-[1.5px] border-maison-ink px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-all hover:bg-maison-ink hover:text-white"
            >
              Ver más productos
            </Link>
          </div>
        </div>
      )}
    </motion.main>
  );
}
