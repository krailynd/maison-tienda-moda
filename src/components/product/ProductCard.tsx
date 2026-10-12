import { useState } from 'react';
import { Link } from 'react-router';
import { motion, AnimatePresence } from 'framer-motion';
import { Heart } from 'lucide-react';
import type { Product } from '@/lib/data';
import { formatPrice } from '@/lib/data';
import { useUIStore } from '@/stores/uiStore';

interface Props {
  product: Product;
  small?: boolean;
}

export default function ProductCard({ product, small }: Props) {
  const { wishlist, toggleWishlist, pushToast, setQuickView } = useUIStore();
  const [loaded, setLoaded] = useState(false);
  const wished = wishlist.includes(product.id);

  return (
    <motion.article
      className="group relative overflow-hidden rounded-3xl bg-white shadow-card transition-all duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] hover:-translate-y-2 hover:shadow-card-hover"
      layout
    >
      <Link to={`/producto/${product.slug}`} className="block">
        <div className={`relative overflow-hidden ${small ? 'rounded-t-[20px]' : 'rounded-t-[20px]'} bg-maison-gray`}>
          {!loaded && <div className="absolute inset-0 shimmer-organic" />}
          <img
            src={product.image}
            alt={product.name}
            loading="lazy"
            onLoad={() => setLoaded(true)}
            className={`aspect-[3/4] w-full object-cover transition-all duration-700 ease-[cubic-bezier(0.4,0,0.2,1)] group-hover:scale-[1.06] ${
              loaded ? 'opacity-100' : 'opacity-0'
            }`}
          />
          {/* Badges */}
          <div className="absolute left-4 top-4 flex flex-col gap-2">
            {product.isNew && (
              <span className="label-micro rounded-full bg-grad-champagne px-3 py-1.5 text-maison-ink shadow-soft">
                Nuevo
              </span>
            )}
            {product.isSale && (
              <span className="label-micro rounded-full bg-maison-terracotta/90 px-3 py-1.5 text-white">
                Oferta
              </span>
            )}
          </div>
        </div>
        <div className="p-5 transition-transform duration-300 group-hover:-translate-y-1">
          <h3 className={`font-serif-display ${small ? 'text-base' : 'text-lg'} text-maison-ink`}>
            {product.name}
          </h3>
          <div className="mt-1.5 flex items-baseline gap-2">
            <span className={`font-serif-display ${small ? 'text-sm' : 'text-base'} text-maison-ink`}>
              {formatPrice(product.price)}
            </span>
            {product.originalPrice && (
              <span className="text-sm text-maison-muted line-through">
                {formatPrice(product.originalPrice)}
              </span>
            )}
          </div>
        </div>
      </Link>

      {/* Quick actions */}
      <div className="absolute right-4 top-4 flex flex-col gap-2">
        <motion.button
          aria-label={wished ? 'Quitar de favoritos' : 'Añadir a favoritos'}
          onClick={(e) => {
            e.preventDefault();
            toggleWishlist(product.id);
            pushToast(wished ? 'info' : 'success', wished ? 'Eliminado de favoritos' : 'Añadido a favoritos');
          }}
          initial={{ opacity: 0, scale: 0.8 }}
          whileInView={{ opacity: 1, scale: 1 }}
          whileHover={{ scale: 1.2 }}
          whileTap={{ scale: 1.3 }}
          transition={{ delay: 0.1, type: 'spring', stiffness: 300, damping: 15 }}
          className="flex h-9 w-9 items-center justify-center rounded-full bg-white/90 shadow-soft backdrop-blur md:opacity-0 md:group-hover:opacity-100 transition-opacity"
        >
          <AnimatePresence mode="wait">
            <motion.span key={String(wished)} initial={{ scale: 0.6 }} animate={{ scale: 1 }}>
              <Heart
                size={18}
                strokeWidth={1.5}
                className={wished ? 'fill-maison-rose text-maison-rose' : 'text-maison-ink'}
              />
            </motion.span>
          </AnimatePresence>
        </motion.button>
      </div>

      {/* Quick view */}
      <button
        onClick={(e) => {
          e.preventDefault();
          setQuickView(product.slug);
        }}
        className="absolute inset-x-4 bottom-24 hidden rounded-full bg-white/90 py-2.5 text-[12px] font-medium uppercase tracking-[0.06em] text-maison-ink opacity-0 shadow-soft backdrop-blur transition-all duration-300 hover:bg-white group-hover:opacity-100 md:block"
      >
        Vista rápida
      </button>
    </motion.article>
  );
}
