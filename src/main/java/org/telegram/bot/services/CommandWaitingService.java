package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.CommandWaiting}.
 */
public interface CommandWaitingService {

    /**
     * Get a CommandWaiting.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @return the persisted entity.
     */
    CommandWaiting get(Chat chat, User user);

    /**
     * Get a CommandWaiting.
     *
     * @param chatId id of Chat entity.
     * @param userId id of User entity.
     * @return the persisted entity.
     */
    CommandWaiting get(Long chatId, Integer userId);

    /**
     * Get a CommandWaiting text.
     *
     * @param message received message.
     * @return CommandWaiting text.
     */
    String getText(Message message);

    /**
     * Add a CommandWaiting.
     * @param message the received message.
     * @param commandClass Class of command.
     */
    void add(Message message, Class<?> commandClass);

    /**
     * Add a CommandWaiting.
     * @param message the received message.
     * @param commandClass Class of command.
     * @param commandText text of intermediate text of command.
     */
    void add(Message message, Class<?> commandClass, String commandText);

    /**
     * Save a CommandWaiting.
     *
     * @param commandWaiting the entity to save.
     * @return the persisted entity.
     */
    CommandWaiting save(CommandWaiting commandWaiting);

    /**
     * Remove a CommandWaiting.
     *
     * @param commandWaiting the entity to remove.
     */
    void remove(CommandWaiting commandWaiting);
}
