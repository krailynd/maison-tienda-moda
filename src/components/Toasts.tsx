import { AnimatePresence, motion } from 'framer-motion';
import { Check, AlertCircle, Info } from 'lucide-react';
import { useUIStore } from '@/stores/uiStore';

const ICONS = { success: Check, error: AlertCircle, info: Info };
const COLORS = {
  success: 'border-l-maison-sage',
  error: 'border-l-maison-terracotta',
  info: 'border-l-maison-warmgray',
};

export default function Toasts() {
  const { toasts } = useUIStore();
  return (
    <div className="pointer-events-none fixed bottom-6 left-0 right-0 z-[90] flex flex-col items-center gap-2 px-4">
      <AnimatePresence>
        {toasts.map((t) => {
          const Icon = ICONS[t.type];
          return (
            <motion.div
              key={t.id}
              initial={{ y: 20, opacity: 0, scale: 0.95 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: 10, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              className={`glass flex items-center gap-3 rounded-2xl border-l-[3px] px-6 py-4 shadow-soft ${COLORS[t.type]}`}
            >
              <Icon size={18} strokeWidth={1.5} className="text-maison-soft" />
              <span className="text-sm font-normal text-maison-soft">{t.message}</span>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
