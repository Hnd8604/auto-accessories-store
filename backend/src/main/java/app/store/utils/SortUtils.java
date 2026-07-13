package app.store.utils;

import org.springframework.data.domain.Sort;

public class SortUtils {

    public static Sort buildSort(String sort) {
        try {
            if (sort == null || sort.isEmpty()) {
                return Sort.by(Sort.Direction.ASC, "id");
            }

            String[] sortPart = sort.split(",");
            String sortField = sortPart[0].trim();
            String direction = sortPart.length > 1 ? sortPart[1].trim() : "asc";

            Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());

            return Sort.by(sortDirection, sortField);

        } catch (Exception e) {
            return Sort.by(Sort.Direction.ASC, "id"); // fallback an toàn
        }
    }
}
