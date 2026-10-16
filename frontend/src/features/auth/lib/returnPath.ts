const RETURN_PATH_KEY = "auth:returnPath";

function safeInternalPath(path: string | null | undefined) {
  return path?.startsWith("/") && !path.startsWith("//") && !path.includes("\\")
    ? path
    : undefined;
}

export function returnPathFromState(state: unknown) {
  if (!state || typeof state !== "object" || !("from" in state)) return undefined;
  const from = state.from;
  if (!from || typeof from !== "object" || !("pathname" in from)) return undefined;
  if (typeof from.pathname !== "string") return undefined;

  const search = "search" in from && typeof from.search === "string" ? from.search : "";
  const hash = "hash" in from && typeof from.hash === "string" ? from.hash : "";
  return safeInternalPath(`${from.pathname}${search}${hash}`);
}

export function rememberReturnPath(path: string | undefined) {
  if (path) sessionStorage.setItem(RETURN_PATH_KEY, path);
  else sessionStorage.removeItem(RETURN_PATH_KEY);
}

export function takeRememberedReturnPath() {
  const path = sessionStorage.getItem(RETURN_PATH_KEY);
  sessionStorage.removeItem(RETURN_PATH_KEY);
  return safeInternalPath(path);
}
