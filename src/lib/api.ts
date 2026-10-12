import type { Product } from '@/lib/data';

const API_BASE = '/api/v1';

// ---------- DTOs (backend contract) ----------

export interface ProductSummaryDTO {
  id: string;
  slug: string;
  name: string;
  price: number;
  originalPrice: number | null;
  currency: string;
  image: string;
  category: string;
  categoryName: string;
  categoryId: string;
  brand?: { id: string; name: string; slug: string };
  colors: { name: string; hex: string }[];
  sizes: { label: string; available: boolean }[];
  rating: number;
  reviews: number;
  isNew: boolean;
  isSale: boolean;
  description?: string;
  details?: string[];
  shortDescription?: string;
  inStock: boolean;
}

export interface ProductImageDTO {
  id?: number;
  url: string;
  thumbnailUrl?: string;
  altText?: string;
  primary?: boolean;
  position?: number;
}

export interface VariantDTO {
  id: number;
  name: string;
  sku: string;
  price: number;
  size: { id: number; name: string; code: string };
  color: { name: string; hex: string };
  stock: number;
  inStock: boolean;
  isDefault: boolean;
  isActive: boolean;
}

export interface ProductDetailDTO extends ProductSummaryDTO {
  images: ProductImageDTO[];
  variants: VariantDTO[];
  createdAt?: string;
}

export interface CategoryDTO {
  id: string;
  slug: string;
  name: string;
  description?: string;
  imageUrl?: string | null;
  icon?: string | null;
  sortOrder?: number;
  productCount: number;
  parentId?: string | null;
  children?: CategoryDTO[];
}

export interface ProductVariant {
  id: number;
  name: string;
  sku: string;
  price: number;
  size: { id: number; name: string; code: string };
  color: { name: string; hex: string };
  stock: number;
  inStock: boolean;
  isDefault: boolean;
  isActive: boolean;
}

export interface ProductDetail extends Product {
  images?: ProductImageDTO[];
  variants: ProductVariant[];
  shortDescription?: string;
  createdAt?: string;
}

export interface CheckoutItemPayload {
  productId: number;
  variantId: number;
  quantity: number;
}

export interface CheckoutCustomerPayload {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  region?: string;
  postalCode?: string;
  countryCode: string;
}

export interface CreateOrderPayload {
  items: CheckoutItemPayload[];
  customer: CheckoutCustomerPayload;
  shippingMethodId: number | null;
  notes: string;
}

export interface OrderItemResponse {
  productId?: number | string;
  productName: string;
  variantName?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  imageUrl?: string;
  color?: string;
  size?: string;
}

export interface OrderResponse {
  id?: number;
  orderNumber: string;
  status: string;
  paymentStatus?: string;
  currency?: string;
  subtotal?: number;
  discount?: number;
  shippingCost?: number;
  tax?: number;
  total?: number;
  totalAmount?: number;
  items?: OrderItemResponse[];
  createdAt?: string;
  paidAt?: string | null;
  customer?: CheckoutCustomerPayload;
  shippingAmount?: number;
  shippingMethod?: string;
  payment?: {
    provider?: string;
    method?: string;
    transactionId?: string;
    amount?: number;
    currency?: string;
    status?: string;
    processedAt?: string;
  };
}

export interface PayOrderResponse {
  orderNumber: string;
  status: string;
  paidAt?: string;
  transactionId?: string;
}

// ---------- Errors ----------

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

const STATUS_MESSAGES: Record<number, string> = {
  400: 'La solicitud es inválida.',
  401: 'No autorizado.',
  403: 'No tienes permisos para esta acción.',
  404: 'No se encontró el recurso solicitado.',
  409: 'El recurso ya existe o hay un conflicto.',
  422: 'No se pudo procesar la solicitud. Revisa los datos.',
  500: 'Error interno del servidor. Inténtalo más tarde.',
};

function messageForStatus(status: number): string {
  return STATUS_MESSAGES[status] ?? 'Ocurrió un error inesperado.';
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Ocurrió un error inesperado.';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json', ...init.headers } : init?.headers,
    });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifica tu conexión e inténtalo de nuevo.');
  }

  if (!res.ok) {
    let detail = '';
    try {
      const body = (await res.json()) as { message?: string; detail?: string; title?: string };
      detail = body?.message ?? body?.detail ?? body?.title ?? '';
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, detail || messageForStatus(res.status));
  }

  return (await res.json()) as T;
}

// ---------- Transformers ----------

