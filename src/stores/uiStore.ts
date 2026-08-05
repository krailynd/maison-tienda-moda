import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ToastType = 'success' | 'error' | 'info';
export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

interface UIState {
  cartOpen: boolean;
  mobileMenuOpen: boolean;
  quickViewSlug: string | null;
  wishlist: string[];
  toasts: Toast[];
  setCartOpen: (open: boolean) => void;
  setMobileMenuOpen: (open: boolean) => void;
  setQuickView: (slug: string | null) => void;
  toggleWishlist: (id: string) => void;
  pushToast: (type: ToastType, message: string) => void;
  dismissToast: (id: number) => void;
}

let toastId = 0;

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      cartOpen: false,
      mobileMenuOpen: false,
      quickViewSlug: null,
      wishlist: [],
      toasts: [],
      setCartOpen: (open) => set({ cartOpen: open }),
      setMobileMenuOpen: (open) => set({ mobileMenuOpen: open }),
      setQuickView: (slug) => set({ quickViewSlug: slug }),
      toggleWishlist: (id) =>
        set((s) => ({
          wishlist: s.wishlist.includes(id)
            ? s.wishlist.filter((w) => w !== id)
            : [...s.wishlist, id],
        })),
      pushToast: (type, message) => {
        const id = ++toastId;
        set((s) => ({ toasts: [...s.toasts, { id, type, message }] }));
        setTimeout(() => {
          set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
        }, 3200);
      },
      dismissToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
    }),
    {
      name: 'maison-ui',
      partialize: (s) => ({ wishlist: s.wishlist }),
    }
  )
);
