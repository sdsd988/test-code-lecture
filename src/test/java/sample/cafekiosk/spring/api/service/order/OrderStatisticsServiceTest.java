package sample.cafekiosk.spring.api.service.order;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import sample.cafekiosk.spring.api.service.mail.MailService;
import sample.cafekiosk.spring.client.mail.MailSendClient;
import sample.cafekiosk.spring.domain.history.mail.MailSendHistory;
import sample.cafekiosk.spring.domain.history.mail.MailSendHistoryRepository;
import sample.cafekiosk.spring.domain.order.Order;
import sample.cafekiosk.spring.domain.order.OrderRepository;
import sample.cafekiosk.spring.domain.order.OrderStatus;
import sample.cafekiosk.spring.domain.orderProduct.OrderProductRepository;
import sample.cafekiosk.spring.domain.product.Product;
import sample.cafekiosk.spring.domain.product.ProductRepository;
import sample.cafekiosk.spring.domain.product.ProductSellingStatus;
import sample.cafekiosk.spring.domain.product.ProductType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static sample.cafekiosk.spring.domain.product.ProductSellingStatus.SELLING;
import static sample.cafekiosk.spring.domain.product.ProductType.*;

@SpringBootTest
class OrderStatisticsServiceTest {

    @Autowired
    private OrderStatisticsService orderStatisticsService;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MailSendHistoryRepository mailSendHistoryRepository;

    @MockBean
    private MailSendClient mailSendClient;

    @AfterEach
    void tearDown() {
        orderProductRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        mailSendHistoryRepository.deleteAllInBatch();
    }

    @DisplayName("결제완료 주문들을 조회하여 매출 통계 메일을 전송한다.")
    @Test
    void test() {
        //given

        LocalDateTime now = LocalDateTime.of(2024, 10, 21, 18, 0);

        Product product1 = createProduct(HANDMADE, "001", 1000);
        Product product2 = createProduct(HANDMADE, "002", 2000);
        Product product3 = createProduct(HANDMADE, "003", 3000);
        productRepository.saveAll(List.of(product1, product2, product3));

        List<Product> products = List.of(product1, product2, product3);
        Order order1 = createPayementCompletedOrder(products, LocalDateTime.of(2024,10,20,23,59,59));
        Order order2 = createPayementCompletedOrder(products, now);
        Order order3 = createPayementCompletedOrder(products, LocalDateTime.of(2024,10,21,23,59,59));
        Order order4 = createPayementCompletedOrder(products, LocalDateTime.of(2024,10,22,0,0,0));


        Mockito.when(mailSendClient.sendMail(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(true);
        //when
        boolean result = orderStatisticsService.sendOrderStatisticsMail(LocalDate.of(2024, 10, 21), "test@test.com");

        //then
        assertThat(result).isTrue();
        List<MailSendHistory> histories = mailSendHistoryRepository.findAll();
        assertThat(histories).hasSize(1)
                .extracting("content")
                .contains("총 매출 합계는 18000");

    }

    private Order createPayementCompletedOrder(List<Product> products, LocalDateTime now) {
        Order order = Order.builder()
                .products(products)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED)
                .registeredDateTime(now)
                .build();
        orderRepository.save(order);
        return order;
    }

    private Product createProduct(ProductType type, String ProductNumber, int price) {
        return Product.builder()
                .type(type)
                .productNumber(ProductNumber)
                .price(price)
                .sellingStatus(SELLING)
                .name("메뉴이름")
                .build();
    }


}