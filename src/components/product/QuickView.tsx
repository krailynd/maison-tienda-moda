import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { X, Star } from 'lucide-react';
import { PRODUCTS, formatPrice } from '@/lib/data';
import { useUIStore } from '@/stores/uiStore';
import { useCartStore } from '@/stores/cartStore';
import { ColorSwatches, SizeSelector } from './Selectors';

export default function QuickView() {
  const { quickViewSlug, setQuickView, pushToast, setCartOpen } = useUIStore();
  const addItem = useCartStore((s) => s.addItem);
  const product = PRODUCTS.find((p) => p.slug === quickViewSlug) ?? null;
  const [color, setColor] = useState('');
  const [size, setSize] = useState<string | null>(null);

  useEffect(() => {
    if (product) {
      setColor(product.colors[0].name);
      setSize(null);
    }
  }, [quickViewSlug]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setQuickView(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [setQuickView]);

  return (
    <AnimatePresence>
      {product && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            onClick={() => setQuickView(null)}
            className="fixed inset-0 z-[70] bg-black/40 backdrop-blur-[8px]"
          />
          <div className="fixed inset-0 z-[71] flex items-center justify-center p-4 pointer-events-none">
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.95, opacity: 0, y: 10 }}
              transition={{ type: 'spring', stiffness: 260, damping: 25 }}
              className="pointer-events-auto grid w-full max-w-[900px] overflow-hidden rounded-[32px] glass-strong shadow-elevated md:grid-cols-2"
              role="dialog"
              aria-modal="true"
              aria-label={`Vista rápida de ${product.name}`}
            >
              <div className="relative bg-maison-gray">
                <img src={product.image} alt={product.name} className="h-64 w-full object-cover md:h-full" />
              </div>
              <div className="relative p-8">
                <button
                  aria-label="Cerrar vista rápida"
                  onClick={() => setQuickView(null)}
                  className="absolute right-5 top-5 flex h-9 w-9 items-center justify-center rounded-full bg-white/80 text-maison-ink transition-transform hover:rotate-90"
                >
                  <X size={18} strokeWidth={1.5} />
                </button>
                <h3 className="font-serif-display text-2xl text-maison-ink">{product.name}</h3>
                <div className="mt-2 flex items-center gap-2">
                  <span className="font-serif-display text-xl text-maison-ink">{formatPrice(product.price)}</span>
                  {product.originalPrice && (
                    <span className="text-sm text-maison-muted line-through">{formatPrice(product.originalPrice)}</span>
                  )}
                </div>
                <div className="mt-2 flex items-center gap-1.5">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Star
                      key={i}
                      size={16}
                      className={i < Math.round(product.rating) ? 'fill-maison-terracotta text-maison-terracotta' : 'text-maison-line'}
                    />
                  ))}
                  <span className="ml-1 text-xs text-maison-warmgray">{product.rating} · {product.reviews} reseñas</span>
                </div>
                <p className="mt-4 text-sm leading-relaxed text-maison-warmgray line-clamp-3">{product.description}</p>
                <div className="mt-5">
                  <p className="label-micro text-maison-warmgray mb-2">Color — {color}</p>
                  <ColorSwatches colors={product.colors} selected={color} onSelect={setColor} />
                </div>
                <div className="mt-5">
                  <p className="label-micro text-maison-warmgray mb-2">Talla</p>
                  <SizeSelector sizes={product.sizes} selected={size} onSelect={setSize} />
                </div>
                <div className="mt-7 flex gap-3">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => {
                      if (!size) {
                        pushToast('error', 'Selecciona una talla');
                        return;
                      }
                      addItem(product, color, size);
                      pushToast('success', 'Añadido a tu bolsa');
                      setQuickView(null);
                      setCartOpen(true);
                    }}
                    className="flex-1 rounded-full bg-grad-midnight py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
                  >
                    Añadir al carrito
                  </motion.button>
                  <Link
                    to={`/producto/${product.slug}`}
                    onClick={() => setQuickView(null)}
                    className="rounded-full border-[1.5px] border-maison-ink px-6 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-all hover:bg-maison-ink hover:text-white"
                  >
                    Ver
                  </Link>
                </div>
              </div>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
