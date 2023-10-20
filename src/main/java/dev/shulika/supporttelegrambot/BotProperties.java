package dev.shulika.supporttelegrambot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.userName}")
    private String botUserName;

    @Value("${bot.creatorId}")
    private Long creatorId;

    @Value("${bot.chanelId}")
    private String chanelId;

}