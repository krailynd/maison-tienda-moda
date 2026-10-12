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
  wishlistOpen: boolean;
  mobileMenuOpen: boolean;
  quickViewSlug: string | null;
  authOpen: boolean;
  authOrigin: { x: number; y: number } | null;
  wishlist: string[];
  toasts: Toast[];
  setCartOpen: (open: boolean) => void;
  setWishlistOpen: (open: boolean) => void;
  setMobileMenuOpen: (open: boolean) => void;
  setQuickView: (slug: string | null) => void;
  setAuthOpen: (open: boolean, origin?: { x: number; y: number }) => void;
  toggleWishlist: (id: string) => void;
  pushToast: (type: ToastType, message: string) => void;
  dismissToast: (id: number) => void;
}

let toastId = 0;

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      cartOpen: false,
      wishlistOpen: false,
      mobileMenuOpen: false,
      quickViewSlug: null,
      authOpen: false,
      authOrigin: null,
      wishlist: [],
      toasts: [],
      setCartOpen: (open) => set({ cartOpen: open }),
      setWishlistOpen: (open) => set({ wishlistOpen: open }),
      setMobileMenuOpen: (open) => set({ mobileMenuOpen: open }),
      setQuickView: (slug) => set({ quickViewSlug: slug }),
      setAuthOpen: (open, origin) =>
        set({ authOpen: open, authOrigin: open ? origin ?? null : null }),
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
