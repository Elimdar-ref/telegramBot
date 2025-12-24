package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.listener.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByChatId(Long chatId);

    @Query("SELECT t FROM NotificationTask t " + "WHERE t.notificationDateTime >= :start " +
            "AND t.notificationDateTime < :end")
    List<NotificationTask> findTasksForMinuteRange(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}