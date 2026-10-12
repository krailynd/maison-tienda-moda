import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { ChevronLeft, ChevronRight, Eye } from 'lucide-react';
import {
  AdminApiError,
  fetchAdminOrders,
  type AdminOrderStatus,
  type AdminOrderSummary,
  type AdminPaymentStatus,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

const PAGE_SIZE = 20;

/** Display-only Spanish labels for backend enum values (order & payment statuses). */
const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmado',
  PROCESSING: 'En proceso',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
  REFUNDED: 'Reembolsado',
  RETURNED: 'Devuelto',
  AUTHORIZED: 'Autorizado',
  PAID: 'Pagado',
  FAILED: 'Fallido',
  PARTIALLY_REFUNDED: 'Parcialmente reembolsado',
};

function statusLabel(s: string): string {
  return STATUS_LABELS[s] ?? s;
}

const ORDER_STATUS_OPTIONS: { value: AdminOrderStatus | 'all'; label: string }[] = [
  { value: 'all', label: 'Todos los estados' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'PROCESSING', label: 'En proceso' },
  { value: 'SHIPPED', label: 'Enviado' },
  { value: 'DELIVERED', label: 'Entregado' },
  { value: 'CANCELLED', label: 'Cancelado' },
  { value: 'REFUNDED', label: 'Reembolsado' },
  { value: 'RETURNED', label: 'Devuelto' },
];

const PAYMENT_STATUS_OPTIONS: { value: AdminPaymentStatus | 'all'; label: string }[] = [
  { value: 'all', label: 'Todos los pagos' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'AUTHORIZED', label: 'Autorizado' },
  { value: 'PAID', label: 'Pagado' },
  { value: 'FAILED', label: 'Fallido' },
  { value: 'REFUNDED', label: 'Reembolsado' },
  { value: 'PARTIALLY_REFUNDED', label: 'Parcialmente reembolsado' },
];

/** Tailwind badge classes per order status, mirroring the storefront tones. */
function statusBadgeClass(status: AdminOrderStatus): string {
  switch (status) {
    case 'DELIVERED':
      return 'bg-maison-sage/20 text-maison-soft';
    case 'SHIPPED':
      return 'bg-blue-50 text-blue-700';
    case 'PROCESSING':
      return 'bg-amber-50 text-amber-700';
    case 'CONFIRMED':
      return 'bg-violet-50 text-violet-700';
    case 'CANCELLED':
    case 'REFUNDED':
      return 'bg-red-50 text-red-700';
    case 'RETURNED':
      return 'bg-orange-50 text-orange-700';
    default:
      return 'bg-maison-cream text-maison-warmgray';
  }
}

function paymentBadgeClass(status: AdminPaymentStatus): string {
  return status === 'PAID' ? 'bg-maison-sage/20 text-maison-soft' : 'bg-maison-cream text-maison-warmgray';
}

function formatCurrency(value: number | null | undefined, currency?: string | null): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('es-PE', { style: 'currency', currency: currency ?? 'PEN' }).format(value);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

/**
 * Admin order list (slice E): status / payment status / search filters and
 * pagination fed by the X-Total-Count header. Rows link to the order detail
 * page which owns the status timeline and transitions.
 */
export default function AdminOrders() {
  const navigate = useNavigate();
  const pushToast = useUIStore((s) => s.pushToast);

  const [items, setItems] = useState<AdminOrderSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [status, setStatus] = useState<AdminOrderStatus | 'all'>('all');
  const [paymentStatus, setPaymentStatus] = useState<AdminPaymentStatus | 'all'>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Debounce the search box so each keystroke does not fire a request.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  // A filter change restarts pagination.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, status, paymentStatus]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAdminOrders({
        search: debouncedSearch || undefined,
        status: status === 'all' ? undefined : status,
        paymentStatus: paymentStatus === 'all' ? undefined : paymentStatus,
        page,
        size: PAGE_SIZE,
      });
      setItems(result.items);
      setTotal(result.total);
      setTotalPages(result.totalPages);
    } catch (err) {
      const message = err instanceof AdminApiError ? err.message : 'No se pudieron cargar los pedidos.';
      setError(message);
      pushToast('error', message);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, status, paymentStatus, page, pushToast]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div>
      <div className="mb-6">
        <h1 className="font-serif text-2xl text-maison-ink">Pedidos</h1>
        <p className="mt-1 text-sm text-maison-warmgray">
          Historial completo de pedidos, incluidos los pedidos de invitados. Abre un pedido para
          gestionar su estado.
        </p>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por número de pedido o correo…"
          aria-label="Buscar pedidos"
          className="w-72 rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
        />
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as AdminOrderStatus | 'all')}
          aria-label="Filtrar por estado del pedido"
          className="rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
        >
          {ORDER_STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <select
          value={paymentStatus}
          onChange={(e) => setPaymentStatus(e.target.value as AdminPaymentStatus | 'all')}
          aria-label="Filtrar por estado del pago"
          className="rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
        >
          {PAYMENT_STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {error}
        </div>
      )}

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-maison-line bg-white shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-maison-line bg-maison-warm text-xs uppercase tracking-wide text-maison-warmgray">
                <th className="px-4 py-3 font-medium">Pedido</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Estado</th>
                <th className="px-4 py-3 font-medium">Pago</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Artículos</th>
                <th className="px-4 py-3 font-medium">Fecha</th>
                <th className="px-4 py-3 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading && items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-maison-warmgray">
                    Cargando pedidos…
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-maison-warmgray">
                    Ningún pedido coincide con los filtros actuales.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b border-maison-line/60 last:border-b-0">
                    <td className="px-4 py-3">
                      <p className="font-medium text-maison-ink">#{item.orderNumber}</p>
                      <p className="text-xs text-maison-muted">ID {item.id}</p>
                    </td>
                    <td className="px-4 py-3">
                      <p className="text-maison-ink">{item.customerName ?? 'Cliente invitado'}</p>
                      <p className="text-xs text-maison-muted">{item.customerEmail ?? '—'}</p>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${statusBadgeClass(item.status)}`}
                      >
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${paymentBadgeClass(item.paymentStatus)}`}
                      >
                        {statusLabel(item.paymentStatus)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-maison-ink">
                      {formatCurrency(item.totalAmount, item.currencyCode)}
                    </td>
                    <td className="px-4 py-3 text-maison-warmgray">{item.itemCount}</td>
                    <td className="px-4 py-3 text-maison-warmgray">{formatDate(item.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          title="Ver pedido"
                          onClick={() => navigate(`/admin/orders/${item.id}`)}
                          className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-maison-warm hover:text-maison-ink"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-maison-line bg-maison-warm px-4 py-3">
          <p className="text-xs text-maison-warmgray">
            {total} pedido{total === 1 ? '' : 's'} · página {totalPages > 0 ? page + 1 : 0} de{' '}
            {Math.max(totalPages, 1)}
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page <= 0 || loading}
              onClick={() => setPage((p) => p - 1)}
              className="flex items-center gap-1 rounded-md border border-maison-line bg-white px-3 py-1.5 text-sm text-maison-ink transition-colors hover:bg-maison-warm disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </button>
            <button
              type="button"
              disabled={page >= totalPages - 1 || loading}
              onClick={() => setPage((p) => p + 1)}
              className="flex items-center gap-1 rounded-md border border-maison-line bg-white px-3 py-1.5 text-sm text-maison-ink transition-colors hover:bg-maison-warm disabled:cursor-not-allowed disabled:opacity-50"
            >
              Siguiente
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
