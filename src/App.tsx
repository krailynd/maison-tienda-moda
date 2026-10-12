import { useEffect, useState } from 'react';
import { Routes, Route, useLocation } from 'react-router';
import { AnimatePresence } from 'framer-motion';
import Lenis from 'lenis';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import Preloader from '@/components/Preloader';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';
import MiniCart from '@/components/cart/MiniCart';
import WishlistDrawer from '@/components/wishlist/WishlistDrawer';
import QuickView from '@/components/product/QuickView';
import AuthModal from '@/components/auth/AuthModal';
import Toasts from '@/components/Toasts';
import Home from '@/pages/Home';
import Catalogo from '@/pages/Catalogo';
import Producto from '@/pages/Producto';
import Carrito from '@/pages/Carrito';
import Checkout from '@/pages/Checkout';
import Confirmacion from '@/pages/Confirmacion';
import AdminLayout from '@/components/admin/AdminLayout';
import RequireAdmin from '@/components/admin/RequireAdmin';
import AdminLogin from '@/pages/admin/AdminLogin';
import AdminProducts from '@/pages/admin/AdminProducts';
import AdminProductForm from '@/pages/admin/AdminProductForm';
import AdminCategories from '@/pages/admin/AdminCategories';
import AdminBrands from '@/pages/admin/AdminBrands';
import AdminCollections from '@/pages/admin/AdminCollections';
import AdminOrders from '@/pages/admin/AdminOrders';
import AdminOrderDetail from '@/pages/admin/AdminOrderDetail';

gsap.registerPlugin(ScrollTrigger);

export default function App() {
  const [loading, setLoading] = useState(true);
  const location = useLocation();

  // Lenis smooth scroll
  useEffect(() => {
    const lenis = new Lenis({ lerp: 0.1, smoothWheel: true });
    lenis.on('scroll', ScrollTrigger.update);
    const raf = (time: number) => lenis.raf(time);
    let rafId: number;
    const loop = (time: number) => {
      raf(time);
      rafId = requestAnimationFrame(loop);
    };
    rafId = requestAnimationFrame(loop);
    return () => {
      cancelAnimationFrame(rafId);
      lenis.destroy();
    };
  }, []);

  // Scroll to top on route change
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  // Preloader timing
  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 2400);
    return () => clearTimeout(t);
  }, []);

  const isAdminPath = location.pathname.startsWith('/admin');

  return (
    <div className="min-h-screen bg-maison-warm font-sans">
      <AnimatePresence>{loading && <Preloader />}</AnimatePresence>

      {isAdminPath ? (
        <Routes>
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <AdminLayout />
              </RequireAdmin>
            }
          >
            <Route path="products" element={<AdminProducts />} />
            <Route path="products/new" element={<AdminProductForm />} />
            <Route path="products/:id" element={<AdminProductForm />} />
            <Route path="categories" element={<AdminCategories />} />
            <Route path="brands" element={<AdminBrands />} />
            <Route path="collections" element={<AdminCollections />} />
            <Route path="orders" element={<AdminOrders />} />
            <Route path="orders/:id" element={<AdminOrderDetail />} />
          </Route>
        </Routes>
      ) : (
        <>
          <Header />
          <AnimatePresence mode="wait">
            <Routes location={location} key={location.pathname}>
              <Route path="/" element={<Home />} />
              <Route path="/catalogo" element={<Catalogo />} />
              <Route path="/catalogo/:categoria" element={<Catalogo />} />
              <Route path="/producto/:slug" element={<Producto />} />
              <Route path="/carrito" element={<Carrito />} />
              <Route path="/checkout" element={<Checkout />} />
              <Route path="/confirmacion" element={<Confirmacion />} />
              <Route path="*" element={<Home />} />
            </Routes>
          </AnimatePresence>
          <Footer />
        </>
      )}

      <MiniCart />
      <WishlistDrawer />
      <QuickView />
      <AuthModal />
      <Toasts />
    </div>
  );
}
