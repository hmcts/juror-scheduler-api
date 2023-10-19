package uk.gov.hmcts.juror.scheduler.datastore.repository;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;

import java.time.LocalDateTime;
import java.util.Set;

@Repository
public interface JobRepository extends JpaRepository<APIJobDetailsEntity, String>,
    JpaSpecificationExecutor<APIJobDetailsEntity> {

    interface Specs {

        static Specification<APIJobDetailsEntity> byJobKey(String jobKey) {
            return (root, query, builder) ->
                builder.equal(root.get("key"), jobKey);
        }

        static Specification<APIJobDetailsEntity> byTags(Set<String> tags) {
            return (root, query, builder) -> builder.in(root.join("tags", JoinType.INNER)).value(tags);
        }

        static Specification<APIJobDetailsEntity> byCreateDateGreaterThan(LocalDateTime fromDate) {
            return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("createdAt"), fromDate);
        }

        static Specification<APIJobDetailsEntity> orderByCreatedOn(
            Specification<APIJobDetailsEntity> spec) {
            return (root, query, builder) -> {
                query.orderBy(builder.asc(root.get("createdAt")));
                return spec.toPredicate(root, query, builder);
            };
        }
    }
}

