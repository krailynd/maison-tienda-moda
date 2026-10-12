import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { ChevronLeft, ChevronRight, Eye, EyeOff, Pencil, Plus, Trash2 } from 'lucide-react';
import {
  AdminApiError,
  deleteAdminProduct,
  fetchAdminProducts,
  publishAdminProduct,
  unpublishAdminProduct,
  type AdminProductStatus,
  type AdminProductSummary,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

const PAGE_SIZE = 20;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
}

function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

const STATUS_OPTIONS: { value: AdminProductStatus; label: string }[] = [
  { value: 'all', label: 'Todos los estados' },
  { value: 'active', label: 'Activo (publicado)' },
  { value: 'inactive', label: 'Borrador / inactivo' },
];

/**
 * Admin product catalog list (slice B): search, status filter, pagination fed
 * by the X-Total-Count header, and edit / publish / unpublish / delete actions.
 * Variant & image management is a later slice.
 */
export default function AdminProducts() {
  const navigate = useNavigate();
  const pushToast = useUIStore((s) => s.pushToast);

  const [items, setItems] = useState<AdminProductSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [status, setStatus] = useState<AdminProductStatus>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  // Debounce the search box so each keystroke does not fire a request.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  // A filter change restarts pagination.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, status]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAdminProducts({
        search: debouncedSearch || undefined,
        status,
        page,
        size: PAGE_SIZE,
      });
      setItems(result.items);
      setTotal(result.total);
      setTotalPages(result.totalPages);
    } catch (err) {
      const message = err instanceof AdminApiError ? err.message : 'No se pudieron cargar los productos.';
      setError(message);
      pushToast('error', message);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, status, page, pushToast]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleTogglePublish = async (item: AdminProductSummary) => {
    setBusyId(item.id);
    try {
      if (item.isActive) {
        await unpublishAdminProduct(item.id);
        pushToast('success', `"${item.name}" se despublicó (oculto de la tienda).`);
      } else {
        await publishAdminProduct(item.id);
        pushToast('success', `"${item.name}" se publicó y ya está disponible en la tienda.`);
      }
      await load();
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'La acción falló.');
    } finally {
      setBusyId(null);
    }
  };

  const handleDelete = async (item: AdminProductSummary) => {
    if (!window.confirm(`¿Eliminar "${item.name}"? Se elimina de forma lógica y no se puede deshacer.`)) {
      return;
    }
    setBusyId(item.id);
    try {
      await deleteAdminProduct(item.id);
      pushToast('success', `"${item.name}" fue eliminado.`);
      if (items.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        await load();
      }
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo eliminar.');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-serif text-2xl text-maison-ink">Productos</h1>
          <p className="mt-1 text-sm text-maison-warmgray">
            Gestiona el catálogo completo, incluidos los borradores y los productos no publicados.
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/admin/products/new')}
          className="flex items-center gap-2 rounded-md bg-maison-ink px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft"
        >
          <Plus className="h-4 w-4" />
          Nuevo producto
        </button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nombre…"
          aria-label="Buscar productos"
          className="w-64 rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
        />
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as AdminProductStatus)}
          aria-label="Filtrar por estado"
          className="rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal"
        >
          {STATUS_OPTIONS.map((opt) => (
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
                <th className="px-4 py-3 font-medium">Producto</th>
                <th className="px-4 py-3 font-medium">Precio</th>
                <th className="px-4 py-3 font-medium">Estado</th>
                <th className="px-4 py-3 font-medium">Categoría</th>
                <th className="px-4 py-3 font-medium">Marca</th>
                <th className="px-4 py-3 font-medium">Stock</th>
                <th className="px-4 py-3 font-medium">Actualizado</th>
                <th className="px-4 py-3 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading && items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-maison-warmgray">
                    Cargando productos…
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-maison-warmgray">
                    Ningún producto coincide con los filtros actuales.
                  </td>
                </tr>
              ) : (
                items.map((item) => {
                  const busy = busyId === item.id;
                  return (
                    <tr key={item.id} className="border-b border-maison-line/60 last:border-b-0">
                      <td className="px-4 py-3">
                        <p className="font-medium text-maison-ink">{item.name}</p>
                        <p className="text-xs text-maison-muted">{item.slug}</p>
                      </td>
                      <td className="px-4 py-3 text-maison-ink">{formatCurrency(item.price)}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                            item.isActive
                              ? 'bg-maison-sage/20 text-maison-soft'
                              : 'bg-maison-cream text-maison-warmgray'
                          }`}
                        >
                          {item.isActive ? 'Activo' : 'Borrador'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {item.categoryName ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {item.brandName ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">{item.totalStock}</td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {formatDate(item.updatedAt)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            title={item.isActive ? 'Despublicar' : 'Publicar'}
                            disabled={busy}
                            onClick={() => handleTogglePublish(item)}
                            className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-maison-warm hover:text-maison-ink disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {item.isActive ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                          <button
                            type="button"
                            title="Editar"
                            disabled={busy}
                            onClick={() => navigate(`/admin/products/${item.id}`)}
                            className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-maison-warm hover:text-maison-ink disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button
                            type="button"
                            title="Eliminar"
                            disabled={busy}
                            onClick={() => handleDelete(item)}
                            className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-maison-line bg-maison-warm px-4 py-3">
          <p className="text-xs text-maison-warmgray">
            {total} producto{total === 1 ? '' : 's'} · página {totalPages > 0 ? page + 1 : 0} de{' '}
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
