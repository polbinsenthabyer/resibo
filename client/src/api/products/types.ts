/**
 * Product contract — mirrors com.api.resibo.dtos.ProductDto / ProductListDto
 * and the product table in sql/tables/product.sql.
 *
 * ⚠️ Money is typed as `number` because the backend serialises its
 * BigDecimal column as a JSON number. That is lossy for arithmetic in
 * JavaScript, so totals must not be computed here — a POS that adds prices in
 * floating point eventually disagrees with its own receipts. Display what the
 * server sends, and let the server do the arithmetic. If the frontend ever has
 * to calculate, the right fix is to ask the backend to serialise decimals as
 * strings, not to work around it here.
 */
export interface Product {
  id: number
  productName: string
  productDescription: string | null
  productPrice: number
  productRemainingStocks: number
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

/**
 * Create-or-update payload. One endpoint does both: omit `id` to create, set
 * it to update. Audit fields are set by the server and are not accepted here.
 */
export interface ProductUpsertRequest {
  id?: number
  productName: string
  productDescription?: string
  /** Defaults to 0.00 server-side when omitted. */
  productPrice?: number
  /** Defaults to 0 server-side when omitted. */
  productRemainingStocks?: number
}

/** Sort keys the backend recognises. Anything else falls back to `id`. */
export type ProductSortBy = 'id' | 'productName' | 'productPrice' | 'productRemainingStocks'

/**
 * Filters, sorting and pagination for the listing. Every field is optional,
 * but the body must still be a JSON object — `{}` is valid, nothing is not.
 */
export interface ProductListRequest {
  productName?: string
  /** True for an exact name match; otherwise a contains-match. */
  isExactMatch?: boolean
  minPrice?: number
  maxPrice?: number
  inStockOnly?: boolean
  sortBy?: ProductSortBy
  sortDirection?: 'ASC' | 'DESC'
  /** Zero-based. */
  page?: number
  /** Clamped server-side to 1..200. */
  size?: number
}
