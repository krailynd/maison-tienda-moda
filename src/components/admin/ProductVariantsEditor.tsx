import { useEffect, useState } from 'react';
import { Pencil, Plus, Save, Trash2, X } from 'lucide-react';
import {
  AdminApiError,
  createAdminVariant,
  deleteAdminVariant,
  fetchAdminColors,
  fetchAdminSizes,
  updateAdminVariant,
  type AdminColorOption,
  type AdminSizeOption,
  type AdminVariant,
  type AdminVariantPayload,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

const inputClass =
  'w-full rounded-md border border-maison-line bg-maison-warm px-2 py-1.5 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal';

interface VariantFormState {
  id: number | null;
  name: string;
  sku: string;
  sizeId: string;
  colorId: string;
  price: string;
  comparePrice: string;
  costPrice: string;
  stock: string;
  isDefault: boolean;
  isActive: boolean;
}

function emptyForm(): VariantFormState {
  return {
    id: null,
    name: '',
    sku: '',
    sizeId: '',
    colorId: '',
    price: '',
    comparePrice: '',
    costPrice: '',
    stock: '0',
    isDefault: false,
    isActive: true,
  };
}

interface Props {
  productId: number;
  variants: AdminVariant[];
  onChanged: () => void;
}

/**
 * Variant editor (slice C): table of the product's variants with an inline
 * create/edit form (size/color refs, pricing, stock and flags) wired to the
 * admin variant endpoints.
 */
export default function ProductVariantsEditor({ productId, variants, onChanged }: Props) {
  const pushToast = useUIStore((s) => s.pushToast);
  const [sizes, setSizes] = useState<AdminSizeOption[]>([]);
  const [colors, setColors] = useState<AdminColorOption[]>([]);
  const [form, setForm] = useState<VariantFormState>(emptyForm());
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const [sizeOptions, colorOptions] = await Promise.all([fetchAdminSizes(), fetchAdminColors()]);
        setSizes(sizeOptions);
        setColors(colorOptions);
      } catch {
        pushToast('error', 'No se pudieron cargar las opciones de talla y color.');
      }
    })();
  }, [pushToast]);

  const startEdit = (v: AdminVariant) => {
    setFormError(null);
    setForm({
      id: v.id,
      name: v.name,
      sku: v.sku,
      sizeId: v.size ? String(v.size.id) : '',
      colorId: v.color ? String(v.color.id) : '',
      price: v.price != null ? String(v.price) : '',
      comparePrice: v.comparePrice != null ? String(v.comparePrice) : '',
      costPrice: v.costPrice != null ? String(v.costPrice) : '',
      stock: String(v.stock),
      isDefault: v.isDefault,
      isActive: v.isActive,
    });
  };

  const cancelEdit = () => {
    setForm(emptyForm());
    setFormError(null);
  };

  const submit = async () => {
    if (!form.name.trim() || !form.sku.trim()) {
      setFormError('El nombre y el SKU son obligatorios.');
      return;
    }
    const payload: AdminVariantPayload = {
      name: form.name.trim(),
      sku: form.sku.trim(),
      sizeId: form.sizeId ? Number(form.sizeId) : null,
      colorId: form.colorId ? Number(form.colorId) : null,
      price: form.price === '' ? null : Number(form.price),
      comparePrice: form.comparePrice === '' ? null : Number(form.comparePrice),
      costPrice: form.costPrice === '' ? null : Number(form.costPrice),
      stock: form.stock === '' ? null : Number(form.stock),
      isDefault: form.isDefault,
      isActive: form.isActive,
    };
    setBusy(true);
    setFormError(null);
    try {
      if (form.id != null) {
        await updateAdminVariant(form.id, payload);
        pushToast('success', 'Variante actualizada.');
      } else {
        await createAdminVariant(productId, payload);
        pushToast('success', 'Variante creada.');
      }
      cancelEdit();
      onChanged();
    } catch (err) {
      setFormError(err instanceof AdminApiError ? err.message : 'No se pudo guardar la variante.');
    } finally {
      setBusy(false);
    }
  };

  const remove = async (v: AdminVariant) => {
    if (!window.confirm(`¿Eliminar la variante "${v.name}"? Se elimina de forma lógica y no se puede deshacer.`)) {
      return;
    }
    setDeletingId(v.id);
    try {
      await deleteAdminVariant(v.id);
      pushToast('success', 'Variante eliminada.');
      if (form.id === v.id) cancelEdit();
      onChanged();
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo eliminar.');
    } finally {
      setDeletingId(null);
    }
  };

  const editing = form.id != null;

  return (
    <div className="space-y-4">
      {/* List */}
      {variants.length === 0 ? (
        <p className="text-sm text-maison-warmgray">Aún no hay variantes — añade la primera a continuación.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-maison-line bg-maison-warm text-xs uppercase tracking-wide text-maison-warmgray">
                <th className="px-3 py-2 font-medium">Nombre</th>
                <th className="px-3 py-2 font-medium">SKU</th>
                <th className="px-3 py-2 font-medium">Talla / Color</th>
                <th className="px-3 py-2 font-medium">Precio</th>
                <th className="px-3 py-2 font-medium">Stock</th>
                <th className="px-3 py-2 font-medium">Predeterminada</th>
                <th className="px-3 py-2 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {variants.map((v) => (
                <tr key={v.id} className="border-b border-maison-line/60 last:border-b-0">
                  <td className="px-3 py-2">
                    <span className="font-medium text-maison-ink">{v.name}</span>
                    {!v.isActive && (
                      <span className="ml-2 rounded-full bg-maison-cream px-2 py-0.5 text-xs text-maison-warmgray">
                        Inactiva
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2 font-mono text-xs text-maison-warmgray">{v.sku}</td>
                  <td className="px-3 py-2 text-maison-warmgray">
                    {v.size ? `${v.size.code} / ` : '— / '}
                    {v.color ? v.color.name : '—'}
                  </td>
                  <td className="px-3 py-2 text-maison-ink">
                    {v.price != null ? `S/ ${v.price}` : '—'}
                  </td>
                  <td className="px-3 py-2 text-maison-warmgray">{v.stock}</td>
                  <td className="px-3 py-2 text-maison-warmgray">{v.isDefault ? 'Sí' : '—'}</td>
                  <td className="px-3 py-2">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        type="button"
                        title="Editar variante"
                        disabled={deletingId === v.id}
                        onClick={() => startEdit(v)}
                        className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-maison-warm hover:text-maison-ink disabled:opacity-50"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        title="Eliminar variante"
                        disabled={deletingId === v.id}
                        onClick={() => remove(v)}
                        className="rounded-md p-1.5 text-maison-warmgray transition-colors hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {formError && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {formError}
        </div>
      )}

      {/* Inline create/edit form */}
      <div className="rounded-md border border-maison-line bg-maison-warm/60 p-4">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-medium text-maison-ink">{editing ? 'Editar variante' : 'Nueva variante'}</h3>
          {editing && (
            <button type="button" onClick={cancelEdit} className="text-xs text-maison-warmgray hover:text-maison-ink">
              Cancelar edición
            </button>
          )}
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          <label className="block text-xs text-maison-ink">
            Nombre *
            <input
              className={inputClass}
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Blusa - M / Negro"
            />
          </label>
          <label className="block text-xs text-maison-ink">
            SKU *
            <input
              className={inputClass}
              value={form.sku}
              onChange={(e) => setForm({ ...form, sku: e.target.value })}
              placeholder="MAIS-001-01"
            />
          </label>
          <label className="block text-xs text-maison-ink">
            Talla
            <select
              className={inputClass}
              value={form.sizeId}
              onChange={(e) => setForm({ ...form, sizeId: e.target.value })}
            >
              <option value="">—</option>
              {sizes.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.code})
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-maison-ink">
            Color
            <select
              className={inputClass}
              value={form.colorId}
              onChange={(e) => setForm({ ...form, colorId: e.target.value })}
            >
              <option value="">—</option>
              {colors.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs text-maison-ink">
            Precio (PEN)
            <input
              type="number"
              step="0.01"
              min="0"
              className={inputClass}
              value={form.price}
              onChange={(e) => setForm({ ...form, price: e.target.value })}
            />
          </label>
          <label className="block text-xs text-maison-ink">
            Precio de comparación
            <input
              type="number"
              step="0.01"
              min="0"
              className={inputClass}
              value={form.comparePrice}
              onChange={(e) => setForm({ ...form, comparePrice: e.target.value })}
            />
          </label>
          <label className="block text-xs text-maison-ink">
            Precio de costo
            <input
              type="number"
              step="0.01"
              min="0"
              className={inputClass}
              value={form.costPrice}
              onChange={(e) => setForm({ ...form, costPrice: e.target.value })}
            />
          </label>
          <label className="block text-xs text-maison-ink">
            Stock disponible
            <input
              type="number"
              step="1"
              min="0"
              className={inputClass}
              value={form.stock}
              onChange={(e) => setForm({ ...form, stock: e.target.value })}
            />
          </label>
        </div>
        <div className="mt-3 flex items-center justify-between">
          <div className="flex items-center gap-4 text-xs text-maison-ink">
            <label className="flex items-center gap-1.5">
              <input
                type="checkbox"
                checked={form.isDefault}
                onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
                className="h-3.5 w-3.5 rounded border-maison-line accent-maison-ink"
              />
              Predeterminada
            </label>
            <label className="flex items-center gap-1.5">
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                className="h-3.5 w-3.5 rounded border-maison-line accent-maison-ink"
              />
              Activa
            </label>
          </div>
          <div className="flex items-center gap-2">
            {editing && (
              <button
                type="button"
                onClick={cancelEdit}
                className="flex items-center gap-1.5 rounded-md border border-maison-line bg-white px-3 py-1.5 text-xs text-maison-ink hover:bg-maison-warm"
              >
                <X className="h-3.5 w-3.5" />
                Cancelar
              </button>
            )}
            <button
              type="button"
              disabled={busy}
              onClick={submit}
              className="flex items-center gap-1.5 rounded-md bg-maison-ink px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-60"
            >
              {editing ? <Save className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
              {busy ? 'Guardando…' : editing ? 'Guardar variante' : 'Añadir variante'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
