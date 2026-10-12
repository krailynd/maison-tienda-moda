import { useRef, useState } from 'react';
import { ImagePlus, Loader2, Star, Trash2 } from 'lucide-react';
import {
  AdminApiError,
  deleteAdminImage,
  setPrimaryAdminImage,
  uploadAdminImage,
} from '@/lib/adminApi';
import { useUIStore } from '@/stores/uiStore';

const ACCEPTED_TYPES = 'image/jpeg,image/png,image/webp,image/gif';
const MAX_SIZE_BYTES = 10 * 1024 * 1024;

export interface ProductImageItem {
  id: number;
  url: string;
  altText: string | null;
  primary: boolean;
}

interface Props {
  productId: number;
  images: ProductImageItem[];
  onChanged: () => void;
}

/**
 * Image manager (slice C): gallery of the product's images with upload,
 * "set primary" (exactly one primary per product) and delete (removes the
 * physical file too). Thumbnails are not generated yet — the full image is
 * shown.
 */
export default function ProductImagesManager({ productId, images, onChanged }: Props) {
  const pushToast = useUIStore((s) => s.pushToast);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [altText, setAltText] = useState('');
  const [uploading, setUploading] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    if (file.size > MAX_SIZE_BYTES) {
      setError('El archivo supera el límite de 10MB.');
      return;
    }
    setError(null);
    setUploading(true);
    try {
      await uploadAdminImage(productId, file, { altText: altText.trim() || undefined });
      pushToast('success', 'Imagen subida.');
      setAltText('');
      if (fileInputRef.current) fileInputRef.current.value = '';
      onChanged();
    } catch (err) {
      setError(err instanceof AdminApiError ? err.message : 'No se pudo subir la imagen.');
    } finally {
      setUploading(false);
    }
  };

  const makePrimary = async (image: ProductImageItem) => {
    if (image.primary) return;
    setBusyId(image.id);
    try {
      await setPrimaryAdminImage(image.id);
      pushToast('success', 'Imagen principal actualizada.');
      onChanged();
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo completar la acción.');
    } finally {
      setBusyId(null);
    }
  };

  const remove = async (image: ProductImageItem) => {
    if (!window.confirm('¿Eliminar esta imagen? También se elimina el archivo del servidor.')) {
      return;
    }
    setBusyId(image.id);
    try {
      await deleteAdminImage(image.id);
      pushToast('success', 'Imagen eliminada.');
      onChanged();
    } catch (err) {
      pushToast('error', err instanceof AdminApiError ? err.message : 'No se pudo eliminar.');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-4">
      {error && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {error}
        </div>
      )}

      {images.length === 0 ? (
        <p className="text-sm text-maison-warmgray">Aún no hay imágenes — sube la primera (se convertirá en la principal).</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {images.map((image) => {
            const busy = busyId === image.id;
            return (
              <div
                key={image.id}
                className="group relative overflow-hidden rounded-lg border border-maison-line bg-maison-warm"
              >
                <img
                  src={image.url}
                  alt={image.altText ?? 'Imagen del producto'}
                  className="aspect-square w-full object-cover"
                />
                {image.primary && (
                  <span className="absolute left-2 top-2 inline-flex items-center gap-1 rounded-full bg-maison-ink/85 px-2 py-0.5 text-xs font-medium text-white">
                    <Star className="h-3 w-3 fill-current" />
                    Principal
                  </span>
                )}
                {image.altText && (
                  <p className="absolute bottom-0 left-0 right-0 truncate bg-maison-ink/60 px-2 py-1 text-xs text-white">
                    {image.altText}
                  </p>
                )}
                <div className="absolute inset-0 flex items-center justify-center gap-2 bg-maison-ink/40 opacity-0 transition-opacity group-hover:opacity-100">
                  <button
                    type="button"
                    title={image.primary ? 'Ya es la principal' : 'Marcar como principal'}
                    disabled={busy || image.primary}
                    onClick={() => makePrimary(image)}
                    className="rounded-md bg-white/95 p-2 text-maison-ink transition-colors hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Star className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    title="Eliminar imagen"
                    disabled={busy}
                    onClick={() => remove(image)}
                    className="rounded-md bg-white/95 p-2 text-red-600 transition-colors hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Upload */}
      <div className="flex flex-wrap items-end gap-3 rounded-md border border-dashed border-maison-line bg-maison-warm/40 p-4">
        <label className="block text-xs text-maison-ink">
          Texto alternativo
          <input
            className="mt-1 w-56 rounded-md border border-maison-line bg-white px-2 py-1.5 text-sm text-maison-ink outline-none focus:border-maison-charcoal"
            value={altText}
            onChange={(e) => setAltText(e.target.value)}
            placeholder="Blusa frente"
          />
        </label>
        <input
          ref={fileInputRef}
          type="file"
          accept={ACCEPTED_TYPES}
          className="hidden"
          onChange={(e) => handleFile(e.target.files?.[0])}
        />
        <button
          type="button"
          disabled={uploading}
          onClick={() => fileInputRef.current?.click()}
          className="flex items-center gap-2 rounded-md bg-maison-ink px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-maison-soft disabled:cursor-not-allowed disabled:opacity-60"
        >
          {uploading ? <Loader2 className="h-4 w-4 animate-spin" /> : <ImagePlus className="h-4 w-4" />}
          {uploading ? 'Subiendo…' : 'Subir imagen'}
        </button>
        <p className="text-xs text-maison-warmgray">JPEG, PNG, WEBP o GIF · máx. 10MB</p>
      </div>
    </div>
  );
}
