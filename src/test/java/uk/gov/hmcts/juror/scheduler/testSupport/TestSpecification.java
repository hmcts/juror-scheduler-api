package uk.gov.hmcts.juror.scheduler.testSupport;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;

public class TestSpecification implements Specification<APIJobDetailsEntity> {


    private final String name;

    public TestSpecification(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Predicate toPredicate(Root<APIJobDetailsEntity> root, CriteriaQuery<?> query,
                                 CriteriaBuilder criteriaBuilder) {
        throw new UnsupportedOperationException("Only here to met contract");
    }
}
