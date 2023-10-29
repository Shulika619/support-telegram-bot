package dev.shulika.supporttelegrambot;

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

import static dev.shulika.supporttelegrambot.Constants.*;
import static dev.shulika.supporttelegrambot.MessageFactory.*;
import static dev.shulika.supporttelegrambot.UserState.AWAITING_QUESTION;
import static dev.shulika.supporttelegrambot.UserState.SUPPORT;

@Slf4j
public class ResponseHandler {
    private final MessageSender sender;
    private final SilentSender silentSender;
    private final Map<Long, UserState> chatStates;
    private final Map<Long, Long> usersTicket;
    private final Long chanelId;
    private final Long chanelChatId;

    public ResponseHandler(MessageSender sender, SilentSender silentSender, DBContext db, BotProperties botProperties) {
        this.sender = sender;
        this.silentSender = silentSender;
        chatStates = db.getMap(CHAT_STATES);
        usersTicket = db.getMap(USERS_TICKET);
        this.chanelId = botProperties.getChanelId();
        this.chanelChatId = botProperties.getChanelChatId();
    }

    public void replyToStart(Long chatId) {
        log.info("+++ IN ResponseHandler :: replyToStart :: START");
        sendMessage(text(chatId, START_TEXT));
        chatStates.put(chatId, AWAITING_QUESTION);
    }

    public void replyToStop(Long chatId) {
        log.info("--- IN ResponseHandler :: replyToStop :: STOP");
        sendMessage(text(chatId, STOP_TEXT));
        chatStates.remove(chatId);

        if (usersTicket.containsKey(chatId)) {
            Long messageId = usersTicket.get(chatId);
            sendTextReplyToMessage(textReply(CLOSE_TEXT, chanelChatId, messageId.intValue()));
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
            sendTextReplyToMessage(textReply(ALREADY_CLOSED, chanelChatId, messageId.intValue()));
        }
    }

    private void unexpectedMessage(Long chatId) {
        log.error("--- IN ResponseHandler :: unexpectedMessage :: UNSUPPORTED");
        sendMessage(text(chatId, UNSUPPORTED));
    }

    public void messageDispatcher(Long chatId, Message message) {

        if (message.hasText() && message.getText().equalsIgnoreCase(STOP_COMMAND)) {
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
        sendMessage(text(chatId, QUESTION_PROCESSED));
        silentSender.execute(forwardMessage(chanelId, message.getChatId(), message.getMessageId()));
    }

    /** Binding new messageId in support chat with user chatId in bot
    and save to Map <Long, Long> usersTicket (messageId:chatId and chatId:messageId) **/
    public void saveData(Integer messageId, Long chatId) {
        log.info("+++ IN ResponseHandler :: saveData :: SAVE messageId+chatId +++");
        usersTicket.put(Long.valueOf(messageId), chatId);
        usersTicket.put(chatId, Long.valueOf(messageId));
    }

    public void supportAnswer(Message message) {
        log.info("+++ IN ResponseHandler :: supportAnswer :: ANSWER FROM SUPPORT <--");
        Long messageId = Long.valueOf(message.getReplyToMessage().getMessageId());
        if (!usersTicket.containsKey(messageId)) {
            log.info("--- IN ResponseHandler :: supportAnswer :: usersTicket.containsKey == null");
            sendTextReplyToMessage(textReply(ALREADY_CLOSED, chanelChatId, messageId.intValue()));
            return;
        }
        Long userChatId = usersTicket.get(messageId);
        if (message.hasText()) {
            sendMessage(text(userChatId, message.getText()));
        } else if (message.hasPhoto()) {
            String photoId = Iterables.getLast(message.getPhoto()).getFileId();
            sendPhotoMessage(photo(message.getCaption(), userChatId, photoId));
        } else if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            sendDocumentMessage(document(message.getCaption(), userChatId, fileId));
        } else log.info("--- IN ResponseHandler :: supportAnswer :: unsupported message type");
    }

    public void replyToSupport(Long chatId, Message message) {
        log.info("+++ IN ResponseHandler :: replyToSupport :: COMMENTS TO THE SUPPORT TICKET -->");
        Long messageId = usersTicket.get(chatId);
        if (message.hasText()) {
            sendTextReplyToMessage(textReply(message.getText(), chanelChatId, messageId.intValue()));
        } else if (message.hasPhoto()) {
            String photoId = Iterables.getLast(message.getPhoto()).getFileId();
            sendPhotoReplyToMessage(photoReply(message.getCaption(), chanelChatId, messageId.intValue(), photoId));
        } else if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            sendDocumentReplyToMessage(documentReply(message.getCaption(), chanelChatId, messageId.intValue(), fileId));
        } else log.info("--- IN ResponseHandler :: replyToSupport :: unsupported message type");
    }

    private void sendTextReplyToMessage(SendMessage sendMessage) {
        silentSender.execute(sendMessage);
    }

    private void sendPhotoReplyToMessage(SendPhoto sendPhoto) {
        try {
            sender.sendPhoto(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("--- IN ResponseHandler :: sendPhotoReplyToMessage :: " + e);
        }
    }

    private void sendDocumentReplyToMessage(SendDocument sendDocument) {
        try {
            sender.sendDocument(sendDocument);
        } catch (TelegramApiException e) {
            log.error("--- IN ResponseHandler :: sendDocumentReplyToMessage :: " + e);
        }
    }

    private void sendMessage(SendMessage sendMessage) {
        silentSender.execute(sendMessage);
    }

    private void sendPhotoMessage(SendPhoto sendPhoto) {
        try {
            sender.sendPhoto(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("--- IN ResponseHandler :: sendPhotoMessage :: " + e);
        }
    }

    private void sendDocumentMessage(SendDocument sendDocument) {
        try {
            sender.sendDocument(sendDocument);
        } catch (TelegramApiException e) {
            log.error("--- IN ResponseHandler :: sendDocumentMessage :: " + e);
        }
    }

    public boolean isActiveUser(Long chatId) {
        return chatStates.containsKey(chatId);
    }

}
