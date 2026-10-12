import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { ArrowLeft, Package, RefreshCw } from 'lucide-react';
import {
  AdminApiError,
  fetchAdminOrder,
  updateAdminOrderStatus,
  type AdminOrderCustomer,
  type AdminOrderDetail,
  type AdminOrderHistoryEntry,
  type AdminOrderStatus,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

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

function formatCurrency(value: number | null | undefined, currency?: string | null): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('es-PE', { style: 'currency', currency: currency ?? 'PEN' }).format(value);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function AddressCard({ title, address }: { title: string; address: AdminOrderCustomer | null }) {
  const lines = address
    ? [
        [address.firstName, address.lastName].filter(Boolean).join(' ') || null,
        address.phone,
        address.email,
        [address.addressLine1, address.addressLine2].filter(Boolean).join(', ') || null,
        [address.city, address.region].filter(Boolean).join(', ') || null,
        [address.postalCode, address.countryCode].filter(Boolean).join(' ') || null,
      ].filter(Boolean)
    : [];
  return (
    <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
      <h3 className="text-xs font-medium uppercase tracking-wide text-maison-warmgray">{title}</h3>
      {lines.length === 0 ? (
        <p className="mt-2 text-sm text-maison-muted">No proporcionado</p>
      ) : (
        <address className="mt-2 space-y-0.5 text-sm not-italic text-maison-ink">
          {lines.map((line, i) => (
            <p key={i}>{line}</p>
          ))}
        </address>
      )}
    </div>
  );
}

function Timeline({ entries }: { entries: AdminOrderHistoryEntry[] }) {
  if (entries.length === 0) {
    return <p className="text-sm text-maison-muted">Aún no se registraron cambios de estado.</p>;
  }
  return (
    <ol className="relative ml-3 space-y-6 border-l border-maison-line pl-6">
      {entries.map((entry, i) => (
        <li key={entry.id ?? i} className="relative">
          <span className="absolute -left-[31px] top-1 h-3 w-3 rounded-full border-2 border-maison-soft bg-white" />
          <div className="flex flex-wrap items-baseline gap-x-2">
            <span className="text-sm font-medium text-maison-ink">
              {statusLabel(entry.oldStatus ?? '—')} → {statusLabel(entry.newStatus)}
            </span>
            <span className="text-xs text-maison-muted">{formatDate(entry.changedAt)}</span>
          </div>
          {entry.reason && <p className="mt-0.5 text-sm text-maison-warmgray">“{entry.reason}”</p>}
        </li>
      ))}
    </ol>
  );
}

/**
 * Admin order detail (slice E): customer/addresses (null-safe for guest
 * checkouts), items, payments, totals, the chronological status timeline and
 * the transition control driven by the backend's allowed-transitions matrix.
 */
export default function AdminOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const navigate = useNavigate();
  const pushToast = useUIStore((s) => s.pushToast);

  const [order, setOrder] = useState<AdminOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Transition form state.
  const [target, setTarget] = useState<AdminOrderStatus | ''>('');
  const [notes, setNotes] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setOrder(await fetchAdminOrder(orderId));
    } catch (err) {
      const message = err instanceof AdminApiError ? err.message : 'No se pudo cargar el pedido.';
      setError(message);
      pushToast('error', message);
    } finally {
      setLoading(false);
    }
  }, [orderId, pushToast]);

  useEffect(() => {
    void load();
  }, [load]);

  const allowedTransitions = useMemo(
    () => (order?.allowedTransitions ?? []) as AdminOrderStatus[],
    [order]
  );

  // Keep the selected target valid as the order changes.
  useEffect(() => {
    if (order && (target === '' || !allowedTransitions.includes(target))) {
      setTarget(allowedTransitions[0] ?? '');
    }
  }, [order, allowedTransitions, target]);

  const needsRefundAmount = target === 'REFUNDED';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!order || !target) return;
    setSubmitting(true);
    try {
      const updated = await updateAdminOrderStatus(order.id, {
        newStatus: target,
        notes: notes.trim() || null,
        refundAmount: needsRefundAmount && refundAmount !== '' ? Number(refundAmount) : null,
      });
      setOrder(updated);
      setNotes('');
      setRefundAmount('');
      pushToast('success', `El pedido #${order.orderNumber} pasó a ${statusLabel(target)}.`);
    } catch (err) {
      const message =
        err instanceof AdminApiError ? err.message : 'No se pudo cambiar el estado.';
      pushToast('error', message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading && !order) {
    return <p className="text-sm text-maison-warmgray">Cargando pedido…</p>;
  }

  if (error && !order) {
    return (
      <div>
        <button
          type="button"
          onClick={() => navigate('/admin/orders')}
          className="mb-4 flex items-center gap-1 text-sm text-maison-warmgray transition-colors hover:text-maison-ink"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver a pedidos
        </button>
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {error}
        </div>
      </div>
    );
  }

  if (!order) return null;

  const payments = order.payments ?? [];
  const items = order.items ?? [];
  const history = order.statusHistory ?? [];
  const totals = [
    { label: 'Subtotal', value: formatCurrency(order.subTotal, order.currencyCode) },
    { label: 'Descuento', value: formatCurrency(order.discountAmount, order.currencyCode) },
    { label: 'Envío', value: formatCurrency(order.shippingCost, order.currencyCode) },
    { label: 'Impuestos', value: formatCurrency(order.taxAmount, order.currencyCode) },
  ];

  return (
    <div>
      <button
        type="button"
        onClick={() => navigate('/admin/orders')}
        className="mb-4 flex items-center gap-1 text-sm text-maison-warmgray transition-colors hover:text-maison-ink"
      >
        <ArrowLeft className="h-4 w-4" />
        Volver a pedidos
      </button>

      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-serif text-2xl text-maison-ink">Pedido #{order.orderNumber}</h1>
          <p className="mt-1 text-sm text-maison-warmgray">
            Realizado {formatDate(order.createdAt)} · ID {order.id}
            {order.userEmail ? ` · Cliente registrado` : ' · Pedido de invitado'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium ${statusBadgeClass(order.status)}`}
          >
            {statusLabel(order.status)}
          </span>
          <span className="inline-flex items-center gap-1 rounded-full bg-maison-cream px-3 py-1 text-xs font-medium text-maison-warmgray">
            Pago: {statusLabel(order.paymentStatus)}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left column: customer + addresses */}
        <div className="space-y-4">
          <AddressCard title="Dirección de envío" address={order.shippingAddress} />
          <AddressCard title="Dirección de facturación" address={order.billingAddress} />

          <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
            <h3 className="text-xs font-medium uppercase tracking-wide text-maison-warmgray">
              Cumplimiento
            </h3>
            <dl className="mt-2 space-y-1.5 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-maison-warmgray">Método de envío</dt>
                <dd className="text-maison-ink">{order.shippingMethod ?? '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-maison-warmgray">Transportista</dt>
                <dd className="text-maison-ink">{order.carrier ?? '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-maison-warmgray">Número de seguimiento</dt>
                <dd className="text-maison-ink">{order.trackingNumber ?? '—'}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-maison-warmgray">Método de pago</dt>
                <dd className="text-maison-ink">{order.paymentMethod ?? '—'}</dd>
              </div>
              {order.cancelledAt && (
                <div className="flex justify-between gap-3">
                  <dt className="text-maison-warmgray">Cancelado</dt>
                  <dd className="text-maison-ink">{formatDate(order.cancelledAt)}</dd>
                </div>
              )}
              {order.cancellationReason && (
                <div className="flex justify-between gap-3">
                  <dt className="text-maison-warmgray">Motivo de cancelación</dt>
                  <dd className="text-maison-ink">{order.cancellationReason}</dd>
                </div>
              )}
              {order.refundAmount != null && (
                <div className="flex justify-between gap-3">
                  <dt className="text-maison-warmgray">Monto reembolsado</dt>
                  <dd className="text-maison-ink">{formatCurrency(order.refundAmount, order.currencyCode)}</dd>
                </div>
              )}
            </dl>
          </div>

          {(order.customerNotes || order.internalNotes) && (
            <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
              <h3 className="text-xs font-medium uppercase tracking-wide text-maison-warmgray">Notas</h3>
              {order.customerNotes && (
                <p className="mt-2 text-sm text-maison-ink">
                  <span className="text-maison-warmgray">Cliente: </span>
                  {order.customerNotes}
                </p>
              )}
              {order.internalNotes && (
                <p className="mt-2 text-sm text-maison-ink">
                  <span className="text-maison-warmgray">Interna: </span>
                  {order.internalNotes}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Middle column: items + totals */}
        <div className="space-y-4 lg:col-span-1">
          <div className="overflow-hidden rounded-lg border border-maison-line bg-white shadow-card">
            <div className="border-b border-maison-line bg-maison-warm px-4 py-3">
              <h2 className="text-sm font-medium text-maison-ink">Artículos</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-maison-line text-xs uppercase tracking-wide text-maison-warmgray">
                    <th className="px-4 py-2 font-medium">Producto</th>
                    <th className="px-4 py-2 font-medium">Cant.</th>
                    <th className="px-4 py-2 font-medium">Unitario</th>
                    <th className="px-4 py-2 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.id} className="border-b border-maison-line/60 last:border-b-0">
                      <td className="px-4 py-3">
                        <p className="font-medium text-maison-ink">{item.productName ?? '—'}</p>
                        <p className="text-xs text-maison-muted">
                          {item.variantName ?? '—'}
                          {item.sku ? ` · ${item.sku}` : ''}
                        </p>
                        {item.fulfillmentStatus && (
                          <p className="mt-0.5 text-xs text-maison-warmgray">
                            Cumplimiento: {statusLabel(item.fulfillmentStatus)}
                          </p>
                        )}
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">{item.quantity}</td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {formatCurrency(item.unitPrice, order.currencyCode)}
                      </td>
                      <td className="px-4 py-3 text-right text-maison-ink">
                        {formatCurrency(item.totalPrice, order.currencyCode)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="border-t border-maison-line bg-maison-warm px-4 py-3">
              <dl className="space-y-1 text-sm">
                {totals.map((row) => (
                  <div key={row.label} className="flex justify-between gap-3">
                    <dt className="text-maison-warmgray">{row.label}</dt>
                    <dd className="text-maison-ink">{row.value}</dd>
                  </div>
                ))}
                <div className="flex justify-between gap-3 border-t border-maison-line pt-2">
                  <dt className="font-medium text-maison-ink">Total</dt>
                  <dd className="font-semibold text-maison-ink">
                    {formatCurrency(order.totalAmount, order.currencyCode)}
                  </dd>
                </div>
              </dl>
            </div>
          </div>

          {payments.length > 0 && (
            <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
              <h3 className="text-xs font-medium uppercase tracking-wide text-maison-warmgray">Pagos</h3>
              <ul className="mt-2 space-y-2">
                {payments.map((p, i) => (
                  <li key={i} className="flex items-center justify-between gap-3 text-sm">
                    <span className="text-maison-ink">
                      {p.provider ?? '—'}
                      {p.transactionId ? ` (${p.transactionId})` : ''}
                    </span>
                    <span className="text-maison-warmgray">
                      {formatCurrency(p.amount, p.currency)} · {statusLabel(p.status ?? '—')}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        {/* Right column: timeline + transition control */}
        <div className="space-y-4">
          <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-medium text-maison-ink">Historial de estados</h2>
              <button
                type="button"
                onClick={() => void load()}
                title="Actualizar pedido"
                className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-maison-warm hover:text-maison-ink"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            </div>
            <Timeline entries={history} />
          </div>

          <div className="rounded-lg border border-maison-line bg-white p-4 shadow-card">
            <h2 className="text-sm font-medium text-maison-ink">Cambiar estado</h2>
            {allowedTransitions.length === 0 ? (
              <p className="mt-2 text-sm text-maison-warmgray">
                Este pedido está en su estado final ({statusLabel(order.status)}) y no puede cambiar
                más.
              </p>
            ) : (
              <form onSubmit={handleSubmit} className="mt-3 space-y-3">
                <div>
                  <label
                    htmlFor="target-status"
                    className="mb-1 block text-xs font-medium uppercase tracking-wide text-maison-warmgray"
                  >
                    Mover a
                  </label>
                  <select
                    id="target-status"
                    value={target}
                    onChange={(e) => setTarget(e.target.value as AdminOrderStatus)}
                    className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
                  >
                    {allowedTransitions.map((s) => (
                      <option key={s} value={s}>
                        {statusLabel(s)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label
                    htmlFor="transition-notes"
                    className="mb-1 block text-xs font-medium uppercase tracking-wide text-maison-warmgray"
                  >
                    Notas {target === 'CANCELLED' ? '(se guarda como motivo de cancelación)' : '(opcional)'}
                  </label>
                  <textarea
                    id="transition-notes"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={2}
                    className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
                  />
                </div>
                {needsRefundAmount && (
                  <div>
                    <label
                      htmlFor="refund-amount"
                      className="mb-1 block text-xs font-medium uppercase tracking-wide text-maison-warmgray"
                    >
                      Monto a reembolsar
                    </label>
                    <input
                      id="refund-amount"
                      type="number"
                      step="0.01"
                      min="0"
                      value={refundAmount}
                      onChange={(e) => setRefundAmount(e.target.value)}
                      placeholder={order.totalAmount != null ? String(order.totalAmount) : '0.00'}
                      className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
                    />
                  </div>
                )}
                <button
                  type="submit"
                  disabled={submitting || !target}
                  className="flex w-full items-center justify-center gap-2 rounded-md bg-maison-ink px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {submitting ? (
                    <>
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      Actualizando…
                    </>
                  ) : (
                    <>
                      <Package className="h-4 w-4" />
                      Mover a {statusLabel(target)}
                    </>
                  )}
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
