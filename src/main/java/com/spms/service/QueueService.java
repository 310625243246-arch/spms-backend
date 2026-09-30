package com.spms.service;

import com.spms.entity.*;
import com.spms.repository.QueueEntryRepository;
import com.spms.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final TokenRepository tokenRepository;
    private final QueueEntryRepository queueEntryRepository;

    @Value("${spms.queue.average-service-minutes}")
    private int averageServiceMinutes;

    /**
     * Issues a globally unique, readable token and queue position for a booking,
     * and keeps the center's daily QueueEntry.totalPeople in sync.
     */
    @Transactional
    public Token issueToken(Booking booking) {
        Long centerId = booking.getProcurementCenter().getId();
        LocalDate date = booking.getDate();

        long existingCount = tokenRepository.countByBooking_ProcurementCenterIdAndBooking_Date(centerId, date);
        int queuePosition = (int) existingCount + 1;
        String tokenNumber = "P-" + date.toString().replace("-", "") + "-" + centerId + "-"
            + String.format("%03d", queuePosition);

        Token token = Token.builder()
                .tokenNumber(tokenNumber)
                .booking(booking)
                .queuePosition(queuePosition)
                .status(TokenStatus.WAITING)
                .build();
        token = tokenRepository.save(token);

        QueueEntry queueEntry = queueEntryRepository.findByProcurementCenterIdAndDate(centerId, date)
                .orElseGet(() -> QueueEntry.builder()
                        .procurementCenter(booking.getProcurementCenter())
                        .date(date)
                        .currentPosition(0)
                        .totalPeople(0)
                        .build());
        queueEntry.setTotalPeople(queueEntry.getTotalPeople() + 1);
        queueEntryRepository.save(queueEntry);

        return token;
    }

    public int estimatedWaitMinutes(Long centerId, LocalDate date, int queuePosition) {
        int currentPosition = queueEntryRepository.findByProcurementCenterIdAndDate(centerId, date)
                .map(QueueEntry::getCurrentPosition)
                .orElse(0);
        int peopleAhead = Math.max(0, queuePosition - currentPosition - 1);
        return peopleAhead * averageServiceMinutes;
    }

    public List<Token> todaysQueue(Long centerId, LocalDate date) {
        return tokenRepository.findByBooking_ProcurementCenterIdAndBooking_DateOrderByQueuePositionAsc(centerId, date);
    }

    @Transactional
    public Optional<Token> callNext(Long centerId, LocalDate date) {
        Optional<Token> next = tokenRepository
                .findFirstByBooking_ProcurementCenterIdAndBooking_DateAndStatusOrderByQueuePositionAsc(
                        centerId, date, TokenStatus.WAITING);

        next.ifPresent(token -> {
            token.setStatus(TokenStatus.CALLED);
            token.getBooking().setStatus(BookingStatus.CHECKED_IN);
            tokenRepository.save(token);

            QueueEntry queueEntry = queueEntryRepository.findByProcurementCenterIdAndDate(centerId, date)
                    .orElseGet(() -> QueueEntry.builder()
                            .procurementCenter(token.getBooking().getProcurementCenter())
                            .date(date)
                            .currentPosition(0)
                            .totalPeople(0)
                            .build());
            queueEntry.setCurrentPosition(token.getQueuePosition());
            queueEntryRepository.save(queueEntry);
        });

        return next;
    }
}
