package com.borrowhub.config;

import com.borrowhub.model.entity.Category;
import com.borrowhub.model.entity.Resource;
import com.borrowhub.model.entity.ResourceUnit;
import com.borrowhub.model.entity.User;
import com.borrowhub.model.enums.*;
import com.borrowhub.repository.CategoryRepository;
import com.borrowhub.repository.ResourceRepository;
import com.borrowhub.repository.ResourceUnitRepository;
import com.borrowhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ResourceRepository resourceRepository,
                           ResourceUnitRepository resourceUnitRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedCategoriesAndResources();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            logger.info("Users already seeded. Skipping user initialization.");
            return;
        }

        logger.info("Seeding default users...");

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@borrowhub.edu");
        admin.setPasswordHash(passwordEncoder.encode(requiredSeedPassword("BORROWHUB_SEED_ADMIN_PASSWORD")));
        admin.setFirstName("Platform");
        admin.setLastName("Administrator");
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setDepartment("Information Technology Services");
        admin.setPhone("+91 98765 00001");
        admin.setInstitutionalId("EMP-ADM-001");

        User faculty = new User();
        faculty.setUsername("faculty1");
        faculty.setEmail("faculty1@borrowhub.edu");
        faculty.setPasswordHash(passwordEncoder.encode(requiredSeedPassword("BORROWHUB_SEED_FACULTY_PASSWORD")));
        faculty.setFirstName("Robert");
        faculty.setLastName("Vance");
        faculty.setRole(Role.FACULTY);
        faculty.setAccountStatus(AccountStatus.ACTIVE);
        faculty.setDepartment("Computer Science & Engineering");
        faculty.setPhone("+91 98765 00002");
        faculty.setInstitutionalId("EMP-FAC-001");

        User staff = new User();
        staff.setUsername("staff1");
        staff.setEmail("staff1@borrowhub.edu");
        staff.setPasswordHash(passwordEncoder.encode(requiredSeedPassword("BORROWHUB_SEED_STAFF_PASSWORD")));
        staff.setFirstName("Sarah");
        staff.setLastName("Jenkins");
        staff.setRole(Role.ADMIN);
        staff.setAccountStatus(AccountStatus.ACTIVE);
        staff.setDepartment("Central Equipment Depot");
        staff.setPhone("+91 98765 00003");
        staff.setInstitutionalId("EMP-STF-001");

        User student = new User();
        student.setUsername("student1");
        student.setEmail("student1@borrowhub.edu");
        student.setPasswordHash(passwordEncoder.encode(requiredSeedPassword("BORROWHUB_SEED_STUDENT_PASSWORD")));
        student.setFirstName("Alexander");
        student.setLastName("Hayes");
        student.setRole(Role.STUDENT);
        student.setAccountStatus(AccountStatus.ACTIVE);
        student.setDepartment("Electronics & Communication");
        student.setPhone("+91 98765 00004");
        student.setInstitutionalId("STU-ENG-2024-001");

        userRepository.saveAll(Arrays.asList(admin, faculty, staff, student));
        logger.info("Default users seeded: admin, faculty1, staff1, student1.");
    }


    private String requiredSeedPassword(String variableName) {
        String value = System.getenv(variableName);
        if (value == null || value.isBlank() || value.startsWith("replace-with-")) {
            throw new IllegalStateException("Set a unique local value for environment variable: " + variableName);
        }
        return value;
    }

    private void seedCategoriesAndResources() {
        if (categoryRepository.count() > 0) {
            logger.info("Categories already seeded. Skipping resource initialization.");
            return;
        }

        logger.info("Seeding categories and resources...");

        Category computing = new Category();
        computing.setName("Computing & Laptops");
        computing.setSlug("computing-laptops");
        computing.setDescription("High-performance workstations, developer laptops, and portable tablets.");
        computing.setIconName("Laptop");
        computing.setDisplayOrder(1);
        computing = categoryRepository.save(computing);

        Category labEq = new Category();
        labEq.setName("Lab Equipment & Sensors");
        labEq.setSlug("lab-equipment");
        labEq.setDescription("Precision electrical, optical, and mechanical engineering testing instruments.");
        labEq.setIconName("Cpu");
        labEq.setDisplayOrder(2);
        labEq = categoryRepository.save(labEq);

        Category av = new Category();
        av.setName("Audio / Visual & Presentation");
        av.setSlug("audio-visual");
        av.setDescription("4K DSLR cameras, microphones, projectors, and broadcasting gear.");
        av.setIconName("Camera");
        av.setDisplayOrder(3);
        av = categoryRepository.save(av);

        Category robotics = new Category();
        robotics.setName("Robotics & Microcontrollers");
        robotics.setSlug("robotics-microcontrollers");
        robotics.setDescription("Development kits, single-board computers, sensors, and actuators.");
        robotics.setIconName("Radio");
        robotics.setDisplayOrder(4);
        robotics = categoryRepository.save(robotics);

        // Resource 1: Dell XPS 15 (INDIVIDUAL)
        Resource laptop = new Resource();
        laptop.setName("Dell XPS 15 9530 Developer Laptop");
        laptop.setResourceCode("LAP-DELL-001");
        laptop.setCategory(computing);
        laptop.setInventoryMode(InventoryMode.INDIVIDUAL);
        laptop.setTotalQuantity(3);
        laptop.setMaxBorrowDays(14);
        laptop.setDescription("Intel Core i7-13700H, 32GB DDR5, 1TB NVMe, RTX 4060, 15.6\" 3.5K OLED Touch");
        laptop.setImageUrl("https://images.unsplash.com/photo-1593642632823-8f785ba67e45?auto=format&fit=crop&w=600&q=80");
        laptop.setReservable(true);
        laptop.setActive(true);
        laptop = resourceRepository.save(laptop);

        ResourceUnit u1 = createUnit(laptop, "BH-LAP-001", "DXP15-2024-001", "Dell XPS 15 9530", PhysicalCondition.NEW, "Depot Shelf A-1");
        ResourceUnit u2 = createUnit(laptop, "BH-LAP-002", "DXP15-2024-002", "Dell XPS 15 9530", PhysicalCondition.GOOD, "Depot Shelf A-1");
        ResourceUnit u3 = createUnit(laptop, "BH-LAP-003", "DXP15-2024-003", "Dell XPS 15 9530", PhysicalCondition.GOOD, "Depot Shelf A-1");
        resourceUnitRepository.saveAll(Arrays.asList(u1, u2, u3));

        // Resource 2: Arduino Mega 2560 (QUANTITY)
        Resource arduino = new Resource();
        arduino.setName("Arduino Mega 2560 Starter Kit");
        arduino.setResourceCode("KIT-ARD-001");
        arduino.setCategory(robotics);
        arduino.setInventoryMode(InventoryMode.QUANTITY);
        arduino.setTotalQuantity(20);
        arduino.setMaxBorrowDays(7);
        arduino.setDescription("ATmega2560, 54 digital I/O pins, 16 analog inputs, bundled with sensor modules and breadboards");
        arduino.setImageUrl("https://images.unsplash.com/photo-1553406830-ef2513450d76?auto=format&fit=crop&w=600&q=80");
        arduino.setReservable(true);
        arduino.setActive(true);
        arduino = resourceRepository.save(arduino);

        // Resource 3: Sony Alpha A7 IV (INDIVIDUAL)
        Resource camera = new Resource();
        camera.setName("Sony Alpha A7 IV 4K Production Kit");
        camera.setResourceCode("CAM-SONY-001");
        camera.setCategory(av);
        camera.setInventoryMode(InventoryMode.INDIVIDUAL);
        camera.setTotalQuantity(2);
        camera.setMaxBorrowDays(3);
        camera.setDescription("33MP Full-Frame Exmor R CMOS Sensor, 4K 60p 10-Bit 4:2:2, FE 24-70mm f/2.8 GM Lens");
        camera.setImageUrl("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=600&q=80");
        camera.setReservable(true);
        camera.setActive(true);
        camera = resourceRepository.save(camera);

        ResourceUnit c1 = createUnit(camera, "BH-CAM-001", "SNY-A7IV-901", "Sony Alpha A7 IV", PhysicalCondition.NEW, "Media Vault Safe 2");
        ResourceUnit c2 = createUnit(camera, "BH-CAM-002", "SNY-A7IV-902", "Sony Alpha A7 IV", PhysicalCondition.GOOD, "Media Vault Safe 2");
        resourceUnitRepository.saveAll(Arrays.asList(c1, c2));

        // Resource 4: Digital Oscilloscope 100MHz (INDIVIDUAL)
        Resource osc = new Resource();
        osc.setName("Tektronix 100MHz Digital Storage Oscilloscope");
        osc.setResourceCode("LAB-OSC-001");
        osc.setCategory(labEq);
        osc.setInventoryMode(InventoryMode.INDIVIDUAL);
        osc.setTotalQuantity(4);
        osc.setMaxBorrowDays(5);
        osc.setDescription("2-Channel, 1 GS/s sampling rate, 20k record length, FFT spectrum analysis");
        osc.setImageUrl("https://images.unsplash.com/photo-1581092335397-9583fe92d232?auto=format&fit=crop&w=600&q=80");
        osc.setReservable(true);
        osc.setActive(true);
        osc = resourceRepository.save(osc);

        ResourceUnit o1 = createUnit(osc, "BH-OSC-001", "TEK-TBS-001", "Tektronix TBS1102B", PhysicalCondition.GOOD, "Lab Bench Bay 3");
        ResourceUnit o2 = createUnit(osc, "BH-OSC-002", "TEK-TBS-002", "Tektronix TBS1102B", PhysicalCondition.GOOD, "Lab Bench Bay 3");
        ResourceUnit o3 = createUnit(osc, "BH-OSC-003", "TEK-TBS-003", "Tektronix TBS1102B", PhysicalCondition.GOOD, "Lab Bench Bay 3");
        ResourceUnit o4 = createUnit(osc, "BH-OSC-004", "TEK-TBS-004", "Tektronix TBS1102B", PhysicalCondition.GOOD, "Lab Bench Bay 3");
        resourceUnitRepository.saveAll(Arrays.asList(o1, o2, o3, o4));

        // Resource 5: Raspberry Pi 4 (QUANTITY)
        Resource rpi = new Resource();
        rpi.setName("Raspberry Pi 4 Model B (8GB RAM)");
        rpi.setResourceCode("RPI-4B-001");
        rpi.setCategory(robotics);
        rpi.setInventoryMode(InventoryMode.QUANTITY);
        rpi.setTotalQuantity(15);
        rpi.setMaxBorrowDays(10);
        rpi.setDescription("Quad core Cortex-A72 @ 1.5GHz, 8GB LPDDR4, dual micro-HDMI 4K, gigabit Ethernet");
        rpi.setImageUrl("https://images.unsplash.com/photo-1517077304055-6e89abbf09b0?auto=format&fit=crop&w=600&q=80");
        rpi.setReservable(true);
        rpi.setActive(true);
        rpi = resourceRepository.save(rpi);

        logger.info("Sample catalog seeded: 5 resources, 9 individual units, categories.");
    }

    private ResourceUnit createUnit(Resource resource, String assetCode, String serial, String model, PhysicalCondition condition, String location) {
        ResourceUnit unit = new ResourceUnit();
        unit.setResource(resource);
        unit.setAssetCode(assetCode);
        unit.setQrCodeData("BORROWHUB://UNIT/" + assetCode);
        unit.setSerialNumber(serial);
        unit.setBrandModel(model);
        unit.setPhysicalCondition(condition);
        unit.setStatus(UnitStatus.AVAILABLE);
        unit.setStorageLocation(location);
        unit.setAcquisitionDate(LocalDate.now());
        return unit;
    }
}
