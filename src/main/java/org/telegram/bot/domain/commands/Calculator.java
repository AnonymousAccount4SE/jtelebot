package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class Calculator implements CommandParent<SendMessage> {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate defaultRestTemplate;
    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = commandWaitingService.getText(message);
        String responseText;

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            log.debug("Empty request. Enabling command waiting");
            responseText = "теперь напиши мне что нужно посчитать";
        } else {
            textMessage = textMessage.replace(",", ".");
            log.debug("Request to calculate {}", textMessage);
            final String MATH_JS_URL = "http://api.mathjs.org/v4/?expr=";

            JSONObject expressionData = new JSONObject();
            expressionData.put("expr", textMessage);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(expressionData.toString(), headers);

            try {
                ResponseEntity<String> response = defaultRestTemplate.postForEntity(MATH_JS_URL, request, String.class);
                if (response.getBody() == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
                }

                String result;
                try {
                    result = new JSONObject(response.getBody()).getString("result");
                } catch (JSONException e) {
                    botStats.incrementErrors(update, e, "Ошибка при попытке получить ответ к калькулятору");
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }

                try {
                    responseText =  new BigDecimal(result).toPlainString();
                } catch (NumberFormatException e) {
                    responseText = result;
                }

                responseText = "`" + responseText + "`";
            } catch (HttpClientErrorException hce) {
                log.error("Error from api:", hce);
                responseText = new JSONObject(hce.getResponseBodyAsString()).getString("error");
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }
}
