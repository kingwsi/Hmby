package org.example.hmby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableJpaAuditing
@EnableFeignClients(basePackages = "org.example.hmby")
@SpringBootApplication
public class HmbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmbyApplication.class, args);
    }

}
