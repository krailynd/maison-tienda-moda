import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate, useParams } from 'react-router';
import { ArrowLeft, Save } from 'lucide-react';
import ProductImagesManager from '@/components/admin/ProductImagesManager';
import ProductVariantsEditor from '@/components/admin/ProductVariantsEditor';
import {
  AdminApiError,
  createAdminProduct,
  fetchAdminBrandOptions,
  fetchAdminCategoryOptions,
  fetchAdminProduct,
  updateAdminProduct,
  type AdminBrandOption,
  type AdminCategoryOption,
  type AdminProductDetail,
  type AdminProductPayload,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

// ---------- Zod schema (mirrors the backend AdminProductRequest) ----------

const optionalNumber = z.preprocess(
  (value) => (value === '' || value == null ? null : Number(value)),
  z.number({ message: 'Debe ser un número' }).nullable().optional()
);

const priceSchema = z.preprocess(
  (value) => (value === '' || value == null ? Number.NaN : Number(value)),
  z
    .number({ message: 'El precio debe ser un número' })
    .positive('El precio debe ser mayor que cero')
);

const formSchema = z.object({
  name: z.string().min(1, 'El nombre es obligatorio').max(255, 'El nombre no puede superar 255 caracteres'),
  slug: z
    .string()
    .min(1, 'El slug es obligatorio')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'Solo minúsculas, números y guiones')
    .max(255, 'El slug no puede superar 255 caracteres'),
  price: priceSchema,
  description: z.string().optional(),
  shortDescription: z.string().max(500, 'Máximo 500 caracteres').optional(),
  skuPrefix: z.string().max(50, 'Máximo 50 caracteres').optional(),
  categoryId: optionalNumber,
  brandId: optionalNumber,
  comparePrice: optionalNumber,
  costPrice: optionalNumber,
  taxRate: optionalNumber,
  weight: optionalNumber,
  lengthCm: optionalNumber,
  widthCm: optionalNumber,
  heightCm: optionalNumber,
  isFeatured: z.boolean().optional(),
  isNew: z.boolean().optional(),
  isEcoFriendly: z.boolean().optional(),
  stockTracking: z.boolean().optional(),
  lowStockThreshold: optionalNumber,
  minOrderQuantity: optionalNumber,
  maxOrderQuantity: optionalNumber,
  seoTitle: z.string().max(255, 'Máximo 255 caracteres').optional(),
  seoDescription: z.string().optional(),
  metaKeywords: z.string().optional(),
});

// The form carries RAW values (strings from inputs); preprocess converts them
// to numbers/null before validation, so the resolver bridges z.input → z.output.
type FormInput = z.input<typeof formSchema>;
type FormValues = z.output<typeof formSchema>;

const emptyDefaults: FormInput = {
  name: '',
  slug: '',
  price: '',
  description: '',
  shortDescription: '',
  skuPrefix: '',
  categoryId: null,
  brandId: null,
  comparePrice: null,
  costPrice: null,
  taxRate: null,
  weight: null,
  lengthCm: null,
  widthCm: null,
  heightCm: null,
  isFeatured: false,
  isNew: false,
  isEcoFriendly: false,
  stockTracking: true,
  lowStockThreshold: null,
  minOrderQuantity: null,
  maxOrderQuantity: null,
  seoTitle: '',
  seoDescription: '',
  metaKeywords: '',
};

function toPayload(values: FormValues): AdminProductPayload {
  return {
    name: values.name.trim(),
    slug: values.slug.trim(),
    description: values.description || null,
    shortDescription: values.shortDescription || null,
    skuPrefix: values.skuPrefix || null,
    categoryId: values.categoryId ?? null,
    brandId: values.brandId ?? null,
    price: values.price,
    comparePrice: values.comparePrice ?? null,
    costPrice: values.costPrice ?? null,
    taxRate: values.taxRate ?? null,
    weight: values.weight ?? null,
    lengthCm: values.lengthCm ?? null,
    widthCm: values.widthCm ?? null,
    heightCm: values.heightCm ?? null,
    isFeatured: values.isFeatured ?? false,
    isNew: values.isNew ?? false,
    isEcoFriendly: values.isEcoFriendly ?? false,
    stockTracking: values.stockTracking ?? true,
    lowStockThreshold: values.lowStockThreshold ?? null,
    minOrderQuantity: values.minOrderQuantity ?? null,
    maxOrderQuantity: values.maxOrderQuantity ?? null,
    seoTitle: values.seoTitle || null,
    seoDescription: values.seoDescription || null,
    metaKeywords: values.metaKeywords || null,
  };
}

// ---------- Small local field helpers ----------

