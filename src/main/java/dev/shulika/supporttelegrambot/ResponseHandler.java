package dev.shulika.supporttelegrambot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;

import static dev.shulika.supporttelegrambot.Constants.*;
import static dev.shulika.supporttelegrambot.UserState.AWAITING_QUESTION;
import static dev.shulika.supporttelegrambot.UserState.SUPPORT;

@Slf4j
public class ResponseHandler {
    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final Long chanelId;
    private final Long chanelChatId;
    private Map<Long, Long> userChatPost = new HashMap<>();

    public ResponseHandler(SilentSender sender, DBContext db, BotProperties botProperties) {
        this.sender = sender;
        chatStates = db.getMap(CHAT_STATES);
        this.chanelId = botProperties.getChanelId();
        this.chanelChatId = botProperties.getChanelChatId();
    }

    public void replyToStart(long chatId) {
        log.info("+++ IN ResponseHandler :: replyToStart :: START");
        sendMessage(chatId, START_TEXT);
        chatStates.put(chatId, AWAITING_QUESTION);
    }

    public void replyToStop(long chatId) {
        log.info("--- IN ResponseHandler :: replyToStop :: STOP");
        sendMessage(chatId, STOP_TEXT);
        chatStates.remove(chatId);
    }

    private void unexpectedMessage(long chatId) {
        log.error("--- IN ResponseHandler :: unexpectedMessage :: UNSUPPORTED");
        sendMessage(chatId, UNSUPPORTED);
    }

    public void messageDispatcher(long chatId, Message message) {

        if (message.getText().equalsIgnoreCase("/stop")) {
            replyToStop(chatId);
            return;
        }

        switch (chatStates.get(chatId)) {
            case AWAITING_QUESTION -> createTicket(chatId, message);
            case SUPPORT -> replyToSupport(chatId, message);
            default -> unexpectedMessage(chatId);
        }
    }

    private void createTicket(long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: createTicket :: FIRST QUESTION TO SUPPORT-->");
        chatStates.put(chatId, SUPPORT);
        sendMessage(chatId, QUESTION_PROCESSED);

        ForwardMessage forwardMessage = ForwardMessage.builder()
                .chatId(chanelId)
                .fromChatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
        sender.execute(forwardMessage);
    }

    public void supportAnswer(long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: supportAnswer :: ANSWER FROM SUPPORT<--");
        long messageId = message.getReplyToMessage().getMessageId();
        long userChatId = userChatPost.get(messageId);
        sendMessage(userChatId, message.getText());
    }

    public void replyToSupport(long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: replyToSupport :: COMMENTS TO SUPPORT -->");
        long messageId = userChatPost.get(chatId);

        SendMessage sendMessage = SendMessage.builder()
                .text(message.getText())
                .chatId(chanelChatId)
                .replyToMessageId((int) messageId)
                .build();
        sender.execute(sendMessage);
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

    // Binding new post in chat with user chatId in bot and save
    public void saveData(long messageId, long chatId) {
        log.info("+++ IN ResponseHandler :: saveData :: SAVE messageId+chatId +++");
        userChatPost.put(messageId, chatId);
        userChatPost.put(chatId, messageId);
    }

}
