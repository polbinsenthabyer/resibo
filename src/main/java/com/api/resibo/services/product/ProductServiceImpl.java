package com.api.resibo.services.product;

import com.api.resibo.dtos.PageDto;
import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.entities.Product;
import com.api.resibo.repositories.ProductRepository;
import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Audit actor until authentication exists. Matches the column default in
     * sql/tables/product.sql. Replace with the authenticated
     * principal once §1.3 of resibo-plan.md is answered and auth is built —
     * audit columns that always say "manual" record nothing useful.
     */
    private static final String AUDIT_ACTOR = "manual";

    /**
     * Sorting is restricted to real, indexed-or-cheap columns. An unknown
     * value falls back to id instead of throwing: callers should not be able
     * to turn a typo into a 500, and an open sort property lets a caller
     * order by anything the entity exposes.
     */
    private static final Set<String> SORTABLE =
            Set.of("id", "productName", "productPrice", "productRemainingStocks", "createdAt", "updatedAt");

    private static final int MAX_PAGE_SIZE = 200;

    @Override
    @Transactional
    public ResponseDto setProduct(ProductDto dto) {
        if (dto == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }

        String name = dto.getProductName() == null ? null : dto.getProductName().trim();
        if (name == null || name.isEmpty()) {
            return ResponseUtility.getErrorResponse("E0002", MessageType.ERROR_PRODUCT_NAME_REQUIRED);
        }
        // Checked here rather than left to the database: a column-length
        // violation surfaces as an opaque SQL error, not a usable message.
        if (name.length() > 100) {
            return ResponseUtility.getErrorResponse("E0003", MessageType.ERROR_PRODUCT_NAME_TOO_LONG);
        }
        if (dto.getProductDescription() != null && dto.getProductDescription().length() > 250) {
            return ResponseUtility.getErrorResponse("E0004", MessageType.ERROR_PRODUCT_DESCRIPTION_TOO_LONG);
        }
        if (dto.getProductPrice() != null && dto.getProductPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseUtility.getErrorResponse("E0005", MessageType.ERROR_PRODUCT_PRICE_NEGATIVE);
        }
        if (dto.getProductRemainingStocks() != null && dto.getProductRemainingStocks() < 0) {
            return ResponseUtility.getErrorResponse("E0006", MessageType.ERROR_PRODUCT_STOCKS_NEGATIVE);
        }

        try {
            Product product;
            Date now = new Date();

            if (dto.getId() == null) {
                product = new Product();
                product.setCreatedBy(AUDIT_ACTOR);
                product.setCreatedAt(now);
            } else {
                product = productRepository.findById(dto.getId()).orElse(null);
                if (product == null) {
                    return ResponseUtility.getErrorResponse("E0007", MessageType.ERROR_PRODUCT_NOT_EXIST);
                }
            }

            product.setProductName(name);
            product.setProductDescription(dto.getProductDescription());
            // Absent values fall back to the same defaults the column declares,
            // so creating via the API and inserting by hand agree.
            product.setProductPrice(dto.getProductPrice() == null
                    ? BigDecimal.ZERO : dto.getProductPrice());
            product.setProductRemainingStocks(dto.getProductRemainingStocks() == null
                    ? 0 : dto.getProductRemainingStocks());
            product.setUpdatedBy(AUDIT_ACTOR);
            product.setUpdatedAt(now);

            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_SET_PRODUCT, productRepository.save(product));
        } catch (Exception e) {
            // Logged, not returned: the caller gets a stable code, the detail
            // stays in the server log rather than going out over the wire.
            log.error("setProduct failed for id={}", dto.getId(), e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseDto listProduct(ProductListDto dto) {
        if (dto == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }

        try {
            int page = dto.getPage() == null || dto.getPage() < 0 ? 0 : dto.getPage();
            int size = dto.getSize() == null || dto.getSize() < 1
                    ? 10 : Math.min(dto.getSize(), MAX_PAGE_SIZE);

            String sortBy = dto.getSortBy() != null && SORTABLE.contains(dto.getSortBy())
                    ? dto.getSortBy() : "id";
            Sort.Direction direction = "DESC".equalsIgnoreCase(dto.getSortDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            String rawName = dto.getProductName() == null ? null : dto.getProductName().trim();
            String name = rawName == null || rawName.isEmpty() ? null : rawName;
            boolean exact = Boolean.TRUE.equals(dto.getIsExactMatch());

            Page<Product> result = productRepository.search(
                    exact ? name : null,
                    exact ? null : name,
                    dto.getMinPrice(),
                    dto.getMaxPrice(),
                    Boolean.TRUE.equals(dto.getInStockOnly()) ? 1 : null,
                    PageRequest.of(page, size, Sort.by(direction, sortBy)));

            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_FETCHED_DATA, PageDto.of(result));
        } catch (Exception e) {
            log.error("listProduct failed", e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseDto getProductById(Long productId) {
        if (productId == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseUtility.getErrorResponse("E0007", MessageType.ERROR_PRODUCT_NOT_EXIST);
        }
        return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_FETCHED_DATA, product);
    }

    @Override
    @Transactional
    public ResponseDto deleteProductById(Long productId) {
        if (productId == null) {
            return ResponseUtility.getErrorResponse("E0001", MessageType.ERROR_DTO_IS_NULL_OR_INCOMPLETE);
        }
        if (!productRepository.existsById(productId)) {
            return ResponseUtility.getErrorResponse("E0007", MessageType.ERROR_PRODUCT_NOT_EXIST);
        }
        try {
            productRepository.deleteById(productId);
            return ResponseUtility.getSuccessResponse(MessageType.SUCCESSFULLY_DELETED_PRODUCT);
        } catch (Exception e) {
            log.error("deleteProductById failed for id={}", productId, e);
            return ResponseUtility.getErrorResponse("E9999", MessageType.ERROR_SOMETHING_WENT_WRONG);
        }
    }
}
