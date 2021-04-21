package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;
import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.TextUtils.formatFileSize;

@Component
@AllArgsConstructor
public class Files implements CommandParent<PartialBotApiMethod<?>> {

    private final FileService fileService;
    private final ChatService chatService;
    private final UserService userService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    private final String CALLBACK_COMMAND = "files ";
    private final String SELECT_FILE_COMMAND = "s";
    private final String SELECT_PAGE = "p";
    private final String CALLBACK_SELECT_FILE_COMMAND = CALLBACK_COMMAND + SELECT_FILE_COMMAND;
    private final String DELETE_FILE_COMMAND = "d";
    private final String CALLBACK_DELETE_FILE_COMMAND = CALLBACK_COMMAND + DELETE_FILE_COMMAND;
    private final String ADD_FILE_COMMAND = "a";
    private final String CALLBACK_ADD_FILE_COMMAND = CALLBACK_COMMAND + ADD_FILE_COMMAND;
    private final String OPEN_FILE_COMMAND = "o";
    private final String CALLBACK_OPEN_FILE_COMMAND = CALLBACK_COMMAND + OPEN_FILE_COMMAND;
    private final String MAKE_DIR_COMMAND = "m";
    private final String CALLBACK_MAKE_DIR_COMMAND = CALLBACK_COMMAND + MAKE_DIR_COMMAND;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        Chat chat = chatService.get(message.getChatId());
        String textMessage = message.getText();
        boolean callback = false;
        String EMPTY_COMMAND = "files";

