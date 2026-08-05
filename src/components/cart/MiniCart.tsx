import { useNavigate } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { X, ShoppingBag } from 'lucide-react';
import { useCartStore, useCartTotals } from '@/stores/cartStore';
import { useUIStore } from '@/stores/uiStore';
import { formatPrice, FREE_SHIPPING_THRESHOLD } from '@/lib/data';
import { QtyStepper } from '@/components/product/Selectors';
import { drawerRight, overlayFade } from '@/lib/animations';

export default function MiniCart() {
  const { cartOpen, setCartOpen } = useUIStore();
  const { items, updateQty, removeItem } = useCartStore();
  const { subtotal } = useCartTotals();
  const navigate = useNavigate();
  const remaining = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
  const progress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);

  return (
    <AnimatePresence>
      {cartOpen && (
        <>
          <motion.div
            variants={overlayFade}
            initial="hidden"
            animate="visible"
            exit="exit"
            onClick={() => setCartOpen(false)}
            className="fixed inset-0 z-[60] bg-black/30 backdrop-blur-[8px]"
          />
          <motion.aside
            variants={drawerRight}
            initial="hidden"
            animate="visible"
            exit="exit"
            className="fixed right-0 top-0 z-[61] flex h-full w-full flex-col rounded-l-[32px] bg-maison-warm shadow-elevated sm:w-[420px]"
            aria-label="Bolsa de compras"
          >
            <div className="flex items-center justify-between px-7 pt-7 pb-4">
              <h3 className="font-serif-display text-2xl text-maison-ink">Tu bolsa</h3>
              <button
                aria-label="Cerrar bolsa"
                onClick={() => setCartOpen(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full text-maison-ink transition-all hover:rotate-90 hover:bg-maison-gray"
              >
                <X size={20} strokeWidth={1.5} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-7">
              {items.length === 0 ? (
                <div className="flex h-full flex-col items-center justify-center text-center">
                  <ShoppingBag size={48} strokeWidth={1} className="text-maison-muted" />
                  <p className="mt-4 font-serif-display text-xl text-maison-ink">Tu bolsa está vacía</p>
                  <p className="mt-1 text-sm text-maison-warmgray">Descubre piezas que te enamorarán</p>
                </div>
              ) : (
                <ul className="space-y-5 py-2">
                  <AnimatePresence initial={false}>
                    {items.map((item, i) => (
                      <motion.li
                        key={`${item.product.id}-${item.color}-${item.size}`}
                        initial={{ y: 20, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ opacity: 0, x: 30 }}
                        transition={{ delay: i * 0.05 }}
                        className="flex gap-4"
                      >
                        <img
                          src={item.product.image}
                          alt={item.product.name}
                          className="h-[100px] w-20 rounded-xl object-cover"
                        />
                        <div className="flex flex-1 flex-col">
                          <div className="flex items-start justify-between gap-2">
                            <div>
                              <p className="font-serif-display text-sm text-maison-ink">{item.product.name}</p>
                              <p className="mt-0.5 text-xs text-maison-warmgray">
                                {item.color} · Talla {item.size}
                              </p>
                            </div>
                            <button
                              aria-label={`Eliminar ${item.product.name}`}
                              onClick={() => removeItem(item.product.id, item.color, item.size)}
                              className="text-maison-warmgray transition-all hover:scale-110 hover:text-maison-terracotta"
                            >
                              <X size={16} strokeWidth={1.5} />
                            </button>
                          </div>
                          <div className="mt-auto flex items-center justify-between pt-2">
                            <QtyStepper
                              qty={item.qty}
                              onChange={(q) => updateQty(item.product.id, item.color, item.size, q)}
                            />
                            <span className="text-sm font-normal text-maison-ink">
                              {formatPrice(item.product.price * item.qty)}
                            </span>
                          </div>
                        </div>
                      </motion.li>
                    ))}
                  </AnimatePresence>
                </ul>
              )}
            </div>

            {items.length > 0 && (
              <div className="border-t border-maison-line px-7 py-6">
                {/* Free shipping bar */}
                <div className="mb-5">
                  <div className="h-2 overflow-hidden rounded-full bg-maison-gray">
                    <motion.div
                      className="h-full rounded-full bg-grad-champagne"
                      initial={{ width: 0 }}
                      animate={{ width: `${progress}%` }}
                      transition={{ duration: 0.6, ease: 'easeOut' }}
                    />
                  </div>
                  <p className="mt-2 text-xs text-maison-warmgray">
                    {remaining > 0
                      ? `Te faltan ${formatPrice(remaining)} para envío gratis`
                      : '¡Tienes envío gratis!'}
                  </p>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-maison-warmgray">Subtotal</span>
                  <span className="font-serif-display text-xl text-maison-ink">{formatPrice(subtotal)}</span>
                </div>
                <motion.button
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => {
                    setCartOpen(false);
                    navigate('/checkout');
                  }}
                  className="mt-5 w-full rounded-full bg-grad-midnight py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
                >
                  Ir al checkout
                </motion.button>
                <button
                  onClick={() => setCartOpen(false)}
                  className="mt-3 w-full text-center text-[13px] text-maison-warmgray underline-offset-4 transition-colors hover:text-maison-ink hover:underline"
                >
                  Seguir comprando
                </button>
              </div>
            )}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
