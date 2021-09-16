package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class Logs implements CommandParent<SendDocument> {

    @Override
    public SendDocument parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        if (chatId < 0) {
            chatId = message.getFrom().getId();
        }

        File logs = new File("logs/log.log");
        log.debug("Request to send logs to {}", chatId);

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());
        sendDocument.setReplyToMessageId(message.getMessageId());
        sendDocument.setDocument(new InputFile(logs, "logs.log"));

        return sendDocument;
    }
}
