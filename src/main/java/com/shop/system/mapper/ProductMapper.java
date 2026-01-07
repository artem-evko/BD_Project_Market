package com.shop.system.mapper;

import com.shop.system.domain.entity.Manufacturer;
import com.shop.system.domain.entity.Product;
import com.shop.system.domain.entity.ProductCategory;
import com.shop.system.domain.entity.ProductCategoryLink;
import com.shop.system.dto.DimensionsDto;
import com.shop.system.dto.ManufacturerDto;
import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.ProductDetailResponse;
import com.shop.system.dto.response.ProductResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProductMapper {

    public ProductResponse toListItem(Product product, BigDecimal currentPrice) {
        String manufacturerName = Optional.ofNullable(product.getManufacturer())
                .map(Manufacturer::getManufacturerName)
                .orElse(null);

        String categoryName = product.getCategoryLinks().stream()
                .map(ProductCategoryLink::getCategory)
                .filter(Objects::nonNull)
                .map(ProductCategory::getName)
                .sorted()
                .findFirst()
                .orElse(null);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .barcode(product.getBarcode())
                .manufacturer(manufacturerName)
                .category(categoryName)
                .unitOfMeasure(product.getUnitOfMeasure())
                .shelfLifeDays(product.getShelfLifeDays())
                .currentPrice(currentPrice)
                .build();
    }

    public Product fromCreateRequest(ProductCreateRequest request, Manufacturer manufacturer) {
        return Product.builder()
                .name(request.getName())
                .barcode(request.getBarcode())
                .manufacturer(manufacturer)
                .unitOfMeasure(request.getUnitOfMeasure())
                .shelfLifeDays(request.getShelfLifeDays())
                .length(toStringOrNull(request.getLength()))
                .width(toStringOrNull(request.getWidth()))
                .height(toStringOrNull(request.getHeight()))
                .additionalInfo(request.getAdditionalInfo())
                .archived(false)
                .build();
    }

    private String toStringOrNull(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    public void updateEntity(Product product,
                             ProductUpdateRequest request,
                             Manufacturer manufacturer) {

        product.setName(request.getName());
        product.setBarcode(request.getBarcode());
        product.setManufacturer(manufacturer);
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setShelfLifeDays(request.getShelfLifeDays());
        product.setLength(toStringOrNull(request.getLength()));
        product.setWidth(toStringOrNull(request.getWidth()));
        product.setHeight(toStringOrNull(request.getHeight()));
        product.setAdditionalInfo(request.getAdditionalInfo());
    }

    public ProductDetailResponse toDetail(Product product) {
        String categoryName = product.getCategoryLinks().stream()
                .map(ProductCategoryLink::getCategory)
                .filter(Objects::nonNull)
                .map(ProductCategory::getName)
                .sorted()
                .findFirst()
                .orElse(null);

        DimensionsDto dimensions = DimensionsDto.builder()
                .length(parseBigDecimal(product.getLength()))
                .width(parseBigDecimal(product.getWidth()))
                .height(parseBigDecimal(product.getHeight()))
                .build();

        ManufacturerDto manufacturerDto = null;
        Manufacturer m = product.getManufacturer();
        if (m != null) {
            manufacturerDto = ManufacturerDto.builder()
                    .id(m.getId())
                    .name(m.getManufacturerName())
                    .country(m.getManufacturerCountry())
                    .build();
        }

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .barcode(product.getBarcode())
                .category(categoryName)
                .unitOfMeasure(product.getUnitOfMeasure())
                .shelfLifeDays(product.getShelfLifeDays())
                .dimensions(dimensions)
                .manufacturer(manufacturerDto)
                .build();
    }

    private java.math.BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
