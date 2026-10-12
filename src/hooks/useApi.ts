import { useEffect, useState } from 'react';
import {
  fetchCategories,
  fetchOrder,
  fetchProductBySlug,
  fetchProducts,
  getErrorMessage,
  toCategory,
  type Category,
  type OrderResponse,
  type ProductDetail,
} from '@/lib/api';
import type { Product } from '@/lib/data';

export function useProducts(size = 20) {
  const [products, setProducts] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    fetchProducts(size)
      .then(({ products: list, total: count }) => {
        if (!active) return;
        setProducts(list);
        setTotal(count);
      })
      .catch((e) => {
        if (!active) return;
        setError(getErrorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [size]);

  return { products, total, loading, error };
}

export function useProduct(slug?: string) {
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) {
      setProduct(null);
      setLoading(false);
      setError(null);
      return;
    }
    let active = true;
    setLoading(true);
    setError(null);
    fetchProductBySlug(slug)
      .then((detail) => {
        if (active) setProduct(detail);
      })
      .catch((e) => {
        if (active) setError(getErrorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [slug]);

  return { product, loading, error };
}

export function useCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    fetchCategories()
      .then((dtos) => {
        if (!active) return;
        const roots = dtos
          .filter((c) => c.parentId == null)
          .map(toCategory)
          .filter((c) => c.productCount > 0);
        setCategories(roots);
      })
      .catch((e) => {
        if (!active) return;
        setError(getErrorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return { categories, loading, error };
}

export function useOrder(orderNumber?: string) {
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderNumber) {
      setOrder(null);
      setLoading(false);
      setError(null);
      return;
    }
    let active = true;
    setLoading(true);
    setError(null);
    fetchOrder(orderNumber)
      .then((data) => {
        if (active) setOrder(data);
      })
      .catch((e) => {
        if (active) setError(getErrorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [orderNumber]);

  return { order, loading, error };
}
