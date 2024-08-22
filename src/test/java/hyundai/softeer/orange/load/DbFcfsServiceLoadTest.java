package hyundai.softeer.orange.load;

import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEvent;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventRepository;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventWinningInfoRepository;
import hyundai.softeer.orange.event.fcfs.service.DbFcfsService;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class DbFcfsServiceLoadTest {

    @Autowired
    private DbFcfsService dbFcfsService;

    @Autowired
    private FcfsEventRepository fcfsEventRepository;

    @Autowired
    private EventUserRepository eventUserRepository;

    @Autowired
    private FcfsEventWinningInfoRepository fcfsEventWinningInfoRepository;

    @Autowired
    private RedisTemplate<String, Boolean> booleanRedisTemplate;

    Long numberOfWinners = 100L;
    int numberOfThreads = 200; // 스레드 수
    int numberOfUsers = 1000; // 동시 참여 사용자 수
    String eventId = "HD_240808_001";

    @Autowired
    private EventMetadataRepository eventMetadataRepository;
    @Qualifier("stringRedisTemplate")
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        // 초기화
        fcfsEventWinningInfoRepository.deleteAll();
        eventUserRepository.deleteAll();
        fcfsEventRepository.deleteAll();
        booleanRedisTemplate.getConnectionFactory().getConnection().flushAll();
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();

        // 이벤트 생성
        EventMetadata eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .build();
        eventMetadataRepository.save(eventMetadata);
        FcfsEvent fcfsEvent = FcfsEvent.of(Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), numberOfWinners, "prizeInfo", eventMetadata);
        fcfsEventRepository.save(fcfsEvent);
        stringRedisTemplate.opsForValue().set(eventId, fcfsEvent.getId().toString());

        // 유저 생성
        for (int i = 0; i < numberOfUsers; i++) {
            eventUserRepository.save(EventUser.of("user", "phone" + i, null, "user"+i));
        }
    }

    @AfterEach
    void tearDown() {
        fcfsEventWinningInfoRepository.deleteAll();
        eventUserRepository.deleteAll();
        fcfsEventRepository.deleteAll();
    }

    @Test
    void participateTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        for (int i = 0; i < numberOfUsers; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    boolean result = dbFcfsService.participate(eventId, "user" + index);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long endTime = System.currentTimeMillis();
        log.info("Total time: {} ms", endTime - startTime);
        assertThat(fcfsEventWinningInfoRepository.count()).isEqualTo(numberOfWinners);
        executorService.shutdown();
    }
}
