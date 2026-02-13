package org.folio.dcb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class DcbApplication {
  public static void main(String[] args) {
    SpringApplication.run(DcbApplication.class, args);
  }
}
