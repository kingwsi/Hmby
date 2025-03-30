package org.example.hmby;

import jakarta.transaction.Transactional;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.repository.MediaInfoRepository;
import org.example.hmby.sceurity.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = HmbyApplication.class)
class HmbyApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(HmbyApplicationTests.class);

    @Autowired
    private MediaInfoRepository mediaInfoRepository;

    public static void main(String[] args) {
        String token = SecurityUtils.generateToken("1", "dev", "0");
        log.info("test token: {}", token);
    }
    
    @Test
    @Transactional
    void auditTest() {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setUserId("00");
        mediaInfo.setCodec("hevc");
        mediaInfo.setFileName("test");
        mediaInfoRepository.save(mediaInfo);
        mediaInfoRepository.findById(mediaInfo.getId()).ifPresent(info -> {
            log.info("mediaInfo: {}", info.getCreatedDate());
        });
    }

}
