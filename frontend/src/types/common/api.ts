// Core API wrapper types
export interface ApiResponse<T> {
  code?: number;
  message?: string;
  result?: T;
}

// Pagination types
export interface SortObject {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface PageableObject {
  offset: number;
  sort: SortObject;
  paged: boolean;
  pageNumber: number;
  pageSize: number;
  unpaged: boolean;
}

export interface PageResponse<T> {
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
  size: number;
  content: T[];
  number: number;
  sort: SortObject;
  numberOfElements: number;
  pageable: PageableObject;
  empty: boolean;
}

// Pagination request params
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
