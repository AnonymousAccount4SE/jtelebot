package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;

@Component
@RequiredArgsConstructor
@Slf4j
public class WolframRequestsTimer extends TimerParent {
    private final BotStats botStats;

    @Override
    @Scheduled(cron = "0 0 0 1 * ?")
    public void execute() {
        botStats.resetWolframRequests();
    }
}
