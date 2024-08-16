package hyundai.softeer.orange.comment.repository;

import hyundai.softeer.orange.comment.entity.Comment;
import org.springframework.data.jpa.domain.Specification;

public class CommentSpecification {
    public static Specification<Comment> matchEventId(String eventId) {
        return (comment, query, cb) -> cb.equal(

                comment.join("eventFrame")
                        .join("eventMetadataList")
                        .get("eventId"), eventId);
    }

    public static Specification<Comment> searchOnContent(String search, boolean conjunctionOnNull) {
        return (comment, query, cb) -> {
            if (search == null || search.isEmpty()) return (conjunctionOnNull ? cb.conjunction() : cb.disjunction());
            return cb.like(comment.get("content"), "%" + search + "%");
        };
    }

    public static Specification<Comment> searchOnContent(String search) {
        return searchOnContent(search, true);
    }
}
