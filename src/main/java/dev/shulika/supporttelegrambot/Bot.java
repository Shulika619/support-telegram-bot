package dev.shulika.supporttelegrambot;

import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;

import static dev.shulika.supporttelegrambot.Constants.START_DESCRIPTION;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class Bot extends AbilityBot {

    private final BotProperties botProperties;
    private final ResponseHandler responseHandler;

    public Bot(BotProperties botProperties) {
        super(botProperties.getBotToken(), botProperties.getBotUserName());
        this.botProperties = botProperties;
        responseHandler = new ResponseHandler(silent, db);
    }

    @Override
    public long creatorId() {
        return botProperties.getCreatorId();
    }

    public Ability startBot() {
        return Ability
                .builder()
                .name("start")
                .info(START_DESCRIPTION)
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

}
