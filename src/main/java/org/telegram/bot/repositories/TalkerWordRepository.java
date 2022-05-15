package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.telegram.bot.domain.entities.TalkerWord;

import java.util.List;
import java.util.Set;

/**
 * Spring Data repository for the TalkerWord entity.
 */
public interface TalkerWordRepository extends JpaRepository<TalkerWord, Long> {
    Set<TalkerWord> findAllByWordInIgnoreCase(List<String> words);

    @Query("SELECT tw FROM TalkerWord tw " +
            "INNER JOIN tw.phrases tp " +
            "INNER JOIN tp.chat c " +
            "WHERE tw.word in (:words) AND c.chatId = :chatId")
    Set<TalkerWord> findAllByWordInIgnoreCaseAndPhrasesChatIdEq(@Param("words") List<String> words, @Param("chatId") Long chatId);
}