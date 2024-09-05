package hyundai.softeer.orange.event.common.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import hyundai.softeer.orange.event.common.entity.QEventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;

import java.util.Set;

public class EventBooleanOp {
    public static BooleanExpression searchOnName(String search) {
        return search == null ? Expressions.TRUE : QEventMetadata.eventMetadata.name.contains(search);
    }

    public static BooleanExpression searchOnEventId(String search) {
        return search == null ? Expressions.TRUE : QEventMetadata.eventMetadata.eventId.contains(search);
    }

    public static BooleanExpression searchOnEventIdDefaultReject(String search) {
        return search == null ? Expressions.FALSE : QEventMetadata.eventMetadata.eventId.contains(search);
    }

    public static BooleanExpression isEventTypeOf(EventType eventType) {
        return QEventMetadata.eventMetadata.eventType.eq(eventType);
    }

    public static BooleanExpression isEventTypeIn(Set<EventType> types) {
            if (types.isEmpty() || types.size() == EventType.values().length) return null;
            return QEventMetadata.eventMetadata.eventType.in(types);
    }
}
