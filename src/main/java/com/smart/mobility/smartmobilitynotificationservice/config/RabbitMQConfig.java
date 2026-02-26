package com.smart.mobility.smartmobilitynotificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String ACCOUNT_EXCHANGE = "account.exchange";

    public static final String NOTIFICATION_PAYMENT_QUEUE = "notification.payment.queue";
    public static final String NOTIFICATION_PAYMENT_DLQ = "notification.payment.dlq";

    public static final String NOTIFICATION_ACCOUNT_QUEUE = "notification.account.queue";
    public static final String NOTIFICATION_ACCOUNT_DLQ = "notification.account.dlq";

    public static final String ROUTING_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_PAYMENT_FAILED = "payment.failed";
    public static final String ROUTING_ACCOUNT_CREDITED = "account.credited";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange accountExchange() {
        return new TopicExchange(ACCOUNT_EXCHANGE, true, false);
    }

    // --- Payment Queue & DLQ ---
    @Bean
    public Queue notificationPaymentDlq() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_DLQ).build();
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.payment.dlq.routing")
                .build();
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange()).with(ROUTING_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange()).with(ROUTING_PAYMENT_FAILED);
    }

    @Bean
    public Binding paymentDlqBinding() {
        return BindingBuilder.bind(notificationPaymentDlq()).to(paymentExchange())
                .with("notification.payment.dlq.routing");
    }

    // --- Account Queue & DLQ ---
    @Bean
    public Queue notificationAccountDlq() {
        return QueueBuilder.durable(NOTIFICATION_ACCOUNT_DLQ).build();
    }

    @Bean
    public Queue notificationAccountQueue() {
        return QueueBuilder.durable(NOTIFICATION_ACCOUNT_QUEUE)
                .withArgument("x-dead-letter-exchange", ACCOUNT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.account.dlq.routing")
                .build();
    }

    @Bean
    public Binding accountCreditedBinding() {
        return BindingBuilder.bind(notificationAccountQueue()).to(accountExchange()).with(ROUTING_ACCOUNT_CREDITED);
    }

    @Bean
    public Binding accountDlqBinding() {
        return BindingBuilder.bind(notificationAccountDlq()).to(accountExchange())
                .with("notification.account.dlq.routing");
    }

    // --- Message Converter ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // Configure retry or concurrent consumers if needed
        return factory;
    }
}
