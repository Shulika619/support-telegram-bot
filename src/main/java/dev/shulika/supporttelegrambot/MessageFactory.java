package dev.shulika.supporttelegrambot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@Component
public class MessageFactory {
    private MessageFactory() {}

    public static SendMessage text(Long chatId, String text){
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
    }

    public static SendPhoto photo(String text, Long chatId, String photoId){
        return SendPhoto.builder()
                .caption(text)
                .chatId(chatId)
                .photo(new InputFile(photoId))
                .build();
    }

    public static SendDocument document(String text, Long chatId, String fileId){
        return  SendDocument.builder()
                .caption(text)
                .chatId(chatId)
                .document(new InputFile(fileId))
                .build();
    }

    public static ForwardMessage forwardMessage(Long toChatId, Long fromChatId, Integer messageId){
        return ForwardMessage.builder()
                .chatId(toChatId)
                .fromChatId(fromChatId)
                .messageId(messageId)
                .build();
    }

    public static SendMessage textReply(String text, Long chatId, Integer messageId){
        return SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .replyToMessageId(messageId)
                .build();
    }

    public static SendPhoto photoReply(String text, Long chatId, Integer messageId, String photoId){
        return SendPhoto.builder()
                .caption(text)
                .chatId(chatId)
                .photo(new InputFile(photoId))
                .replyToMessageId(messageId)
                .build();
    }

    public static SendDocument documentReply(String text, Long chatId, Integer messageId, String fileId){
        return SendDocument.builder()
                .caption(text)
                .chatId(chatId)
                .document(new InputFile(fileId))
                .replyToMessageId(messageId)
                .build();
    }

}
