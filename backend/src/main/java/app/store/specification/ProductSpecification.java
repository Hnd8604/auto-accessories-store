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
            if (req.getKeyword() != null) {
                String pattern = "%" + req.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // danh mục phải đúng tên
            if (req.getCategory() != null) {
                predicates.add(cb.equal(root.get("category").get("name"), req.getCategory()));
            }

            // Thương hiệu phải đúng tên
            if (req.getBrand() != null) {
                predicates.add(cb.equal(root.get("brand").get("name"), req.getBrand()));
            }

            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), req.getMinPrice()));
            }

            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), req.getMaxPrice()));
            }

            if (req.getInStock() != null) {
                if (req.getInStock()) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(cb.equal(root.get("stockQuantity"), 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
