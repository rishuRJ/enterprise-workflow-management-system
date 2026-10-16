package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestHistoryResponseDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.dto.UserSummaryDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.RequestHistory;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Action;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.event.RequestLifecycleEvent;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.mapper.RequestMapper;
import com.rishu.workflow.repository.RequestRepository;
import com.rishu.workflow.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private RequestMapper requestMapper;
    @Mock private UserRepository userRepository;
    @Mock private RequestHistoryService requestHistoryService;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User employee() {
        return User.builder().id(1L).email("employee@test.com").role(Role.ROLE_EMPLOYEE).build();
    }

    private User manager() {
        return User.builder().id(2L).email("manager@test.com").role(Role.ROLE_MANAGER).build();
    }

    private User admin() {
        return User.builder().id(3L).email("admin@test.com").role(Role.ROLE_ADMIN).build();
    }
    @Nested
    class CreateRequest
    {
        @Test
        void throwsWhenUserIsAdmin() {

            when(currentUserService.getCurrentUser()).thenReturn(admin());

            assertThatThrownBy(() -> requestService.createRequest(new CreateRequestDto()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requestRepository, outboxService);
        }

        @Test
        void throwsWhenGivenManagerIdDoesNotExist() {
            when(currentUserService.getCurrentUser()).thenReturn(employee());

            CreateRequestDto dto = CreateRequestDto.builder()
                    .managerId(1L)
                    .build();

            when(userRepository.findById(dto.getManagerId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> requestService.createRequest(dto)).isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(requestRepository, outboxService);

        }

        @Test
        void throwsWhenGivenManagerIdIsNotOfManager() {
            when(currentUserService.getCurrentUser()).thenReturn(employee());

            CreateRequestDto dto = CreateRequestDto.builder()
                    .managerId(99L)
                    .build();
            when(userRepository.findById(dto.getManagerId())).thenReturn(Optional.of(employee()));

            assertThatThrownBy(() -> requestService.createRequest(dto)).isInstanceOf(BusinessException.class);

            verifyNoInteractions(requestRepository, outboxService);

        }

        @Test
        void requestSavedWhenUserIsNotAdminAndManagerExist() {
            CreateRequestDto dto = validDto();
            User employee = employee();
            when(currentUserService.getCurrentUser()).thenReturn(employee);
            User manager = manager();
            when(userRepository.findById(dto.getManagerId())).thenReturn(Optional.of(manager));

            requestService.createRequest(dto);

            ArgumentCaptor<Request> savedRequest = ArgumentCaptor.forClass(Request.class);
            verify(requestRepository).save(savedRequest.capture());

            assertThat(savedRequest.getValue().getTitle()).isEqualTo(dto.getTitle());
            assertThat(savedRequest.getValue().getDescription()).isEqualTo(dto.getDescription());
            assertThat(savedRequest.getValue().getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(savedRequest.getValue().getEmployee()).isSameAs(employee);
            assertThat(savedRequest.getValue().getManager()).isSameAs(manager);
        }

        @Test
        void requestHistoryServiceAndOutboxServiceSaved() {
            CreateRequestDto dto = validDto();
            User employee = employee();
            when(currentUserService.getCurrentUser()).thenReturn(employee);
            when(userRepository.findById(dto.getManagerId())).thenReturn(Optional.of(manager()));

            requestService.createRequest(dto);

            ArgumentCaptor<RequestLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(RequestLifecycleEvent.class);
            verify(outboxService).save(eventCaptor.capture());


            assertThat(eventCaptor.getValue().action()).isEqualTo(Action.REQUEST_SUBMITTED);
            assertThat(eventCaptor.getValue().employeeEmail()).isEqualTo(employee.getEmail());

            verify(requestHistoryService).saveRequestHistory(
                    any(Request.class), eq(employee), eq(Action.REQUEST_SUBMITTED), isNull());

        }
    }

    @Nested
    class ApproveRequest
    {
        @Test
        void throwsWhenCurrentUserIsNotManager(){
            User currentUser = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            assertThatThrownBy(() -> requestService.approveRequest(1L)).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requestRepository, outboxService);
        }

        @Test
        void throwsForInvalidRequest(){
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> requestService.approveRequest(1L)).isInstanceOf(ResourceNotFoundException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void throwsWhenMangerIsNotSameAsAssignedManager(){
            User employee = employee();
            User currentUser = manager();
            Request request = pendingRequest(employee, User.builder().id(99L).build());
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.approveRequest(1L)).isInstanceOf(AccessDeniedException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);

        }

        @Test
        void throwsWhenRequestStatusIsNotPending(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = Request.builder().id(1L).employee(employee).manager(manager()).status(RequestStatus.APPROVED).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.approveRequest(1L)).isInstanceOf(BusinessException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);

        }

        @Test
        void validRequestApprovalSavedInDB(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = pendingRequest(employee,currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            requestService.approveRequest(1L);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
            verify(requestRepository).save(request);
        }

        @Test
        void validRequestApprovalHistoryAndEventSavedInDB(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = pendingRequest(employee,currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            requestService.approveRequest(1L);

            verify(requestHistoryService).saveRequestHistory(
                    request,currentUser,Action.REQUEST_APPROVED, RequestStatus.PENDING );

            ArgumentCaptor<RequestLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(RequestLifecycleEvent.class);

            verify(outboxService).save(eventCaptor.capture());

            assertThat(eventCaptor.getValue().action()).isEqualTo(Action.REQUEST_APPROVED);
            assertThat(eventCaptor.getValue().employeeEmail()).isEqualTo(employee.getEmail());
            assertThat(eventCaptor.getValue().actorEmail()).isEqualTo(currentUser.getEmail());




        }

    }

    @Nested
    class RejectRequest
    {
        @Test
        void throwsWhenCurrentUserIsNotManager(){
            User currentUser = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            assertThatThrownBy(() -> requestService.rejectRequest(1L)).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requestRepository, outboxService);
        }

        @Test
        void throwsForInvalidRequest(){
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> requestService.rejectRequest(1L)).isInstanceOf(ResourceNotFoundException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void throwsWhenMangerIsNotSameAsAssignedManager(){
            User employee = employee();
            User currentUser = manager();
            Request request = pendingRequest(employee, User.builder().id(99L).build());
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.rejectRequest(1L)).isInstanceOf(AccessDeniedException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);

        }

        @Test
        void throwsWhenRequestStatusIsNotPending(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = Request.builder().id(1L).employee(employee).manager(manager()).status(RequestStatus.REJECTED).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.rejectRequest(1L)).isInstanceOf(BusinessException.class);

            verify(requestRepository, never()).save(any());
            verifyNoInteractions(outboxService);

        }

        @Test
        void validRequestRejectionSaved(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = pendingRequest(employee,currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            requestService.rejectRequest(1L);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
            verify(requestRepository).save(request);
        }

        @Test
        void validRejectionWritesHistoryAndEvent(){
            User employee = employee();
            User currentUser = manager();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = pendingRequest(employee,currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            requestService.rejectRequest(1L);

            verify(requestHistoryService).saveRequestHistory(
                    request,currentUser,Action.REQUEST_REJECTED, RequestStatus.PENDING );

            ArgumentCaptor<RequestLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(RequestLifecycleEvent.class);

            verify(outboxService).save(eventCaptor.capture());

            assertThat(eventCaptor.getValue().action()).isEqualTo(Action.REQUEST_REJECTED);
            assertThat(eventCaptor.getValue().employeeEmail()).isEqualTo(employee.getEmail());
            assertThat(eventCaptor.getValue().actorEmail()).isEqualTo(currentUser.getEmail());

        }

    }

    @Nested
    class GetAllRequest{

        @Test
        void noRequestInDb(){
            when(requestRepository.findAll()).thenReturn(List.of());

            requestService.getAllRequest();
            assertThat(requestService.getAllRequest()).isEmpty();
        }

        @Test
        void returnListOfDtos(){
            Request request1 = Request.builder().id(1L).employee(employee()).build();
            Request request2 = Request.builder().id(2L).employee(employee()).build();
            RequestResponseDto dto1 = RequestResponseDto.builder().id(1L).build();
            RequestResponseDto dto2 = RequestResponseDto.builder().id(2L).build();
            when(requestRepository.findAll()).thenReturn(List.of(request1,request2));
            when(requestMapper.toDto(request1)).thenReturn(dto1);
            when(requestMapper.toDto(request2)).thenReturn(dto2);
            List<RequestResponseDto> result = requestService.getAllRequest();


            assertThat(result).containsExactly(dto1, dto2);
        }
    }

    @Nested
    class GetRequestHistory{

        @Test
        void throwsWhenInvalidRequestId(){
            User employee = employee();
            when(currentUserService.getCurrentUser()).thenReturn(employee);
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> requestService.getRequestHistory(1L)).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(requestHistoryService);
        }

        @Test
        void authorizedUserCanAccessGetRequestHistoryMethod(){
            User employee = employee();
            when(currentUserService.getCurrentUser()).thenReturn(employee);
            Request request = Request.builder().id(1L).employee(employee).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            RequestHistory requestHistory1 = RequestHistory.builder().id(1L).request(request).build();
            RequestHistory requestHistory2 = RequestHistory.builder().id(2L).request(request).build();
            RequestHistoryResponseDto dto1 = RequestHistoryResponseDto.builder().action(Action.REQUEST_SUBMITTED).build();
            RequestHistoryResponseDto dto2 = RequestHistoryResponseDto.builder().action(Action.REQUEST_APPROVED).build();
            when(requestHistoryService.getHistory(request)).thenReturn(List.of(requestHistory1,requestHistory2));
            when(requestMapper.toHistoryDto(requestHistory1)).thenReturn(dto1);
            when(requestMapper.toHistoryDto(requestHistory2)).thenReturn(dto2);
            List<RequestHistoryResponseDto> result = requestService.getRequestHistory(1L);
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        void throwsWhenUnauthorizedUserTryingToGetRequestHistoryMethod(){
            when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(99L).build());
            when(requestRepository.findById(1L)).thenReturn(Optional.of(Request.builder().id(1L).employee(employee()).manager(manager()).build()));

            assertThatThrownBy(() -> requestService.getRequestHistory(1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(requestHistoryService);
        }
    }


    @Nested
    class GetRequestById{

        @Test
        void invalidRequestId(){
            User currentUser = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            when(requestRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> requestService.getRequestById(1L)).isInstanceOf(ResourceNotFoundException.class);

        }

        @Test
        void adminCanAccessAllRequests(){
            User currentUser = admin();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = Request.builder().id(1L).employee(employee()).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            RequestResponseDto dto = RequestResponseDto.builder().id(1L).build();
            when(requestMapper.toDto(request)).thenReturn(dto);

            assertThat(requestService.getRequestById(1L)).isSameAs(dto);
        }

        @Test
        void employeeWhoRaisedCanSeeRequest(){
            User currentUser = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = Request.builder().id(1L).employee(currentUser).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));


            RequestResponseDto dto = RequestResponseDto.builder().id(1L).build();
            when(requestMapper.toDto(request)).thenReturn(dto);

            assertThat(requestService.getRequestById(1L)).isSameAs(dto);
        }


        @Test
        void managerInRequestCanSeeRequestDetail(){
            User currentUser = manager();
            User employee = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);

            Request request = Request.builder().id(1L).employee(employee).manager(currentUser).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));


            RequestResponseDto dto = RequestResponseDto.builder().id(1L).build();

            when(requestMapper.toDto(request)).thenReturn(dto);

            assertThat(requestService.getRequestById(1L)).isSameAs(dto);
        }

        @Test
        void anyoneElseShouldNotAccessTheRequest(){
            User currentUser = employee();
            when(currentUserService.getCurrentUser()).thenReturn(currentUser);
            Request request = Request.builder().id(1L).employee(User.builder().id(99L).build()).manager(User.builder().id(98L).build()).build();
            when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

            assertThatThrownBy(()-> requestService.getRequestById(1L)).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requestMapper);
        }


    }


    //Helpers
    private CreateRequestDto validDto() {
        return CreateRequestDto.builder()
                .title("title")
                .description("description")
                .managerId(2L)
                .build();
    }

    private Request pendingRequest(User employee, User manager) {
        return Request.builder()
                .id(1L)
                .title("title")
                .status(RequestStatus.PENDING)
                .employee(employee)
                .manager(manager)
                .build();
    }



}
