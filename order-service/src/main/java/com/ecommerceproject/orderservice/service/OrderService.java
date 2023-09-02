package com.ecommerceproject.orderservice.service;

import com.ecommerceproject.orderservice.dto.InventoryResponse;
import com.ecommerceproject.orderservice.dto.OrderItemsDto;
import com.ecommerceproject.orderservice.dto.OrderRequest;
import com.ecommerceproject.orderservice.event.OrderPlacedEvent;
import com.ecommerceproject.orderservice.model.Order;
import com.ecommerceproject.orderservice.model.OrderItems;
import com.ecommerceproject.orderservice.repository.OrderRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObservationRegistry observationRegistry;
//    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    @ExceptionHandler
    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();

        order.setOrderNumber((UUID.randomUUID().toString()));

        List<OrderItems> orderItems = orderRequest.getOrderItemsDtoList()
        .stream()
        .map(this::mapToDto)
        .toList();

        order.setOrderItems(orderItems);
        List<String> skuCodes = order.getOrderItems().stream().map(OrderItems::getSkuCode).toList();
        // Call Inventory service, and place order if product is in stock.
//        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-service-lookup",
                this.observationRegistry);
        inventoryServiceObservation.lowCardinalityKeyValue("call", "inventory-service");
        return inventoryServiceObservation.observe(() -> {
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                applicationEventPublisher.publishEvent( new OrderPlacedEvent(order.getOrderNumber()));
                // publish Order Placed Event
                System.out.println("Order placed!");
                return "Order Placed";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        });

    }
    private OrderItems mapToDto(OrderItemsDto orderItemsDto){
        OrderItems orderItems = new OrderItems();
        orderItems.setPrice(orderItemsDto.getPrice());
        orderItems.setQuantity(orderItemsDto.getQuantity());
        orderItems.setSkuCode(orderItemsDto.getSkuCode());
        return orderItems;
    }

}
