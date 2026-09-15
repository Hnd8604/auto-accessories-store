package app.store.dto.request;

import lombok.Builder;

@Builder
public record EmailRequest(
        String to,
        String subject,
        String text,
        String htmlText,
        boolean isHtml
) {
}
