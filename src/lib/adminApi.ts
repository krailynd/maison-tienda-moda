import { useAuthStore, type AdminUser } from '@/stores/authStore';

const API_BASE = '/api/v1';

/**
 * Multipart bodies must NOT get an explicit Content-Type: the browser sets the
 * boundary header itself. Only JSON bodies default to application/json.
 */
function shouldSetJsonContentType(init?: RequestInit): boolean {
  return init?.body != null && !(init.body instanceof FormData);
}

// ---------- DTOs (backend contract) ----------

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AdminUser;
}

interface EnvelopeError {
  error?: { code?: string; message?: string; details?: Record<string, string> };
}

// ---------- Errors ----------

export class AdminApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: Record<string, string>;

  constructor(status: number, code: string, message: string, details?: Record<string, string>) {
    super(message);
    this.name = 'AdminApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

async function parseError(res: Response): Promise<AdminApiError> {
  let code = '';
  let message = '';
  let details: Record<string, string> | undefined;
  try {
    const body = (await res.json()) as EnvelopeError;
    code = body.error?.code ?? '';
    message = body.error?.message ?? '';
    details = body.error?.details;
  } catch {
    // non-JSON error body
  }
  return new AdminApiError(
    res.status,
    code || String(res.status),
    message || `La solicitud falló con el estado ${res.status}`,
    details
  );
}

// ---------- Plain (no-auth) requests: login / logout ----------

async function plainRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (shouldSetJsonContentType(init) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

// ---------- Session helpers ----------

let refreshInFlight: Promise<string | null> | null = null;

async function doRefresh(refreshToken: string): Promise<string | null> {
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return null;
    const data = (await res.json()) as AuthResponse;
    useAuthStore.getState().setSession(data.accessToken, data.refreshToken, data.user);
    return data.accessToken;
  } catch {
    return null;
  }
}

/**
 * Single-flight refresh: concurrent 401s share one refresh call instead of
 * hammering the endpoint.
 */
function refreshAccessToken(): Promise<string | null> {
  const { refreshToken } = useAuthStore.getState();
  if (!refreshToken) return Promise.resolve(null);
  if (!refreshInFlight) {
    refreshInFlight = doRefresh(refreshToken).finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

function redirectToAdminLogin(): void {
  useAuthStore.getState().clearSession();
  if (!window.location.hash.startsWith('#/admin/login')) {
    window.location.hash = '#/admin/login';
  }
}

// ---------- Authenticated admin requests (with one-shot refresh) ----------

/**
 * Fetch wrapper for admin APIs. Adds the Bearer header, parses the
 * {@code {error:{code,message,details}}} envelope, and on a 401 retries the
 * request ONCE after refreshing the token. If the refresh fails, the session
 * is cleared and the user is redirected to /admin/login.
 */
export async function adminRequest<T>(path: string, init?: RequestInit, retried = false): Promise<T> {
  const { accessToken } = useAuthStore.getState();
  const headers = new Headers(init?.headers);
  if (shouldSetJsonContentType(init) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (res.status === 401 && !retried) {
    const newToken = await refreshAccessToken();
    if (newToken) return adminRequest<T>(path, init, true);
    redirectToAdminLogin();
    throw new AdminApiError(401, 'UNAUTHORIZED', 'La sesión expiró. Inicia sesión nuevamente.');
  }

  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export interface AdminResponseMeta<T> {
  data: T;
  /** Value of the X-Total-Count header, or null when absent. */
  totalCount: number | null;
}

/**
 * Same as {@link adminRequest} but also exposes response headers (used for the
 * X-Total-Count contract on admin list endpoints). Retries once on 401.
 */
export async function adminRequestWithMeta<T>(
  path: string,
  init?: RequestInit,
  retried = false
): Promise<AdminResponseMeta<T>> {
  const { accessToken } = useAuthStore.getState();
  const headers = new Headers(init?.headers);
  if (shouldSetJsonContentType(init) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (res.status === 401 && !retried) {
    const newToken = await refreshAccessToken();
    if (newToken) return adminRequestWithMeta<T>(path, init, true);
    redirectToAdminLogin();
    throw new AdminApiError(401, 'UNAUTHORIZED', 'La sesión expiró. Inicia sesión nuevamente.');
  }

  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return { data: undefined as T, totalCount: null };
  const totalHeader = res.headers.get('X-Total-Count');
  const data = (await res.json()) as T;
  return { data, totalCount: totalHeader != null ? Number(totalHeader) : null };
}

// ---------- Auth endpoints ----------

export async function loginAdmin(email: string, password: string): Promise<AuthResponse> {
  const data = await plainRequest<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  useAuthStore.getState().setSession(data.accessToken, data.refreshToken, data.user);
  return data;
}

export async function registerCustomer(
  firstName: string,
  lastName: string,
  email: string,
  password: string
): Promise<AuthResponse> {
  const data = await plainRequest<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ firstName, lastName, email, password }),
  });
  useAuthStore.getState().setSession(data.accessToken, data.refreshToken, data.user);
  return data;
}

export async function logoutAdmin(): Promise<void> {
  const { refreshToken } = useAuthStore.getState();
  if (refreshToken) {
    try {
      await plainRequest<void>('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      });
    } catch {
      // Best-effort server revocation; the local session is cleared regardless.
    }
  }
  useAuthStore.getState().clearSession();
}

// ---------- Admin catalog DTOs (backend contract, slice B) ----------

export interface AdminRef {
  id: string;
  name: string;
  slug: string;
}

export interface AdminProductSummary {
  id: number;
  slug: string;
  name: string;
  price: number;
  comparePrice: number | null;
  isActive: boolean;
  isFeatured: boolean;
  isNew: boolean;
  categoryId: number | null;
  categoryName: string | null;
  brandId: number | null;
  brandName: string | null;
  totalStock: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminProductDetail extends AdminProductSummary {
  description: string | null;
  shortDescription: string | null;
  skuPrefix: string | null;
  costPrice: number | null;
  taxRate: number | null;
  weight: number | null;
  lengthCm: number | null;
  widthCm: number | null;
  heightCm: number | null;
  isEcoFriendly: boolean;
  stockTracking: boolean;
  lowStockThreshold: number | null;
  minOrderQuantity: number | null;
  maxOrderQuantity: number | null;
  averageRating: number | null;
  reviewCount: number | null;
  viewCount: number | null;
  purchaseCount: number | null;
  seoTitle: string | null;
  seoDescription: string | null;
  metaKeywords: string | null;
  category: AdminRef | null;
  brand: AdminRef | null;
  images: { id: number; url: string; thumbnailUrl: string | null; altText: string | null; primary: boolean; position: number | null }[];
  variants: AdminVariant[];
}

/**
 * Write payload for POST/PUT /api/v1/admin/products. Backend ignores
 * isActive here — products are always created as drafts.
 */
export interface AdminProductPayload {
  name: string;
  slug: string;
  description?: string | null;
  shortDescription?: string | null;
  skuPrefix?: string | null;
  brandId?: number | null;
  categoryId?: number | null;
  price: number;
  comparePrice?: number | null;
  costPrice?: number | null;
  taxRate?: number | null;
  weight?: number | null;
  lengthCm?: number | null;
  widthCm?: number | null;
  heightCm?: number | null;
  isFeatured?: boolean;
  isNew?: boolean;
  isEcoFriendly?: boolean;
  stockTracking?: boolean;
  lowStockThreshold?: number | null;
  minOrderQuantity?: number | null;
  maxOrderQuantity?: number | null;
  seoTitle?: string | null;
  seoDescription?: string | null;
  metaKeywords?: string | null;
}

export interface AdminPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type AdminProductStatus = 'all' | 'active' | 'inactive';

export interface AdminProductQuery {
  search?: string;
  status?: AdminProductStatus;
  categoryId?: number;
  brandId?: number;
  page?: number;
  size?: number;
}

export interface AdminProductListResult {
  items: AdminProductSummary[];
  page: number;
  size: number;
  /** From the X-Total-Count header; falls back to the body totalElements. */
  total: number;
  totalPages: number;
}

// ---------- Admin catalog endpoints ----------

export async function fetchAdminProducts(query: AdminProductQuery = {}): Promise<AdminProductListResult> {
  const params = new URLSearchParams();
  if (query.search) params.set('search', query.search);
  if (query.status && query.status !== 'all') params.set('status', query.status);
  if (query.categoryId != null) params.set('categoryId', String(query.categoryId));
  if (query.brandId != null) params.set('brandId', String(query.brandId));
  params.set('page', String(query.page ?? 0));
  params.set('size', String(query.size ?? 20));
  const { data, totalCount } = await adminRequestWithMeta<AdminPage<AdminProductSummary>>(
    `/admin/products?${params.toString()}`
  );
  return {
    items: data.content,
    page: data.page,
    size: data.size,
    total: totalCount ?? data.totalElements,
    totalPages: data.totalPages,
  };
}

export async function fetchAdminProduct(id: number): Promise<AdminProductDetail> {
  return adminRequest<AdminProductDetail>(`/admin/products/${id}`);
}

export async function createAdminProduct(payload: AdminProductPayload): Promise<AdminProductDetail> {
  return adminRequest<AdminProductDetail>('/admin/products', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminProduct(id: number, payload: AdminProductPayload): Promise<AdminProductDetail> {
  return adminRequest<AdminProductDetail>(`/admin/products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminProduct(id: number): Promise<void> {
  return adminRequest<void>(`/admin/products/${id}`, { method: 'DELETE' });
}

export async function publishAdminProduct(id: number): Promise<AdminProductDetail> {
  return adminRequest<AdminProductDetail>(`/admin/products/${id}/publish`, { method: 'POST' });
}

export async function unpublishAdminProduct(id: number): Promise<AdminProductDetail> {
  return adminRequest<AdminProductDetail>(`/admin/products/${id}/unpublish`, { method: 'POST' });
}

// ---------- Admin variants (slice C) ----------

export interface AdminVariant {
  id: number;
  name: string;
  sku: string;
  price: number | null;
  comparePrice: number | null;
  costPrice: number | null;
  size: { id: number; name: string; code: string } | null;
  color: { id: number; name: string; hexCode: string } | null;
  stock: number;
  inStock: boolean;
  isDefault: boolean;
  isActive: boolean;
  position: number | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Write payload for POST/PUT admin variant endpoints. {@code stock} maps to
 * the quantity on hand in the variant's default-warehouse inventory row.
 */
export interface AdminVariantPayload {
  name: string;
  sku: string;
  sizeId?: number | null;
  colorId?: number | null;
  price?: number | null;
  comparePrice?: number | null;
  costPrice?: number | null;
  isDefault?: boolean;
  isActive?: boolean;
  position?: number | null;
  stock?: number | null;
}

export interface AdminSizeOption {
  id: number;
  name: string;
  code: string;
}

export interface AdminColorOption {
  id: number;
  name: string;
  hexCode: string;
}

export async function fetchAdminSizes(): Promise<AdminSizeOption[]> {
  return adminRequest<AdminSizeOption[]>('/admin/sizes');
}

export async function fetchAdminColors(): Promise<AdminColorOption[]> {
  return adminRequest<AdminColorOption[]>('/admin/colors');
}

export async function createAdminVariant(productId: number, payload: AdminVariantPayload): Promise<AdminVariant> {
  return adminRequest<AdminVariant>(`/admin/products/${productId}/variants`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminVariant(variantId: number, payload: AdminVariantPayload): Promise<AdminVariant> {
  return adminRequest<AdminVariant>(`/admin/variants/${variantId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminVariant(variantId: number): Promise<void> {
  return adminRequest<void>(`/admin/variants/${variantId}`, { method: 'DELETE' });
}

// ---------- Admin images (slice C) ----------

export interface AdminImage {
  id: number;
  imageUrl: string;
  thumbnailUrl: string | null;
  altText: string | null;
  position: number | null;
  isPrimary: boolean;
}

export async function uploadAdminImage(
  productId: number,
  file: File,
  opts?: { altText?: string; isPrimary?: boolean }
): Promise<AdminImage> {
  const form = new FormData();
  form.append('file', file);
  if (opts?.altText) form.append('altText', opts.altText);
  if (opts?.isPrimary) form.append('isPrimary', String(opts.isPrimary));
  return adminRequest<AdminImage>(`/admin/products/${productId}/images`, {
    method: 'POST',
    body: form,
  });
}

export async function setPrimaryAdminImage(imageId: number): Promise<AdminImage> {
  return adminRequest<AdminImage>(`/admin/images/${imageId}/primary`, { method: 'POST' });
}

export async function deleteAdminImage(imageId: number): Promise<void> {
  return adminRequest<void>(`/admin/images/${imageId}`, { method: 'DELETE' });
}

// ---------- Admin reference catalog: categories / brands / collections (slice D) ----------

export interface AdminListQuery {
  page?: number;
  size?: number;
}

export interface AdminListResult<T> {
  items: T[];
  page: number;
  size: number;
  /** From the X-Total-Count header; falls back to the body totalElements. */
  total: number;
  totalPages: number;
}

async function fetchAdminList<T>(path: string, query: AdminListQuery = {}): Promise<AdminListResult<T>> {
  const params = new URLSearchParams();
  params.set('page', String(query.page ?? 0));
  params.set('size', String(query.size ?? 20));
  const { data, totalCount } = await adminRequestWithMeta<AdminPage<T>>(`${path}?${params.toString()}`);
  return {
    items: data.content,
    page: data.page,
    size: data.size,
    total: totalCount ?? data.totalElements,
    totalPages: data.totalPages,
  };
}

// ---------- Admin categories (slice D) ----------

export interface AdminCategory {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  parentId: number | null;
  parentName: string | null;
  imageUrl: string | null;
  isActive: boolean;
  iconClass: string | null;
  displayOrder: number | null;
  seoTitle: string | null;
  seoDescription: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCategoryPayload {
  name: string;
  slug: string;
  description?: string | null;
  parentId?: number | null;
  imageUrl?: string | null;
  isActive?: boolean;
  iconClass?: string | null;
  displayOrder?: number | null;
  seoTitle?: string | null;
  seoDescription?: string | null;
}

export async function fetchAdminCategories(query: AdminListQuery = {}): Promise<AdminListResult<AdminCategory>> {
  return fetchAdminList<AdminCategory>('/admin/categories', query);
}

export async function fetchAdminCategory(id: number): Promise<AdminCategory> {
  return adminRequest<AdminCategory>(`/admin/categories/${id}`);
}

export async function createAdminCategory(payload: AdminCategoryPayload): Promise<AdminCategory> {
  return adminRequest<AdminCategory>('/admin/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminCategory(id: number, payload: AdminCategoryPayload): Promise<AdminCategory> {
  return adminRequest<AdminCategory>(`/admin/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminCategory(id: number): Promise<void> {
  return adminRequest<void>(`/admin/categories/${id}`, { method: 'DELETE' });
}

// ---------- Admin brands (slice D) ----------

export interface AdminBrand {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  websiteUrl: string | null;
  isActive: boolean;
  isFeatured: boolean;
  displayOrder: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminBrandPayload {
  name: string;
  slug: string;
  description?: string | null;
  logoUrl?: string | null;
  websiteUrl?: string | null;
  isActive?: boolean;
  isFeatured?: boolean;
  displayOrder?: number | null;
}

export async function fetchAdminBrands(query: AdminListQuery = {}): Promise<AdminListResult<AdminBrand>> {
  return fetchAdminList<AdminBrand>('/admin/brands', query);
}

export async function fetchAdminBrand(id: number): Promise<AdminBrand> {
  return adminRequest<AdminBrand>(`/admin/brands/${id}`);
}

export async function createAdminBrand(payload: AdminBrandPayload): Promise<AdminBrand> {
  return adminRequest<AdminBrand>('/admin/brands', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminBrand(id: number, payload: AdminBrandPayload): Promise<AdminBrand> {
  return adminRequest<AdminBrand>(`/admin/brands/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminBrand(id: number): Promise<void> {
  return adminRequest<void>(`/admin/brands/${id}`, { method: 'DELETE' });
}

// ---------- Admin collections (slice D) ----------

export interface AdminCollection {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  bannerUrl: string | null;
  seasonId: number | null;
  seasonName: string | null;
  isFeatured: boolean;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminCollectionPayload {
  name: string;
  slug: string;
  description?: string | null;
  bannerUrl?: string | null;
  seasonId?: number | null;
  isFeatured?: boolean;
  startDate?: string | null;
  endDate?: string | null;
}

export async function fetchAdminCollections(query: AdminListQuery = {}): Promise<AdminListResult<AdminCollection>> {
  return fetchAdminList<AdminCollection>('/admin/collections', query);
}

export async function fetchAdminCollection(id: number): Promise<AdminCollection> {
  return adminRequest<AdminCollection>(`/admin/collections/${id}`);
}

export async function createAdminCollection(payload: AdminCollectionPayload): Promise<AdminCollection> {
  return adminRequest<AdminCollection>('/admin/collections', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminCollection(id: number, payload: AdminCollectionPayload): Promise<AdminCollection> {
  return adminRequest<AdminCollection>(`/admin/collections/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteAdminCollection(id: number): Promise<void> {
  return adminRequest<void>(`/admin/collections/${id}`, { method: 'DELETE' });
}

// ---------- Admin seasons (read-only reference for the collection editor) ----------

export interface AdminSeasonOption {
  id: number;
  name: string;
  code: string;
  isActive: boolean;
}

export async function fetchAdminSeasons(): Promise<AdminSeasonOption[]> {
  return adminRequest<AdminSeasonOption[]>('/admin/seasons');
}

// ---------- Reference options (admin endpoints, used by the product form) ----------

export interface AdminCategoryOption {
  id: string;
  name: string;
  slug: string;
}

export interface AdminBrandOption {
  id: string;
  name: string;
  slug: string;
}

/**
 * Category options for the product form. Uses the ADMIN list endpoint (not the
 * storefront one) so inactive categories are included for assignment; capped at
 * 100 rows, which covers the catalog's realistic tree depth.
 */
export async function fetchAdminCategoryOptions(): Promise<AdminCategoryOption[]> {
  const { items } = await fetchAdminCategories({ page: 0, size: 100 });
  return items.map((c) => ({ id: String(c.id), name: c.name, slug: c.slug }));
}

/**
 * Brand options for the product form, from the ADMIN list endpoint so inactive
 * brands are included; capped at 100 rows.
 */
export async function fetchAdminBrandOptions(): Promise<AdminBrandOption[]> {
  const { items } = await fetchAdminBrands({ page: 0, size: 100 });
  return items.map((b) => ({ id: String(b.id), name: b.name, slug: b.slug }));
}

// ---------- Admin orders (slice E) ----------

export type AdminOrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'RETURNED';

export type AdminPaymentStatus =
  | 'PENDING'
  | 'AUTHORIZED'
  | 'PAID'
  | 'FAILED'
  | 'REFUNDED'
  | 'PARTIALLY_REFUNDED';

export interface AdminOrderSummary {
  id: number;
  orderNumber: string;
  status: AdminOrderStatus;
  paymentStatus: AdminPaymentStatus;
  customerEmail: string | null;
  customerName: string | null;
  totalAmount: number;
  currencyCode: string | null;
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminOrderCustomer {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  region: string | null;
  postalCode: string | null;
  countryCode: string | null;
}

export interface AdminOrderItem {
  id: number;
  variantId: number | null;
  productName: string | null;
  variantName: string | null;
  sku: string | null;
  quantity: number;
  unitPrice: number | null;
  discountAmount: number | null;
  taxAmount: number | null;
  totalPrice: number | null;
  fulfillmentStatus: string | null;
  shippedQuantity: number | null;
  returnedQuantity: number | null;
}

export interface AdminOrderPayment {
  provider: string | null;
  method: string | null;
  transactionId: string | null;
  amount: number | null;
  currency: string | null;
  status: string | null;
  processedAt: string | null;
}

export interface AdminOrderHistoryEntry {
  id: number;
  oldStatus: string | null;
  newStatus: string;
  reason: string | null;
  changedAt: string | null;
}

export interface AdminOrderDetail {
  id: number;
  orderNumber: string;
  status: AdminOrderStatus;
  paymentStatus: AdminPaymentStatus;
  userEmail: string | null;
  customerName: string | null;
  customer: AdminOrderCustomer | null;
  billingAddress: AdminOrderCustomer | null;
  shippingAddress: AdminOrderCustomer | null;
  subTotal: number | null;
  discountAmount: number | null;
  shippingCost: number | null;
  taxAmount: number | null;
  totalAmount: number | null;
  currencyCode: string | null;
  shippingMethod: string | null;
  trackingNumber: string | null;
  carrier: string | null;
  paymentMethod: string | null;
  paymentIntentId: string | null;
  customerNotes: string | null;
  internalNotes: string | null;
  couponCode: string | null;
  couponDiscount: number | null;
  refundAmount: number | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  items: AdminOrderItem[];
  payments: AdminOrderPayment[];
  statusHistory: AdminOrderHistoryEntry[];
  allowedTransitions: string[];
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AdminOrderQuery {
  search?: string;
  status?: AdminOrderStatus;
  paymentStatus?: AdminPaymentStatus;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export interface AdminOrderStatusPayload {
  newStatus: AdminOrderStatus;
  notes?: string | null;
  refundAmount?: number | null;
}

export async function fetchAdminOrders(query: AdminOrderQuery = {}): Promise<AdminListResult<AdminOrderSummary>> {
  const params = new URLSearchParams();
  if (query.search) params.set('search', query.search);
  if (query.status) params.set('status', query.status);
  if (query.paymentStatus) params.set('paymentStatus', query.paymentStatus);
  if (query.dateFrom) params.set('dateFrom', query.dateFrom);
  if (query.dateTo) params.set('dateTo', query.dateTo);
  params.set('page', String(query.page ?? 0));
  params.set('size', String(query.size ?? 20));
  const { data, totalCount } = await adminRequestWithMeta<AdminPage<AdminOrderSummary>>(
    `/admin/orders?${params.toString()}`
  );
  return {
    items: data.content,
    page: data.page,
    size: data.size,
    total: totalCount ?? data.totalElements,
    totalPages: data.totalPages,
  };
}

export async function fetchAdminOrder(id: number): Promise<AdminOrderDetail> {
  return adminRequest<AdminOrderDetail>(`/admin/orders/${id}`);
}

export async function updateAdminOrderStatus(
  id: number,
  payload: AdminOrderStatusPayload
): Promise<AdminOrderDetail> {
  return adminRequest<AdminOrderDetail>(`/admin/orders/${id}/status`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
