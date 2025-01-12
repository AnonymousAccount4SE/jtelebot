package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Zodiac;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import static org.telegram.bot.utils.TextUtils.isTextLengthIncludedInLimit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Horoscope implements CommandParent<SendMessage> {

    private final String HOROSCOPE_DATA_URL = "https://ignio.com/r/daily/";

    private final Bot bot;
    private final UserZodiacService userZodiacService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());

        org.telegram.bot.domain.enums.Horoscope horoscopeType;
        if (textMessage == null) {
            horoscopeType = org.telegram.bot.domain.enums.Horoscope.COM;
        } else {
            if (textMessage.startsWith("_")) {
                textMessage = textMessage.substring(1);
            }
            try {
                horoscopeType = org.telegram.bot.domain.enums.Horoscope.findByName(textMessage);
            } catch (IllegalArgumentException e) {
                horoscopeType = null;
            }
        }

        if (horoscopeType != null) {
            UserZodiac userZodiac = userZodiacService.get(chat, user);
            if (userZodiac == null || Zodiac.NOT_CHOSEN.equals(userZodiac.getZodiac())) {
                log.debug("Request to {} horoscope for all zodiacs", horoscopeType);
                responseText = getHoroscopeForAllZodiacs(horoscopeType);
            } else {
                log.debug("Request to get {} horoscope for {}", horoscopeType, userZodiac);
                responseText = getHoroscopeForZodiacs(horoscopeType, userZodiac.getZodiac());
            }
        } else {
            responseText = getResponseTextWithHoroscopeTypeList();
        }

        if (!isTextLengthIncludedInLimit(responseText)) {
            responseText = "Текст гороскопа оказался слишком длинным. Задай знак зодиака командой /set";
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getResponseTextWithHoroscopeTypeList() {
        StringBuilder buf = new StringBuilder("Не знаю такой тип гороскопа. Существуют:\n");

        Arrays.stream(org.telegram.bot.domain.enums.Horoscope.values())
                .forEach(horoscope -> buf
                        .append(horoscope.getRuName())
                        .append(" — /")
                        .append(this.getClass().getSimpleName().toLowerCase(Locale.ROOT))
                        .append("_")
                        .append(horoscope.name().toLowerCase(Locale.ROOT))
                        .append("\n"));

        return buf.toString();
    }

    private String getHoroscopeForAllZodiacs(org.telegram.bot.domain.enums.Horoscope horoscope) {
        HoroscopeData horoscopeData = getHoroByHoroscopeType(horoscope);

        StringBuilder buf = new StringBuilder("Гороскоп <b>" + horoscope.getRuName() + "</b>\n");
        buf.append("(").append(horoscopeData.getDate().getToday()).append(")\n\n");

        Arrays.stream(Zodiac.values()).forEach(zodiac -> {
            if (Zodiac.NOT_CHOSEN.equals(zodiac)) {
                return;
            }

            HoroscopeElement horoscopeElement = getHoroscopeElementByZodiacName(horoscopeData, zodiac);

            buf.append("<u><a href=\"").append(HOROSCOPE_DATA_URL).append("\">").append(zodiac.getEmoji()).append(zodiac.getNameRu()).append("</a></u>");
            buf.append(horoscopeElement.getToday()).append("\n");
        });

        return buf.toString();
    }

    private String getHoroscopeForZodiacs(org.telegram.bot.domain.enums.Horoscope horoscope, Zodiac zodiac) {
        HoroscopeData horoscopeData = getHoroByHoroscopeType(horoscope);

        StringBuilder buf = new StringBuilder("Гороскоп <b>" + horoscope.getRuName() + "</b>\n");
        buf.append("(").append(horoscopeData.getDate().getToday()).append(")\n");

        HoroscopeElement horoscopeElement = getHoroscopeElementByZodiacName(horoscopeData, zodiac);

        buf.append("<u><a href=\"").append(HOROSCOPE_DATA_URL).append("\">").append(zodiac.getEmoji()).append(zodiac.getNameRu()).append("</a></u>");
        buf.append(horoscopeElement.getToday());

        return buf.toString();
    }

    private HoroscopeElement getHoroscopeElementByZodiacName(HoroscopeData horoscopeData, Zodiac zodiac) {
        String methodName = "get" + StringUtils.capitalize(zodiac.name().toLowerCase(Locale.ROOT));

        try {
            Method method = HoroscopeData.class.getMethod(methodName);
            return (HoroscopeElement) method.invoke(horoscopeData);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private HoroscopeData getHoroByHoroscopeType(org.telegram.bot.domain.enums.Horoscope horoscope) {
        String horoscopeFileName = "horoscope/" + horoscope.name().toLowerCase(Locale.ROOT) + ".xml";
        File file = new File(horoscopeFileName);
        XmlMapper xmlMapper = new XmlMapper();

        try {
            return xmlMapper.readValue(file, HoroscopeData.class);
        } catch (IOException e) {
            log.error("Cannot read file {}: {}", horoscopeFileName, e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    @Data
    private static class HoroscopeData {
        @XmlElement
        private Horo horo;

        @XmlAttribute
        private Date date;

        @XmlElement
        private HoroscopeElement aries;

        @XmlElement
        private HoroscopeElement taurus;

        @XmlElement
        private HoroscopeElement gemini;

        @XmlElement
        private HoroscopeElement cancer;

        @XmlElement
        private HoroscopeElement leo;

        @XmlElement
        private HoroscopeElement virgo;

        @XmlElement
        private HoroscopeElement libra;

        @XmlElement
        private HoroscopeElement scorpio;

        @XmlElement
        private HoroscopeElement sagittarius;

        @XmlElement
        private HoroscopeElement capricorn;

        @XmlElement
        private HoroscopeElement aquarius;

        @XmlElement
        private HoroscopeElement pisces;
    }

    @Data
    private static class Horo {}

    @Data
    private static class Date {
        @XmlElement
        private String yesterday;

        @XmlElement
        private String today;

        @XmlElement
        private String tomorrow;

        @XmlElement
        private String tomorrow02;
    }

    @Data
    private static class HoroscopeElement {
        @XmlElement
        private String yesterday;

        @XmlElement
        private String today;

        @XmlElement
        private String tomorrow;

        @XmlElement
        private String tomorrow02;
    }
}
