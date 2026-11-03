// Cả hai HTTP client trong services/axios.ts đều bắt AxiosError rồi ném lại
// `new Error(message)`, nên thứ tới tay caller luôn là Error thuần. Helper này
// thu hẹp kiểu `unknown` của biến catch mà không cần cast `any`.
export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message;
  if (typeof error === "string" && error) return error;
  return fallback;
}
