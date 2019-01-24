package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import picocli.CommandLine.ParameterException;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application {

    private static class ValidParams {

    }

    public static void main(String[] args) {
        try {
            CliValidation.validate(args);
        } catch (ParameterException e) {
            System.exit(1);
        }

        SpringApplication.run(Application.class, args);
    }
}