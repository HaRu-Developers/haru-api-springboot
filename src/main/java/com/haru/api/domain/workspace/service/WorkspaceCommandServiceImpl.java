package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.converter.UserDocumentLastOpenedConverter;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.entity.Meeting;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.entity.MoodTracker;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.converter.WorkspaceConverter;
import com.haru.api.domain.workspace.dto.WorkspaceRequestDTO;
import com.haru.api.domain.workspace.dto.WorkspaceResponseDTO;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.domain.workspaceInvitation.entity.WorkspaceInvitation;
import com.haru.api.domain.workspaceInvitation.repository.WorkspaceInvitationRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceInvitationHandler;
import com.haru.api.infra.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceCommandServiceImpl implements WorkspaceCommandService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final MeetingRepository meetingRepository;
    private final SnsEventRepository snsEventRepository;
    private final MoodTrackerRepository moodTrackerRepository;
    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;

    private final EmailSender emailSender;

    @Value("${invite-url}")
    private String inviteBaseUrl;

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace createWorkspace(Long userId, WorkspaceRequestDTO.WorkspaceCreateRequest request) {

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // workspace 생성 및 저장
        Workspace workspace = workspaceRepository.save(Workspace.builder()
                .title(request.getName())
                .build());

        // s3에 사진 추가하는 메서드

        // request로 받은 이메일로 초대 메일 전송하는 메서드

        // users_workspaces 테이블에 생성자 정보 저장
        userWorkspaceRepository.save(UserWorkspace.builder()
                .user(foundUser)
                .workspace(workspace)
                .auth(Auth.ADMIN)
                .build());

        return WorkspaceConverter.toWorkspaceDTO(workspaceRepository.save(workspace));
    }

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace updateWorkspace(Long userId, Long workspaceId, WorkspaceRequestDTO.WorkspaceUpdateRequest request) {

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(foundWorkspace.getId(), foundUser.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if(userWorkspace.getAuth() != Auth.ADMIN)
            throw new WorkspaceHandler(ErrorStatus.WORKSPACE_MODIFY_NOT_ALLOWED);

        foundWorkspace.updateTitle(request.getTitle());

        return WorkspaceConverter.toWorkspaceDTO(foundWorkspace);
    }

    @Transactional
    @Override
    public WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token) {

        WorkspaceInvitation foundWorkspaceInvitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new WorkspaceInvitationHandler(ErrorStatus.INVITATION_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(foundWorkspaceInvitation.getWorkspace().getId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        // 이미 수락된 초대장이면 예외 발생
        if(foundWorkspaceInvitation.isAccepted())
            throw new WorkspaceInvitationHandler(ErrorStatus.ALREADY_ACCEPTED);

        // 초대받은 이메일로 가입된 사용자가 있는지 확인
        Optional<User> foundUser = userRepository.findByEmail(foundWorkspaceInvitation.getEmail());

        boolean isAlreadyRegistered = foundUser.isPresent();

        // 이미 가입된 사용자
        if(isAlreadyRegistered) {
            // 초대장을 수락했다고 db에 저장
            foundWorkspaceInvitation.setAccepted();
        } else {
            // 가입되지 않은 사용자면 not success
            return WorkspaceConverter.toInvitationAcceptResult(false, false, foundWorkspace);
        }

        // 가입된 사용자인 경우 워크스페이스에 추가
        userWorkspaceRepository.save(UserWorkspace.builder()
                .workspace(foundWorkspace)
                .user(foundUser.get())
                .auth(Auth.MEMBER)
                .build());

        // 각 문서 조회
        // 각 문서 UserDocumentLastOpened로 변환
        List<UserDocumentLastOpened> userDocumentLastOpenedList = addDocumentsToUserLastOpened(foundWorkspace, foundUser.get());

        // 워크스페이스에 속해있는 모든 문서를 user_document_last_opened에 추가
        // last_opened는 null
        userDocumentLastOpenedList.forEach(userDocumentLastOpened -> {
            userDocumentLastOpenedRepository.saveAll(userDocumentLastOpenedList);
        });

        return WorkspaceConverter.toInvitationAcceptResult(true, true, foundWorkspace);
    }

    @Transactional
    @Override
    public  WorkspaceResponseDTO.InvitationAcceptResult acceptInvite(String token, User signedUser) {
        WorkspaceInvitation foundWorkspaceInvitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new WorkspaceInvitationHandler(ErrorStatus.INVITATION_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(foundWorkspaceInvitation.getWorkspace().getId())
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        if(foundWorkspaceInvitation.isAccepted())
            throw new WorkspaceInvitationHandler(ErrorStatus.ALREADY_ACCEPTED);

        foundWorkspaceInvitation.setAccepted();

        // 가입된 사용자인 경우 워크스페이스에 추가
        userWorkspaceRepository.save(UserWorkspace.builder()
                .workspace(foundWorkspace)
                .user(signedUser) // 인자로 받은 User 객체 사용
                .auth(Auth.MEMBER)
                .build());

        // 각 문서 조회
        // 각 문서 UserDocumentLastOpened로 변환
        List<UserDocumentLastOpened> userDocumentLastOpenedList = addDocumentsToUserLastOpened(foundWorkspace, signedUser);

        // 워크스페이스에 속해있는 모든 문서를 user_document_last_opened에 추가
        // last_opened는 null
        userDocumentLastOpenedList.forEach(userDocumentLastOpened -> {
            userDocumentLastOpenedRepository.saveAll(userDocumentLastOpenedList);
        });

        return WorkspaceConverter.toInvitationAcceptResult(true, true, foundWorkspace);
    }

    @Transactional
    @Override
    public void sendInviteEmail(Long userId, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request) {
        Long workspaceId = request.getWorkspaceId();
        List<String> emails = request.getEmails();

        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        userWorkspaceRepository.findByUserAndWorkspace(foundUser, foundWorkspace)
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        // 이메일마다 invitation 생성하여 저장, 초대 수락 토큰 생성, 초대 이메일 발송
        for(String email: emails) {
            String token = UUID.randomUUID().toString();

            WorkspaceInvitation workspaceInvitation = WorkspaceInvitation.builder()
                    .email(email)
                    .workspace(foundWorkspace)
                    .token(token)
                    .isAccepted(false)
                    .build();
            workspaceInvitationRepository.save(workspaceInvitation);

            String invitationLink = inviteBaseUrl + "?token=" + token;

            String subject = String.format("[%s] 에서 [%s] 님이 당신을 초대했어요!", foundWorkspace.getTitle(), foundUser.getName());
            String content = generateInvitationEmailContentHtml(email, foundUser.getName(), foundWorkspace.getTitle(), invitationLink);

            emailSender.send(email, subject, content);
        }

    }

    private List<UserDocumentLastOpened> addDocumentsToUserLastOpened(Workspace workspace, User user) {
        List<Meeting> meetingList = meetingRepository.findAllByWorkspaceId(workspace.getId());
        List<SnsEvent> snsEventList = snsEventRepository.findAllByWorkspaceId(workspace.getId());
        List<MoodTracker> moodTrackerList = moodTrackerRepository.findAllByWorkspaceId(workspace.getId());

        List<UserDocumentLastOpened> userDocumentLastOpenedList = new ArrayList<>();
        for(Meeting meeting : meetingList)
            userDocumentLastOpenedList.add(UserDocumentLastOpenedConverter.toUserDocumentLastOpened(meeting, user));
        for(SnsEvent snsEvent : snsEventList)
            userDocumentLastOpenedList.add(UserDocumentLastOpenedConverter.toUserDocumentLastOpened(snsEvent, user));
        for(MoodTracker moodTracker : moodTrackerList)
            userDocumentLastOpenedList.add(UserDocumentLastOpenedConverter.toUserDocumentLastOpened(moodTracker, user));

        userDocumentLastOpenedRepository.saveAll(userDocumentLastOpenedList);

        return userDocumentLastOpenedList;
    }

    private String generateInvitationEmailContentHtml(String invitedEmail, String inviterName, String workspaceName, String invitationLink) {
        return String.format(
                "<html>" +
                        "<head></head>" +
                        "<body>" +
                        "  <p>안녕하세요, %s님,</p>" +
                        "  <p>%s님께서 <b>%s</b> 워크스페이스에 당신을 초대했습니다.</p>" +
                        "  <p>아래 버튼을 클릭하여 워크스페이스에 합류해 주세요!</p>" +
                        "  <p style=\"margin-top: 20px;\">" + // 버튼 스타일
                        "    <a href=\"%s\" " +
                        "       style=\"display: inline-block; padding: 10px 20px; font-size: 16px; color: white; background-color: #007bff; text-decoration: none; border-radius: 5px;\">" +
                        "      초대 수락하기" +
                        "    </a>" +
                        "  </p>" +
                        "  <p style=\"margin-top: 30px;\">감사합니다.<br/><b>HaRu 팀 드림</b></p>" +
                        "</body>" +
                        "</html>",
                invitedEmail, inviterName, workspaceName, invitationLink
        );
    }

}
