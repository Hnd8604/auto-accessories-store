/**
 * Đọc claim `exp` (giây) từ payload JWT để biết token sắp hết hạn chưa.
 * Không kiểm tra chữ ký — việc đó do server làm; ở đây chỉ để quyết định có refresh trước hay không.
 * Token không đọc được thì coi như đã hết hạn.
 */
export function isTokenExpiringSoon(token: string, skewSeconds = 0): boolean {
  try {
    const payloadPart = token.split(".")[1];
    if (!payloadPart) return true;

    const base64 = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const payload: { exp?: unknown } = JSON.parse(atob(padded));

    if (typeof payload.exp !== "number") return true;
    return payload.exp * 1000 <= Date.now() + skewSeconds * 1000;
  } catch {
    return true;
  }
}
