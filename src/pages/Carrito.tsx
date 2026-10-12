import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ShoppingBag, Check } from 'lucide-react';
import { useCartStore, useCartTotals } from '@/stores/cartStore';
import { formatPrice, FREE_SHIPPING_THRESHOLD } from '@/lib/data';
import { QtyStepper } from '@/components/product/Selectors';
import { pageEnter } from '@/lib/animations';

export default function Carrito() {
  const { items, updateQty, removeItem } = useCartStore();
  const { subtotal } = useCartTotals();
  const [coupon, setCoupon] = useState('');
  const [couponOk, setCouponOk] = useState(false);
  const navigate = useNavigate();

  const discount = couponOk ? subtotal * 0.1 : 0;
  const shipping = subtotal - discount >= FREE_SHIPPING_THRESHOLD || items.length === 0 ? 0 : 25;
  const total = subtotal - discount + shipping;

  return (
    <motion.main variants={pageEnter} initial="hidden" animate="visible" exit="exit" className="mx-auto max-w-7xl px-5 pt-28 md:px-8 md:pt-36">
      <h1 className="font-serif-display text-4xl text-maison-ink md:text-5xl">Tu bolsa</h1>
      <p className="mt-2 text-sm text-maison-warmgray">
        {items.length === 0 ? 'No tienes productos aún' : `${items.length} ${items.length === 1 ? 'artículo' : 'artículos'}`}
      </p>

      {items.length === 0 ? (
        <div className="mt-16 flex flex-col items-center text-center">
          <ShoppingBag size={56} strokeWidth={1} className="text-maison-muted" />
          <p className="mt-5 font-serif-display text-2xl text-maison-ink">Tu bolsa está vacía</p>
          <Link to="/catalogo">
            <motion.span
              whileHover={{ scale: 1.03 }}
              className="mt-6 inline-block rounded-full bg-grad-midnight px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white"
            >
              Descubrir la colección
            </motion.span>
          </Link>
        </div>
      ) : (
        <div className="mt-10 grid gap-10 lg:grid-cols-[65%_35%]">
          {/* Items */}
          <ul className="space-y-4">
            <AnimatePresence initial={false}>
              {items.map((item) => (
                <motion.li
                  key={`${item.product.id}-${item.color}-${item.size}`}
                  layout
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: 40 }}
                  className="flex gap-5 rounded-3xl bg-white p-4 shadow-[0_2px_12px_rgba(0,0,0,0.03)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-soft"
                >
                  <Link to={`/producto/${item.product.slug}`}>
                    <img
                      src={item.product.image}
                      alt={item.product.name}
                      className="h-[130px] w-[100px] rounded-2xl object-cover"
                    />
                  </Link>
                  <div className="flex flex-1 flex-col py-1">
                    <div className="flex items-start justify-between">
                      <div>
                        <Link to={`/producto/${item.product.slug}`} className="font-serif-display text-lg text-maison-ink hover:opacity-70 transition-opacity">
                          {item.product.name}
                        </Link>
                        <p className="mt-1 text-xs text-maison-warmgray">
                          Color {item.color} · Talla {item.size}
                        </p>
                      </div>
                      <button
                        aria-label={`Eliminar ${item.product.name}`}
                        onClick={() => removeItem(item.product.id, item.color, item.size)}
                        className="text-maison-warmgray transition-all hover:scale-110 hover:text-maison-terracotta"
                      >
                        <X size={18} strokeWidth={1.5} />
                      </button>
                    </div>
                    <div className="mt-auto flex items-center justify-between">
                      <QtyStepper
                        qty={item.qty}
                        onChange={(q) => updateQty(item.product.id, item.color, item.size, q)}
                      />
                      <span className="font-serif-display text-lg text-maison-ink">
                        {formatPrice(item.product.price * item.qty)}
                      </span>
                    </div>
                  </div>
                </motion.li>
              ))}
            </AnimatePresence>
          </ul>

          {/* Summary */}
          <aside className="h-fit rounded-3xl border border-maison-gray bg-white p-8 lg:sticky lg:top-24">
            <h2 className="font-serif-display text-2xl text-maison-ink">Resumen</h2>
            <div className="mt-5 flex gap-2">
              <input
                value={coupon}
                onChange={(e) => { setCoupon(e.target.value); setCouponOk(false); }}
                placeholder="Código de descuento"
                aria-label="Cupón de descuento"
                className={`flex-1 rounded-full border bg-white px-5 py-3 text-sm outline-none transition-all ${
                  couponOk ? 'border-maison-sage' : 'border-maison-line focus:border-maison-ink'
                }`}
              />
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => coupon.trim().length >= 4 && setCouponOk(true)}
                className="flex items-center gap-1.5 rounded-full bg-maison-gray px-5 py-3 text-[12px] font-medium uppercase tracking-[0.06em] text-maison-ink transition-colors hover:bg-maison-line"
              >
                {couponOk && <Check size={14} className="text-maison-sage" />}
                Aplicar
              </motion.button>
            </div>
            {couponOk && (
              <motion.p initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="mt-2 text-xs text-maison-sage">
                Cupón aplicado: −10%
              </motion.p>
            )}
            <div className="mt-6 space-y-3 text-sm">
              <div className="flex justify-between border-b border-maison-gray pb-3">
                <span className="text-maison-warmgray">Subtotal</span>
                <span className="text-maison-ink">{formatPrice(subtotal)}</span>
              </div>
              {couponOk && (
                <div className="flex justify-between border-b border-maison-gray pb-3">
                  <span className="text-maison-warmgray">Descuento</span>
                  <span className="text-maison-sage">−{formatPrice(discount)}</span>
                </div>
              )}
              <div className="flex justify-between border-b border-maison-gray pb-3">
                <span className="text-maison-warmgray">Envío</span>
                <span className="text-maison-ink">{shipping === 0 ? 'Gratis' : formatPrice(shipping)}</span>
              </div>
              <div className="flex justify-between pt-1">
                <span className="font-medium text-maison-ink">Total</span>
                <span className="font-serif-display text-2xl text-maison-ink">{formatPrice(total)}</span>
              </div>
            </div>
            <motion.button
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => navigate('/checkout')}
              className="mt-6 w-full rounded-full bg-grad-midnight py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
            >
              Proceder al pago
            </motion.button>
            <Link to="/catalogo" className="mt-4 block text-center text-[13px] text-maison-warmgray underline-offset-4 transition-colors hover:text-maison-ink hover:underline">
              Seguir comprando
            </Link>
          </aside>
        </div>
      )}
    </motion.main>
  );
}
