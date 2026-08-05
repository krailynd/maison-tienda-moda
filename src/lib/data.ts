export interface Product {
  id: string;
  slug: string;
  name: string;
  price: number;
  originalPrice?: number;
  category: 'mujer' | 'hombre' | 'accesorios';
  image: string;
  colors: { name: string; hex: string }[];
  sizes: { label: string; available: boolean }[];
  rating: number;
  reviews: number;
  isNew?: boolean;
  isSale?: boolean;
  description: string;
  details: string[];
}

export const PRODUCTS: Product[] = [
  {
    id: '1', slug: 'blusa-seda-marfil', name: 'Blusa de Seda Marfil',
    price: 289, category: 'mujer', image: '/images/p1.png',
    colors: [{ name: 'Marfil', hex: '#F5F0EB' }, { name: 'Champagne', hex: '#E8D4C0' }, { name: 'Negro', hex: '#1C1C1E' }],
    sizes: [{ label: 'XS', available: true }, { label: 'S', available: true }, { label: 'M', available: true }, { label: 'L', available: false }],
    rating: 4.8, reviews: 128, isNew: true,
    description: 'Blusa confeccionada en seda pura de 22 momme con botones de nácar genuino. Corte fluido que cae con elegancia natural, ideal para transiciones del día a la noche.',
    details: ['100% seda de morera', 'Botones de nácar', 'Lavado en seco', 'Hecha en Italia'],
  },
  {
    id: '2', slug: 'abrigo-lana-camel', name: 'Abrigo Oversize Camel',
    price: 890, originalPrice: 1090, category: 'mujer', image: '/images/p2.png',
    colors: [{ name: 'Camel', hex: '#C4956A' }, { name: 'Gris', hex: '#9E9E9E' }],
    sizes: [{ label: 'S', available: true }, { label: 'M', available: true }, { label: 'L', available: true }],
    rating: 4.9, reviews: 86, isSale: true,
    description: 'Abrigo oversize en lana virgen de doble faz, sin forro, con acabados cosidos a mano. Silueta envolvente con hombros caídos y cuello sastre.',
    details: ['100% lana virgen', 'Doble faz sin forro', 'Bolsillos de ribete', 'Limpieza profesional'],
  },
  {
    id: '3', slug: 'pantalon-sastre-negro', name: 'Pantalón Sastre Negro',
    price: 340, category: 'hombre', image: '/images/p3.png',
    colors: [{ name: 'Negro', hex: '#1C1C1E' }, { name: 'Carbón', hex: '#3A3A3C' }],
    sizes: [{ label: '46', available: true }, { label: '48', available: true }, { label: '50', available: false }, { label: '52', available: true }],
    rating: 4.7, reviews: 64,
    description: 'Pantalón de sastrería de talle alto con pinzas y caída recta. Tejido de lana fría con elasticidad natural para máximo confort.',
    details: ['Lana fría Super 120s', 'Talle alto con pinzas', 'Dobladillo sin terminar', 'Hecho en Portugal'],
  },
  {
    id: '4', slug: 'sueter-cachemir-rosa', name: 'Suéter de Cachemir Rosa',
    price: 420, category: 'mujer', image: '/images/p4.png',
    colors: [{ name: 'Rosa empolvado', hex: '#C9A9A6' }, { name: 'Crema', hex: '#F5F0EB' }, { name: 'Salvia', hex: '#A8B5A0' }],
    sizes: [{ label: 'XS', available: true }, { label: 'S', available: true }, { label: 'M', available: true }, { label: 'L', available: true }],
    rating: 4.9, reviews: 212, isNew: true,
    description: 'Suéter de cachemir de grado A, hilado en 12 gauge para una calidez ligera. Tono rosa empolvado teñido con pigmentos naturales.',
    details: ['100% cachemir grado A', 'Teñido natural', 'Cuello redondo', 'Lavar a mano en frío'],
  },
  {
    id: '5', slug: 'vestido-slip-champagne', name: 'Vestido Slip Champagne',
    price: 510, category: 'mujer', image: '/images/p5.png',
    colors: [{ name: 'Champagne', hex: '#E8D4C0' }, { name: 'Negro', hex: '#1C1C1E' }],
    sizes: [{ label: 'XS', available: false }, { label: 'S', available: true }, { label: 'M', available: true }, { label: 'L', available: true }],
    rating: 4.6, reviews: 93,
    description: 'Vestido slip cortado al bies en satén de seda con tirantes ajustables. Un esencial atemporal que fluye con el movimiento.',
    details: ['Satén 100% seda', 'Corte al bies', 'Tirantes ajustables', 'Lavado en seco'],
  },
  {
    id: '6', slug: 'bolso-cuero-cognac', name: 'Bolso de Cuero Cognac',
    price: 1250, category: 'accesorios', image: '/images/p6.png',
    colors: [{ name: 'Cognac', hex: '#8B5A2B' }, { name: 'Negro', hex: '#1C1C1E' }],
    sizes: [{ label: 'Único', available: true }],
    rating: 5.0, reviews: 47, isNew: true,
    description: 'Bolso estructurado en cuero de curtido vegetal con herrajes de latón dorado. Envejece con una pátina única que cuenta tu historia.',
    details: ['Cuero de curtido vegetal', 'Herrajes de latón', 'Forro de algodón', 'Hecho a mano en España'],
  },
  {
    id: '7', slug: 'sneakers-cuero-blanco', name: 'Sneakers de Cuero Blanco',
    price: 380, category: 'hombre', image: '/images/p7.png',
    colors: [{ name: 'Blanco', hex: '#FFFFFF' }, { name: 'Crema', hex: '#F5F0EB' }],
    sizes: [{ label: '40', available: true }, { label: '41', available: true }, { label: '42', available: true }, { label: '43', available: false }, { label: '44', available: true }],
    rating: 4.8, reviews: 156,
    description: 'Sneakers minimalistas en cuero napa con suela de goma natural. Silueta limpia sin logos, el básico definitivo del guardarropa.',
    details: ['Cuero napa', 'Suela de goma natural', 'Plantilla extraíble', 'Hecho en Portugal'],
  },
  {
    id: '8', slug: 'blazer-lino-salvia', name: 'Blazer de Lino Salvia',
    price: 620, originalPrice: 740, category: 'hombre', image: '/images/p8.png',
    colors: [{ name: 'Salvia', hex: '#A8B5A0' }, { name: 'Arena', hex: '#D4B896' }],
    sizes: [{ label: '48', available: true }, { label: '50', available: true }, { label: '52', available: true }],
    rating: 4.7, reviews: 71, isSale: true,
    description: 'Blazer desestructurado en lino belga lavado, hombro natural y construcción ligera. Perfecto para climas cálidos con espíritu sartorial.',
    details: ['100% lino belga', 'Desestructurado', 'Hombro natural', 'Botones de cuerno'],
  },
];

export const CATEGORIES = [
  { slug: 'mujer', title: 'Mujer', image: '/images/cat-mujer.png', tagline: 'Siluetas que abrazan' },
  { slug: 'hombre', title: 'Hombre', image: '/images/cat-hombre.png', tagline: 'Sastrería contemporánea' },
  { slug: 'accesorios', title: 'Accesorios', image: '/images/cat-accesorios.png', tagline: 'Los detalles importan' },
];

export const FREE_SHIPPING_THRESHOLD = 500;

export const formatPrice = (n: number) =>
  `S/ ${n.toLocaleString('es-PE', { minimumFractionDigits: 2 })}`;
