package com.fcfsdraw.draw.entry.metrics;

import com.fcfsdraw.draw.entry.domain.DrawStatus;
import com.fcfsdraw.draw.entry.repository.DrawEntryRepository;
import com.fcfsdraw.draw.entry.repository.DrawEntryRepository.DrawEntryStatusCount;
import com.fcfsdraw.draw.entry.service.DrawQueueService;
import com.fcfsdraw.draw.product.repository.ProductRepository;
import com.fcfsdraw.draw.product.repository.ProductRepository.ProductMetricView;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawQueueMetrics implements MeterBinder {

    private final ProductRepository productRepository;
    private final DrawEntryRepository drawEntryRepository;
    private final DrawQueueService drawQueueService;

    private MultiGauge entryStatusGauge;
    private MultiGauge queueSizeGauge;
    private MultiGauge productQuantityGauge;

    @Override
    public void bindTo(MeterRegistry registry) {
        entryStatusGauge = MultiGauge.builder("draw_entries")
                .description("Draw entry count by product and status")
                .baseUnit("entries")
                .register(registry);
        queueSizeGauge = MultiGauge.builder("draw_queue_size")
                .description("Redis draw queue size by product")
                .baseUnit("entries")
                .register(registry);
        productQuantityGauge = MultiGauge.builder("draw_product_quantity")
                .description("Draw product total and remaining quantity")
                .baseUnit("entries")
                .register(registry);

        refresh();
    }

    @Scheduled(fixedDelayString = "${draw.metrics.refresh-delay-ms:5000}")
    public void refresh() {
        if (entryStatusGauge == null || queueSizeGauge == null || productQuantityGauge == null) {
            return;
        }

        try {
            List<ProductMetricView> products = productRepository.findProductMetricViews();
            Map<Long, EnumMap<DrawStatus, Long>> entryCounts = entryCounts();

            entryStatusGauge.register(entryRows(products, entryCounts), true);
            queueSizeGauge.register(queueRows(products), true);
            productQuantityGauge.register(productRows(products), true);
        } catch (RuntimeException exception) {
            log.warn("failed to refresh draw metrics", exception);
        }
    }

    private Map<Long, EnumMap<DrawStatus, Long>> entryCounts() {
        Map<Long, EnumMap<DrawStatus, Long>> counts = new HashMap<>();
        for (DrawEntryStatusCount count : drawEntryRepository.countByProductIdAndStatus()) {
            counts.computeIfAbsent(count.getProductId(), ignored -> new EnumMap<>(DrawStatus.class))
                    .put(count.getStatus(), count.getCount());
        }
        return counts;
    }

    private List<MultiGauge.Row<?>> entryRows(
            List<ProductMetricView> products,
            Map<Long, EnumMap<DrawStatus, Long>> entryCounts
    ) {
        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (ProductMetricView product : products) {
            EnumMap<DrawStatus, Long> statusCounts = entryCounts.getOrDefault(product.getProductId(), new EnumMap<>(DrawStatus.class));
            for (DrawStatus status : DrawStatus.values()) {
                long value = statusCounts.getOrDefault(status, 0L);
                rows.add(MultiGauge.Row.of(productStatusTags(product.getProductId(), status), value));
            }
        }
        return rows;
    }

    private List<MultiGauge.Row<?>> queueRows(List<ProductMetricView> products) {
        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (ProductMetricView product : products) {
            rows.add(MultiGauge.Row.of(productTags(product.getProductId()), drawQueueService.queueSize(product.getProductId())));
        }
        return rows;
    }

    private List<MultiGauge.Row<?>> productRows(List<ProductMetricView> products) {
        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (ProductMetricView product : products) {
            rows.add(MultiGauge.Row.of(productQuantityTags(product.getProductId(), "total"), product.getTotalQuantity()));
            rows.add(MultiGauge.Row.of(productQuantityTags(product.getProductId(), "remaining"), product.getRemainingQuantity()));
        }
        return rows;
    }

    private Tags productTags(Long productId) {
        return Tags.of(Tag.of("product_id", String.valueOf(productId)));
    }

    private Tags productStatusTags(Long productId, DrawStatus status) {
        return productTags(productId).and("status", status.name());
    }

    private Tags productQuantityTags(Long productId, String type) {
        return productTags(productId).and("type", type);
    }
}
