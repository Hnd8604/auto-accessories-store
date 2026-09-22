// Keep only codes that drive client behavior here. The backend ErrorCode enum is
// the source of truth; messages are display text and must not be used for branching.
export const ERROR_CODE = {
  CONVERSATION_NOT_EXISTED: 8001,
  CONVERSATION_CLOSED: 8002,
} as const;
