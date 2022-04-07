package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.TalkerDegree}.
 */
public interface TalkerDegreeService {
    /**
     * Get a TalkerDegree.
     *
     * @param chatId of TalkerDegree to get.
     * @return the persisted entity.
     */
    TalkerDegree get(Long chatId);

    /**
     * Save a TalkerDegree.
     *
     * @param talkerDegree the entity to save.
     * @return the persisted entity.
     */
    TalkerDegree save(TalkerDegree talkerDegree);
}
