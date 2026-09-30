package com.spms.config;

import com.spms.entity.*;
import com.spms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds demo data on startup so the system can be tested immediately, per
 * spec section 18. Only runs if the users table is empty, so it is safe to
 * restart the app without duplicating data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final OfficerRepository officerRepository;
    private final ProcurementCenterRepository centerRepository;
    private final CropRepository cropRepository;
    private final BookingRepository bookingRepository;
    private final TokenRepository tokenRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<ProcurementCenter> centers = synchronizeCenters();
        List<Crop> crops = synchronizeCrops();

        if (userRepository.count() > 0) {
            log.info("Reference catalogs synchronized; demo accounts already exist.");
            return;
        }

        log.info("Seeding demo data for SPMS...");

        ProcurementCenter center1 = centers.get(0);
        ProcurementCenter center2 = centers.get(1);
        Crop paddy = crops.get(0);
        Crop blackGram = crops.get(1);

        // ---- Admin ----
        User admin = userRepository.save(User.builder()
                .name("Admin User").email("admin@spms.gov.in").phone("9000000001")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).build());

        // ---- Officers ----
        User officerUser1 = userRepository.save(User.builder()
                .name("Officer S. Priya").email("officer@spms.gov.in").phone("9000000002")
                .password(passwordEncoder.encode("officer123")).role(Role.OFFICER).build());
        officerRepository.save(Officer.builder()
                .officerId("OFC-1001").user(officerUser1).name("S. Priya").phone("9000000002")
                .email(officerUser1.getEmail()).procurementCenter(center2).build());

        User officerUser2 = userRepository.save(User.builder()
                .name("Officer R. Kumar").email("officer2@spms.gov.in").phone("9000000003")
                .password(passwordEncoder.encode("officer123")).role(Role.OFFICER).build());
        officerRepository.save(Officer.builder()
                .officerId("OFC-1002").user(officerUser2).name("R. Kumar").phone("9000000003")
                .email(officerUser2.getEmail()).procurementCenter(center1).build());

        // ---- Farmers ----
        Farmer farmer1 = createFarmer("farmer@spms.gov.in", "farmer123", "FRM-20481", "Ramesh Kumar",
                "9876543210", "Kallakurichi", "Kallakurichi");
        Farmer farmer2 = createFarmer("farmer2@spms.gov.in", "farmer123", "FRM-20482", "Selvam K",
                "9876543211", "Ariyalur", "Kallakurichi");
        Farmer farmer3 = createFarmer("farmer3@spms.gov.in", "farmer123", "FRM-20483", "Lakshmi V",
                "9876543212", "Tirukoilur", "Kallakurichi");

        // ---- Bookings + tokens for today, at center2 (matches officer1's center) ----
        LocalDate today = LocalDate.now();
        int position = 1;

        Booking b1 = createBooking(farmer2, center2, paddy, today, LocalTime.of(9, 30), BookingStatus.WEIGHING, 820.0);
        createToken(b1, position++, TokenStatus.IN_PROGRESS);

        Booking b2 = createBooking(farmer3, center2, blackGram, today, LocalTime.of(10, 0), BookingStatus.IN_QUEUE, 210.0);
        createToken(b2, position++, TokenStatus.WAITING);

        Booking b3 = createBooking(farmer1, center2, paddy, today, LocalTime.of(10, 0), BookingStatus.BOOKED, 640.0);
        createToken(b3, position++, TokenStatus.WAITING);

        queueEntryRepository.save(QueueEntry.builder()
                .procurementCenter(center2).date(today).currentPosition(1).totalPeople(position - 1).build());

        // ---- A completed historical booking + payment for farmer1 ----
        Booking pastBooking = createBooking(farmer1, center1, paddy, today.minusDays(6), LocalTime.of(9, 0),
                BookingStatus.COMPLETED, 820.0);
        paymentRepository.save(Payment.builder()
                .farmer(farmer1).booking(pastBooking).amount(new BigDecimal("18200.00"))
                .paymentDate(today.minusDays(6)).status("Completed")
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build());

        // ---- Notifications for farmer1 ----
        notificationRepository.save(Notification.builder().user(farmer1.getUser())
                .message("Your slot for " + today + ", 10:00 AM at " + center2.getName() + " is confirmed. Token: P-103.")
                .readStatus(false).build());
        notificationRepository.save(Notification.builder().user(farmer1.getUser())
                .message("Payment of Rs.18200.00 for your last procurement has been processed.")
                .readStatus(true).build());

        log.info("Demo data seeded successfully.");
        log.info("Login credentials -> Admin: admin@spms.gov.in / admin123 | " +
                "Officer: officer@spms.gov.in / officer123 | Farmer: farmer@spms.gov.in / farmer123");
    }

        private List<ProcurementCenter> synchronizeCenters() {
                List<String[]> catalog = List.of(
                                new String[]{"Kanchipuram", "Kanchipuram"},
                                new String[]{"Uthiramerur", "Uthiramerur"},
                                new String[]{"Walajabad", "Walajabad"}
                );
                List<ProcurementCenter> existing = centerRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
                for (int index = 0; index < existing.size(); index++) {
                        ProcurementCenter center = existing.get(index);
                        if (index < catalog.size()) {
                                String name = catalog.get(index)[0];
                                String location = catalog.get(index)[1];
                                center.setName(name);
                                center.setLocation(location);
                                center.setDistrict("Kanchipuram");
                                center.setAddress(location + ", Kanchipuram, Tamil Nadu");
                                center.setActive(true);
                        } else {
                                center.setActive(false);
                        }
                }
                centerRepository.saveAll(existing);

                for (int index = existing.size(); index < catalog.size(); index++) {
                        String name = catalog.get(index)[0];
                        String location = catalog.get(index)[1];
                        existing.add(centerRepository.save(ProcurementCenter.builder()
                                        .name(name).location(location).district("Kanchipuram")
                                        .address(location + ", Kanchipuram, Tamil Nadu").active(true).build()));
                }
                return existing.subList(0, catalog.size());
        }

        private List<Crop> synchronizeCrops() {
                List<String[]> catalog = List.of(
                                new String[]{"CO 51 Rice", "CO 51 rice variety"},
                                new String[]{"Short grain rice", "Short-grain rice"},
                                new String[]{"Bamboo rice", "Rice harvested from flowering bamboo"},
                                new String[]{"Pure jasmine rice", "Aromatic jasmine rice"},
                                new String[]{"Wild guardian rice", "Traditional wild rice variety"},
                                new String[]{"Women rice", "Traditional rice variety"},
                                new String[]{"Cumin-grained Rice", "Rice with cumin-like grain shape"},
                                new String[]{"Black rice", "Pigmented whole-grain rice"},
                                new String[]{"Bridegroom's rice", "Traditional bridegroom rice variety"}
                );

                cropRepository.findAll().forEach(crop -> {
                        crop.setActive(false);
                        cropRepository.save(crop);
                });

                return catalog.stream().map(item -> {
                        Crop crop = cropRepository.findByNameIgnoreCase(item[0])
                                        .orElseGet(() -> Crop.builder().name(item[0]).build());
                        crop.setName(item[0]);
                        crop.setDescription(item[1]);
                        crop.setActive(true);
                        return cropRepository.save(crop);
                }).toList();
        }

    private Farmer createFarmer(String email, String rawPassword, String farmerId, String name,
                                 String phone, String village, String district) {
        User user = userRepository.save(User.builder()
                .name(name).email(email).phone(phone)
                .password(passwordEncoder.encode(rawPassword)).role(Role.FARMER).build());

        return farmerRepository.save(Farmer.builder()
                .farmerId(farmerId).user(user).name(name).phone(phone).email(email)
                .village(village).district(district).address(village + ", " + district).build());
    }

    private Booking createBooking(Farmer farmer, ProcurementCenter center, Crop crop, LocalDate date,
                                   LocalTime startTime, BookingStatus status, double quantityKg) {
        return bookingRepository.save(Booking.builder()
                .bookingId("BK-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .farmer(farmer).procurementCenter(center).crop(crop).date(date)
                .startTime(startTime).endTime(startTime.plusMinutes(30))
                .status(status).quantityKg(quantityKg).build());
    }

    private Token createToken(Booking booking, int queuePosition, TokenStatus status) {
        return tokenRepository.save(Token.builder()
                .tokenNumber("P-" + (100 + queuePosition))
                .booking(booking).queuePosition(queuePosition).status(status).build());
    }
}
