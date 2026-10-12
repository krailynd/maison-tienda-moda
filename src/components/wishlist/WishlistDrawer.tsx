import { useNavigate } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { Heart, X } from 'lucide-react';
import { useUIStore } from '@/stores/uiStore';
import { useProducts } from '@/hooks/useApi';
import { formatPrice } from '@/lib/data';
import { drawerRight, overlayFade } from '@/lib/animations';

export default function WishlistDrawer() {
  const { wishlistOpen, setWishlistOpen, wishlist, toggleWishlist } = useUIStore();
  const { products, loading } = useProducts(200);
  const navigate = useNavigate();
  const wished = products.filter((p) => wishlist.includes(p.id));

  return (
    <AnimatePresence>
      {wishlistOpen && (
        <>
          <motion.div
            variants={overlayFade}
            initial="hidden"
            animate="visible"
            exit="exit"
            onClick={() => setWishlistOpen(false)}
            className="fixed inset-0 z-[60] bg-black/30 backdrop-blur-[8px]"
          />
          <motion.aside
            variants={drawerRight}
            initial="hidden"
            animate="visible"
            exit="exit"
            className="fixed right-0 top-0 z-[61] flex h-full w-full flex-col rounded-l-[32px] bg-maison-warm shadow-elevated sm:w-[420px]"
            aria-label="Tus favoritos"
          >
            <div className="flex items-center justify-between px-7 pt-7 pb-4">
              <h3 className="font-serif-display text-2xl text-maison-ink">Tus favoritos</h3>
              <button
                aria-label="Cerrar favoritos"
                onClick={() => setWishlistOpen(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full text-maison-ink transition-all hover:rotate-90 hover:bg-maison-gray"
              >
                <X size={20} strokeWidth={1.5} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-7">
              {loading ? (
                <div className="flex h-full flex-col items-center justify-center text-center">
                  <p className="text-sm text-maison-warmgray">Cargando favoritos…</p>
                </div>
              ) : wished.length === 0 ? (
                <div className="flex h-full flex-col items-center justify-center text-center">
                  <Heart size={48} strokeWidth={1} className="text-maison-muted" />
                  <p className="mt-4 font-serif-display text-xl text-maison-ink">Tu lista está vacía</p>
                  <p className="mt-1 text-sm text-maison-warmgray">Toca el corazón de una pieza para guardarla aquí</p>
                </div>
              ) : (
                <ul className="space-y-5 py-2">
                  <AnimatePresence initial={false}>
                    {wished.map((product, i) => (
                      <motion.li
                        key={product.id}
                        initial={{ y: 20, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ opacity: 0, x: 30 }}
                        transition={{ delay: i * 0.05 }}
                        onClick={() => {
                          navigate(`/producto/${product.slug}`);
                          setWishlistOpen(false);
                        }}
                        className="flex cursor-pointer gap-4"
                      >
                        <img
                          src={product.image}
                          alt={product.name}
                          className="h-[100px] w-20 rounded-xl object-cover"
                        />
                        <div className="flex flex-1 flex-col">
                          <div className="flex items-start justify-between gap-2">
                            <p className="font-serif-display text-sm text-maison-ink">{product.name}</p>
                            <button
                              aria-label="Quitar de favoritos"
                              onClick={(e) => {
                                e.stopPropagation();
                                toggleWishlist(product.id);
                              }}
                              className="text-maison-warmgray transition-all hover:scale-110 hover:text-maison-terracotta"
                            >
                              <Heart size={16} strokeWidth={1.5} fill="currentColor" />
                            </button>
                          </div>
                          <p className="mt-0.5 text-xs text-maison-warmgray">
                            {product.categoryName ?? product.category}
                          </p>
                          <div className="mt-auto pt-2">
                            <span className="text-sm font-normal text-maison-ink">
                              {formatPrice(product.price)}
                            </span>
                          </div>
                        </div>
                      </motion.li>
                    ))}
                  </AnimatePresence>
                </ul>
              )}
            </div>

            {wished.length > 0 && (
              <div className="border-t border-maison-line px-7 py-6">
                <motion.button
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => {
                    setWishlistOpen(false);
                    navigate('/catalogo');
                  }}
                  className="w-full rounded-full bg-grad-midnight py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-shadow hover:shadow-glow"
                >
                  Ver todo el catálogo
                </motion.button>
                <button
                  onClick={() => setWishlistOpen(false)}
                  className="mt-3 w-full text-center text-[13px] text-maison-warmgray underline-offset-4 transition-colors hover:text-maison-ink hover:underline"
                >
                  Seguir navegando
                </button>
              </div>
            )}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
