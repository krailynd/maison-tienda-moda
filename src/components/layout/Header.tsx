import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, User, Heart, ShoppingBag, Menu, X } from 'lucide-react';
import { useCartTotals } from '@/stores/cartStore';
import { useUIStore } from '@/stores/uiStore';
import { useAuthStore } from '@/stores/authStore';
import { CATEGORIES } from '@/lib/data';
import { springBounce } from '@/lib/animations';

const NAV = [
  { label: 'MUJER', to: '/catalogo/mujer' },
  { label: 'HOMBRE', to: '/catalogo/hombre' },
  { label: 'COLECCIONES', to: '/catalogo' },
  { label: 'NUEVO', to: '/catalogo?filtro=nuevo' },
  { label: 'OFERTAS', to: '/catalogo?filtro=ofertas' },
];

export default function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [megaOpen, setMegaOpen] = useState(false);
  const megaTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { count } = useCartTotals();
  const { wishlist, setCartOpen, setWishlistOpen, mobileMenuOpen, setMobileMenuOpen, setAuthOpen } = useUIStore();
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  const openAccount = (e: React.MouseEvent<HTMLButtonElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setAuthOpen(true, { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 });
  };

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const openMega = () => {
    if (megaTimer.current) clearTimeout(megaTimer.current);
    megaTimer.current = setTimeout(() => setMegaOpen(true), 150);
  };
  const closeMega = () => {
    if (megaTimer.current) clearTimeout(megaTimer.current);
    setMegaOpen(false);
  };

  return (
    <>
      <motion.header
        initial={{ y: -80, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1], delay: 0.1 }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
          scrolled
            ? 'border-b border-black/5 bg-[rgba(250,249,246,0.85)] shadow-[0_4px_24px_rgba(0,0,0,0.04)] backdrop-blur-[24px] backdrop-saturate-[180%]'
            : 'bg-transparent border-b border-transparent'
        }`}
      >
        <div className="mx-auto flex h-16 md:h-[72px] max-w-7xl items-center justify-between px-5 md:px-8">
          {/* Logo */}
          <Link to="/" aria-label="MAISON inicio" className="relative z-10 transition-opacity duration-300 hover:opacity-70">
            <span className="font-serif-display text-2xl text-maison-ink">MAISON</span>
          </Link>

          {/* Nav desktop */}
          <nav className="hidden lg:flex items-center gap-8" aria-label="Principal">
            {NAV.map((item) => (
              <div
                key={item.label}
                className="relative"
                onMouseEnter={item.label === 'MUJER' || item.label === 'HOMBRE' ? openMega : undefined}
                onMouseLeave={closeMega}
              >
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    `group relative flex flex-col items-center text-[13px] font-medium uppercase tracking-[0.06em] transition-colors duration-200 ${
                      isActive ? 'text-maison-ink' : 'text-maison-soft hover:text-maison-warmgray'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      {item.label}
                      <motion.span
                        className="absolute -bottom-2 h-1 w-1 rounded-full bg-maison-ink"
                        initial={false}
                        animate={isActive ? { scale: 1, y: 0, opacity: 1 } : { scale: 0, y: 4, opacity: 0 }}
                        transition={springBounce}
                      />
                      <motion.span
                        className="absolute -bottom-2 h-1 w-1 rounded-full bg-maison-ink group-hover:opacity-100"
                        initial={{ scale: 0, y: 4, opacity: 0 }}
                        whileHover={{ scale: 1, y: 0, opacity: 1 }}
                        transition={springBounce}
                        style={{ opacity: 0 }}
                      />
                    </>
                  )}
                </NavLink>
              </div>
            ))}
          </nav>

          {/* Utility icons */}
          <div className="flex items-center gap-4 md:gap-5">
            <button aria-label="Buscar" onClick={() => navigate('/catalogo')} className="hidden sm:block text-maison-ink transition-all duration-200 hover:scale-110">
              <Search size={22} strokeWidth={1.5} />
            </button>
            <button aria-label="Cuenta" onClick={openAccount} className="hidden sm:block text-maison-ink transition-all duration-200 hover:scale-110">
              {user ? (
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-maison-ink text-sm font-medium text-white">
                  {user.firstName.charAt(0).toUpperCase()}
                </span>
              ) : (
                <User size={22} strokeWidth={1.5} />
              )}
            </button>
            <button aria-label="Favoritos" onClick={() => setWishlistOpen(true)} className="relative text-maison-ink transition-all duration-200 hover:scale-110">
              <Heart size={22} strokeWidth={1.5} />
              <AnimatePresence>
                {wishlist.length > 0 && (
                  <motion.span
                    key={wishlist.length}
                    initial={{ scale: 0 }}
                    animate={{ scale: [0, 1.2, 1] }}
                    exit={{ scale: 0 }}
                    transition={{ duration: 0.3 }}
                    className="absolute -right-2 -top-2 flex h-[18px] w-[18px] items-center justify-center rounded-full bg-maison-ink text-[10px] font-medium text-white"
                  >
                    {wishlist.length}
                  </motion.span>
                )}
              </AnimatePresence>
            </button>
            <button aria-label="Bolsa de compras" onClick={() => setCartOpen(true)} className="relative text-maison-ink transition-all duration-200 hover:scale-110">
              <ShoppingBag size={22} strokeWidth={1.5} />
              <AnimatePresence>
                {count > 0 && (
                  <motion.span
                    key={count}
                    initial={{ scale: 0 }}
                    animate={{ scale: [0, 1.2, 1] }}
                    exit={{ scale: 0 }}
                    transition={{ duration: 0.3 }}
                    className="absolute -right-2 -top-2 flex h-[18px] w-[18px] items-center justify-center rounded-full bg-maison-ink text-[10px] font-medium text-white"
                  >
                    {count}
                  </motion.span>
                )}
              </AnimatePresence>
            </button>
            <button
              aria-label={mobileMenuOpen ? 'Cerrar menú' : 'Abrir menú'}
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden text-maison-ink"
            >
              {mobileMenuOpen ? <X size={24} strokeWidth={1.5} /> : <Menu size={24} strokeWidth={1.5} />}
            </button>
          </div>
        </div>

        {/* Mega menu */}
        <AnimatePresence>
          {megaOpen && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
              onMouseEnter={openMega}
              onMouseLeave={closeMega}
              className="absolute left-0 right-0 hidden lg:block rounded-b-3xl shadow-[0_32px_64px_rgba(0,0,0,0.08)] glass-strong"
            >
              <div className="mx-auto grid max-w-7xl grid-cols-4 gap-6 px-8 py-10">
                {CATEGORIES.map((cat, i) => (
                  <motion.div
                    key={cat.slug}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.05 * i, duration: 0.4 }}
                  >
                    <Link to={`/catalogo/${cat.slug}`} onClick={closeMega} className="group block">
                      <div className="overflow-hidden rounded-2xl">
                        <img
                          src={cat.image}
                          alt={cat.title}
                          loading="lazy"
                          className="aspect-[3/4] w-full object-cover transition-transform duration-[600ms] ease-out group-hover:scale-[1.04]"
                        />
                      </div>
                      <p className="mt-3 font-serif-display text-lg text-maison-ink">{cat.title}</p>
                      <p className="text-sm text-maison-warmgray">{cat.tagline}</p>
                    </Link>
                  </motion.div>
                ))}
                <motion.div
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.15, duration: 0.4 }}
                  className="flex flex-col justify-center"
                >
                  <p className="label-micro text-maison-warmgray">Temporada</p>
                  <p className="mt-2 font-serif-display text-2xl text-maison-ink">Primavera 2026</p>
                  <p className="mt-2 text-sm text-maison-warmgray max-w-[200px]">
                    Piezas esenciales en tonos tierra, tejidos naturales y cortes impecables.
                  </p>
                  <Link to="/catalogo" onClick={closeMega} className="group mt-4 inline-flex items-center gap-2 text-[13px] font-medium uppercase tracking-[0.06em] text-maison-ink">
                    Ver todo
                    <span className="transition-transform duration-300 group-hover:translate-x-1.5">→</span>
                  </Link>
                </motion.div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.header>

      {/* Mobile menu */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileMenuOpen(false)}
              className="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm lg:hidden"
            />
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', stiffness: 260, damping: 30 }}
              className="fixed right-0 top-0 z-40 h-full w-[85%] max-w-sm rounded-l-3xl glass-strong p-8 pt-24 lg:hidden"
            >
              <nav className="flex flex-col gap-6" aria-label="Móvil">
                {NAV.map((item, i) => (
                  <motion.div
                    key={item.label}
                    initial={{ x: 20, opacity: 0 }}
                    animate={{ x: 0, opacity: 1 }}
                    transition={{ delay: 0.08 + i * 0.05 }}
                  >
                    <Link
                      to={item.to}
                      onClick={() => setMobileMenuOpen(false)}
                      className="font-serif-display text-3xl text-maison-ink"
                    >
                      {item.label.charAt(0) + item.label.slice(1).toLowerCase()}
                    </Link>
                  </motion.div>
                ))}
                <motion.div
                  initial={{ x: 20, opacity: 0 }}
                  animate={{ x: 0, opacity: 1 }}
                  transition={{ delay: 0.08 + NAV.length * 0.05 }}
                >
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      setAuthOpen(true);
                    }}
                    className="font-serif-display text-3xl text-maison-ink"
                  >
                    {user ? `Hola, ${user.firstName}` : 'Cuenta'}
                  </button>
                </motion.div>
              </nav>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
