package cl.duoc.campuslab.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // =========================
    // EXCHANGES
    // =========================

    @Value("${campuslab.rabbitmq.exchanges.direct}")
    private String directExchangeName;

    @Value("${campuslab.rabbitmq.exchanges.topic}")
    private String topicExchangeName;

    @Value("${campuslab.rabbitmq.exchanges.dead}")
    private String deadLetterExchangeName;


    // =========================
    // COLAS PRINCIPALES
    // =========================

    @Value("${campuslab.rabbitmq.queues.email}")
    private String emailQueueName;

    @Value("${campuslab.rabbitmq.queues.prep}")
    private String prepQueueName;

    @Value("${campuslab.rabbitmq.queues.voucher}")
    private String voucherQueueName;


    // =========================
    // DEAD LETTER QUEUES
    // =========================

    @Value("${campuslab.rabbitmq.queues.email-dlq}")
    private String emailDlqName;

    @Value("${campuslab.rabbitmq.queues.prep-dlq}")
    private String prepDlqName;

    @Value("${campuslab.rabbitmq.queues.voucher-dlq}")
    private String voucherDlqName;


    // =========================
    // ROUTING KEYS DIRECT
    // =========================

    @Value("${campuslab.rabbitmq.routing.email}")
    private String emailRoutingKey;

    @Value("${campuslab.rabbitmq.routing.prep}")
    private String prepRoutingKey;

    @Value("${campuslab.rabbitmq.routing.voucher}")
    private String voucherRoutingKey;


    // =========================
    // ROUTING KEYS TOPIC
    // =========================

    @Value("${campuslab.rabbitmq.topic-routing.email}")
    private String emailTopicRoutingKey;

    @Value("${campuslab.rabbitmq.topic-routing.prep}")
    private String prepTopicRoutingKey;

    @Value("${campuslab.rabbitmq.topic-routing.voucher}")
    private String voucherTopicRoutingKey;


    // =========================
    // EXCHANGES
    // =========================

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(
                directExchangeName,
                true,
                false
        );
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(
                topicExchangeName,
                true,
                false
        );
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(
                deadLetterExchangeName,
                true,
                false
        );
    }


    // =========================
    // COLAS PRINCIPALES
    // =========================

    @Bean
    public Queue emailQueue() {
        return QueueBuilder
                .durable(emailQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(emailDlqName)
                .build();
    }

    @Bean
    public Queue prepQueue() {
        return QueueBuilder
                .durable(prepQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(prepDlqName)
                .build();
    }

    @Bean
    public Queue voucherQueue() {
        return QueueBuilder
                .durable(voucherQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(voucherDlqName)
                .build();
    }


    // =========================
    // DEAD LETTER QUEUES
    // =========================

    @Bean
    public Queue emailDlq() {
        return QueueBuilder
                .durable(emailDlqName)
                .build();
    }

    @Bean
    public Queue prepDlq() {
        return QueueBuilder
                .durable(prepDlqName)
                .build();
    }

    @Bean
    public Queue voucherDlq() {
        return QueueBuilder
                .durable(voucherDlqName)
                .build();
    }


    // =========================
    // BINDINGS DIRECT
    // =========================

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(directExchange())
                .with(emailRoutingKey);
    }

    @Bean
    public Binding prepBinding() {
        return BindingBuilder
                .bind(prepQueue())
                .to(directExchange())
                .with(prepRoutingKey);
    }

    @Bean
    public Binding voucherBinding() {
        return BindingBuilder
                .bind(voucherQueue())
                .to(directExchange())
                .with(voucherRoutingKey);
    }


    // =========================
    // BINDINGS TOPIC
    // =========================

    @Bean
    public Binding emailTopicBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(topicExchange())
                .with(emailTopicRoutingKey);
    }

    @Bean
    public Binding prepTopicBinding() {
        return BindingBuilder
                .bind(prepQueue())
                .to(topicExchange())
                .with(prepTopicRoutingKey);
    }

    @Bean
    public Binding voucherTopicBinding() {
        return BindingBuilder
                .bind(voucherQueue())
                .to(topicExchange())
                .with(voucherTopicRoutingKey);
    }


    // =========================
    // BINDINGS DLQ
    // =========================

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder
                .bind(emailDlq())
                .to(deadLetterExchange())
                .with(emailDlqName);
    }

    @Bean
    public Binding prepDlqBinding() {
        return BindingBuilder
                .bind(prepDlq())
                .to(deadLetterExchange())
                .with(prepDlqName);
    }

    @Bean
    public Binding voucherDlqBinding() {
        return BindingBuilder
                .bind(voucherDlq())
                .to(deadLetterExchange())
                .with(voucherDlqName);
    }


    // =========================
    // CONVERSIÓN JSON
    // =========================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}