export function toProduct(dto: ProductSummaryDTO): Product {
  return {
    id: dto.id,
    slug: dto.slug,
    name: dto.name,
    price: dto.price,
    originalPrice: dto.originalPrice ?? undefined,
    category: dto.category as Product['category'],
    image: dto.image,
    colors: dto.colors,
    sizes: dto.sizes,
    rating: dto.rating,
    reviews: dto.reviews,
    isNew: dto.isNew,
    isSale: dto.isSale,
    description: dto.description ?? '',
    details: dto.details ?? [],
    inStock: dto.inStock,
    shortDescription: dto.shortDescription,
    categoryId: dto.categoryId,
    categoryName: dto.categoryName,
    brand: dto.brand,
  };
}

export function toProductDetail(dto: ProductDetailDTO): ProductDetail {
  return {
    ...toProduct(dto),
    images: dto.images,
    variants: dto.variants,
    shortDescription: dto.shortDescription,
    createdAt: dto.createdAt,
  };
}

export interface Category {
  slug: string;
  name: string;
  title: string;
  image: string;
  tagline: string;
  productCount: number;
}

const CATEGORY_FALLBACKS: Record<string, { image: string; tagline: string }> = {
  mujer: { image: '/images/cat-mujer.png', tagline: 'Siluetas que abrazan' },
  hombre: { image: '/images/cat-hombre.png', tagline: 'Sastrería contemporánea' },
  accesorios: { image: '/images/cat-accesorios.png', tagline: 'Los detalles importan' },
};

export function toCategory(dto: CategoryDTO): Category {
  const fallback = CATEGORY_FALLBACKS[dto.slug];
  return {
    slug: dto.slug,
    name: dto.name,
    title: dto.name,
    image: fallback?.image ?? dto.imageUrl ?? '/images/hero.png',
    tagline: fallback?.tagline ?? '',
    productCount: dto.productCount,
  };
}

// ---------- Endpoints ----------

export async function fetchProducts(size = 20): Promise<{ products: Product[]; total: number }> {
  const data = await request<{
    content: ProductSummaryDTO[];
    totalElements: number;
  }>(`/products?size=${size}`);
  return { products: data.content.map(toProduct), total: data.totalElements };
}

export async function fetchProductBySlug(slug: string): Promise<ProductDetail> {
  const dto = await request<ProductDetailDTO>(`/products/${encodeURIComponent(slug)}`);
  return toProductDetail(dto);
}

export async function fetchCategories(): Promise<CategoryDTO[]> {
  return request<CategoryDTO[]>('/categories');
}

export async function createOrder(payload: CreateOrderPayload): Promise<OrderResponse> {
  return request<OrderResponse>('/checkout/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function payOrder(orderNumber: string, method = 'CARD'): Promise<PayOrderResponse> {
  return request<PayOrderResponse>(`/checkout/orders/${encodeURIComponent(orderNumber)}/pay`, {
    method: 'POST',
    body: JSON.stringify({ method }),
  });
}

export async function fetchOrder(orderNumber: string): Promise<OrderResponse> {
  return request<OrderResponse>(`/orders/${encodeURIComponent(orderNumber)}`);
}

// ---------- Variants & checkout helpers ----------

export function resolveVariant(
  detail: ProductDetail,
  color: string,
  size: string
): ProductVariant | undefined {
  if (!detail.variants?.length) return undefined;
  const exact = detail.variants.find(
    (v) => v.isActive && v.size.code === size && v.color.name === color
  );
  if (exact) return exact;
  const sameColor = detail.variants.find((v) => v.isActive && v.color.name === color);
  if (sameColor) return sameColor;
  return (
    detail.variants.find((v) => v.isActive && v.isDefault) ??
    detail.variants.find((v) => v.isActive)
  );
}

export interface ResolvableCartItem {
  product: Product;
  color: string;
  size: string;
  qty: number;
  variantId?: number;
}

export interface ResolvedCheckoutItem {
  productId: number;
  variantId: number;
  qty: number;
}

export async function resolveCheckoutItems(items: ResolvableCartItem[]): Promise<ResolvedCheckoutItem[]> {
  const resolved: ResolvedCheckoutItem[] = [];
  for (const item of items) {
    const productId = Number(item.product.id);
    let variantId = item.variantId;
    if (!variantId) {
      // Legacy items persisted before variantId existed: resolve via detail fallback.
      try {
        const detail = await fetchProductBySlug(item.product.slug);
        variantId = resolveVariant(detail, item.color, item.size)?.id;
      } catch {
        variantId = undefined;
      }
    }
    if (!variantId) {
      throw new ApiError(
        0,
        `No se pudo determinar la variante de "${item.product.name}". Vuelve a agregar el producto al carrito.`
      );
    }
    resolved.push({ productId, variantId, qty: item.qty });
  }
  return resolved;
}
