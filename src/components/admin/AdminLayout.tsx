import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { LayoutDashboard, LogOut, Menu, Package, ShoppingCart, Tags, Store, Sparkles } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { logoutAdmin } from '@/lib/adminApi';

const NAV_ITEMS = [
  { to: '/admin/products', label: 'Productos', icon: Package },
  { to: '/admin/categories', label: 'Categorías', icon: Tags },
  { to: '/admin/brands', label: 'Marcas', icon: Store },
  { to: '/admin/collections', label: 'Colecciones', icon: Sparkles },
  { to: '/admin/orders', label: 'Pedidos', icon: ShoppingCart },
];

function SidebarContent({ variant, onNavigate }: { variant: 'desktop' | 'mobile'; onNavigate?: () => void }) {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const handleLogout = async () => {
    await logoutAdmin();
    navigate('/admin/login', { replace: true });
  };

  return (
    <>
      <div className="flex items-center gap-2 px-6 py-5">
        <LayoutDashboard className="h-5 w-5 text-maison-terracotta" />
        <span className="font-serif text-lg tracking-wide">MAISON Admin</span>
      </div>
      <nav className="flex-1 space-y-1 px-3 py-4">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `relative block rounded-xl ${isActive ? 'text-white' : 'text-maison-muted hover:text-white'}`
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <motion.span
                    layoutId={`admin-nav-pill-${variant}`}
                    className="absolute inset-0 rounded-xl bg-maison-soft"
                    transition={{ type: 'spring', stiffness: 400, damping: 32 }}
                  />
                )}
                <motion.span
                  whileHover={{ scale: isActive ? 1 : 1.02 }}
                  whileTap={{ scale: 0.97 }}
                  className={`relative z-10 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-colors ${
                    isActive ? '' : 'hover:bg-maison-soft/60'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </motion.span>
              </>
            )}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-maison-line/20 px-6 py-4">
        <p className="truncate text-sm font-medium">{user?.email ?? 'Administrador'}</p>
        <button
          type="button"
          onClick={handleLogout}
          className="mt-3 flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm text-maison-muted transition-colors hover:bg-maison-soft hover:text-white"
        >
          <LogOut className="h-4 w-4" />
          Cerrar sesión
        </button>
      </div>
    </>
  );
}

/**
 * Admin shell: responsive sidebar navigation + session footer, rendering the
 * current admin page through the Outlet. Desktop gets a fixed sidebar; mobile
 * gets a hamburger top bar and a spring slide-in drawer.
 */
export default function AdminLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const closeNav = () => setMobileNavOpen(false);

  useEffect(() => {
    if (!mobileNavOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileNavOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [mobileNavOpen]);

  useEffect(() => {
    if (!mobileNavOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [mobileNavOpen]);

  return (
    <div className="flex min-h-screen bg-maison-gray">
      <motion.aside
        initial={{ x: -32, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ duration: 0.4, ease: 'easeOut' }}
        className="hidden w-64 shrink-0 flex-col border-r border-maison-line bg-maison-ink text-maison-white md:flex"
      >
        <SidebarContent variant="desktop" />
      </motion.aside>

      <header className="fixed inset-x-0 top-0 z-40 flex h-16 items-center justify-between bg-maison-ink px-4 text-white md:hidden">
        <button
          type="button"
          aria-label="Abrir menú"
          onClick={() => setMobileNavOpen(true)}
          className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-white/10"
        >
          <Menu className="h-5 w-5" />
        </button>
        <span className="font-serif text-lg tracking-wide">MAISON Admin</span>
        <div className="h-9 w-9" />
      </header>

      <AnimatePresence>
        {mobileNavOpen && (
          <>
            <motion.div
              key="admin-nav-overlay"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={closeNav}
              className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm md:hidden"
            />
            <motion.aside
              key="admin-nav-drawer"
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              className="fixed inset-y-0 left-0 z-50 flex w-72 max-w-[85vw] flex-col bg-maison-ink text-maison-white md:hidden"
            >
              <SidebarContent variant="mobile" onNavigate={closeNav} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      <main className="min-w-0 flex-1 pt-16 md:pt-0">
        <div className="p-4 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
