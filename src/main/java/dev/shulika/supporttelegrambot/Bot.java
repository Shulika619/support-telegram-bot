package dev.shulika.supporttelegrambot;

import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;

import static dev.shulika.supporttelegrambot.Constants.START_DESCRIPTION;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component
public class Bot extends AbilityBot {

    private final BotProperties botProperties;
    private final ResponseHandler responseHandler;

    public Bot(BotProperties botProperties) {
        super(botProperties.getBotToken(), botProperties.getBotUserName());
        this.botProperties = botProperties;
        responseHandler = new ResponseHandler(silent, db, botProperties.getChanelId());
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

    public Reply replyToMessage() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.messageDispatcher(getChatId(upd), upd.getMessage());
        return Reply.of(action, Flag.TEXT, upd -> responseHandler.userIsActive(getChatId(upd)));
    }

}
