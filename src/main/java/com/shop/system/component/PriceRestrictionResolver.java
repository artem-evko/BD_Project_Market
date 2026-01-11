package com.shop.system.component;

import com.shop.system.domain.entity.PriceRestriction;
import com.shop.system.domain.enums.PriceRestrictionScopeType;
import com.shop.system.repository.PriceRestrictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PriceRestrictionResolver {

    private final PriceRestrictionRepository priceRestrictionRepository;

    public Optional<PriceRestriction> resolve(UUID storageLocationId, UUID productId, List<UUID> categoryIds) {
        List<PriceRestriction> rules = priceRestrictionRepository.findForStoreWithGlobals(storageLocationId);

        for (PriceRestriction r : rules) {
            PriceRestrictionScopeType scope = PriceRestrictionScopeType.valueOf(r.getScopeType().toUpperCase());

            switch (scope) {
                case PRODUCT -> {
                    if (r.getProduct() != null && r.getProduct().getId().equals(productId)) {
                        return Optional.of(r);
                    }
                }
                case CATEGORY -> {
                    if (r.getCategory() != null && categoryIds != null && !categoryIds.isEmpty()) {
                        UUID ruleCategoryId = r.getCategory().getId();
                        if (categoryIds.contains(ruleCategoryId)) {
                            return Optional.of(r);
                        }
                    }
                }
                case ALL -> {
                    return Optional.of(r);
                }
            }
        }

        return Optional.empty();
    }
}