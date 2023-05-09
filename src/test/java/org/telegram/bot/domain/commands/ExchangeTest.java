package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeTest {

    @Mock
    private SpeechService speechService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private XmlMapper xmlMapper;

    @InjectMocks
    private Exchange exchange;

    private static final String XML_URL = "http://www.cbr.ru/scripts/XML_daily.asp";
    private static final String CURRENT_DATE = "02.01.2007";
    private static final String PREVIOUS_DATE = "01.01.2007";

    @Test
    void parseWithIOExceptionTest() throws IOException {
        final String expectedErrorMessage = "no response";
        Update update = TestUtils.getUpdate();

        when(networkUtils.readStringFromURL(XML_URL, Charset.forName("windows-1251"))).thenThrow(new IOException());
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void parseWithEmptyTextMessage() throws IOException {
        final String expectedResponseText = IOUtils.toString(new FileInputStream("src/test/java/org/telegram/bot/domain/commands/parse_with_empty_text_message_expected_response.txt"), StandardCharsets.UTF_8);
        Update update = TestUtils.getUpdate();
        Exchange.ValCurs valCurs1 = getCurrentValCurs();
        Exchange.ValCurs valCurs2 = getPreviousValCurs();

        String urlWithDate = XML_URL + "?date_req=" + PREVIOUS_DATE.replaceAll("\\.", "/");
        when(networkUtils.readStringFromURL(XML_URL, Charset.forName("windows-1251"))).thenReturn("1");
        when(networkUtils.readStringFromURL(urlWithDate, Charset.forName("windows-1251"))).thenReturn("2");
        when(xmlMapper.readValue("1", Exchange.ValCurs.class)).thenReturn(valCurs1);
        when(xmlMapper.readValue("2", Exchange.ValCurs.class)).thenReturn(valCurs2);

        SendMessage sendMessage = exchange.parse(update);
        assertNotNull(sendMessage);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getRublesForCurrencyValueWithWrongArgumentTest() {
        final String expectedErrorMessage = "wrong input";
        Update update = TestUtils.getUpdate("exchange 5");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void getRublesForCurrencyValueWithUnknownValuteTest() throws IOException {
        final String unknownValuteCode = "btlc";
        final String expectedResponseText = IOUtils.toString(new FileInputStream("src/test/java/org/telegram/bot/domain/commands/get_rubles_for_currency_value_with_unknown_valute_expected_response.txt"), StandardCharsets.UTF_8);

        Update update = TestUtils.getUpdate("exchange 5 " + unknownValuteCode);
        Exchange.ValCurs valCurs = getCurrentValCurs();
        CommandProperties commandProperties = new CommandProperties().setCommandName("exchange");

        when(networkUtils.readStringFromURL(XML_URL, Charset.forName("windows-1251"))).thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);
        when(commandPropertiesService.getCommand(Exchange.class)).thenReturn(commandProperties);

        SendMessage sendMessage = exchange.parse(update);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getRublesForCurrencyValueTest() throws IOException {
        final String expectedResponseText = IOUtils.toString(new FileInputStream("src/test/java/org/telegram/bot/domain/commands/get_rubles_for_currency_value_expected_response.txt"), StandardCharsets.UTF_8);
        Update update = TestUtils.getUpdate("exchange 5 usd");
        Exchange.ValCurs valCurs = getCurrentValCurs();

        when(networkUtils.readStringFromURL(XML_URL, Charset.forName("windows-1251"))).thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);

        SendMessage sendMessage = exchange.parse(update);
        assertNotNull(sendMessage);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getExchangeRatesForUnknownCodeTest() throws IOException {
        final String unknownValuteCode = "btlc";
        final String expectedResponseText = IOUtils.toString(new FileInputStream("src/test/java/org/telegram/bot/domain/commands/get_exchange_rates_for_unknown_code_expected_response.txt"), StandardCharsets.UTF_8);

        Update update = TestUtils.getUpdate("exchange_" + unknownValuteCode);
        Exchange.ValCurs valCurs = getCurrentValCurs();
        CommandProperties commandProperties = new CommandProperties().setCommandName("exchange");

        when(networkUtils.readStringFromURL(anyString(), any())).thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);
        when(commandPropertiesService.getCommand(Exchange.class)).thenReturn(commandProperties);

        SendMessage sendMessage = exchange.parse(update);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getExchangeRatesForCodeTest() throws IOException {
        final String expectedResponseText = IOUtils.toString(new FileInputStream("src/test/java/org/telegram/bot/domain/commands/get_exchange_rates_for_code_expected_response.txt"), StandardCharsets.UTF_8);
        Update update = TestUtils.getUpdate("exchange usd");
        Exchange.ValCurs valCurs1 = getCurrentValCurs();
        Exchange.ValCurs valCurs2 = getPreviousValCurs();

        String urlWithDate = XML_URL + "?date_req=" + PREVIOUS_DATE.replaceAll("\\.", "/");
        when(networkUtils.readStringFromURL(XML_URL, Charset.forName("windows-1251"))).thenReturn("1");
        when(networkUtils.readStringFromURL(urlWithDate, Charset.forName("windows-1251"))).thenReturn("2");
        when(xmlMapper.readValue("1", Exchange.ValCurs.class)).thenReturn(valCurs1);
        when(xmlMapper.readValue("2", Exchange.ValCurs.class)).thenReturn(valCurs2);

        SendMessage sendMessage = exchange.parse(update);
        assertNotNull(sendMessage);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    private Exchange.ValCurs getCurrentValCurs() {
        Exchange.Valute usd = new Exchange.Valute();
        usd.setId("R01235");
        usd.setNumCode("840");
        usd.setCharCode("USD");
        usd.setNominal("1");
        usd.setName("Доллар США");
        usd.setValue("76,8207");

        Exchange.Valute eur = new Exchange.Valute();
        eur.setId("R01239");
        eur.setNumCode("978");
        eur.setCharCode("EUR");
        eur.setNominal("1");
        eur.setName("Евро");
        eur.setValue("84,9073");

        Exchange.ValCurs valCurs = new Exchange.ValCurs();
        valCurs.setDate(CURRENT_DATE);
        valCurs.setName("Foreign Currency Market");
        valCurs.setValute(List.of(usd, eur));

        return valCurs;
    }

    private Exchange.ValCurs getPreviousValCurs() {
        Exchange.Valute usd = new Exchange.Valute();
        usd.setId("R01235");
        usd.setNumCode("840");
        usd.setCharCode("USD");
        usd.setNominal("1");
        usd.setName("Доллар США");
        usd.setValue("72,8318");

        Exchange.Valute eur = new Exchange.Valute();
        eur.setId("R01239");
        eur.setNumCode("978");
        eur.setCharCode("EUR");
        eur.setNominal("1");
        eur.setName("Евро");
        eur.setValue("88,9723");

        Exchange.ValCurs valCurs = new Exchange.ValCurs();
        valCurs.setDate(PREVIOUS_DATE);
        valCurs.setName("Foreign Currency Market");
        valCurs.setValute(List.of(usd, eur));

        return valCurs;
    }
}