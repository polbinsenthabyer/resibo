package com.api.resibo.services.product;

import com.api.resibo.dtos.PageDto;
import com.api.resibo.dtos.ProductDto;
import com.api.resibo.dtos.ProductListDto;
import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.entities.Product;
import com.api.resibo.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    // --- setProduct: create vs update -------------------------------------

    @Test
    void setWithoutIdCreatesAndAppliesColumnDefaults() {
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
        Sort.Order order = pageable.getValue().getSort().getOrderFor("productPrice");
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
