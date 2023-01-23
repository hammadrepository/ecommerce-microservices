package com.ecommerceproject.orderservice.service;

import com.ecommerceproject.orderservice.dto.OrderItemsDto;
import com.ecommerceproject.orderservice.dto.OrderRequest;
import com.ecommerceproject.orderservice.model.Order;
import com.ecommerceproject.orderservice.model.OrderItems;
import com.ecommerceproject.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();

        order.setOrderNumber((UUID.randomUUID().toString()));

        List<OrderItems> orderItems = orderRequest.getOrderItemsDtoList()
        .stream()
        .map(this::mapToDto)
        .toList();

        order.setOrderItems(orderItems);
        orderRepository.save(order);
    }

    private OrderItems mapToDto(OrderItemsDto orderItemsDto){
        OrderItems orderItems = new OrderItems();
        orderItems.setPrice(orderItemsDto.getPrice());
        orderItems.setQuantity(orderItemsDto.getQuantity());
        orderItems.setSkuCode(orderItemsDto.getSkuCode());
        return orderItems;
    }

}
