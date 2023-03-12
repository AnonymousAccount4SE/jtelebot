package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadTest {

    @Mock
    NetworkUtils networkUtils;
    @Mock
    SpeechService speechService;
    @Mock
    CommandWaitingService commandWaitingService;
    @Mock
    InputStream fileFromUrl;

    @InjectMocks
    Download download;

    private final static String DEFAULT_FILE_NAME = "file";
    private final static String FILE_NAME = "favicon.ico";
    private final static String URL = "http://example.org/";

    @Test
    void parseWithEmptyArgumentTest() {
        Update update = TestUtils.getUpdate("download");

        PartialBotApiMethod<?> method = download.parse(update);

        assertNotNull(method);
        assertTrue(method instanceof SendMessage);
        Mockito.verify(commandWaitingService).add(update.getMessage(), Download.class);
    }

    @Test
    void parseWithTwoWrongArgumentsTest() {
        Update update = TestUtils.getUpdate("download test test");

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"download " + URL + " " + FILE_NAME, "download " + FILE_NAME + " " + URL})
    void parseWithTwoArgumentsTest(String command) throws Exception {
        Update update = TestUtils.getUpdate(command);

        when(networkUtils.getFileFromUrl(anyString(), anyInt())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        assertNotNull(method);
        assertTrue(method instanceof SendDocument);

        SendDocument sendDocument = (SendDocument) method;
        InputFile inputFile = sendDocument.getDocument();
        assertEquals(FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithOneWrongArgument() {
        Update update = TestUtils.getUpdate("download test");

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithoutFilenameInUrlTest() throws Exception {
        Update update = TestUtils.getUpdate("download " + URL);

        when(networkUtils.getFileFromUrl(anyString(), anyInt())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        assertNotNull(method);
        assertTrue(method instanceof SendDocument);

        SendDocument sendDocument = (SendDocument) method;
        InputFile inputFile = sendDocument.getDocument();
        assertEquals(DEFAULT_FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithOneArgumentTest() throws Exception {
        Update update = TestUtils.getUpdate("download " + URL + FILE_NAME);

        when(networkUtils.getFileFromUrl(anyString(), anyInt())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        assertNotNull(method);
        assertTrue(method instanceof SendDocument);

        SendDocument sendDocument = (SendDocument) method;
        InputFile inputFile = sendDocument.getDocument();
        assertEquals(FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithLargeFileTest() throws Exception {
        Update update = TestUtils.getUpdate("download " + URL);

        when(networkUtils.getFileFromUrl(anyString(), anyInt())).thenThrow(new Exception());

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE);
    }

}