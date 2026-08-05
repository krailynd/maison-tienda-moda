import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { Star, Heart, ChevronDown, Check, Truck, RotateCcw, ShieldCheck } from 'lucide-react';
import { PRODUCTS, formatPrice } from '@/lib/data';
import { useCartStore } from '@/stores/cartStore';
import { useUIStore } from '@/stores/uiStore';
import { ColorSwatches, SizeSelector, QtyStepper } from '@/components/product/Selectors';
import ProductCard from '@/components/product/ProductCard';
import { pageEnter, staggerContainer, staggerItem } from '@/lib/animations';

function Accordion({ title, children, defaultOpen = false }: { title: string; children: React.ReactNode; defaultOpen?: boolean }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="border-b border-maison-gray">
      <button
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className="flex w-full items-center justify-between py-5 text-left"
      >
        <span className="text-[13px] font-medium uppercase tracking-[0.06em] text-maison-ink">{title}</span>
        <motion.span animate={{ rotate: open ? 180 : 0 }} transition={{ type: 'spring', stiffness: 300, damping: 20 }}>
          <ChevronDown size={18} strokeWidth={1.5} className="text-maison-warmgray" />
        </motion.span>
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.35, ease: [0.4, 0, 0.2, 1] }}
            className="overflow-hidden"
          >
            <div className="pb-5 text-sm leading-relaxed text-maison-warmgray">{children}</div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default function Producto() {
  const { slug } = useParams();
  const product = PRODUCTS.find((p) => p.slug === slug);
  const addItem = useCartStore((s) => s.addItem);
  const { pushToast, setCartOpen, wishlist, toggleWishlist } = useUIStore();

  const [color, setColor] = useState('');
  const [size, setSize] = useState<string | null>(null);
  const [qty, setQty] = useState(1);
  const [adding, setAdding] = useState<'idle' | 'loading' | 'done'>('idle');
  const [zoom, setZoom] = useState(false);
  const [zoomPos, setZoomPos] = useState({ x: 50, y: 50 });
  const [heartBurst, setHeartBurst] = useState(0);
  const imgRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (product) {
      setColor(product.colors[0].name);
      setSize(null);
      setQty(1);
      setAdding('idle');
    }
  }, [slug]);

  if (!product) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <p className="font-serif-display text-3xl text-maison-ink">Producto no encontrado</p>
          <Link to="/catalogo" className="mt-4 inline-block text-sm text-maison-warmgray underline">
            Volver al catálogo
          </Link>
        </div>
      </main>
    );
  }

  const wished = wishlist.includes(product.id);
  const related = PRODUCTS.filter((p) => p.category === product.category && p.id !== product.id).slice(0, 4);

  const onZoomMove = (e: React.MouseEvent) => {
    const rect = imgRef.current?.getBoundingClientRect();
    if (!rect) return;
    setZoomPos({
      x: ((e.clientX - rect.left) / rect.width) * 100,
      y: ((e.clientY - rect.top) / rect.height) * 100,
    });
  };

  const handleAdd = () => {
    if (!size) {
      pushToast('error', 'Selecciona una talla');
      return;
    }
    setAdding('loading');
    setTimeout(() => {
      addItem(product, color, size, qty);
      setAdding('done');
      pushToast('success', 'Añadido a tu bolsa');
      setTimeout(() => {
        setAdding('idle');
        setCartOpen(true);
      }, 1200);
    }, 700);
  };

  return (
    <motion.main variants={pageEnter} initial="hidden" animate="visible" exit="exit" className="mx-auto max-w-7xl px-5 pt-24 md:px-8 md:pt-32">
      <div className="grid gap-10 lg:grid-cols-[55%_45%]">
        {/* Gallery */}
        <div>
          <div
            ref={imgRef}
            onMouseEnter={() => setZoom(true)}
            onMouseLeave={() => setZoom(false)}
            onMouseMove={onZoomMove}
            className={`relative overflow-hidden rounded-[32px] bg-maison-gray ${zoom ? 'cursor-zoom-in' : ''}`}
          >
            <AnimatePresence mode="wait">
              <motion.img
                key={product.image + color}
                src={product.image}
                alt={product.name}
                initial={{ opacity: 0, filter: 'blur(4px)' }}
                animate={{ opacity: 1, filter: 'blur(0px)' }}
                exit={{ opacity: 0, filter: 'blur(4px)' }}
                transition={{ duration: 0.4 }}
                className="aspect-[3/4] w-full object-cover transition-transform duration-300"
                style={
                  zoom
                    ? { transform: 'scale(1.5)', transformOrigin: `${zoomPos.x}% ${zoomPos.y}%` }
                    : undefined
                }
              />
            </AnimatePresence>
            <div className="absolute left-4 top-4 flex flex-col gap-2">
              {product.isNew && (
                <span className="label-micro rounded-full bg-grad-champagne px-3 py-1.5 text-maison-ink">Nuevo</span>
              )}
              {product.isSale && (
                <span className="label-micro rounded-full bg-maison-terracotta/90 px-3 py-1.5 text-white">Oferta</span>
              )}
            </div>
          </div>
        </div>

        {/* Info */}
        <motion.div variants={staggerContainer(0.08)} initial="hidden" animate="visible">
          <motion.div variants={staggerItem}>
            <span className="glass inline-block rounded-full px-4 py-1.5 text-xs text-maison-soft">
              {product.category.charAt(0).toUpperCase() + product.category.slice(1)} / {product.name}
            </span>
          </motion.div>
          <motion.h1 variants={staggerItem} className="mt-4 font-serif-display text-[32px] leading-[1.1] text-maison-ink">
            {product.name}
          </motion.h1>
          <motion.div variants={staggerItem} className="mt-3 flex items-center gap-3">
            <span className="font-serif-display text-2xl text-maison-ink">{formatPrice(product.price)}</span>
            {product.originalPrice && (
              <span className="text-lg text-maison-muted line-through">{formatPrice(product.originalPrice)}</span>
            )}
          </motion.div>
          <motion.div variants={staggerItem} className="group relative mt-3 flex w-fit items-center gap-1.5">
            {Array.from({ length: 5 }).map((_, i) => (
              <Star
                key={i}
                size={16}
                className={`transition-transform group-hover:scale-110 ${
                  i < Math.round(product.rating) ? 'fill-maison-terracotta text-maison-terracotta' : 'text-maison-line'
                }`}
              />
            ))}
            <span className="ml-1 text-xs text-maison-warmgray">
              {product.rating} de 5 — {product.reviews} reseñas
            </span>
          </motion.div>
          <motion.p variants={staggerItem} className="mt-5 text-[15px] leading-relaxed text-maison-warmgray">
            {product.description}
          </motion.p>

          <motion.div variants={staggerItem} className="mt-7">
            <p className="label-micro mb-3 text-maison-warmgray">Color — {color}</p>
            <ColorSwatches colors={product.colors} selected={color} onSelect={setColor} />
          </motion.div>
          <motion.div variants={staggerItem} className="mt-6">
            <p className="label-micro mb-3 text-maison-warmgray">Talla</p>
            <SizeSelector sizes={product.sizes} selected={size} onSelect={setSize} />
          </motion.div>
          <motion.div variants={staggerItem} className="mt-6">
            <p className="label-micro mb-3 text-maison-warmgray">Cantidad</p>
            <QtyStepper qty={qty} onChange={setQty} />
          </motion.div>

          {/* Actions */}
          <motion.div variants={staggerItem} className="mt-8 flex items-center gap-3">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={handleAdd}
              disabled={adding !== 'idle'}
              className={`flex h-[52px] flex-1 items-center justify-center gap-2 rounded-full px-12 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-all duration-300 ${
                adding === 'done' ? 'bg-maison-sage' : 'bg-grad-midnight hover:shadow-glow'
              }`}
            >
              {adding === 'loading' ? (
                <svg className="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none" aria-label="Añadiendo">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" strokeDasharray="32" strokeDashoffset="10" strokeLinecap="round" />
                </svg>
              ) : adding === 'done' ? (
                <>
                  <Check size={18} /> Añadido
                </>
              ) : (
                'Añadir al carrito'
              )}
            </motion.button>
            <div className="relative">
              <motion.button
                aria-label={wished ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                whileHover={{ scale: 1.2 }}
                whileTap={{ scale: 1.3 }}
                onClick={() => {
                  toggleWishlist(product.id);
                  if (!wished) setHeartBurst((b) => b + 1);
                  pushToast(wished ? 'info' : 'success', wished ? 'Eliminado de favoritos' : 'Añadido a favoritos');
                }}
                className="flex h-[52px] w-[52px] items-center justify-center rounded-full border-[1.5px] border-maison-line bg-white"
              >
                <Heart
                  size={24}
                  strokeWidth={1.5}
                  className={wished ? 'fill-maison-rose text-maison-rose' : 'text-maison-ink'}
                />
              </motion.button>
              {heartBurst > 0 && (
                <div key={heartBurst} className="pointer-events-none absolute inset-0">
                  {[0, 1, 2, 3].map((i) => (
                    <motion.span
                      key={i}
                      initial={{ opacity: 1, x: 0, y: 0, scale: 0.5 }}
                      animate={{
                        opacity: 0,
                        x: Math.cos((i * Math.PI) / 2) * 28,
                        y: Math.sin((i * Math.PI) / 2) * 28 - 12,
                        scale: 1,
                      }}
                      transition={{ duration: 0.8, ease: 'easeOut' }}
                      className="absolute left-1/2 top-1/2"
                    >
                      <Heart size={10} className="fill-maison-rose text-maison-rose" />
                    </motion.span>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
          <motion.button
            variants={staggerItem}
            whileHover={{ scale: 1.01 }}
            onClick={() => {
              if (!size) {
                pushToast('error', 'Selecciona una talla');
                return;
              }
              addItem(product, color, size, qty);
              setCartOpen(true);
            }}
            className="mt-3 w-full rounded-full border-[1.5px] border-maison-ink py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-all duration-300 hover:bg-maison-ink hover:text-white"
          >
            Comprar ahora
          </motion.button>

          {/* Trust */}
          <motion.div variants={staggerItem} className="mt-7 flex justify-between rounded-2xl bg-maison-cream px-6 py-4">
            {[
              [Truck, 'Envío gratis +S/500'],
              [RotateCcw, 'Devolución 30 días'],
              [ShieldCheck, 'Compra protegida'],
            ].map(([Icon, text]) => (
              <div key={text as string} className="flex flex-col items-center gap-1.5 text-center">
                <Icon size={20} strokeWidth={1.5} className="text-maison-soft" />
                <span className="text-[11px] text-maison-warmgray">{text as string}</span>
              </div>
            ))}
          </motion.div>

          {/* Accordions */}
          <motion.div variants={staggerItem} className="mt-8">
            <Accordion title="Descripción" defaultOpen>
              {product.description}
            </Accordion>
            <Accordion title="Detalles y cuidados">
              <ul className="list-disc space-y-1 pl-5">
                {product.details.map((d) => (
                  <li key={d}>{d}</li>
                ))}
              </ul>
            </Accordion>
            <Accordion title="Envío y devoluciones">
              Envío estándar de 2 a 4 días hábiles. Gratis en compras sobre S/ 500. Devoluciones gratuitas
              dentro de los 30 días posteriores a la entrega.
            </Accordion>
            <Accordion title={`Reseñas (${product.reviews})`}>
              Valoración media de {product.rating} sobre 5 basada en {product.reviews} reseñas verificadas.
              Las clientas destacan la calidad del tejido y la fidelidad de las tallas.
            </Accordion>
          </motion.div>
        </motion.div>
      </div>

      {/* Related */}
      {related.length > 0 && (
        <section className="mt-24">
          <h2 className="font-serif-display text-3xl text-maison-ink">Completa tu look</h2>
          <div className="mt-8 grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-6">
            {related.map((p) => (
              <ProductCard key={p.id} product={p} small />
            ))}
          </div>
        </section>
      )}
    </motion.main>
  );
}
