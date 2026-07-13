// Post Response Types
export interface PostResponse {
  id: number;
  title: string;
  slug: string;
  shortDescription?: string;
  thumbnailUrl?: string;
  content: string;
  published: boolean;
  viewCount?: number;
  createdAt?: string;
  updatedAt?: string;
  categoryName?: string;
  authorId?: string;
  authorName?: string;
}

// Post Request Types
export interface PostRequest {
  title: string;
  shortDescription?: string;
  content: string;
  published: boolean;
  categoryId: number;
}
