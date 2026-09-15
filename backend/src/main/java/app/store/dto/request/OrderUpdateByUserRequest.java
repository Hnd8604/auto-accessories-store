package app.store.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class OrderUpdateByUserRequest {
    String nameRecipient;
    String phoneRecipient;
    String addressRecipient;
    String note;
}