package cl.duoc.campuslab.messaging.producer;

import cl.duoc.campuslab.messaging.dto.BookingEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookingMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${campuslab.rabbitmq.exchanges.direct}")
    private String directExchange;

    @Value("${campuslab.rabbitmq.routing.email}")
    private String emailRoutingKey;

    @Value("${campuslab.rabbitmq.routing.prep}")
    private String prepRoutingKey;

    @Value("${campuslab.rabbitmq.routing.voucher}")
    private String voucherRoutingKey;

    public BookingMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendEmailMessage(BookingEventMessage message) {
        rabbitTemplate.convertAndSend(
                directExchange,
                emailRoutingKey,
                message
        );
    }

    public void sendPrepMessage(BookingEventMessage message) {
        rabbitTemplate.convertAndSend(
                directExchange,
                prepRoutingKey,
                message
        );
    }

    public void sendVoucherMessage(BookingEventMessage message) {
        rabbitTemplate.convertAndSend(
                directExchange,
                voucherRoutingKey,
                message
        );
    }
}