        CommandWaiting commandWaiting = commandWaitingService.get(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()));

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            textMessage = cutCommandInText(callbackQuery.getData());
            callback = true;
        } else if (commandWaiting != null) {
            if (textMessage == null) {
                textMessage = "";
            }
            textMessage = cutCommandInText(commandWaiting.getTextMessage() + textMessage);
            commandWaitingService.remove(commandWaiting);
        } else {
            textMessage = cutCommandInText(textMessage);
        }

        if (callback) {
            User user = userService.get(update.getCallbackQuery().getFrom().getId());

            if (textMessage.equals(EMPTY_COMMAND)) {
                return selectDirectory(message, chat, user, false, 0, null);
            } else if (textMessage.startsWith(SELECT_FILE_COMMAND)) {
                return selectFileByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(DELETE_FILE_COMMAND)) {
                return deleteFileByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(ADD_FILE_COMMAND)) {
                return addFileByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(OPEN_FILE_COMMAND)) {
                return sendFile(message, chat, user, textMessage);
            } else if (textMessage.startsWith(MAKE_DIR_COMMAND)) {
                return makeDirByCallback(message, chat, user, textMessage);
            }
        }

        User user = userService.get(message.getFrom().getId());
        if (textMessage == null || textMessage.equals(EMPTY_COMMAND)) {
            return selectDirectory(message,  chat, user, true, 0, null);
        } else if (textMessage.startsWith(ADD_FILE_COMMAND)) {
            return addFiles(message, chat, user, textMessage);
        } else if (textMessage.startsWith(MAKE_DIR_COMMAND)) {
            return makeDir(message, chat, user, textMessage);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private SendDocument sendFile(Message message, Chat chat, User user, String textCommand) throws BotException {
        long fileId;
        try {
            fileId = Long.parseLong(textCommand.substring(OPEN_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (!chat.equals(file.getChat())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chat.getChatId().toString());
        sendDocument.setDocument(new InputFile(file.getFileId()));

        return sendDocument;
    }

    private PartialBotApiMethod<?> addFiles(Message message, Chat chat, User user, String textCommand) throws BotException {
        if (!message.hasDocument()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        File parent;
        try {
            parent = fileService.get(Long.parseLong(textCommand.substring(ADD_FILE_COMMAND.length()).trim()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (parent == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Document document = message.getDocument();
        File file = new File();

        file.setFileId(document.getFileId());
        file.setFileUniqueId(document.getFileUniqueId());
        file.setName(document.getFileName());
        file.setType(document.getMimeType());
        file.setSize(document.getFileSize());
        file.setChat(chat);
        file.setUser(user);
        file.setDate(LocalDateTime.now());
        file.setParentId(parent.getId());

        fileService.save(file);

        return selectDirectory(message, chat, user, true, 0, parent);
    }

    private PartialBotApiMethod<?> makeDir(Message message, Chat chat, User user, String textCommand) throws BotException {
        textCommand = textCommand.trim();

        File parent;
        String dirName;
        try {
            int i = textCommand.indexOf(" ");
            parent = fileService.get(Long.parseLong(textCommand.substring(ADD_FILE_COMMAND.length(), i)));
            dirName = textCommand.substring(i + 1);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (parent == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        File file = new File();

        file.setName(dirName);
        file.setChat(chat);
        file.setUser(user);
        file.setDate(LocalDateTime.now());
        file.setParentId(parent.getId());

        fileService.save(file);

        return selectDirectory(message, chat, user, true, 0, parent);
    }

    private EditMessageText makeDirByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText("\nТеперь напиши мне имя папки");

        return editMessageText;
    }

    private SendMessage addFileByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("\nТеперь пришли мне сообщение с необходимым файлом");

        return sendMessage;
    }

    private EditMessageText deleteFileByCallback(Message message, Chat chat, User user, String textCommand) throws BotException {
        long fileId;
        try {
            fileId = Long.parseLong(textCommand.substring(DELETE_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (!user.equals(file.getUser())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        fileService.remove(chat, file);

        File dir = fileService.get(file.getParentId());
        if (dir == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return (EditMessageText) selectDirectory(message, chat, user, false, 0, dir);
    }

    private EditMessageText selectFileByCallback(Message message, Chat chat, User user, String textCommand) throws BotException {
        int page = 0;
        long fileId;
        textCommand = textCommand.trim();
        try {
            fileId = Long.parseLong(textCommand.substring(SELECT_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            try {
                fileId = Long.parseLong(textCommand.substring(textCommand.indexOf(SELECT_FILE_COMMAND) + SELECT_FILE_COMMAND.length(), textCommand.indexOf(" ")));
                page = Integer.parseInt(textCommand.substring(textCommand.indexOf(SELECT_PAGE) + SELECT_PAGE.length()));
            } catch (NumberFormatException en) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        } else if (file.getType() == null) {
            return (EditMessageText) selectDirectory(message, chat, user, false, page, file);
        }

        String fileInfo = "<b>" + file.getName() + "</b>\n" +
                            "Автор: " + getLinkToUser(file.getUser(), true) + "\n" +
                            "Создан: " + formatDateTime(file.getDate()) + "\n" +
                            "Тип: " + file.getType() + "\n" +
                            "Размер: " + formatFileSize(file.getSize()) + " (" + file.getSize() + " bytes)\n";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(fileInfo);
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(getFileManagingKeyboard(file));

        return editMessageText;
    }

    private InlineKeyboardMarkup getFileManagingKeyboard(File file) {
        List<List<InlineKeyboardButton>> fileManagingRows = new ArrayList<>();
        List<InlineKeyboardButton> managingRow = new ArrayList<>();

        InlineKeyboardButton downloadButton = new InlineKeyboardButton();
        downloadButton.setText(Emoji.DOWN_ARROW.getEmoji());
        downloadButton.setCallbackData(CALLBACK_OPEN_FILE_COMMAND + file.getId());

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji());
        deleteButton.setCallbackData(CALLBACK_DELETE_FILE_COMMAND + file.getId());

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji());
        backButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + file.getParentId());

        managingRow.add(downloadButton);
        managingRow.add(deleteButton);
        managingRow.add(backButton);

        fileManagingRows.add(managingRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(fileManagingRows);

        return inlineKeyboardMarkup;
    }

    private PartialBotApiMethod<?> selectDirectory(Message message, Chat chat, User user, boolean newMessage, int page, File directory) throws BotException {
        if (directory == null) {
            directory = fileService.get(0L);
            if (directory == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        Page<File> fileList = fileService.get(chat, user, directory, page);
        List<List<InlineKeyboardButton>> dirContent = new ArrayList<>();

        if (fileList.isEmpty() && directory.getId() != 0L) {
            List<InlineKeyboardButton> fileRow = new ArrayList<>();

            InlineKeyboardButton deleteEmptyDirButton = new InlineKeyboardButton();
            deleteEmptyDirButton.setText(Emoji.DELETE.getEmoji() + "Удалить папку");
            deleteEmptyDirButton.setCallbackData(CALLBACK_DELETE_FILE_COMMAND + directory.getId());

            fileRow.add(deleteEmptyDirButton);
            dirContent.add(fileRow);
        } else {
            dirContent = fileList.stream().map(file -> {
                List<InlineKeyboardButton> fileRow = new ArrayList<>();

                InlineKeyboardButton fileButton = new InlineKeyboardButton();

                String fileName = getEmojiByType(file.getType()) + file.getName();
                if (fileName.length() > 30) {
                    fileName = fileName.substring(0, 30) + "...";
                }
                fileButton.setText(fileName);
                fileButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + file.getId());

                fileRow.add(fileButton);

                return fileRow;
            }).collect(Collectors.toList());

            List<InlineKeyboardButton> pagesRow = new ArrayList<>();
            if (page > 0) {
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText(Emoji.LEFT_ARROW.getEmoji());
                backButton.setCallbackData(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page - 1));

                pagesRow.add(backButton);
            }

            int totalPages = fileList.getTotalPages();
            if (page + 1 < totalPages && totalPages > 1) {
                InlineKeyboardButton forwardButton = new InlineKeyboardButton();
                forwardButton.setText(Emoji.RIGHT_ARROW.getEmoji());
                forwardButton.setCallbackData(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page + 1));

                pagesRow.add(forwardButton);
            }

            dirContent.add(pagesRow);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(dirContent, directory));

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("Папка: <b>" + directory.getName() + "</b>\n");
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText("Папка: <b>" + directory.getName() + "</b>\n");
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows, File parent) {
        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Файл");
        addButton.setCallbackData(CALLBACK_ADD_FILE_COMMAND + parent.getId());

        InlineKeyboardButton newDirButton = new InlineKeyboardButton();
        newDirButton.setText(Emoji.NEW.getEmoji() + "Папка");
        newDirButton.setCallbackData(CALLBACK_MAKE_DIR_COMMAND + parent.getId());

        addButtonRow.add(addButton);
        addButtonRow.add(newDirButton);

        List<InlineKeyboardButton> managingRow = new ArrayList<>();

        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + parent.getId());

        if (parent.getId() != 0L) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.BACK.getEmoji() + "Вверх");
            backButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + parent.getParentId());
            managingRow.add(backButton);
        }

        rows.add(addButtonRow);
        rows.add(managingRow);

        return rows;
    }

    private String getEmojiByType(String mimeType) {
        if (mimeType == null) {
            return Emoji.FOLDER.getEmoji();
        } else if (mimeType.startsWith("audio")) {
            return Emoji.HEADPHONE.getEmoji();
        } else if (mimeType.startsWith("image")) {
            return Emoji.PICTURE.getEmoji();
        } else if (mimeType.startsWith("text")) {
            return Emoji.CLIPBOARD.getEmoji();
        } else if (mimeType.startsWith("video")) {
            return Emoji.MOVIE_CAMERA.getEmoji();
        } else {
            return Emoji.MEMO.getEmoji();
        }
    }
}
