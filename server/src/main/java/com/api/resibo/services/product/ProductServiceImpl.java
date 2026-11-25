package com.api.resibo.services.product;

import com.api.resibo.dtos.PageDto;
import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.entities.Product;
import com.api.resibo.repositories.ProductRepository;
import com.api.resibo.utilities.MessageType;
import com.api.resibo.utilities.ResponseUtility;
import com.api.resibo.utilities.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SecurityUtil securityUtil;

    /**
     * Sortable fields, mapped from the API's field name to the database
     * column.
     *
     * <p>The mapping is required, not cosmetic: the listing runs as a
     * <b>native</b> query, so Spring Data appends the {@code ORDER BY} using
     * whatever string the {@code Sort} carries. Passing {@code productName}
     * straight through would produce {@code ORDER BY productName} and fail —
     * the column is {@code product_name}.
     *
     * <p>It doubles as the allow-list. An unrecognised value falls back to
     * {@code id} rather than throwing, so a typo cannot turn into a 500, and
     * nothing a caller sends ever reaches the SQL verbatim.
     */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "productName", "product_name",
            "productPrice", "product_price",
            "productRemainingStocks", "product_remaining_stocks",
            "createdAt", "created_at",
            "updatedAt", "updated_at");

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

        // Product names are unique. Checked here so the caller gets a usable
        // message; the database enforces it too (uq_product_name), which is
        // what actually makes it true — see the catch below.
        //
        // Case-insensitive, because the column collates as general_ci: renaming
        // "coffee" to "COFFEE" is not a clash with itself, but creating
        // "COFFEE" alongside an existing "coffee" is.
        Product clash = productRepository.findByProductName(name).orElse(null);
        if (clash != null && (dto.getId() == null || !clash.getId().equals(dto.getId()))) {
            return ResponseUtility.getErrorResponse("E0008", MessageType.ERROR_PRODUCT_NAME_ALREADY_EXISTS);
        }

        try {
            Product product;
            Date now = new Date();
            // Real username now that authentication exists; "manual" only if
            // somehow unauthenticated, which SecurityConfig should prevent.
            String actor = securityUtil.currentUsername();

            if (dto.getId() == null) {
                product = new Product();
                product.setCreatedBy(actor);
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
            product.setUpdatedBy(actor);
            product.setUpdatedAt(now);

            return ResponseUtility.getSuccessResponse(
                    MessageType.SUCCESSFULLY_SET_PRODUCT, productRepository.save(product));
        } catch (DataIntegrityViolationException e) {
            // The check above can lose a race: two concurrent creates both see
            // no clash, then one hits the unique index. The database is the
            // real arbiter, so translate its rejection into the same code
            // rather than reporting a generic failure.
            log.warn("setProduct hit a constraint violation for name={}", name);
            return ResponseUtility.getErrorResponse("E0008", MessageType.ERROR_PRODUCT_NAME_ALREADY_EXISTS);
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

            // Resolve to a column name; unknown values fall back to id.
            // dto.getSortBy() can be an explicit JSON null (Jackson overwrites
            // the field's "id" default), and Map.of(...)'s getOrDefault(null, ..)
            // throws NPE rather than returning the default — guard first.
            String sortColumn = dto.getSortBy() == null
                    ? "id" : SORT_COLUMNS.getOrDefault(dto.getSortBy(), "id");
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
                    PageRequest.of(page, size, Sort.by(direction, sortColumn)));

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
