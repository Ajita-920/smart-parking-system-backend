package com.projectwork.Smart.Parking.System;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class SmartParkingSystemApplication {

    public static void main(String[] args) {
        // Configuration of DotEnv Java
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        env.entries().forEach((DotenvEntry entry) -> System.setProperty(
                entry.getKey(), entry.getValue()));

        SpringApplication.run(SmartParkingSystemApplication.class, args);
    }

}
