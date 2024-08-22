package hyundai.softeer.orange.eventuser.repository;

import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class EventUserSpecification {
    public static Specification<EventUser> search(String search, String field) {
        return (user, query, cb) -> {
            user.fetch("eventFrame");

//            Join<EventUser, EventFrame> join = user.join("eventFrame", JoinType.LEFT);

            if("userName".equals(field)) return cb.like(user.get("userName"), "%" + search + "%");
            else if ("phoneNumber".equals(field)) return cb.like(user.get("phoneNumber"), "%" + search + "%");
            else if ("frameId".equals(field)) return cb.like(user.get("eventFrame").get("eventFrame").get("frameId"), "%" + search + "%");
            return cb.conjunction();
        };
    }
}
