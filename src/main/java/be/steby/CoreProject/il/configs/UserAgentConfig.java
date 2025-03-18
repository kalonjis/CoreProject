package be.steby.CoreProject.il.configs;

import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentConfig {
    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000) // Taille du cache ajustée
                .build();
    }
}
