package com.api.resibo.services.product;

import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;

public interface ProductService {

    /**
     * Creates a product when {@code dto.id} is null, updates that product when
     * it is set. There is no separate update operation by design.
     */
    ResponseDto setProduct(ProductDto dto);

    /** Filtered, sorted, paginated listing. Data is a {@code PageDto<Product>}. */
    ResponseDto listProduct(ProductListDto dto);

    /** Single product by id. */
    ResponseDto getProductById(Long productId);

    /** Hard delete by id. */
    ResponseDto deleteProductById(Long productId);
}
