package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.commands.Holidays;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.TimerService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidaysTimer extends TimerParent {

    private final Bot bot;
    private final TimerService timerService;
    private final ChatService chatService;
    private final Holidays holidays;

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("holidaysTimer");
        if (timer == null) {
            timer = new Timer()
                    .setName("holidaysTimer")
                    .setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            LocalDate dateNow = LocalDate.now();

            chatService.getChatsWithHolidays()
                    .stream()
                    .map(chat -> {
                        String textMessage = holidays.getHolidaysForDate(chat, dateNow);
                        if (textMessage == null) {
                            return null;
                        }

                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(chat.getChatId().toString());
                        sendMessage.enableHtml(true);
                        sendMessage.disableWebPagePreview();
                        sendMessage.setText(textMessage);

                        return sendMessage;
            })
                    .forEach(sendMessage -> {
                        if (sendMessage != null) {
                            try {
                                bot.execute(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
            });

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}
