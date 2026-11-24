package com.api.resibo.services.product;

import com.api.resibo.dtos.PageDto;
import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.entities.Product;
import com.api.resibo.repositories.ProductRepository;
import com.api.resibo.utilities.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the product service. No Spring context and no database —
 * these cover the validation, defaulting and pagination rules, which is where
 * the logic actually lives.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ProductServiceImpl service;

    // --- setProduct: validation ------------------------------------------

    @Test
    void setRejectsNullDto() {
        ResponseDto res = service.setProduct(null);
        assertThat(res.isFail()).isTrue();
        assertThat(res.getCode()).isEqualTo("E0001");
        verify(productRepository, never()).save(any());
    }

    @Test
    void setRejectsBlankName() {
        ProductDto dto = new ProductDto();
        dto.setProductName("   ");
        ResponseDto res = service.setProduct(dto);
        assertThat(res.getCode()).isEqualTo("E0002");
        verify(productRepository, never()).save(any());
    }

    @Test
    void setRejectsNameLongerThanTheColumn() {
        ProductDto dto = new ProductDto();
        dto.setProductName("x".repeat(101));
        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0003");
    }

    @Test
    void setRejectsNegativePrice() {
        ProductDto dto = new ProductDto();
        dto.setProductName("Coffee");
        dto.setProductPrice(new BigDecimal("-0.01"));
        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0005");
    }

    @Test
    void setRejectsNegativeStocks() {
        ProductDto dto = new ProductDto();
        dto.setProductName("Coffee");
        dto.setProductRemainingStocks(-1);
        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0006");
    }

    // --- setProduct: name uniqueness --------------------------------------

    @Test
    void setRejectsCreateWhenTheNameIsTaken() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setProductName("Coffee");
        when(productRepository.findByProductName("Coffee")).thenReturn(Optional.of(existing));

        ProductDto dto = new ProductDto();
        dto.setProductName("Coffee");

        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0008");
        verify(productRepository, never()).save(any());
    }

    @Test
    void setRejectsUpdateThatStealsAnotherProductsName() {
        Product other = new Product();
        other.setId(2L);
        other.setProductName("Coffee");
        when(productRepository.findByProductName("Coffee")).thenReturn(Optional.of(other));

        ProductDto dto = new ProductDto();
        dto.setId(1L);                 // a different product
        dto.setProductName("Coffee");  // trying to take id=2's name

        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0008");
        verify(productRepository, never()).save(any());
    }

    @Test
    void setAllowsAProductToKeepItsOwnNameOnUpdate() {
        Product self = new Product();
        self.setId(1L);
        self.setProductName("Coffee");
        when(productRepository.findByProductName("Coffee")).thenReturn(Optional.of(self));
        when(productRepository.findById(1L)).thenReturn(Optional.of(self));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductDto dto = new ProductDto();
        dto.setId(1L);
        dto.setProductName("Coffee");   // unchanged — must not clash with itself
        dto.setProductPrice(new BigDecimal("99.00"));

        assertThat(service.setProduct(dto).isSuccess()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void setTranslatesAConstraintViolationIntoTheDuplicateCode() {
        // Simulates losing the race: the pre-check sees nothing, the database
        // then rejects the insert on uq_product_name.
        when(productRepository.findByProductName("Coffee")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate entry"));

        ProductDto dto = new ProductDto();
        dto.setProductName("Coffee");

        ResponseDto res = service.setProduct(dto);
        assertThat(res.getCode()).isEqualTo("E0008");
        assertThat(res.isFail()).isTrue();
    }

    // --- setProduct: create vs update -------------------------------------

    @Test
    void setWithoutIdCreatesAndAppliesColumnDefaults() {
        when(securityUtil.currentUsername()).thenReturn("manual");
        ProductDto dto = new ProductDto();
        dto.setProductName("  Barako Coffee  ");          // trimmed by the service
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ResponseDto res = service.setProduct(dto);
        assertThat(res.isSuccess()).isTrue();

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        Product p = saved.getValue();

        assertThat(p.getProductName()).isEqualTo("Barako Coffee");
        assertThat(p.getProductPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getProductRemainingStocks()).isZero();
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getCreatedBy()).isEqualTo("manual");
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    void setWithIdUpdatesWithoutTouchingCreationAudit() {
        Product existing = new Product();
        existing.setId(7L);
        existing.setCreatedBy("someone-else");
        java.util.Date originalCreatedAt = new java.util.Date(0);
        existing.setCreatedAt(originalCreatedAt);
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductDto dto = new ProductDto();
        dto.setId(7L);
        dto.setProductName("Renamed");
        dto.setProductPrice(new BigDecimal("12.50"));

        assertThat(service.setProduct(dto).isSuccess()).isTrue();

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        Product p = saved.getValue();

        assertThat(p.getProductName()).isEqualTo("Renamed");
        assertThat(p.getProductPrice()).isEqualByComparingTo("12.50");
        assertThat(p.getCreatedBy()).isEqualTo("someone-else");
        assertThat(p.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void setWithUnknownIdDoesNotSilentlyCreate() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());
        ProductDto dto = new ProductDto();
        dto.setId(404L);
        dto.setProductName("Ghost");

        assertThat(service.setProduct(dto).getCode()).isEqualTo("E0007");
        verify(productRepository, never()).save(any());
    }

    // --- listProduct: pagination and sorting ------------------------------

    @Test
    void listClampsPageSizeAndFallsBackToIdForUnknownSort() {
        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 200), 0));

        ProductListDto dto = new ProductListDto();
        dto.setSize(10_000);                 // over the cap
        dto.setPage(-5);                     // negative
        dto.setSortBy("; DROP TABLE product");  // not a sortable property

        assertThat(service.listProduct(dto).isSuccess()).isTrue();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).search(any(), any(), any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getPageSize()).isEqualTo(200);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void listSendsExactNameOnlyWhenExactMatchRequested() {
        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        ProductListDto dto = new ProductListDto();
        dto.setProductName("Coffee");
        dto.setIsExactMatch(true);
        service.listProduct(dto);

        verify(productRepository).search(
                org.mockito.ArgumentMatchers.eq("Coffee"),
                org.mockito.ArgumentMatchers.isNull(),
                any(), any(), any(), any(Pageable.class));
    }

    @Test
    void listHonoursDescendingDirection() {
        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        ProductListDto dto = new ProductListDto();
        dto.setSortBy("productPrice");
        dto.setSortDirection("desc");
        service.listProduct(dto);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).search(any(), any(), any(), any(), any(), pageable.capture());

        // The API field name is productPrice, but the Sort must carry the
        // COLUMN name: the listing is a native query, so Spring Data appends
        // this string straight into ORDER BY. "productPrice" would be invalid
        // SQL. Asserting the translated name is the point of this test.
        assertThat(pageable.getValue().getSort().getOrderFor("productPrice")).isNull();
        Sort.Order order = pageable.getValue().getSort().getOrderFor("product_price");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listReturnsPaginationMetadata() {
        Product p = new Product();
        p.setId(1L);
        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(1, 5), 11));

        ResponseDto res = service.listProduct(new ProductListDto());
        assertThat(res.isSuccess()).isTrue();

        PageDto<Product> page = (PageDto<Product>) res.getData();
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.isHasPrevious()).isTrue();
    }

    // --- get and delete ---------------------------------------------------

    @Test
    void getUnknownIdReturnsNotExist() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThat(service.getProductById(1L).getCode()).isEqualTo("E0007");
    }

    @Test
    void deleteUnknownIdDoesNotCallDelete() {
        when(productRepository.existsById(9L)).thenReturn(false);
        assertThat(service.deleteProductById(9L).getCode()).isEqualTo("E0007");
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteExistingIdSucceeds() {
        when(productRepository.existsById(3L)).thenReturn(true);
        assertThat(service.deleteProductById(3L).isSuccess()).isTrue();
        verify(productRepository).deleteById(3L);
    }
}

