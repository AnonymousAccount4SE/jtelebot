package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeleteMessageExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return DeleteMessage.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        DeleteMessage deleteMessage = (DeleteMessage) method;
        log.info("Deleting message {}", deleteMessage.getMessageId());

        try {
            bot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
