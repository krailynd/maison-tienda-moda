import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Pencil, Plus, Trash2, X } from 'lucide-react';
import {
  AdminApiError,
  createAdminCollection,
  deleteAdminCollection,
  fetchAdminCollections,
  fetchAdminSeasons,
  updateAdminCollection,
  type AdminCollection,
  type AdminSeasonOption,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

const PAGE_SIZE = 20;

function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

interface FormState {
  id: number | null;
  name: string;
  slug: string;
  description: string;
  bannerUrl: string;
  seasonId: string;
  startDate: string;
  endDate: string;
  isFeatured: boolean;
}

const emptyForm: FormState = {
  id: null,
  name: '',
  slug: '',
  description: '',
  bannerUrl: '',
  seasonId: '',
  startDate: '',
  endDate: '',
  isFeatured: false,
};

function slugify(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

/**
 * Admin collection manager (slice D): paginated table fed by X-Total-Count,
 * inline create/edit form with the season dropdown (seeded PV2026 / OI2026)
 * and soft delete.
 */
export default function AdminCollections() {
  const pushToast = useUIStore((s) => s.pushToast);

  const [items, setItems] = useState<AdminCollection[]>([]);
  const [seasons, setSeasons] = useState<AdminSeasonOption[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAdminCollections({ page, size: PAGE_SIZE });
      setItems(result.items);
      setTotal(result.total);
      setTotalPages(result.totalPages);
    } catch (err) {
      const message = err instanceof AdminApiError ? err.message : 'No se pudieron cargar las colecciones.';
      setError(message);
      pushToast('error', message);
    } finally {
      setLoading(false);
    }
  }, [page, pushToast]);

  useEffect(() => {
    void load();
  }, [load]);

  // Season options for the form dropdown (read-only reference endpoint).
  useEffect(() => {
    void fetchAdminSeasons()
      .then(setSeasons)
      .catch(() => pushToast('error', 'No se pudieron cargar las temporadas.'));
  }, [pushToast]);

  const openCreate = () => {
    setForm(emptyForm);
    setFormOpen(true);
  };

  const openEdit = (item: AdminCollection) => {
    setForm({
      id: item.id,
      name: item.name,
      slug: item.slug,
      description: item.description ?? '',
      bannerUrl: item.bannerUrl ?? '',
      seasonId: item.seasonId != null ? String(item.seasonId) : '',
      startDate: item.startDate ?? '',
      endDate: item.endDate ?? '',
      isFeatured: item.isFeatured,
    });
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setForm(emptyForm);
  };

  const set = (field: keyof FormState, value: string | boolean) =>
    setForm((f) => ({ ...f, [field]: value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.slug.trim()) {
      pushToast('error', 'El nombre y el slug son obligatorios.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        slug: form.slug.trim(),
        description: form.description || null,
        bannerUrl: form.bannerUrl || null,
        seasonId: form.seasonId ? Number(form.seasonId) : null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        isFeatured: form.isFeatured,
      };
      if (form.id == null) {
        await createAdminCollection(payload);
        pushToast('success', `La colección "${payload.name}" fue creada.`);
      } else {
        await updateAdminCollection(form.id, payload);
        pushToast('success', `La colección "${payload.name}" fue actualizada.`);
      }
      closeForm();
      await load();
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo guardar.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (item: AdminCollection) => {
    if (!window.confirm(`¿Eliminar "${item.name}"? No se puede deshacer.`)) return;
    setBusyId(item.id);
    try {
      await deleteAdminCollection(item.id);
      pushToast('success', `La colección "${item.name}" fue eliminada.`);
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
          <h1 className="font-serif text-2xl text-maison-ink">Colecciones</h1>
          <p className="mt-1 text-sm text-maison-warmgray">
            Gestiona las cápsulas de temporada vinculadas a las temporadas iniciales (PV2026 /
            OI2026).
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-2 rounded-md bg-maison-ink px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft"
        >
          <Plus className="h-4 w-4" />
          Nueva colección
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {error}
        </div>
      )}

      {formOpen && (
        <form
          onSubmit={handleSubmit}
          className="mb-6 rounded-lg border border-maison-line bg-white p-5 shadow-card"
        >
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-serif text-lg text-maison-ink">
              {form.id == null ? 'Nueva colección' : `Editar: ${form.name}`}
            </h2>
            <button
              type="button"
              onClick={closeForm}
              className="rounded-md p-1 text-maison-warmgray hover:bg-maison-warm hover:text-maison-ink"
              aria-label="Cerrar formulario"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">Nombre *</span>
              <input
                type="text"
                value={form.name}
                onChange={(e) => {
                  set('name', e.target.value);
                  if (form.id == null && !form.slug) set('slug', slugify(e.target.value));
                }}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">Slug *</span>
              <input
                type="text"
                value={form.slug}
                onChange={(e) => set('slug', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">Temporada</span>
              <select
                value={form.seasonId}
                onChange={(e) => set('seasonId', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              >
                <option value="">Sin temporada</option>
                {seasons.map((s) => (
                  <option key={s.id} value={String(s.id)}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm sm:col-span-2">
              <span className="mb-1 block font-medium text-maison-ink">Descripción</span>
              <textarea
                rows={2}
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">URL del banner</span>
              <input
                type="url"
                value={form.bannerUrl}
                onChange={(e) => set('bannerUrl', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">Fecha de inicio</span>
              <input
                type="date"
                value={form.startDate}
                onChange={(e) => set('startDate', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-maison-ink">Fecha de fin</span>
              <input
                type="date"
                value={form.endDate}
                onChange={(e) => set('endDate', e.target.value)}
                className="w-full rounded-md border border-maison-line bg-white px-3 py-2 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
              />
            </label>
            <label className="flex items-center gap-2 text-sm text-maison-ink sm:col-span-2">
              <input
                type="checkbox"
                checked={form.isFeatured}
                onChange={(e) => set('isFeatured', e.target.checked)}
                className="h-4 w-4 rounded border-maison-line"
              />
              Destacada en la tienda
            </label>
          </div>
          <div className="mt-4 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={closeForm}
              className="rounded-md border border-maison-line bg-white px-4 py-2 text-sm text-maison-ink transition-colors hover:bg-maison-warm"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-md bg-maison-ink px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? 'Guardando…' : form.id == null ? 'Crear' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      )}

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-maison-line bg-white shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-maison-line bg-maison-warm text-xs uppercase tracking-wide text-maison-warmgray">
                <th className="px-4 py-3 font-medium">Colección</th>
                <th className="px-4 py-3 font-medium">Temporada</th>
                <th className="px-4 py-3 font-medium">Destacada</th>
                <th className="px-4 py-3 font-medium">Periodo</th>
                <th className="px-4 py-3 font-medium">Actualizado</th>
                <th className="px-4 py-3 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading && items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-maison-warmgray">
                    Cargando colecciones…
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-maison-warmgray">
                    Aún no hay colecciones. Crea la primera.
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
                      <td className="px-4 py-3 text-maison-warmgray">{item.seasonName ?? '—'}</td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {item.isFeatured ? 'Sí' : '—'}
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">
                        {item.startDate ?? '—'} → {item.endDate ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-maison-warmgray">{formatDate(item.updatedAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            title="Editar"
                            disabled={busy}
                            onClick={() => openEdit(item)}
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
            {total} {total === 1 ? 'colección' : 'colecciones'} · página {totalPages > 0 ? page + 1 : 0} de{' '}
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
