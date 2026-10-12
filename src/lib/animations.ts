import type { Variants, Transition } from 'framer-motion';

export const easeOut: Transition['ease'] = [0.4, 0, 0.2, 1];

export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 40 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: easeOut } },
};

export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.8 } },
};

export const scaleIn: Variants = {
  hidden: { opacity: 0, scale: 0.95 },
  visible: { opacity: 1, scale: 1, transition: { duration: 0.6, ease: easeOut } },
};

export const staggerContainer = (stagger = 0.08, delay = 0): Variants => ({
  hidden: {},
  visible: { transition: { staggerChildren: stagger, delayChildren: delay } },
});

export const staggerItem: Variants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: easeOut } },
};

export const pageEnter: Variants = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: easeOut } },
  exit: { opacity: 0, transition: { duration: 0.2 } },
};

export const springBounce: Transition = { type: 'spring', stiffness: 300, damping: 20 };

export const drawerRight: Variants = {
  hidden: { x: '100%' },
  visible: { x: '0%', transition: { type: 'spring', stiffness: 260, damping: 30 } },
  exit: { x: '100%', transition: { duration: 0.3, ease: easeOut } },
};

export const overlayFade: Variants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.3 } },
  exit: { opacity: 0, transition: { duration: 0.25 } },
};
