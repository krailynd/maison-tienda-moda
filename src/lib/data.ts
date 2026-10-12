export type ProductCategory = 'mujer' | 'hombre' | 'accesorios';

export interface Product {
  id: string;
  slug: string;
  name: string;
  price: number;
  originalPrice?: number;
  category: ProductCategory;
  image: string;
  colors: { name: string; hex: string }[];
  sizes: { label: string; available: boolean }[];
  rating: number;
  reviews: number;
  isNew?: boolean;
  isSale?: boolean;
  description: string;
  details: string[];
  shortDescription?: string;
  inStock?: boolean;
  categoryId?: string;
  categoryName?: string;
  brand?: { id: string; name: string; slug: string };
}

// Products are loaded from the REST API (see src/lib/api.ts). Pages use the
// useProducts / useProduct / useCategories hooks instead of a static array.
export const CATEGORIES = [
  { slug: 'mujer', title: 'Mujer', image: '/images/cat-mujer.png', tagline: 'Siluetas que abrazan' },
  { slug: 'hombre', title: 'Hombre', image: '/images/cat-hombre.png', tagline: 'Sastrería contemporánea' },
  { slug: 'accesorios', title: 'Accesorios', image: '/images/cat-accesorios.png', tagline: 'Los detalles importan' },
];

export const FREE_SHIPPING_THRESHOLD = 500;

export const formatPrice = (n: number) =>
  `S/ ${n.toLocaleString('es-PE', { minimumFractionDigits: 2 })}`;
