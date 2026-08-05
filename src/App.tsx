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
import QuickView from '@/components/product/QuickView';
import Toasts from '@/components/Toasts';
import Home from '@/pages/Home';
import Catalogo from '@/pages/Catalogo';
import Producto from '@/pages/Producto';
import Carrito from '@/pages/Carrito';
import Checkout from '@/pages/Checkout';

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

  return (
    <div className="min-h-screen bg-maison-warm font-sans">
      <AnimatePresence>{loading && <Preloader />}</AnimatePresence>
      <Header />
      <AnimatePresence mode="wait">
        <Routes location={location} key={location.pathname}>
          <Route path="/" element={<Home />} />
          <Route path="/catalogo" element={<Catalogo />} />
          <Route path="/catalogo/:categoria" element={<Catalogo />} />
          <Route path="/producto/:slug" element={<Producto />} />
          <Route path="/carrito" element={<Carrito />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="*" element={<Home />} />
        </Routes>
      </AnimatePresence>
      <Footer />
      <MiniCart />
      <QuickView />
      <Toasts />
    </div>
  );
}
