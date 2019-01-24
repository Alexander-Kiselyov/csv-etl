package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import picocli.CommandLine.ParameterException;

/**
 * Main launcher class.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application {

    public static void main(String[] args) {
        try {
            CliValidation.validate(args);
        } catch (ParameterException e) {
            System.exit(1);
        }

        SpringApplication.run(Application.class, args);
    }
}