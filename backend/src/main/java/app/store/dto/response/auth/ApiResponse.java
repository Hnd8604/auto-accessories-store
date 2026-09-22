package app.store.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // các field null không thêm vào json
public record ApiResponse<T>(
        int code,
        String message,
        T result) {
    public static final int SUCCESS_CODE = 1000;

    public ApiResponse {
        // builder không truyền code thì int mặc định là 0 -> dùng mã thành công 1000
        if (code == 0) {
            code = SUCCESS_CODE;
        }
    }
}
