package com.ecommerceproject.orderservice.service;

import com.ecommerceproject.orderservice.dto.InventoryResponse;
import com.ecommerceproject.orderservice.dto.OrderItemsDto;
import com.ecommerceproject.orderservice.dto.OrderRequest;
import com.ecommerceproject.orderservice.model.Order;
import com.ecommerceproject.orderservice.model.OrderItems;
import com.ecommerceproject.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                      uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

        if(allProductsInStock){
            orderRepository.save(order);
            return "Order placed successfully";
        }else{
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }

    }

    private OrderItems mapToDto(OrderItemsDto orderItemsDto){
        OrderItems orderItems = new OrderItems();
        orderItems.setPrice(orderItemsDto.getPrice());
        orderItems.setQuantity(orderItemsDto.getQuantity());
        orderItems.setSkuCode(orderItemsDto.getSkuCode());
        return orderItems;
    }

}
