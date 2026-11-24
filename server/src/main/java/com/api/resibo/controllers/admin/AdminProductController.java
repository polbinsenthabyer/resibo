package com.api.resibo.controllers.admin;

import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Products CRUD, administrator side.
 *
 * <p>Create and update are one operation — {@code /set} — distinguished by
 * whether the payload carries an id.
 *
 * <p>Mounted under {@code /api/admin/**}, which {@code SecurityConfig}
 * restricts to the ADMIN role. An EMPLOYEE token reaching these endpoints
 * gets 403, an absent or invalid token gets 401 — neither reaches this class.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    /** Create when id is null, update when it is set. */
    @PostMapping("/set")
    public ResponseDto setProduct(@RequestBody ProductDto dto) {
        return productService.setProduct(dto);
    }

    /**
     * Filtered, sorted, paginated listing.
     *
     * <p>POST rather than GET, despite being a read. The filter and pagination
     * payload belongs in a request body, and a GET body is not reliably sent
     * by proxies, browsers or HTTP clients — gainsoclock pairs GET with
     * {@code @RequestBody}, which works until something in the path drops it.
     */
    @PostMapping("/list")
    public ResponseDto listProduct(@RequestBody ProductListDto dto) {
        return productService.listProduct(dto);
    }

    @GetMapping("/get/{productId}")
    public ResponseDto getProductById(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    @DeleteMapping("/delete/{productId}")
    public ResponseDto deleteProductById(@PathVariable Long productId) {
        return productService.deleteProductById(productId);
    }
}
