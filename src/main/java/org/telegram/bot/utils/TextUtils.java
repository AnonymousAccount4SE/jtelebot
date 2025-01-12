package org.telegram.bot.utils;

import org.telegram.bot.domain.entities.User;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    public static final String BORDER = "-----------------------------\n";

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9Ёё]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern WORD_PATTERN = Pattern.compile("\\W$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("/[\\w,\\s-]+\\.[A-Za-z]+$");

    /**
     * Gets a potential command from text.
     *
     * @param text - text to be processed.
     * @return potential command without rest text.
     */
    public static String getPotentialCommandInText(String text) {
        if (text.charAt(0) == '/') {
            text = text.substring(1);
        }
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.find()) {
            String buf = matcher.group(0).trim();
            matcher = WORD_PATTERN.matcher(buf);
            if (matcher.find()) {
                return buf.substring(0, buf.length() - 1).toLowerCase();
            }
            return buf.toLowerCase();
        }

        return null;
    }

    public static String cutMarkdownSymbolsInText(String text) {
        return text.replaceAll("[*_`\\[\\]()]", "").replaceAll("<.*?>","");
    }

    public static String reduceSpaces(String text) {
        while (text.contains("  ")) {
            text = text.replaceAll(" +", " ");
        }
        while (text.contains("\n\n")) {
            text = text.replaceAll("\n\n", "\n");
        }

        return text.trim();
    }

    public static String cutHtmlTags(String text) {
        text = text.replaceAll("<.*?>","");
        return text.replaceAll("<", "");
    }

    public static String withCapital(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public static String removeCapital(String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    public static Boolean startsWithElementInList(String text, List<String> symbolsList) {
        return symbolsList.stream().anyMatch(text::startsWith);
    }

    public static Boolean isTextLengthIncludedInLimit(String text) {
        return text.length() < 4096;
    }

    public static String cutIfLongerThan(String text, int limit) {
        if (text.length() > limit) {
            return text.substring(0, limit-3) + "...";
        }

        return text;
    }

    public static String deleteWordsInText(String wordStartsWith, String text) {
        int i = text.indexOf(wordStartsWith);
        while (i >= 0) {
            String word = text.substring(i);
            int endOfWord = word.indexOf(" ");
            if (endOfWord < 0) {
                endOfWord = word.length();
            }
            word = word.substring(0, endOfWord);
            text = text.replace(word, "");
            i = text.indexOf(wordStartsWith);
        }

        return text;
    }

    public static String getLinkToUser(User user, boolean htmlMode) {
        return getLinkToUser(user.getUserId(), htmlMode, user.getUsername());
    }

    public static String getLinkToUser(org.telegram.telegrambots.meta.api.objects.User user, boolean htmlMode) {
        return getLinkToUser(user.getId(), htmlMode, user.getUserName());
    }

    public static String getLinkToUser(User user, boolean htmlMode, String caption) {
        return getLinkToUser(user.getUserId(), htmlMode, caption);
    }

    public static String getLinkToUser(Long userId, boolean htmlMode, String caption) {
        if (htmlMode) {
            return "<a href=\"tg://user?id=" + userId + "\">" + caption + "</a>";
        }
        return "[" + caption + "](tg://user?id=" + userId + ")";
    }

    public static String wrapTextToSpoiler(String text) {
        return "<tg-spoiler>" + text + "</tg-spoiler>";
    }

    public static String formatLongValue(long value) {
        final long E = 1000000000000000000L;
        final long P = 1000000000000000L;
        final long T = 1000000000000L;
        final long G = 1000000000L;
        final long M = 1000000L;
        final long K = 1000L;

        if (value > E) {
            return value / E + "E";
        } else if (value > P) {
            return value / P + "P";
        } else if (value > T) {
            return value / T + "T";
        } else if (value > G) {
            return value / G + "G";
        } else if (value > M) {
            return value / M + "M";
        } else if (value > K) {
            return value / K + "K";
        } else {
            return String.valueOf(value);
        }
    }

    public static String formatFileSize(long size) {
        return formatFileSize((double) size);
    }

    public static String formatFileSize(double size) {
        float value = (float) size;
        String unit;
        float result;

        final float G = 1073741824;
        final float M = 1048576;
        final float K = 1024;

        if (size > G) {
            unit = "Gb";
            result = value / G;
        } else if (size > M) {
            unit = "Mb";
            result = value / M;
        } else if (size > K) {
            unit = "Kb";
            result = value / M;
        } else {
            unit = "b";
            result = value;
        }

        return String.format("%.2f", result) + unit;
    }

    public static boolean startsWithNumber(String text) {
        try {
            Integer.parseInt(text.substring(0, 1));
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    public static Float parseFloat(String text) {
        return Float.parseFloat(text.replace(",", "."));
    }

    public static boolean isThatUrl(String text) {
        try {
            new URL(text);
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }

    @Nullable
    public static String getFileNameFromUrl(String url) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(url);

        if (matcher.find()) {
            return url.substring(matcher.start() + 1, matcher.end());
        }

        return null;
    }

    public static boolean isThatInteger(String text) {
        try {
            Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
