package com.rishu.workflow.service;

import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.RequestHistory;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Action;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.repository.RequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@RequiredArgsConstructor
@Service
public class RequestHistoryService {

    private final RequestHistoryRepository requestHistoryRepository;

    public void saveRequestHistory(Request request, User actor, Action action, RequestStatus previousStatus) {

        RequestHistory requestHistory = RequestHistory.builder()
                .action(action)
                .actor(actor)
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .request(request)
                .occurredAt(request.getUpdatedAt())
        .build();

        requestHistoryRepository.save(requestHistory);
    }

    public List<RequestHistory> getHistory(Request request){
        return requestHistoryRepository.findByRequestOrderByOccurredAtAsc(request);
    }
}
