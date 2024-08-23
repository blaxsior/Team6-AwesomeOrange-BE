package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.event.draw.component.picker.PickTarget;
import hyundai.softeer.orange.event.draw.component.picker.WinnerPicker;
import hyundai.softeer.orange.event.draw.component.score.ScoreCalculator;
import hyundai.softeer.orange.event.draw.dto.DrawEventWinningInfoBulkInsertDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.entity.DrawEventMetadata;
import hyundai.softeer.orange.event.draw.entity.DrawEventScorePolicy;
import hyundai.softeer.orange.event.draw.repository.DrawEventRepository;
import hyundai.softeer.orange.event.draw.repository.DrawEventWinningInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 실제 추첨 작업을 진행하는 클래스
 */
@RequiredArgsConstructor
@Component
public class DrawEventDrawMachine {
    private final DrawEventWinningInfoRepository deWinningInfoRepository;
    private final DrawEventRepository drawEventRepository;
    private final WinnerPicker picker;
    private final ScoreCalculator calculator;

    @Async
    @Transactional
    public CompletableFuture<Void> draw(DrawEvent drawEvent) {
        try{
            Thread.sleep(1000 * 5 * 1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long drawEventRawId = drawEvent.getId();
        // 점수 계산. 추후 추첨 과정과 분리될 수도 있음.
        List<DrawEventScorePolicy> policies = drawEvent.getPolicyList();
        var userScoreMap = calculator.calculate(drawEventRawId, policies);

        // 추첨 타겟 리스트 생성
        List<PickTarget> targets = userScoreMap.entrySet().stream()
                .map(it -> new PickTarget(it.getKey(), it.getValue())).toList();

        // 몇 등이 몇명이나 있는지 적혀 있는 정보. 등급끼리 정렬해서 1 ~ n 등 순서로 정렬
        // 확률 높은 사람이 손해보면 안됨
        List<DrawEventMetadata> metadataList = drawEvent.getMetadataList();
        metadataList.sort(Comparator.comparing(DrawEventMetadata::getGrade));

        // 총 당첨 인원 설정
        long pickCount = metadataList.stream().mapToLong(DrawEventMetadata::getCount).sum();

        // 당첨된 인원 구하기
        var pickedTargets = picker.pick(targets, pickCount);

        var insertTargets = makeDrawEventWinningInfo(pickedTargets, metadataList, drawEventRawId);
        deWinningInfoRepository.insertMany(insertTargets);

        drawEvent.setDrawn(true);
        drawEventRepository.save(drawEvent);
        // TODO: draw event 이미 추첨한 것으로 변경

        return CompletableFuture.completedFuture(null);
    }

    protected List<DrawEventWinningInfoBulkInsertDto> makeDrawEventWinningInfo(List<PickTarget> pickedTargets, List<DrawEventMetadata> metadataList, Long drawEventRawId) {
        List<DrawEventWinningInfoBulkInsertDto> insertTargets = new ArrayList<>();
        int mdIdx = -1;
        long remain = 0;
        long grade = -1;
        DrawEventMetadata metadata = null;

        for(var target : pickedTargets) {
            if(remain <= 0) {
                mdIdx++;
                metadata = metadataList.get(mdIdx);
                grade = metadata.getGrade();
                remain = metadata.getCount();
            }

            insertTargets.add(DrawEventWinningInfoBulkInsertDto.of(
                    target.key(),
                    grade,
                    drawEventRawId
            ));
            remain--;
        }
        return insertTargets;
    }
}
