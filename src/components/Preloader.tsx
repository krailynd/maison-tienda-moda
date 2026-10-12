import { motion } from 'framer-motion';

export default function Preloader() {
  return (
    <motion.div
      className="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-maison-warm bg-grain"
      initial={{ opacity: 1 }}
      exit={{ opacity: 0, scale: 1, filter: 'blur(8px)', transition: { duration: 0.6, ease: 'easeOut' } }}
    >
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: 'easeOut' }}
        className="text-center"
      >
        <span className="font-serif-display text-4xl md:text-5xl text-maison-ink tracking-tight">
          MAISON
        </span>
        <p className="label-micro text-maison-warmgray mt-2">Moda Premium</p>
      </motion.div>
      <div className="mt-8 h-[2px] w-[120px] overflow-hidden rounded-full bg-maison-line">
        <motion.div
          className="h-full rounded-full bg-grad-champagne"
          initial={{ width: '0%' }}
          animate={{ width: '100%' }}
          transition={{ duration: 2, ease: 'easeInOut' }}
        />
      </div>
    </motion.div>
  );
}
