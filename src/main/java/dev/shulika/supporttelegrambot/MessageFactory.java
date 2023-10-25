package dev.shulika.supporttelegrambot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@Component
public class MessageFactory {

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

}
