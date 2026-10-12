import { Link } from 'react-router';

export default function Footer() {
  return (
    <footer className="mt-24 bg-maison-charcoal text-white/80">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-16 md:grid-cols-4 md:px-8">
        <div>
          <span className="font-serif-display text-3xl text-white">MAISON</span>
          <p className="mt-4 max-w-xs text-sm leading-relaxed text-white/60">
            Moda premium consciente. Piezas atemporales confeccionadas con los mejores materiales naturales.
          </p>
        </div>
        <div>
          <p className="label-micro text-white/50">Tienda</p>
          <ul className="mt-4 space-y-2 text-sm">
            <li><Link className="transition-colors hover:text-white" to="/catalogo/mujer">Mujer</Link></li>
            <li><Link className="transition-colors hover:text-white" to="/catalogo/hombre">Hombre</Link></li>
            <li><Link className="transition-colors hover:text-white" to="/catalogo/accesorios">Accesorios</Link></li>
            <li><Link className="transition-colors hover:text-white" to="/catalogo?filtro=ofertas">Ofertas</Link></li>
          </ul>
        </div>
        <div>
          <p className="label-micro text-white/50">Ayuda</p>
          <ul className="mt-4 space-y-2 text-sm">
            <li className="text-white/60">Envíos y devoluciones</li>
            <li className="text-white/60">Guía de tallas</li>
            <li className="text-white/60">Cuidado de prendas</li>
            <li className="text-white/60">Contacto</li>
          </ul>
        </div>
        <div>
          <p className="label-micro text-white/50">Atelier</p>
          <ul className="mt-4 space-y-2 text-sm text-white/60">
            <li>Av. José Pardo 231, Miraflores</li>
            <li>Lima, Perú</li>
            <li>hola@maison.pe</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-white/10 py-6 text-center text-xs text-white/40">
        © 2026 MAISON. Todos los derechos reservados.
      </div>
    </footer>
  );
}
