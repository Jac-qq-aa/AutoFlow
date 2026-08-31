package io.autoflow.fulfillment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("io.autoflow.fulfillment.persistence")
@ComponentScan("io.autoflow")
public class FulfillmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(FulfillmentApplication.class, args);
    }
}

