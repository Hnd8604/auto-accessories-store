package app.store.dto.request;

import app.store.dto.request.user.UserUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private OrderCreationRequest.OrderCreationRequestBuilder validOrder() {
        return OrderCreationRequest.builder()
                .nameRecipient("Nguyen Van A")
                .phoneRecipient("0912345678")
                .addressRecipient("1 Le Loi, Q1")
                .orderDetails(List.of(new OrderDetailRequest(1L, 2)));
    }

    private Set<String> messages(Object target) {
        return validator.validate(target).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void orderCreation_valid_hasNoViolations() {
        assertThat(validator.validate(validOrder().build())).isEmpty();
    }

    @Test
    void orderCreation_emptyDetails_isRejected() {
        assertThat(messages(validOrder().orderDetails(List.of()).build()))
                .containsExactly("Order must contain at least one item");
    }

    @Test
    void orderCreation_cascadesIntoDetails() {
        OrderCreationRequest request = validOrder()
                .orderDetails(List.of(new OrderDetailRequest(1L, 0)))
                .build();

        assertThat(messages(request)).containsExactly("Quantity must be greater than 0");
    }

    @Test
    void orderCreation_acceptsVietnamesePhoneFormats() {
        assertThat(validator.validate(validOrder().phoneRecipient("+84912345678").build())).isEmpty();
        assertThat(validator.validate(validOrder().phoneRecipient("02838123456").build())).isEmpty();
        assertThat(messages(validOrder().phoneRecipient("12345").build()))
                .containsExactly("Recipient phone is invalid");
    }

    @Test
    void userUpdate_allowsLongUsernameGeneratedFromGoogleEmail() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("a.very.long.google.account.name_1a2b3c")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void userUpdate_shortPassword_isRejected() {
        UserUpdateRequest request = UserUpdateRequest.builder().username("john").password("1234567").build();

        assertThat(messages(request)).containsExactly("Password must be at least 8 characters");
    }

    @Test
    void userUpdate_missingUsername_isRejected() {
        // PUT thay thế toàn bộ resource và users.username là NOT NULL
        UserUpdateRequest request = UserUpdateRequest.builder().build();

        assertThat(messages(request)).containsExactly("Username is required");
    }

    @Test
    void categoryBrands_nullElement_isRejected() {
        CategoryBrandsRequest request = new CategoryBrandsRequest(Arrays.asList(1L, null));

        assertThat(messages(request)).containsExactly("Brand ID must not be null");
    }
}
