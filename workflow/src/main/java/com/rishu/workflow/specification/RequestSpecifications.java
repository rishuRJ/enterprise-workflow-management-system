package com.rishu.workflow.specification;

import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class RequestSpecifications {
    public static Specification<Request> hasEmployee(User employee) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("employee"), employee);
    }

    public static Specification<Request> hasStatuses(List<RequestStatus> statuses) {
        if(statuses == null || statuses.isEmpty()){
            return null;
        }
        return (root, query, criteriaBuilder) ->
                (root.get("status").in(statuses));
    }

    public static Specification<Request> hasCreatedAt(LocalDateTime createdAt) {
        if(createdAt == null){
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("createdAt"), createdAt);
    }

    public static Specification<Request> hasManager(User manager) {
        if(manager == null){
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("manager"), manager);
    }

    public static Specification<Request> hasManagerId(Long managerId) {
        if(managerId == null){
            return null;
        }
        return (root, query, cb) ->
                cb.equal(
                        root.get("manager").get("id"),
                        managerId
                );
    }

    public static Specification<Request> titleContains(String title) {
        if(title == null || title.isBlank()){
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + title.toLowerCase() + "%"
        );
    }

    public static Specification<Request> hasUpdatedAt(LocalDateTime updatedAt) {
        if(updatedAt == null){
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("updatedAt"), updatedAt);
    }
}
