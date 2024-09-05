package hyundai.softeer.orange.event.common.repository;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.entity.QEventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.dto.BriefEventDto;
import hyundai.softeer.orange.event.dto.EventSearchHintDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CustomEventMetadataRepositoryImpl implements CustomEventMetadataRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<BriefEventDto> findBriefsBySearch(String search, Set<EventType> eventTypes, Pageable pageable) {
        QEventMetadata event = QEventMetadata.eventMetadata;

        var searchOnName = EventBooleanOp.searchOnName(search);
        var searchOnEventId = EventBooleanOp.searchOnEventId(search);
        var isEventTypeIn = EventBooleanOp.isEventTypeIn(eventTypes);

        BooleanExpression condition = searchOnName.or(searchOnEventId).and(isEventTypeIn);

        var query = queryFactory.from(event)
                .where(condition);
        if (pageable instanceof PageRequest pageRequest) {
            var sort = pageRequest.getSort();
            query.orderBy(parseSort(sort));
        }

        var events = query.
                select(
                        Projections.fields(
                                BriefEventDto.class,
                                event.eventId, event.name, event.startTime, event.endTime, event.eventType)
                )
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        var count = query.select(event.count()).fetchOne();
        assert count != null;

        return new PageImpl<>(events, pageable, count);
    }

    @Override
    public List<EventSearchHintDto> findHintsBySearch(String search) {
        QEventMetadata event = QEventMetadata.eventMetadata;

        var searchOnEventIdDefaultReject = EventBooleanOp.searchOnEventIdDefaultReject(search);
        var isDrawEvent = EventBooleanOp.isEventTypeOf(EventType.draw);

        var condition = searchOnEventIdDefaultReject.and(isDrawEvent);

        return queryFactory.from(event)
                .select(
                        Projections.fields(
                        EventSearchHintDto.class,
                        event.eventId, event.name
                )
                ).where(condition)
                .fetch();
    }

    private OrderSpecifier<?>[] parseSort(Sort sort) {
        PathBuilder<EventMetadata> entityPath = new PathBuilder<>(EventMetadata.class, "entity");

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Expression<?> ex = entityPath.get(order.getProperty());

            boolean ascending = order.getDirection().isAscending();
            if (ascending) {
                orders.add(new OrderSpecifier(Order.ASC, ex));
            } else {
                orders.add(new OrderSpecifier(Order.DESC, ex));
            }
        }
        // findBy를 이용하려면 Sort와 Page를 하나로 몰아넣으면 안된다.
        return orders.toArray(OrderSpecifier[]::new);
    }

}
