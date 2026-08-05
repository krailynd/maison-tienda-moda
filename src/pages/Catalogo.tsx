import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronDown, LayoutGrid, Rows3 } from 'lucide-react';
import { PRODUCTS, CATEGORIES } from '@/lib/data';
import ProductCard from '@/components/product/ProductCard';
import { staggerContainer, staggerItem, pageEnter } from '@/lib/animations';

type Sort = 'relevancia' | 'precio-asc' | 'precio-desc' | 'nuevo';

const SORTS: { value: Sort; label: string }[] = [
  { value: 'relevancia', label: 'Relevancia' },
  { value: 'nuevo', label: 'Novedades' },
  { value: 'precio-asc', label: 'Precio: menor a mayor' },
  { value: 'precio-desc', label: 'Precio: mayor a menor' },
];

const FILTERS = [
  { value: 'todos', label: 'Todos' },
  { value: 'nuevo', label: 'Nuevo' },
  { value: 'ofertas', label: 'Ofertas' },
];

export default function Catalogo() {
  const { categoria } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const filtro = searchParams.get('filtro') ?? 'todos';
  const [sort, setSort] = useState<Sort>('relevancia');
  const [sortOpen, setSortOpen] = useState(false);
  const [view, setView] = useState<'grid' | 'list'>('grid');

  const catInfo = CATEGORIES.find((c) => c.slug === categoria);
  const title = catInfo?.title ?? (filtro === 'ofertas' ? 'Ofertas' : filtro === 'nuevo' ? 'Nuevo' : 'Colecciones');

  const products = useMemo(() => {
    let list = [...PRODUCTS];
    if (categoria) list = list.filter((p) => p.category === categoria);
    if (filtro === 'nuevo') list = list.filter((p) => p.isNew);
    if (filtro === 'ofertas') list = list.filter((p) => p.isSale);
    switch (sort) {
      case 'precio-asc': list.sort((a, b) => a.price - b.price); break;
      case 'precio-desc': list.sort((a, b) => b.price - a.price); break;
      case 'nuevo': list.sort((a, b) => Number(b.isNew ?? false) - Number(a.isNew ?? false)); break;
    }
    return list;
  }, [categoria, filtro, sort]);

  return (
    <motion.main variants={pageEnter} initial="hidden" animate="visible" exit="exit" className="pb-8">
      {/* Category header */}
      <div className="relative h-[40vh] overflow-hidden rounded-b-[32px]">
        <motion.img
          src={catInfo?.image ?? '/images/hero.png'}
          alt={`Banner ${title}`}
          className="h-full w-full object-cover"
          initial={{ scale: 1.1 }}
          animate={{ scale: 1 }}
          transition={{ duration: 1.2, ease: [0.4, 0, 0.2, 1] }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
        <h1 className="absolute bottom-10 left-1/2 -translate-x-1/2 w-full max-w-7xl px-5 md:px-8 font-serif-display text-5xl text-white [text-shadow:0_2px_24px_rgba(0,0,0,0.3)]">
          {title}
        </h1>
        <div className="absolute bottom-[-1px] left-1/2 -translate-x-1/2 w-full max-w-7xl px-5 md:px-8 pb-3">
          <span className="glass inline-block rounded-full px-4 py-1.5 text-xs text-maison-soft">
            Inicio / Catálogo / {title}
          </span>
        </div>
      </div>

      {/* Toolbar */}
      <div className="mx-auto mt-8 flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 md:px-8">
        <div className="flex gap-2.5">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setSearchParams(f.value === 'todos' ? {} : { filtro: f.value })}
              className={`rounded-full px-5 py-2.5 text-[13px] font-medium transition-all duration-200 ${
                filtro === f.value
                  ? 'bg-maison-ink text-white'
                  : 'bg-maison-gray text-maison-soft hover:bg-maison-line'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <button
              onClick={() => setSortOpen(!sortOpen)}
              className="flex items-center gap-2 rounded-full bg-maison-gray px-5 py-2.5 text-[13px] font-medium text-maison-soft transition-colors hover:bg-maison-line"
              aria-haspopup="listbox"
              aria-expanded={sortOpen}
            >
              {SORTS.find((s) => s.value === sort)?.label}
              <motion.span animate={{ rotate: sortOpen ? 180 : 0 }} transition={{ type: 'spring', stiffness: 300, damping: 20 }}>
                <ChevronDown size={16} strokeWidth={1.5} />
              </motion.span>
            </button>
            <AnimatePresence>
              {sortOpen && (
                <motion.ul
                  role="listbox"
                  initial={{ opacity: 0, y: -8, scale: 0.97 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -8, scale: 0.97 }}
                  transition={{ duration: 0.2 }}
                  className="absolute right-0 z-30 mt-2 w-56 overflow-hidden rounded-2xl glass-strong p-1.5 shadow-elevated"
                >
                  {SORTS.map((s, i) => (
                    <motion.li
                      key={s.value}
                      initial={{ opacity: 0, x: -8 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.04 }}
                    >
                      <button
                        role="option"
                        aria-selected={sort === s.value}
                        onClick={() => { setSort(s.value); setSortOpen(false); }}
                        className={`w-full rounded-xl px-4 py-2.5 text-left text-sm transition-colors ${
                          sort === s.value ? 'bg-maison-ink text-white' : 'text-maison-soft hover:bg-maison-gray'
                        }`}
                      >
                        {s.label}
                      </button>
                    </motion.li>
                  ))}
                </motion.ul>
              )}
            </AnimatePresence>
          </div>
          <div className="flex rounded-full bg-maison-gray p-1">
            {([['grid', LayoutGrid], ['list', Rows3]] as const).map(([v, Icon]) => (
              <button
                key={v}
                aria-label={`Vista ${v}`}
                onClick={() => setView(v)}
                className={`rounded-full p-2 transition-all ${view === v ? 'bg-white shadow-soft' : 'text-maison-warmgray'}`}
              >
                <Icon size={18} strokeWidth={1.5} />
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Grid */}
      <motion.div
        variants={staggerContainer(0.08)}
        initial="hidden"
        animate="visible"
        key={`${categoria}-${filtro}-${sort}-${view}`}
        className={`mx-auto mt-10 max-w-7xl px-5 md:px-8 ${
          view === 'grid'
            ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-5 lg:grid-cols-4 lg:gap-6'
            : 'mx-auto grid max-w-3xl grid-cols-1 gap-5'
        }`}
      >
        {products.map((p) => (
          <motion.div key={p.id} variants={staggerItem}>
            <ProductCard product={p} />
          </motion.div>
        ))}
      </motion.div>
      {products.length === 0 && (
        <p className="mt-20 text-center text-maison-warmgray">No hay productos en esta selección.</p>
      )}
    </motion.main>
  );
}
