import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Product } from '@/lib/data';

export interface CartItem {
  product: Product;
  color: string;
  size: string;
  qty: number;
  variantId?: number;
}

interface CartState {
  items: CartItem[];
  addItem: (product: Product, color: string, size: string, qty?: number, variantId?: number) => void;
  removeItem: (id: string, color: string, size: string) => void;
  updateQty: (id: string, color: string, size: string, qty: number) => void;
  clear: () => void;
}

const key = (id: string, color: string, size: string) => `${id}|${color}|${size}`;

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      addItem: (product, color, size, qty = 1, variantId) =>
        set((s) => {
          const k = key(product.id, color, size);
          const existing = s.items.find((i) => key(i.product.id, i.color, i.size) === k);
          if (existing) {
            return {
              items: s.items.map((i) =>
                key(i.product.id, i.color, i.size) === k
                  ? { ...i, qty: i.qty + qty, variantId: variantId ?? i.variantId }
                  : i
              ),
            };
          }
          return { items: [...s.items, { product, color, size, qty, variantId }] };
        }),
      removeItem: (id, color, size) =>
        set((s) => ({ items: s.items.filter((i) => key(i.product.id, i.color, i.size) !== key(id, color, size)) })),
      updateQty: (id, color, size, qty) =>
        set((s) => ({
          items:
            qty <= 0
              ? s.items.filter((i) => key(i.product.id, i.color, i.size) !== key(id, color, size))
              : s.items.map((i) =>
                  key(i.product.id, i.color, i.size) === key(id, color, size) ? { ...i, qty } : i
                ),
        })),
      clear: () => set({ items: [] }),
    }),
    { name: 'maison-cart' }
  )
);

export const useCartTotals = () => {
  const items = useCartStore((s) => s.items);
  const subtotal = items.reduce((acc, i) => acc + i.product.price * i.qty, 0);
  const count = items.reduce((acc, i) => acc + i.qty, 0);
  return { subtotal, count };
};
