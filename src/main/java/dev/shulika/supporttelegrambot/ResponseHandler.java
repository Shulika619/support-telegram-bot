package dev.shulika.supporttelegrambot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

import static dev.shulika.supporttelegrambot.Constants.*;
import static dev.shulika.supporttelegrambot.UserState.*;

@Slf4j
public class ResponseHandler {
    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;

    public ResponseHandler(SilentSender sender, DBContext db) {
        this.sender = sender;
        chatStates = db.getMap(CHAT_STATES);
    }

    public void replyToStart(long chatId) {
        log.info("+++ IN ResponseHandler :: replyToStart :: START");
        sendMessage(chatId, START_TEXT);
        chatStates.put(chatId, AWAITING_QUESTION);
    }

    private void stopChat(long chatId) {
        log.info("--- IN ResponseHandler :: stopChat :: STOP");
        sendMessage(chatId, STOP_TEXT);
        chatStates.remove(chatId);
    }

    private void unexpectedMessage(long chatId) {
        log.error("--- IN ResponseHandler :: unexpectedMessage :: UNSUPPORTED");
        sendMessage(chatId, UNSUPPORTED);
    }

    public void messageDispatcher(long chatId, Message message) {

        if (message.getText().equalsIgnoreCase("/stop")) {
            stopChat(chatId);
        }

        switch (chatStates.get(chatId)) {
            case AWAITING_QUESTION -> replyToQuestion(chatId, message);
            case SUPPORT -> replyToSupport(chatId, message);
            default -> unexpectedMessage(chatId);
        }
    }

    private void replyToQuestion(long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: replyToQuestion :: QUESTION");
        chatStates.put(chatId, SUPPORT);
        sendMessage(chatId, QUESTION_PROCESSED);
    }

    private void replyToSupport(long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: replyToSupport :: SUPPORT");

    }

    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sender.execute(sendMessage);
    }

    public boolean userIsActive(Long chatId) {
        return chatStates.containsKey(chatId);
    }

}
