package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class NetworkUtils {

    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    public InputStream getFileFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.connect();

        return connection.getInputStream();
    }

    public InputStream getFileFromUrl(String url, int limitBytes) throws Exception {
        byte[] file = IOUtils.toByteArray(getFileFromUrl(url));
        if (file.length > limitBytes) {
            throw new Exception("the file is not included in the limit");
        }

        return new ByteArrayInputStream(Objects.requireNonNull(file));
    }

    public InputStream getFileFromTelegram(Bot bot, String fileId) throws TelegramApiException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);

        return bot.downloadFileAsStream(bot.execute(getFile).getFilePath());
    }

    public String readStringFromURL(String url) throws IOException {
        return readStringFromURL(new URL(url).toString(), StandardCharsets.UTF_8);
    }

    public String readStringFromURL(String url, Charset encoding) throws IOException {
        return IOUtils.toString(new URL(url), encoding);
    }

    public SyndFeed getRssFeedFromUrl(String url) throws IOException, FeedException {
        return new SyndFeedInput().build(new XmlReader(new URL(url)));
    }
}
