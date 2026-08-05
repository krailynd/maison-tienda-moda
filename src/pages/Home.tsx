import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { motion, useScroll, useTransform } from 'framer-motion';
import { ArrowRight, ArrowDown, Check } from 'lucide-react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { PRODUCTS, CATEGORIES } from '@/lib/data';
import ProductCard from '@/components/product/ProductCard';
import { fadeUp, staggerContainer, staggerItem } from '@/lib/animations';
import { useUIStore } from '@/stores/uiStore';

gsap.registerPlugin(ScrollTrigger);

function Hero() {
  const ref = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start start', 'end start'] });
  const y = useTransform(scrollYProgress, [0, 1], ['0%', '50%']);
  const opacity = useTransform(scrollYProgress, [0, 0.8], [1, 0]);

  return (
    <section ref={ref} className="relative h-[85vh] md:h-screen overflow-hidden">
      <motion.div style={{ y }} className="absolute inset-0 scale-110">
        <img
          src="/images/hero.png"
          alt="Colección Primavera 2026 — modelo con vestido de seda color crema"
          className="h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent" />
      </motion.div>
      <motion.div
        style={{ opacity }}
        className="relative z-10 flex h-full flex-col items-center justify-end pb-24 md:pb-28 text-center px-5"
      >
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.7 }}
          className="label-micro text-white/80"
        >
          Nueva colección
        </motion.p>
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45, duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
          className="mt-3 font-serif-display text-[32px] leading-[1.05] text-white [text-shadow:0_2px_24px_rgba(0,0,0,0.25)] md:text-[56px]"
        >
          La elegancia
          <br />
          <em className="font-normal">en cada hilo</em>
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.7 }}
          className="mt-4 max-w-md text-base font-light text-white/90"
        >
          Piezas atemporales confeccionadas con seda, cachemir y lino de la más alta calidad.
        </motion.p>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.75, duration: 0.7 }}
        >
          <Link to="/catalogo">
            <motion.span
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.98 }}
              className="mt-8 inline-block rounded-full bg-white px-10 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-maison-ink transition-shadow hover:shadow-[0_0_40px_rgba(255,255,255,0.4)]"
            >
              Explorar la colección
            </motion.span>
          </Link>
        </motion.div>
      </motion.div>
      <motion.div
        animate={{ y: [0, 8, 0] }}
        transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
        className="absolute bottom-8 left-1/2 z-10 -translate-x-1/2 flex h-10 w-10 items-center justify-center rounded-full border border-white/40 text-white"
        aria-hidden
      >
        <ArrowDown size={18} strokeWidth={1.5} />
      </motion.div>
    </section>
  );
}

