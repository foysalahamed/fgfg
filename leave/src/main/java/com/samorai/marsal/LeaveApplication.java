

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableFeignClients
@Configuration
public class LeaveApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveApplication.class, args);
    }
}
