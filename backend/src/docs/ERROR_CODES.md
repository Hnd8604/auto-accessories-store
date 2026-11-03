# Error code convention

API failures use a stable four-digit application code in `ApiResponse.code`.
The HTTP status describes the transport outcome; the application code identifies
the exact failure and is what clients should use for conditional behavior.
[`ErrorCode`](../main/java/app/store/exception/ErrorCode.java) is the source of
truth for the current catalog.

## Format

`DNNN`

- `D` is the owning domain.
- `NNN` is a sequence allocated by that domain. Related features should use a
  contiguous sub-range (for example, cart errors use `31xx`).
- `1000` is reserved for successful responses and is not an error code.

| Range | Owner |
| --- | --- |
| `1xxx` | Identity and access |
| `2xxx` | Product catalog |
| `3xxx` | Commerce: orders, carts, and payments |
| `4xxx` | Content management |
| `5xxx` | External integrations (reserved) |
| `6xxx` | Notifications |
| `7xxx` | Professional services |
| `8xxx` | Live chat |
| `9xxx` | HTTP/request processing and platform failures |

## Rules

1. Every error code is globally unique.
2. Published codes are immutable and must never be reused for a different
   meaning. Deprecate an obsolete enum entry instead of recycling its number.
3. Add new codes to `ErrorCode` in the range documented by the comments in that
   file. No separate domain type is required.
4. Name enum entries as `<RESOURCE>_<CONDITION>` and use the enum everywhere;
   do not put numeric literals in backend business logic.
5. Do not derive application codes from HTTP statuses. Several application
   failures can intentionally share the same HTTP status.
6. Messages are for people and may change or be localized. Clients must branch
   on `code`, never on `message`.
7. Unexpected internal failures return `9999`; logs retain the underlying
   exception, while responses must not expose internal details.

`ErrorCodeTest` enforces uniqueness and the four-digit format. A pull request
that duplicates a code or uses an invalid format will fail the backend test
suite.

## Migration note

Introducing this convention reallocates legacy codes that were duplicated or
owned by the wrong domain. Any external client that compares numeric error codes
must update against the current `ErrorCode` catalog. The in-repository live-chat
client codes (`8001` and `8002`) are unchanged.