function Categories() {
  return (
    <motion.section
      variants={staggerContainer(0.12)}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: '-80px' }}
      className="mx-auto max-w-7xl px-5 pt-24 md:px-8"
    >
      <motion.p variants={staggerItem} className="label-micro text-maison-warmgray">
        Explora por universo
      </motion.p>
      <motion.h2 variants={staggerItem} className="mt-2 font-serif-display text-4xl text-maison-ink">
        Categorías
      </motion.h2>
      <div className="mt-10 grid gap-6 md:grid-cols-3">
        {CATEGORIES.map((cat) => (
          <motion.div key={cat.slug} variants={staggerItem}>
            <Link to={`/catalogo/${cat.slug}`} className="group relative block overflow-hidden rounded-[32px]">
              <img
                src={cat.image}
                alt={`Categoría ${cat.title}`}
                loading="lazy"
                className="aspect-[3/4] w-full object-cover transition-transform duration-[800ms] ease-[cubic-bezier(0.4,0,0.2,1)] group-hover:scale-[1.08]"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent transition-opacity duration-500 group-hover:opacity-80" />
              <div className="absolute inset-0 flex flex-col items-center justify-end pb-10">
                <h3 className="font-serif-display text-[28px] text-white transition-transform duration-400 group-hover:-translate-y-2">
                  {cat.title}
                </h3>
                <p className="mt-1 text-sm text-white/70">{cat.tagline}</p>
                <span className="mt-4 translate-y-2.5 rounded-full bg-white px-6 py-2.5 text-[12px] font-medium uppercase tracking-[0.06em] text-maison-ink opacity-0 transition-all duration-400 group-hover:translate-y-0 group-hover:opacity-100">
                  Explorar
                </span>
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </motion.section>
  );
}

function Featured() {
  const featured = PRODUCTS.filter((p) => p.isNew || p.isSale).slice(0, 6);
  const dragRef = useRef<HTMLDivElement>(null);
  const [dragWidth, setDragWidth] = useState(0);

  useEffect(() => {
    const measure = () => {
      if (dragRef.current) setDragWidth(dragRef.current.scrollWidth - dragRef.current.offsetWidth);
    };
    measure();
    window.addEventListener('resize', measure);
    return () => window.removeEventListener('resize', measure);
  }, []);

  return (
    <section className="pt-24">
      <div className="mx-auto flex max-w-7xl items-end justify-between px-5 md:px-8">
        <motion.div variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }}>
          <p className="label-micro text-maison-warmgray">Recién llegados</p>
          <h2 className="mt-2 font-serif-display text-4xl text-maison-ink">Nuevos Lanzamientos</h2>
        </motion.div>
        <Link to="/catalogo" className="group hidden items-center gap-2 text-[13px] font-medium uppercase tracking-[0.06em] text-maison-ink sm:flex">
          Ver todo
          <ArrowRight size={16} strokeWidth={1.5} className="transition-transform duration-300 group-hover:translate-x-1" />
        </Link>
      </div>
      <div ref={dragRef} className="mt-10 overflow-hidden pl-5 md:pl-[max(2rem,calc((100vw-80rem)/2+2rem))]">
        <motion.div
          drag="x"
          dragConstraints={{ left: -dragWidth, right: 0 }}
          dragElastic={0.08}
          className="flex cursor-grab gap-6 active:cursor-grabbing"
          whileTap={{ cursor: 'grabbing' }}
        >
          {featured.map((p) => (
            <div key={p.id} className="w-[260px] shrink-0 md:w-[300px]" onDragStart={(e) => e.preventDefault()}>
              <ProductCard product={p} />
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

function Editorial() {
  const sectionRef = useRef<HTMLElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);
  const circleRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const ctx = gsap.context(() => {
      if (imgRef.current) {
        gsap.fromTo(
          imgRef.current,
          { yPercent: -10 },
          {
            yPercent: 10,
            ease: 'none',
            scrollTrigger: { trigger: sectionRef.current, start: 'top bottom', end: 'bottom top', scrub: true },
          }
        );
      }
      if (circleRef.current) {
        gsap.fromTo(
          circleRef.current,
          { y: -30 },
          {
            y: 60,
            ease: 'none',
            scrollTrigger: { trigger: sectionRef.current, start: 'top bottom', end: 'bottom top', scrub: 0.5 },
          }
        );
      }
    }, sectionRef);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={sectionRef} className="relative mx-auto max-w-7xl px-5 pt-28 md:px-8">
      <div
        ref={circleRef}
        className="pointer-events-none absolute right-0 top-10 h-[300px] w-[300px] rounded-full"
        style={{ background: 'radial-gradient(circle, rgba(196,149,106,0.1) 0%, transparent 70%)' }}
        aria-hidden
      />
      <div className="grid items-center gap-10 md:grid-cols-5">
        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
          className="overflow-hidden rounded-[32px] md:col-span-3"
        >
          <img
            ref={imgRef}
            src="/images/editorial.png"
            alt="Editorial Primavera 2026 — modelos en tonos neutros"
            loading="lazy"
            className="aspect-[3/2] w-full scale-[1.2] object-cover"
          />
        </motion.div>
        <motion.div
          variants={staggerContainer(0.1)}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-80px' }}
          className="relative md:col-span-2"
        >
          <motion.p variants={staggerItem} className="label-micro text-maison-warmgray tracking-[0.1em]">
            Colección Primavera 2026
          </motion.p>
          <motion.h2 variants={staggerItem} className="mt-3 font-serif-display text-[40px] leading-[1.1] text-maison-ink">
            Vestir ligero,
            <br />
            vivir despacio
          </motion.h2>
          <motion.p variants={staggerItem} className="mt-4 max-w-[400px] text-base leading-relaxed text-maison-warmgray">
            Una oda a los tejidos naturales y los tonos tierra. Cada pieza de esta colección nace del
            respeto por el oficio artesanal y la belleza de lo esencial.
          </motion.p>
          <motion.div variants={staggerItem}>
            <Link to="/catalogo" className="group mt-6 inline-flex items-center gap-2 text-[13px] font-medium uppercase tracking-[0.06em] text-maison-ink">
              <span className="relative">
                Descubrir la colección
                <span className="absolute -bottom-1 left-0 h-px w-0 bg-maison-ink transition-all duration-400 group-hover:w-full" />
              </span>
              <ArrowRight size={16} strokeWidth={1.5} className="transition-transform duration-300 group-hover:translate-x-2" />
            </Link>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}

function Newsletter() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const { pushToast } = useUIStore();

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.includes('@')) {
      pushToast('error', 'Ingresa un correo válido');
      return;
    }
    setSent(true);
    pushToast('success', '¡Bienvenido(a) al club MAISON!');
    setTimeout(() => setSent(false), 2500);
    setEmail('');
  };

  return (
    <section
      className="mx-auto max-w-5xl px-5 pt-28 md:px-8"
      style={{ background: 'radial-gradient(ellipse at top, #F5F0EB, #FAF9F6)' }}
    >
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-80px' }}
        transition={{ duration: 0.7, ease: [0.4, 0, 0.2, 1] }}
        className="relative overflow-visible rounded-[32px] glass px-6 py-14 text-center shadow-soft md:px-16"
      >
        {/* Confetti */}
        {sent && (
          <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-[32px]">
            {Array.from({ length: 18 }).map((_, i) => (
              <motion.span
                key={i}
                initial={{ y: '20%', x: `${10 + i * 5}%`, opacity: 1, scale: 0 }}
                animate={{ y: `${60 + (i % 3) * 30}%`, opacity: 0, scale: 1, rotate: 180 }}
                transition={{ duration: 1, delay: i * 0.03, ease: 'easeOut' }}
                className="absolute top-0 h-2 w-2 rounded-full"
                style={{ background: i % 2 ? '#D4B896' : '#F5E6D3' }}
              />
            ))}
          </div>
        )}
        <p className="label-micro text-maison-warmgray">Newsletter</p>
        <h2 className="mt-3 font-serif-display text-3xl text-maison-ink md:text-4xl">
          Recibe lo nuevo antes que nadie
        </h2>
        <p className="mx-auto mt-3 max-w-md text-sm text-maison-warmgray">
          Acceso anticipado a colecciones, invitaciones a eventos privados y 10% en tu primera compra.
        </p>
        <form onSubmit={submit} className="mx-auto mt-8 flex max-w-md flex-col gap-3 sm:flex-row">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="tu@correo.com"
            aria-label="Correo electrónico"
            className="flex-1 rounded-full border border-maison-line bg-white px-6 py-4 text-sm text-maison-soft outline-none transition-all focus:border-maison-ink focus:shadow-[0_0_0_3px_rgba(15,15,15,0.08)]"
          />
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            type="submit"
            className={`flex items-center justify-center gap-2 rounded-full px-8 py-4 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-colors ${
              sent ? 'bg-maison-sage' : 'bg-maison-ink hover:shadow-glow'
            }`}
          >
            {sent ? (
              <>
                <Check size={16} /> Suscrito
              </>
            ) : (
              'Suscribirme'
            )}
          </motion.button>
        </form>
      </motion.div>
    </section>
  );
}

export default function Home() {
  return (
    <motion.main
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
    >
      <Hero />
      <Categories />
      <Featured />
      <Editorial />
      <Newsletter />
    </motion.main>
  );
}
