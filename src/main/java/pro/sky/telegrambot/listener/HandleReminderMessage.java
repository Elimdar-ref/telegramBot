package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HandleReminderMessage {

    private final Logger logger = LoggerFactory.getLogger(HandleReminderMessage.class);

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private TelegramBot telegramBot;

    private final Pattern REMINDER_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})(\\s+)(.+)");

    private final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void handleStartCommand(Long chatId) {
        String messageText = "Добро пожаловать! Я ваш телеграм-бот.\n\n" +
                "📝 Чтобы создать напоминание, отправьте сообщение в формате:\n" +
                "ДД.ММ.ГГГГ ЧЧ:ММ Текст напоминания\n\n" +
                "✅ Пример:\n" +
                "01.01.2022 20:00 Сделать домашнюю работу";
        sendMessage(chatId, messageText);
        logger.info("Sent welcome message to chatId: {}", chatId);
    }

    public void handleReminderMessage(Long chatId, String text, Message message) {
        Matcher matcher = REMINDER_PATTERN.matcher(text);

        if (matcher.matches()) {
            String date = matcher.group(1);
            String item = matcher.group(3);

            logger.debug("Extracted - Date: '{}', Item: '{}'", date, item);

            LocalDateTime notificationDateTime = LocalDateTime.parse(date, DATE_TIME_FORMATTER);

            NotificationTask task = new NotificationTask(chatId, item, notificationDateTime);

            // Сохраняем в БД
            notificationTaskRepository.save(task);

            // Форматируем дату для ответа
            String formattedDate = notificationDateTime.format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy 'в' HH:mm")
            );

            // Отправляем подтверждение
            String response = String.format(
                    "Напоминание создано!\n\n" +
                            "Когда: %s\n" +
                            "Что: %s",
                    formattedDate,
                    item
            );
            sendMessage(chatId, response);
            logger.info("Created reminder for chat {}: '{}' at {}", chatId, item, notificationDateTime);
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        telegramBot.execute(message);
    }
}
