# MAISON — Tienda de Moda Premium

> Frontend completo de una tienda de moda online premium: cinematográfico, táctil y elegante. React + TypeScript + Tailwind, con spring physics, smooth scroll y fotografía editorial generada con IA.

![Hero](public/images/hero.png)

## Qué es

**MAISON** es una experiencia e-commerce de lujo construida como SPA. Cada interacción usa física de resortes (spring physics), transiciones con desenfoque y glassmorphism para transmitir calidad a través de la interfaz. Incluye el flujo de compra completo: catálogo → producto → carrito → checkout de 5 pasos → confirmación.

## Stack

| Área | Tecnología |
|---|---|
| Core | React 19, TypeScript estricto, Vite 7 |
| Estilos | Tailwind CSS 3.4 (design tokens propios), shadcn/ui (base), Radix UI |
| Motion | Framer Motion, GSAP 3 + ScrollTrigger, Lenis (lerp 0.1) |
| Estado | Zustand (carrito + UI, persistidos en localStorage) |
| Iconos | Lucide React (stroke 1.5px) |
| Tipografías | Playfair Display (serif) + Inter (sans) |

## Secciones implementadas

- **Preloader de marca** — logo animado, barra de progreso champagne, salida con blur y textura grain
- **Header glassmorphism** — transparente arriba, blur al hacer scroll; mega menú con lookbook (hover con delay 150 ms); menú móvil drawer con stagger
- **Home** — hero 100vh con parallax y scroll indicator, tarjetas de categoría con hover 3D, carrusel "Nuevos Lanzamientos" arrastrable, editorial con parallax GSAP, newsletter con confetti
- **Catálogo** — banner 40vh con parallax, breadcrumb pill, filtros, ordenamiento animado, toggle grid/lista, quick view modal
- **Producto (PDP)** — zoom con lupa que sigue el mouse, swatches de color, selector de tallas, stepper, botón con estados loading/success, wishlist con partículas, acordeones spring
- **Carrito** — mini-cart drawer con barra de envío gratis animada; página con resumen sticky y cupón (−10%)
- **Checkout** — stepper de 5 pasos animado, validación de formularios, métodos de envío/pago, confirmación con checkmark dibujado + confetti y n° de pedido copiable
- **Global** — toasts glassmorphism, page transitions, shimmer orgánico en skeletons, `prefers-reduced-motion`

## Inicio rápido

```bash
npm install
npm run dev      # http://localhost:3000
npm run build    # producción → dist/
```

Requiere Node.js 20+.

## Recursos (imágenes)

Las 13 fotografías editoriales y de producto viven en `public/images/` y fueron generadas con IA con un grading cálido coherente (crema, champagne, terracotta). Si clonas el repo y falta esa carpeta, restaurala desde el paquete comprimido incluido en `release/` (ver abajo).

## Paquete completo (.zip)

En `release/` encontrarás el proyecto completo comprimido (código + imágenes), codificado en base64 en partes por límites de la API. Para restaurarlo:

```bash
bash release/restore-zip.sh        # genera maison-tienda-moda.zip
unzip maison-tienda-moda.zip
```

## Estructura

```
src/
├── components/
│   ├── layout/       Header, Footer
│   ├── product/      ProductCard, QuickView, Selectors
│   ├── cart/         MiniCart (drawer)
│   ├── ui/           shadcn/ui (base)
│   ├── Preloader.tsx
│   └── Toasts.tsx
├── pages/            Home, Catalogo, Producto, Carrito, Checkout
├── stores/           cartStore, uiStore (Zustand + persist)
├── lib/              data.ts (catálogo), animations.ts (variants), utils.ts
├── hooks/
└── index.css         Design tokens MAISON
```

## Design tokens

- Fondos: `#FFFFFF` / `#FAF9F6` / `#F5F0EB` / `#F0EEEB` / `#1C1C1E`
- Texto: `#0F0F0F` / `#2D2D2D` / `#6B6B6B` / `#9E9E9E`
- Acentos: Sage `#A8B5A0`, Terracotta `#C4956A`, Dusty Rose `#C9A9A6`
- Gradientes: Rose Gold, Champagne, Midnight (135deg)
- Radios: 12 / 16 / 24 / 32 px y pill (9999px)

## Nota sobre datos

Es un frontend puro: carrito y favoritos se persisten en `localStorage` del navegador (no se sincronizan entre dispositivos). No hay backend ni pasarela de pago real — el checkout es una simulación completa del flujo.
