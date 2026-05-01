package localnews_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class BackendApplication {

	public static void main(String[] args) {
		// Ensure TLS 1.2/1.3 are used for MongoDB Atlas; applies even when JAVA_OPTS is not set
		System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");
		SpringApplication.run(BackendApplication.class, args);
	}

}
