package dev.shulika.supporttelegrambot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

import static dev.shulika.supporttelegrambot.Constants.*;
import static dev.shulika.supporttelegrambot.UserState.AWAITING_QUESTION;
import static dev.shulika.supporttelegrambot.UserState.SUPPORT;

@Slf4j
public class ResponseHandler {
    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;
    private final Map<Long, Long> usersTicket;
    private final Long chanelId;
    private final Long chanelChatId;

    public ResponseHandler(SilentSender sender, DBContext db, BotProperties botProperties) {
        this.sender = sender;
        chatStates = db.getMap(CHAT_STATES);
        usersTicket = db.getMap(USERS_TICKET);
        this.chanelId = botProperties.getChanelId();
        this.chanelChatId = botProperties.getChanelChatId();
    }

    public void replyToStart(Long chatId) {
        log.info("+++ IN ResponseHandler :: replyToStart :: START");
        sendMessage(chatId, START_TEXT);
        chatStates.put(chatId, AWAITING_QUESTION);
    }

    public void replyToStop(Long chatId) {
        log.info("--- IN ResponseHandler :: replyToStop :: STOP");
        sendMessage(chatId, STOP_TEXT);
        chatStates.remove(chatId);

        if (usersTicket.containsKey(chatId)) {
            Long messageId = usersTicket.get(chatId);
            sendReplyToMessage(CLOSE_TEXT, chanelChatId, messageId.intValue());
            usersTicket.remove(chatId);
            usersTicket.remove(messageId);
        }
    }

    public void replyToCloseTicket(Message message) {
        log.info("--- IN ResponseHandler :: replyToCloseTicket :: CLOSE TICKET ---");
        Long messageId = Long.valueOf(message.getReplyToMessage().getMessageId());
        if (usersTicket.containsKey(messageId)) {
            Long userChatId = usersTicket.get(messageId);
            replyToStop(userChatId);
        } else {
            log.error("--- IN ResponseHandler :: replyToCloseTicket :: usersTicket.containsKey == null");
            sendReplyToMessage(ALREADY_CLOSED, chanelChatId, messageId.intValue());
        }
    }

    private void unexpectedMessage(Long chatId) {
        log.error("--- IN ResponseHandler :: unexpectedMessage :: UNSUPPORTED");
        sendMessage(chatId, UNSUPPORTED);
    }

    public void messageDispatcher(Long chatId, Message message) {

        if (message.hasText() && message.getText().equalsIgnoreCase("/stop")) {
            replyToStop(chatId);
            return;
        }
        switch (chatStates.get(chatId)) {
            case AWAITING_QUESTION -> createTicket(chatId, message);
            case SUPPORT -> replyToSupport(chatId, message);
            default -> unexpectedMessage(chatId);
        }
    }

    private void createTicket(Long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: createTicket :: FIRST QUESTION TO SUPPORT -->");
        chatStates.put(chatId, SUPPORT);
        sendMessage(chatId, QUESTION_PROCESSED);

        ForwardMessage forwardMessage = ForwardMessage.builder()
                .chatId(chanelId)
                .fromChatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
        sender.execute(forwardMessage);
    }

    // Binding new messageId in chat with user chatId in bot
    // and save to Map <Long, Long> usersTicket (messageId:chatId and chatId:messageId)
    public void saveData(Integer messageId, Long chatId) {
        log.info("+++ IN ResponseHandler :: saveData :: SAVE messageId+chatId +++");
        usersTicket.put(Long.valueOf(messageId), chatId);
        usersTicket.put(chatId, Long.valueOf(messageId));
    }

    public void supportAnswer(Message message) {
        log.info("+++ IN ResponseHandler :: supportAnswer :: ANSWER FROM SUPPORT <--");
        Long messageId = Long.valueOf(message.getReplyToMessage().getMessageId());
        if (usersTicket.containsKey(messageId)) {
            Long userChatId = usersTicket.get(messageId);
            sendMessage(userChatId, message.getText());
        } else {
            log.info("--- IN ResponseHandler :: supportAnswer :: usersTicket.containsKey == null");
            sendReplyToMessage(ALREADY_CLOSED, chanelChatId, messageId.intValue());
        }
    }

    public void replyToSupport(Long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: replyToSupport :: COMMENTS TO THE SUPPORT TICKET -->");
        Long messageId = usersTicket.get(chatId);
        sendReplyToMessage(message.getText(), chanelChatId, messageId.intValue());
    }

    private void sendReplyToMessage(String text, Long chatId, Integer messageId) {
        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .replyToMessageId(messageId)
                .build();
        sender.execute(sendMessage);
        log.info("===== usersTicket SIZE: {}", usersTicket.size()); // TODO: delete late
        log.info("===== chatStates SIZE: {}", chatStates.size());
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sender.execute(sendMessage);
        log.info("===== usersTicket SIZE: {}", usersTicket.size()); // TODO: delete late
        log.info("===== chatStates SIZE: {}", chatStates.size());
    }

    public boolean isActiveUser(Long chatId) {
        return chatStates.containsKey(chatId);
    }

}
