package com.haru.api.domain.workspace.service;

import com.haru.api.domain.lastOpened.converter.UserDocumentLastOpenedConverter;
import com.haru.api.domain.lastOpened.entity.Documentable;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.meeting.repository.MeetingRepository;
import com.haru.api.domain.moodTracker.repository.MoodTrackerRepository;
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
import com.haru.api.global.apiPayload.exception.handler.UserWorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceInvitationHandler;
import com.haru.api.infra.mail.EmailSender;
import com.haru.api.infra.s3.AmazonS3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    private final AmazonS3Manager amazonS3Manager;

    private final EmailSender emailSender;

    @Value("${invite-url}")
    private String inviteBaseUrl;

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace createWorkspace(User user, WorkspaceRequestDTO.WorkspaceCreateRequest request, MultipartFile image) {

        String keyName = null;

        if (image != null) {
            // s3에 사진 추가하는 메서드
            String path = amazonS3Manager.generateKeyName("workspace/image");
            keyName = amazonS3Manager.uploadFile(path, image);
        }

        // workspace entity 생성
        Workspace workspace = workspaceRepository.save(Workspace.builder()
                .title(request.getTitle())
                .keyName(keyName)
                .build());

        // users_workspaces 테이블에 생성자 정보 저장
        userWorkspaceRepository.save(UserWorkspace.builder()
                .user(user)
                .workspace(workspace)
                .auth(Auth.ADMIN)
                .build());

        return WorkspaceConverter.toWorkspaceDTO(workspace);
    }

    @Transactional
    @Override
    public WorkspaceResponseDTO.Workspace updateWorkspace(User user, Workspace workspace, WorkspaceRequestDTO.WorkspaceUpdateRequest request, MultipartFile image) {

        UserWorkspace userWorkspace = userWorkspaceRepository.findByWorkspaceIdAndUserId(workspace.getId(), user.getId())
                .orElseThrow(() -> new UserWorkspaceHandler(ErrorStatus.USER_WORKSPACE_NOT_FOUND));

        if(userWorkspace.getAuth() != Auth.ADMIN)
            throw new WorkspaceHandler(ErrorStatus.WORKSPACE_MODIFY_NOT_ALLOWED);

        // 제목 수정
        workspace.updateTitle(request.getTitle());

        // 이미지 수정
        if (image != null) {
            amazonS3Manager.uploadFile(workspace.getKeyName(), image);
        }

        return WorkspaceConverter.toWorkspaceDTO(workspace);
    }

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

        // 각 문서 UserDocumentLastOpened로 변환
        List<UserDocumentLastOpened> userDocumentLastOpenedList = addDocumentsToUserLastOpened(foundWorkspace, foundUser.get());

        // 워크스페이스에 속해있는 모든 문서를 user_document_last_opened에 추가
        // last_opened는 null
        if (!userDocumentLastOpenedList.isEmpty()) {
            userDocumentLastOpenedRepository.saveAll(userDocumentLastOpenedList);
        }

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

        // 각 문서 조회 후, UserDocumentLastOpened로 변환
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
    public void sendInviteEmail(User user, WorkspaceRequestDTO.WorkspaceInviteEmailRequest request) {
        Long workspaceId = request.getWorkspaceId();
        List<String> emails = request.getEmails();

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(ErrorStatus.WORKSPACE_NOT_FOUND));

        userWorkspaceRepository.findByUserAndWorkspace(user, foundWorkspace)
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

            String subject = String.format("[%s] 에서 [%s] 님이 당신을 초대했어요!", foundWorkspace.getTitle(), user.getName());
            String content = generateInvitationEmailContentHtml(email, user.getName(), foundWorkspace.getTitle(), invitationLink);

            emailSender.send(email, subject, content);
        }

    }

    private List<UserDocumentLastOpened> addDocumentsToUserLastOpened(Workspace workspace, User user) {

        List<Documentable> documentList = new ArrayList<>();

        documentList.addAll(meetingRepository.findAllByWorkspaceId(workspace.getId()));
        documentList.addAll(snsEventRepository.findAllByWorkspaceId(workspace.getId()));
        documentList.addAll(moodTrackerRepository.findAllByWorkspaceId(workspace.getId()));

        List<UserDocumentLastOpened> userDocumentLastOpenedList = new ArrayList<>();
        for(Documentable documentable : documentList)
            userDocumentLastOpenedList.add(UserDocumentLastOpenedConverter.toUserDocumentLastOpened(documentable, user));

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
                        "       style=\"display: inline-block; padding: 10px 20px; font-size: 16px; color: white; background-color: #E65787; text-decoration: none; border-radius: 5px;\">" +
                        "      초대 수락하기" +
                        "    </a>" +
                        "  </p>" +
                        "  <p style=\"margin-top: 30px;\">감사합니다.<br/><b>Team HaRu 드림</b></p>" +
                        "</body>" +
                        "</html>",
                invitedEmail, inviterName, workspaceName, invitationLink
        );
    }

}
