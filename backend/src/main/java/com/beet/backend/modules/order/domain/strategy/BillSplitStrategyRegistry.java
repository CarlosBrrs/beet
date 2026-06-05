package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.model.BillSplitMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BillSplitStrategyRegistry {
    private final Map<BillSplitMode, BillSplitStrategy> strategies = new EnumMap<>(BillSplitMode.class);

    public BillSplitStrategyRegistry(List<BillSplitStrategy> strategies) {
        for (BillSplitStrategy strategy : strategies) {
            this.strategies.put(strategy.mode(), strategy);
        }
    }

    public BillSplitStrategy get(BillSplitMode mode) {
        BillSplitStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported bill split mode: " + mode);
        }
        return strategy;
    }
}
