package dev.shulika.supporttelegrambot;

import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static dev.shulika.supporttelegrambot.Constants.CLOSE_COMMAND;
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
        responseHandler = new ResponseHandler(silent, db, botProperties);
    }

    @Override
    public long creatorId() {
        return botProperties.getCreatorId();
    }

    public Ability startBot() {
        return Ability.builder()
                .name("start")
                .info(START_DESCRIPTION)
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

    public Reply messageDispatcher() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) ->
                responseHandler.messageDispatcher(getChatId(upd), upd.getMessage());
        return Reply.of(action, Flag.TEXT, upd -> responseHandler.userIsActive(getChatId(upd)));
    }

    public Reply newPostInChat() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) ->
                responseHandler.saveData(upd.getMessage().getMessageId(), upd.getMessage().getForwardFrom().getId());
        return Reply.of(action, isFromChatAndForwardNotNull());
    }

    public Reply supportAnswer() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) ->
                responseHandler.supportAnswer(upd.getMessage());
        return Reply.of(action, Flag.REPLY, isAnswerFromSupportChat(), hasNotMessageWith(CLOSE_COMMAND));
    }



    public Reply closeTicket() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) ->
                responseHandler.replyToCloseTicket(upd.getMessage());
        return Reply.of(action, Flag.TEXT, Flag.REPLY, isAnswerFromSupportChat(), hasMessageWith(CLOSE_COMMAND));
    }
    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> upd.getMessage().getText().equalsIgnoreCase(msg);
    }
    private Predicate<Update> hasNotMessageWith(String msg) {
        return upd -> !upd.getMessage().getText().equalsIgnoreCase(msg);
    }



    private Predicate<Update> isFromChatAndForwardNotNull() {
        return upd -> upd.getMessage().getChatId().equals(botProperties.getChanelChatId())
                && upd.getMessage().getForwardFrom().getId() != null;
    }

    private Predicate<Update> isAnswerFromSupportChat() {
        return upd -> upd.getMessage().getReplyToMessage().getChatId().equals(botProperties.getChanelChatId());
    }

}
