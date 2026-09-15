package app.store.specification;

import app.store.dto.request.ProductSearchRequest;
import app.store.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> toSpecification(ProductSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // sản phẩm thì có tên hoặc description là được tìm thấy
            if (req.keyword() != null) {
                String pattern = "%" + req.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // danh mục phải đúng tên
            if (req.category() != null) {
                predicates.add(cb.equal(root.get("category").get("name"), req.category()));
            }

            // Thương hiệu phải đúng tên
            if (req.brand() != null) {
                predicates.add(cb.equal(root.get("brand").get("name"), req.brand()));
            }

            if (req.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), req.minPrice()));
            }

            if (req.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), req.maxPrice()));
            }

            if (req.inStock() != null) {
                if (req.inStock()) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(cb.equal(root.get("stockQuantity"), 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
