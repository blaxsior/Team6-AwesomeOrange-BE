package hyundai.softeer.orange.event.url.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.url.dto.ResponseUrlDto;
import hyundai.softeer.orange.event.url.entity.Url;
import hyundai.softeer.orange.event.url.exception.UrlException;
import hyundai.softeer.orange.event.url.repository.UrlRepository;
import hyundai.softeer.orange.event.url.util.UrlTypeValidation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);
    private final UrlRepository urlRepository;

    @Transactional
    public ResponseUrlDto generateUrl(String originalUrl) {
        if(!UrlTypeValidation.isValidURL(originalUrl)){
            throw new UrlException(ErrorCode.INVALID_URL);
        }

        // originalUrl에 userId 추가
        String shortUrl = generateShortUrl();
        while(urlRepository.existsByShortUrl(shortUrl)){
            shortUrl = generateShortUrl();
        }

        Url url = Url.of(originalUrl, shortUrl);
        urlRepository.save(url);
        log.info("shortUrl generated: {}", shortUrl);
        return new ResponseUrlDto(shortUrl);
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlException(ErrorCode.SHORT_URL_NOT_FOUND));
        log.info("shortUrl {}'s originalUrl fetched: {}", shortUrl, url.getOriginalUrl());
        return url.getOriginalUrl();
    }

    private String generateShortUrl() {
        String characters = ConstantUtil.CHARACTERS;
        Random random = new Random();
        StringBuilder shortUrlsb = new StringBuilder(ConstantUtil.SHORT_URL_LENGTH);
        for (int i = 0; i < ConstantUtil.SHORT_URL_LENGTH; i++) {
            shortUrlsb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortUrlsb.toString();
    }
}
