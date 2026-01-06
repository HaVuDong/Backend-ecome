package havudong.baocao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling  // Enable scheduled jobs (UserPreferenceUpdateJob)
public class BaocaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BaocaoApplication.class, args);
	}

}
