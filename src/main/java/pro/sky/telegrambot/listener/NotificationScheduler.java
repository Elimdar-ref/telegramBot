package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {

    private final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private TelegramBot telegramBot;

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