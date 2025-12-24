package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final Pattern REMINDER_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})(\\s+)(.+)");

    private final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() != null && update.message().text() != null) {
                String text = update.message().text();
                Long chatId = update.message().chat().id();

                if ("/start".equals(text)) {
                    handleStartCommand(chatId);
                } else {
                    handleReminderMessage(chatId, text, update.message());
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void handleStartCommand(Long chatId) {
        String messageText = "Добро пожаловать! Я ваш телеграм-бот.\n\n" +
                "📝 Чтобы создать напоминание, отправьте сообщение в формате:\n" +
                "ДД.ММ.ГГГГ ЧЧ:ММ Текст напоминания\n\n" +
                "✅ Пример:\n" +
                "01.01.2022 20:00 Сделать домашнюю работу";
        sendMessage(chatId, messageText);
        logger.info("Sent welcome message to chatId: {}", chatId);
    }

    private void handleReminderMessage(Long chatId, String text, Message message) {
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

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        telegramBot.execute(message);
    }

    @Scheduled(cron = "0/59 * * * * *")
    public void checkAndSendNotifications() {
        // Находим задачи для текущей минуты
        LocalDateTime currentMinute = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MINUTES);

        logger.info("Проверка напоминаний на {}",
                currentMinute.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        // Определяем диапазон поиска
        LocalDateTime startOfMinute = currentMinute;
        LocalDateTime endOfMinute = currentMinute.plusMinutes(1);

        // Ищем в БД
        List<NotificationTask> tasks = notificationTaskRepository
                .findTasksForMinuteRange(startOfMinute, endOfMinute);

        logger.info("📤 Найдено {} напоминаний для отправки", tasks.size());

        int sentCount = 0;
        int errorCount = 0;

        // Отправляем каждую задачу
        for (NotificationTask task : tasks) {
            try {
                // Создаем текст сообщение
                String text = "Напоминание: " + task.getMessageText();

                //Создаем объект сообщения
                SendMessage message = new SendMessage(task.getChatId(), text);

                // Отправляем
                telegramBot.execute(message);

                // Удаляем из БД
                notificationTaskRepository.delete(task);
                sentCount++;

                logger.info("Отправлено напоминание #{} в чат {}", task.getId(), task.getChatId());

            } catch (Exception e) {
                errorCount++;
                logger.error("Ошибка, оставляем задачу #{} : {}", task.getId(), e.getMessage());
            }
        }
            logger.info("Итог: отправлено - {}, ошибок - {}", sentCount, errorCount);
        }
    }