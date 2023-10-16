package uk.gov.hmcts.juror.scheduler.testsupport;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;

public record TestSpecification(String name) implements Specification<APIJobDetailsEntity> {


    @Override
    public Predicate toPredicate(Root<APIJobDetailsEntity> root, CriteriaQuery<?> query,
                                 CriteriaBuilder criteriaBuilder) {
        throw new UnsupportedOperationException("Only here to met contract");
    }
}
