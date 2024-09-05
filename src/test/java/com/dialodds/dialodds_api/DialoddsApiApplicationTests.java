package com.dialodds.dialodds_api;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DialoddsApiApplicationTests {

    @BeforeAll
    static void setup() {
        Dotenv dotenv = Dotenv.configure()
                              .directory("./")
                              .ignoreIfMissing()
                              .load();

        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    @Test
    void contextLoads() {
    }
}