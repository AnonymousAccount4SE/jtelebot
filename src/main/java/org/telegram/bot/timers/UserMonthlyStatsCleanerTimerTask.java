package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMonthlyStatsCleanerTimerTask extends TimerParent {

    private final Bot bot;
    private final UserStatsService userStatsService;

    @Override
    @Scheduled(cron = "0 0 0 1 * ?")
    public void execute() {
        log.info("Timer for cleaning top by month");
        userStatsService.clearMonthlyStats().forEach(sendMessage -> {
            try {
                bot.execute((sendMessage));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }
}