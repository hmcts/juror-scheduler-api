package uk.gov.hmcts.juror.scheduler.datastore.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>, JpaSpecificationExecutor<TaskEntity> {


    void deleteAllByJobKey(String jobKey);

    List<TaskEntity> findAllByJobKey(String jobKey);

    TaskEntity findFirstByJobKeyOrderByCreatedAtDesc(String jobKey);

    Optional<TaskEntity> findByJobKeyAndTaskId(String jobKey, long taskId);

    interface Specs {

        static Specification<TaskEntity> byJobKey(String jobKey) {
            return (root, query, builder) ->
                builder.equal(root.get("job").get("key"), jobKey);
        }

        static Specification<TaskEntity> byStatus(Set<Status> statuses) {
            return (root, query, builder) ->
                builder.in(root.get("status")).value(statuses);
        }

        static Specification<TaskEntity> byCreateDateGreaterThan(LocalDateTime fromDate) {
            return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("createdAt"), fromDate);
        }

        static Specification<TaskEntity> orderByCreatedOn(
            Specification<TaskEntity> spec) {
            return (root, query, builder) -> {
                query.orderBy(builder.asc(root.get("createdAt")));
                return spec.toPredicate(root, query, builder);
            };
        }
    }
}
