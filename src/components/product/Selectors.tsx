import { motion } from 'framer-motion';
import { Minus, Plus } from 'lucide-react';

export function ColorSwatches({
  colors,
  selected,
  onSelect,
}: {
  colors: { name: string; hex: string }[];
  selected: string;
  onSelect: (name: string) => void;
}) {
  return (
    <div className="flex items-center gap-3">
      {colors.map((c) => {
        const active = selected === c.name;
        return (
          <motion.button
            key={c.name}
            title={c.name}
            aria-label={`Color ${c.name}`}
            onClick={() => onSelect(c.name)}
            whileHover={{ scale: 1.15 }}
            whileTap={{ scale: 0.95 }}
            className={`h-10 w-10 rounded-full border-2 transition-shadow ${
              active
                ? 'ring-[3px] ring-maison-ink ring-offset-2 ring-offset-white border-maison-line'
                : 'border-maison-line hover:shadow-[0_4px_12px_rgba(0,0,0,0.1)]'
            }`}
            style={{ backgroundColor: c.hex }}
          />
        );
      })}
    </div>
  );
}

export function SizeSelector({
  sizes,
  selected,
  onSelect,
}: {
  sizes: { label: string; available: boolean }[];
  selected: string | null;
  onSelect: (label: string) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2.5">
      {sizes.map((s) => {
        const active = selected === s.label;
        return (
          <motion.button
            key={s.label}
            disabled={!s.available}
            onClick={() => onSelect(s.label)}
            whileTap={s.available ? { scale: 0.96 } : undefined}
            className={`rounded-full px-6 py-3 text-sm transition-all duration-200 ${
              !s.available
                ? 'cursor-not-allowed bg-transparent text-maison-muted line-through'
                : active
                  ? 'scale-[1.02] bg-maison-ink text-white'
                  : 'bg-maison-gray text-maison-soft hover:bg-maison-line'
            }`}
          >
            {s.label}
          </motion.button>
        );
      })}
    </div>
  );
}

export function QtyStepper({ qty, onChange }: { qty: number; onChange: (q: number) => void }) {
  return (
    <div className="inline-flex items-center rounded-full border border-maison-line">
      <button
        aria-label="Disminuir cantidad"
        onClick={() => onChange(Math.max(1, qty - 1))}
        className="flex h-8 w-8 items-center justify-center rounded-full transition-colors hover:bg-maison-gray m-1"
      >
        <Minus size={16} strokeWidth={1.5} />
      </button>
      <span className="w-12 text-center text-sm font-normal">{qty}</span>
      <button
        aria-label="Aumentar cantidad"
        onClick={() => onChange(qty + 1)}
        className="flex h-8 w-8 items-center justify-center rounded-full transition-colors hover:bg-maison-gray m-1"
      >
        <Plus size={16} strokeWidth={1.5} />
      </button>
    </div>
  );
}
