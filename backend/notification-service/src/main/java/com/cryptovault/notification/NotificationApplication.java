package com.cryptovault.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * <h3>NotificationApplication</h3>
 *
 * <p><b>Why it exists:</b> Bootstraps the execution context for the CryptoVault Notification Service microservice.</p>
 * <p><b>Architectural Layer:</b> Bootstrap / Entry Layer.</p>
 * <p><b>Design Patterns Used:</b> Bootstrapper and Facade patterns.</p>
 * <p><b>Security Concepts Demonstrated:</b> Houses security filter configurations within the base scan package.</p>
 * <p><b>Future AWS Integration Path:</b> Serves as the boot scanning context under which future AWS SQS/SNS configuration beans and listeners are auto-registered.</p>
 * <p><b>Enterprise Relevance:</b> Standard microservice start class enabling Spring Boot autoconfigurations and Spring Cloud client features.</p>
 * <p><b>Interview Talking Points:</b> Annotating with <code>@EnableFeignClients</code> allows scanning and injecting OpenFeign clients if the service needs to query downstream services (e.g. KYC, user details) in the future.</p>
 */
@SpringBootApplication
@EnableFeignClients
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
