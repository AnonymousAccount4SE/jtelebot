package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Webcam implements CommandParent<SendVideo> {

    private final SpeechService speechService;
    private final FileManagerTimer fileManagerTimer;

    private final static String fileNamePrefix = "file";
    private final static String fileNamePostfix = ".mp4";
    private final static int defaultVideoDurationInSeconds = 5;
    private final static int maxVideoDurationInSeconds = 500;

    @Override
    public SendVideo parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = getTextMessage(update);

        String duration;
        String url;
        int spaceIndex = textMessage.indexOf(" ");
        if (spaceIndex < 0) {
            duration = String.valueOf(defaultVideoDurationInSeconds);
            url = textMessage;
        } else {
            url = textMessage.substring(0, spaceIndex);
            duration = textMessage.substring(spaceIndex + 1);
        }

        if (!TextUtils.isThatUrl(url) || !TextUtils.isThatInteger(duration) || Integer.parseInt(duration) > maxVideoDurationInSeconds) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String fileName = fileManagerTimer.addFile(fileNamePrefix, fileNamePostfix);
        final String command = "ffmpeg -re -t " + duration + " -i " + url + " -c:v copy -c:a copy -bsf:a aac_adtstoasc " + fileName;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command(command.split(" "));

            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to call command {}: {}", command, e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(message.getChatId());
        sendVideo.setReplyToMessageId(message.getMessageId());
        sendVideo.setVideo(new InputFile(new File(fileName)));

        return sendVideo;
    }
}