const inputClass =
  'w-full rounded-md border border-maison-line bg-maison-warm px-3 py-2 text-sm text-maison-ink outline-none transition-colors focus:border-maison-charcoal';

function Field({
  label,
  htmlFor,
  required,
  children,
  error,
}: {
  label: string;
  htmlFor: string;
  required?: boolean;
  children: React.ReactNode;
  error?: string;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1 block text-sm font-medium text-maison-ink">
        {label}
        {required && <span className="text-maison-terracotta"> *</span>}
      </label>
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

function Checkbox({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="flex items-center gap-2 text-sm text-maison-ink">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="h-4 w-4 rounded border-maison-line accent-maison-ink"
      />
      {label}
    </label>
  );
}

/**
 * Create / edit product form (slice B). Only base product fields: variants and
 * images land in slice C. On success the user returns to the product list.
 */
export default function AdminProductForm() {
  const navigate = useNavigate();
  const pushToast = useUIStore((s) => s.pushToast);
  const { id } = useParams();
  const editId = id != null && !Number.isNaN(Number(id)) ? Number(id) : null;

  const [categories, setCategories] = useState<AdminCategoryOption[]>([]);
  const [brands, setBrands] = useState<AdminBrandOption[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loadingEdit, setLoadingEdit] = useState(editId != null);
  // Latest admin detail: keeps the variant/image editors in sync with the
  // server without touching the (unsaved) base-form fields.
  const [detail, setDetail] = useState<AdminProductDetail | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: emptyDefaults,
  });

  // Reference options for the category / brand selects (public catalog).
  useEffect(() => {
    void (async () => {
      try {
        const [cats, brs] = await Promise.all([
          fetchAdminCategoryOptions(),
          fetchAdminBrandOptions(),
        ]);
        setCategories(cats);
        setBrands(brs);
      } catch {
        pushToast('error', 'No se pudieron cargar las categorías y marcas.');
      }
    })();
  }, [pushToast]);

  // Edit mode: hydrate the form from the admin detail endpoint.
  useEffect(() => {
    if (editId == null) return;
    let cancelled = false;
    void (async () => {
      try {
        const detail = await fetchAdminProduct(editId);
        if (cancelled) return;
        setDetail(detail);
        reset({
          name: detail.name,
          slug: detail.slug,
          price: detail.price,
          description: detail.description ?? '',
          shortDescription: detail.shortDescription ?? '',
          skuPrefix: detail.skuPrefix ?? '',
          categoryId: detail.categoryId != null ? String(detail.categoryId) : null,
          brandId: detail.brandId != null ? String(detail.brandId) : null,
          comparePrice: detail.comparePrice ?? null,
          costPrice: detail.costPrice ?? null,
          taxRate: detail.taxRate ?? null,
          weight: detail.weight ?? null,
          lengthCm: detail.lengthCm ?? null,
          widthCm: detail.widthCm ?? null,
          heightCm: detail.heightCm ?? null,
          isFeatured: detail.isFeatured,
          isNew: detail.isNew,
          isEcoFriendly: detail.isEcoFriendly,
          stockTracking: detail.stockTracking,
          lowStockThreshold: detail.lowStockThreshold ?? null,
          minOrderQuantity: detail.minOrderQuantity ?? null,
          maxOrderQuantity: detail.maxOrderQuantity ?? null,
          seoTitle: detail.seoTitle ?? '',
          seoDescription: detail.seoDescription ?? '',
          metaKeywords: detail.metaKeywords ?? '',
        });
      } catch (err) {
        if (!cancelled) {
          pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo cargar el producto.');
          navigate('/admin/products', { replace: true });
        }
      } finally {
        if (!cancelled) setLoadingEdit(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [editId, reset, navigate, pushToast]);

  // Refreshes ONLY the detail state (variants/images) after an editor action;
  // the base-form fields are left untouched.
  const reloadDetail = useCallback(async () => {
    if (editId == null) return;
    try {
      setDetail(await fetchAdminProduct(editId));
    } catch {
      pushToast('error', 'No se pudieron actualizar las variantes e imágenes.');
    }
  }, [editId, pushToast]);

  const onSubmit = async (values: FormValues) => {
    setSubmitError(null);
    try {
      const payload = toPayload(values);
      if (editId != null) {
        await updateAdminProduct(editId, payload);
        pushToast('success', 'Producto actualizado.');
      } else {
        await createAdminProduct(payload);
        pushToast('success', 'Producto creado como borrador (publícalo cuando esté listo).');
      }
      navigate('/admin/products');
    } catch (err) {
      if (err instanceof AdminApiError) {
        // Envelope message inline; field-level details mapped to the form.
        setSubmitError(err.message);
        if (err.details) {
          for (const [field, message] of Object.entries(err.details)) {
            setError(field as keyof FormInput, { type: 'server', message });
          }
        }
      } else {
        setSubmitError('No se pudo guardar el producto. Verifica la conexión e inténtalo de nuevo.');
      }
    }
  };

  if (loadingEdit) {
    return <div className="py-16 text-center text-sm text-maison-warmgray">Cargando producto…</div>;
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <Link
          to="/admin/products"
          className="inline-flex items-center gap-1.5 text-sm text-maison-warmgray transition-colors hover:text-maison-ink"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver a productos
        </Link>
        <h1 className="mt-2 font-serif text-2xl text-maison-ink">
          {editId != null ? 'Editar producto' : 'Nuevo producto'}
        </h1>
        <p className="mt-1 text-sm text-maison-warmgray">
          {editId != null
            ? 'Actualiza los campos básicos del producto y luego administra variantes e imágenes más abajo.'
            : 'Los productos se crean como borradores y solo se publican cuando están activos.'}{' '}
        </p>
      </div>

      {submitError && (
        <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {submitError}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
        {/* Basic info */}
        <section className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
          <h2 className="mb-4 font-serif text-lg text-maison-ink">Información básica</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Nombre" htmlFor="name" required error={errors.name?.message}>
              <input id="name" className={inputClass} placeholder="Blusa de Seda Marfil" {...register('name')} />
            </Field>
            <Field label="Slug" htmlFor="slug" required error={errors.slug?.message}>
              <input id="slug" className={inputClass} placeholder="blusa-seda-marfil" {...register('slug')} />
            </Field>
            <Field label="Descripción corta" htmlFor="shortDescription" error={errors.shortDescription?.message}>
              <input
                id="shortDescription"
                className={inputClass}
                placeholder="Seda pura de 22 momme, corte fluido"
                {...register('shortDescription')}
              />
            </Field>
            <Field label="Prefijo de SKU" htmlFor="skuPrefix" error={errors.skuPrefix?.message}>
              <input id="skuPrefix" className={inputClass} placeholder="MAIS" {...register('skuPrefix')} />
            </Field>
            <div className="sm:col-span-2">
              <Field label="Descripción" htmlFor="description" error={errors.description?.message}>
                <textarea
                  id="description"
                  rows={3}
                  className={inputClass}
                  placeholder="Descripción completa del producto…"
                  {...register('description')}
                />
              </Field>
            </div>
            <Field label="Categoría" htmlFor="categoryId" error={errors.categoryId?.message}>
              <select id="categoryId" className={inputClass} {...register('categoryId')}>
                <option value="">Sin categoría</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Marca" htmlFor="brandId" error={errors.brandId?.message}>
              <select id="brandId" className={inputClass} {...register('brandId')}>
                <option value="">Sin marca</option>
                {brands.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </section>

        {/* Pricing */}
        <section className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
          <h2 className="mb-4 font-serif text-lg text-maison-ink">Precios</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Precio (PEN)" htmlFor="price" required error={errors.price?.message}>
              <input id="price" type="number" step="0.01" min="0" className={inputClass} {...register('price')} />
            </Field>
            <Field label="Precio de comparación" htmlFor="comparePrice" error={errors.comparePrice?.message}>
              <input
                id="comparePrice"
                type="number"
                step="0.01"
                min="0"
                className={inputClass}
                {...register('comparePrice')}
              />
            </Field>
            <Field label="Precio de costo" htmlFor="costPrice" error={errors.costPrice?.message}>
              <input id="costPrice" type="number" step="0.01" min="0" className={inputClass} {...register('costPrice')} />
            </Field>
            <Field label="Impuesto (%)" htmlFor="taxRate" error={errors.taxRate?.message}>
              <input id="taxRate" type="number" step="0.01" min="0" className={inputClass} {...register('taxRate')} />
            </Field>
            <Field label="Peso (kg)" htmlFor="weight" error={errors.weight?.message}>
              <input id="weight" type="number" step="0.01" min="0" className={inputClass} {...register('weight')} />
            </Field>
            <div className="grid grid-cols-3 gap-3">
              <Field label="L (cm)" htmlFor="lengthCm" error={errors.lengthCm?.message}>
                <input id="lengthCm" type="number" step="0.01" min="0" className={inputClass} {...register('lengthCm')} />
              </Field>
              <Field label="A (cm)" htmlFor="widthCm" error={errors.widthCm?.message}>
                <input id="widthCm" type="number" step="0.01" min="0" className={inputClass} {...register('widthCm')} />
              </Field>
              <Field label="Al (cm)" htmlFor="heightCm" error={errors.heightCm?.message}>
                <input id="heightCm" type="number" step="0.01" min="0" className={inputClass} {...register('heightCm')} />
              </Field>
            </div>
          </div>
        </section>

        {/* Inventory & flags */}
        <section className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
          <h2 className="mb-4 font-serif text-lg text-maison-ink">Inventario y atributos</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Umbral de stock bajo" htmlFor="lowStockThreshold" error={errors.lowStockThreshold?.message}>
              <input
                id="lowStockThreshold"
                type="number"
                step="1"
                min="0"
                className={inputClass}
                {...register('lowStockThreshold')}
              />
            </Field>
            <Field label="Cantidad mínima de pedido" htmlFor="minOrderQuantity" error={errors.minOrderQuantity?.message}>
              <input
                id="minOrderQuantity"
                type="number"
                step="1"
                min="0"
                className={inputClass}
                {...register('minOrderQuantity')}
              />
            </Field>
            <Field label="Cantidad máxima de pedido" htmlFor="maxOrderQuantity" error={errors.maxOrderQuantity?.message}>
              <input
                id="maxOrderQuantity"
                type="number"
                step="1"
                min="0"
                className={inputClass}
                {...register('maxOrderQuantity')}
              />
            </Field>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Checkbox
              label="Destacado (resaltar en la tienda)"
              checked={watch('isFeatured') ?? false}
              onChange={(checked) => setValue('isFeatured', checked)}
            />
            <Checkbox
              label="Novedad"
              checked={watch('isNew') ?? false}
              onChange={(checked) => setValue('isNew', checked)}
            />
            <Checkbox
              label="Eco-amigable"
              checked={watch('isEcoFriendly') ?? false}
              onChange={(checked) => setValue('isEcoFriendly', checked)}
            />
            <Checkbox
              label="Controlar stock"
              checked={watch('stockTracking') ?? false}
              onChange={(checked) => setValue('stockTracking', checked)}
            />
          </div>
        </section>

        {/* SEO */}
        <section className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
          <h2 className="mb-4 font-serif text-lg text-maison-ink">SEO</h2>
          <div className="grid grid-cols-1 gap-4">
            <Field label="Título SEO" htmlFor="seoTitle" error={errors.seoTitle?.message}>
              <input id="seoTitle" className={inputClass} {...register('seoTitle')} />
            </Field>
            <Field label="Descripción SEO" htmlFor="seoDescription" error={errors.seoDescription?.message}>
              <textarea id="seoDescription" rows={2} className={inputClass} {...register('seoDescription')} />
            </Field>
            <Field label="Meta palabras clave" htmlFor="metaKeywords" error={errors.metaKeywords?.message}>
              <input id="metaKeywords" className={inputClass} placeholder="seda, blusa, lujo" {...register('metaKeywords')} />
            </Field>
          </div>
        </section>

        {/* Variants & images (slice C) — only for an existing product */}
        {editId != null ? (
          <section className="space-y-6">
            <div className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
              <h2 className="mb-1 font-serif text-lg text-maison-ink">Variantes</h2>
              <p className="mb-4 text-sm text-maison-warmgray">
                Opciones de talla y color, con precio y stock por variante (cantidad disponible en el
                almacén principal). Una variante se marca como predeterminada.
              </p>
              <ProductVariantsEditor
                productId={editId}
                variants={detail?.variants ?? []}
                onChanged={reloadDetail}
              />
            </div>
            <div className="rounded-lg border border-maison-line bg-white p-6 shadow-card">
              <h2 className="mb-1 font-serif text-lg text-maison-ink">Imágenes</h2>
              <p className="mb-4 text-sm text-maison-warmgray">
                JPEG, PNG, WEBP o GIF de hasta 10MB. La primera imagen se convierte en la imagen
                principal; se mantiene exactamente una imagen principal por producto.
              </p>
              <ProductImagesManager
                productId={editId}
                images={detail?.images ?? []}
                onChanged={reloadDetail}
              />
            </div>
          </section>
        ) : (
          <p className="rounded-lg border border-maison-line bg-maison-warm px-4 py-3 text-sm text-maison-warmgray">
            Guarda primero el producto: luego podrás administrar variantes e imágenes aquí.
          </p>
        )}

        <div className="flex items-center justify-end gap-3">
          <Link
            to="/admin/products"
            className="rounded-md border border-maison-line bg-white px-4 py-2 text-sm text-maison-ink transition-colors hover:bg-maison-warm"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 rounded-md bg-maison-ink px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Save className="h-4 w-4" />
            {isSubmitting ? 'Guardando…' : editId != null ? 'Guardar cambios' : 'Crear borrador'}
          </button>
        </div>
      </form>
    </div>
  );
}
