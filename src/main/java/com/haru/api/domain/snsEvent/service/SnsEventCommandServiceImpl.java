package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.lastOpened.entity.UserDocumentId;
import com.haru.api.domain.lastOpened.entity.UserDocumentLastOpened;
import com.haru.api.domain.lastOpened.entity.enums.DocumentType;
import com.haru.api.domain.lastOpened.repository.UserDocumentLastOpenedRepository;
import com.haru.api.domain.snsEvent.converter.SnsEventConverter;
import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.snsEvent.entity.enums.Format;
import com.haru.api.domain.snsEvent.entity.enums.ListType;
import com.haru.api.domain.snsEvent.repository.ParticipantRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.snsEvent.repository.WinnerRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.code.status.ErrorStatus;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import com.haru.api.global.apiPayload.exception.handler.UserDocumentLastOpenedHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.infra.api.restTemplate.InstagramOauth2RestTemplate;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.s3.MarkdownFileUploader;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnsEventCommandServiceImpl implements SnsEventCommandService{

    private final SpringTemplateEngine templateEngine;
    @Value("${instagram.client.id}")
    private String instagramClientId;
    @Value("${instagram.client.secret}")
    private String instagramClientSecret;
    @Value("${instagram.redirect.uri}")
    private String instagramRedirectUri;
    private final SnsEventRepository snsEventRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final ParticipantRepository participantRepository;
    private final WinnerRepository winnerRepository;
    private final RestTemplate restTemplate;
    private final UserDocumentLastOpenedRepository userDocumentLastOpenedRepository;
    private final InstagramOauth2RestTemplate instagramOauth2RestTemplate;
    private final int WORD_TABLE_SIZE = 40; // 페이지당 총 아이디 수
    private final int PER_COL = WORD_TABLE_SIZE/ 2; // 한쪽 컬럼에 들어갈 개수
    private final AmazonS3Manager amazonS3Manager;
    private final MarkdownFileUploader markdownFileUploader;

    @Override
    @Transactional
    public SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(
            Long workspaceId,
            SnsEventRequestDTO.CreateSnsRequest request
    ) {
        // SNS 이벤트 생성 및 저장
        Long userId = SecurityUtil.getCurrentUserId();
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(WORKSPACE_NOT_FOUND));
        UserWorkspace foundUserWorkSapce = userWorkspaceRepository.findByUserAndWorkspace(foundUser, foundWorkspace)
                .orElseThrow(() -> new MemberHandler(NOT_BELONG_TO_WORKSPACE));
        SnsEvent createdSnsEvent = SnsEventConverter.toSnsEvent(request, foundUser);
        createdSnsEvent.setWorkspace(foundWorkspace);
        SnsEvent savedSnsEvent = snsEventRepository.save(createdSnsEvent);

        // mood tracker 생성 시 last opened에 추가
        // 마지막으로 연 시간은 null

        UserDocumentId documentId = new UserDocumentId(foundUser.getId(), savedSnsEvent.getId(), DocumentType.SNS_EVENT_ASSISTANT);

        userDocumentLastOpenedRepository.save(
                UserDocumentLastOpened.builder()
                        .id(documentId)
                        .user(foundUser)
                        .title(savedSnsEvent.getTitle())
                        .workspaceId(foundWorkspace.getId())
                        .lastOpened(null)
                        .build()
        );

        // Instagarm API 호출 후 참여라 리스트, 당첨자 리스트 생성 및 저장
        String accessToken = foundWorkspace.getInstagramAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            throw new SnsEventHandler(SNS_EVENT_NO_ACCESS_TOKEN);
        }
        SnsEventResponseDTO.InstagramMediaResponse instagramMediaResponse = fetchInstagramMedia(accessToken);
        String[] splitedSnsEventLink = request.getSnsEventLink().split("/");
        String requestShortCode = splitedSnsEventLink[splitedSnsEventLink.length - 1];
        System.out.println("Request ShortCode: " + requestShortCode);
        List<Participant> filteredCommentList = new ArrayList<>();
        Set<String> filteredCommentSet = new HashSet<>();
        List<Winner> winnerList = new ArrayList<>();
        for (SnsEventResponseDTO.Media media : instagramMediaResponse.getData()) {
            if (requestShortCode.equals(media.getShortcode())) {
                System.out.println("Instagram Media shortCode: " + media.getShortcode());
                System.out.println("Instagram Media id: " + media.getId());
                List<SnsEventResponseDTO.Comment> commentList = getComments(media.getId(), accessToken);
                for (SnsEventResponseDTO.Comment comment : commentList) {
                    boolean pass = true;
                    // 조건 1: 기간 필터
                    LocalDateTime commentTimeStamp = comment.getTimestamp().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
                    if (request.getSnsCondition().getIsPeriod()) {
                        if (comment.getTimestamp() == null || commentTimeStamp.isAfter(request.getSnsCondition().getPeriod())) {
                            pass = false;
                        }
                    }
                    // 조건 2: 키워드 필터
                    if (pass && request.getSnsCondition().getIsKeyword()) {
                        if (comment.getText() == null || !comment.getText().contains(request.getSnsCondition().getKeyword())) {
                            pass = false;
                        }
                    }
                    // 조건 3: 태그 개수 필터 (ex: @username 언급)
                    if (pass && request.getSnsCondition().getIsTaged()) {
                        int tagCount = countOccurrences(comment.getText(), "@");
                        if (tagCount < request.getSnsCondition().getTageCount()) {
                            pass = false;
                        }
                    }
                    if (pass) {
                        filteredCommentSet.add(comment.getFrom().getUsername()); // 중복 제거를 위해 Set 사용
                    }
                }
                break;
            }
            // 마지막까지 돌았는데 shortcode파싱해둔것과 일치하는게 없다면 error처리해야됨.
            throw new SnsEventHandler(SNS_EVENT_LINK_NOT_FOUND);
        }
        // 참여자 저장
        for (String nickname : filteredCommentSet) {
            Participant participant = SnsEventConverter.toParticipant(nickname);
            participant.setSnsEvent(createdSnsEvent);
            filteredCommentList.add(participant);
        }
        participantRepository.saveAll(filteredCommentList);
        // 당첨자 선정 후 저장
        for (String nickname : pickWinners(filteredCommentSet, request.getSnsCondition().getWinnerCount())) {
            Winner winner = SnsEventConverter.toWinner(nickname);
            winner.setSnsEvent(createdSnsEvent);
            winnerList.add(winner);
        }
        winnerRepository.saveAll(winnerList);

        // PDF, DOCX파일 바이트 배열로 생성 및 썸네일 생성 & 업로드 / DB에 keyName저장
        createAndUploadListFileAndThumbnail(
                request,
                savedSnsEvent
        );

        return SnsEventResponseDTO.CreateSnsEventResponse.builder()
                .snsEventId(createdSnsEvent.getId())
                .build();
    }

    private String createListHtml(
            SnsEvent snsEvent,
            ListType listType
    ) {
        if (listType == ListType.PARTICIPANT) {
            List<Participant> participantList = participantRepository.findAllBySnsEvent(snsEvent);
            List<Participant> leftList = new ArrayList<>();
            List<Participant> rightList = new ArrayList<>();
            int total = participantList.size();
            int mid = (total + 1) / 2;
            leftList = participantList.subList(0, mid);
            rightList = participantList.subList(mid, total);
            // Thymeleaf context에 데이터 세팅
            Context context = new Context();
            context.setVariable("leftList", leftList);
            context.setVariable("rightList", rightList);
            // 템플릿 렌더링 → HTML 문자열 생성
            return templateEngine.process("sns-event-list-pdf-template", context);
        } else if (listType == ListType.WINNER) {
            List<Winner> winnerList = winnerRepository.findAllBySnsEvent(snsEvent);
            List<Winner> leftList = new ArrayList<>();
            List<Winner> rightList = new ArrayList<>();
            int total = winnerList.size();
            int mid = (total + 1) / 2;
            leftList = winnerList.subList(0, mid);
            rightList = winnerList.subList(mid, total);
            // Thymeleaf context에 데이터 세팅
            Context context = new Context();
            context.setVariable("leftList", leftList);
            context.setVariable("rightList", rightList);
            // 템플릿 렌더링 → HTML 문자열 생성
            return templateEngine.process("sns-event-list-pdf-template", context);
        } else {
            throw new SnsEventHandler(SNS_EVENT_WRONG_FORMAT);
        }

    }

    private void createAndUploadListFileAndThumbnail(
            SnsEventRequestDTO.CreateSnsRequest request,
            SnsEvent snsEvent
    ){
        String listHtmlParticipant = createListHtml(snsEvent, ListType.PARTICIPANT);
        String listHtmlWinner = createListHtml(snsEvent, ListType.WINNER);
        byte[] pdfBytesParticipant;
        byte[] pdfBytesWinner;
        byte[] docxBytesParticipant;
        byte[] docxBytesWinner;
        try {
            // 폰트 경로
            URL resource = getClass().getClassLoader().getResource("templates/NotoSansKR-Regular.ttf");
            File reg = new File(resource.toURI()); // catch에서 Exception 따로 처리해주기
            listHtmlParticipant = injectPageMarginStyle(listHtmlParticipant);
            listHtmlWinner = injectPageMarginStyle(listHtmlWinner);
            byte[] shiftedPdfBytesParticipant = convertHtmlToPdf(listHtmlParticipant, reg);
            byte[] shiftedPdfBytesWinner = convertHtmlToPdf(listHtmlWinner, reg);
            pdfBytesParticipant =  addPdfTitle(shiftedPdfBytesParticipant, request.getTitle(), reg.getAbsolutePath());
            pdfBytesWinner =  addPdfTitle(shiftedPdfBytesWinner, request.getTitle(), reg.getAbsolutePath());
            docxBytesParticipant =  createWord(ListType.PARTICIPANT, request.getTitle(), snsEvent);
            docxBytesWinner =  createWord(ListType.WINNER, request.getTitle(), snsEvent );
        } catch (Exception e) {
            log.error("Error creating document: {}", e.getMessage());
            throw new SnsEventHandler(SNS_EVENT_DOWNLOAD_LIST_ERROR);
        }
        // PDF, DOCS파일, 썸네일 S3에 업로드 및 DB에 keyName저장
        String fullPath = "sns-event/" + snsEvent.getId();
        String keyNameParicipantPdf = amazonS3Manager.generateKeyName(fullPath) + "." + "pdf";
        String keyNameParicipantWord = amazonS3Manager.generateKeyName(fullPath) + "." + "docx";
        String keyNameWinnerPdf = amazonS3Manager.generateKeyName(fullPath) + "." + "pdf";
        String keyNameWinnerWord = amazonS3Manager.generateKeyName(fullPath) + "." + "docx";
        amazonS3Manager.uploadFile(keyNameParicipantPdf, pdfBytesParticipant, "application/pdf");
        amazonS3Manager.uploadFile(keyNameWinnerPdf, pdfBytesWinner, "application/pdf");
        amazonS3Manager.uploadFile(keyNameParicipantWord, docxBytesParticipant, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        amazonS3Manager.uploadFile(keyNameWinnerWord, docxBytesWinner, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        // SNS 이벤트에 keyName 저장
        snsEvent.updateKeyNameParticipantPdf(
                keyNameParicipantPdf,
                keyNameParicipantWord,
                keyNameWinnerPdf,
                keyNameWinnerWord
        );
        // SNS 이벤트 당첨자 PDF의 첫페이지 썸네일로 저장
        String thumbnailKey = markdownFileUploader.createOrUpdateThumbnailWithPdfBytes(
                pdfBytesWinner,
                "sns-event",
                null
        );
        snsEvent.updateThumbnailKey(thumbnailKey);
    }

    private SnsEventResponseDTO.InstagramMediaResponse fetchInstagramMedia(
            String accessToken
    ) {
        String baseUrl = "https://graph.instagram.com/me/media";
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("fields", "shortcode")
                .queryParam("access_token", accessToken)
                .toUriString();

        // 가져오는 값 없거나 error뜨면 error처리해야됨.
        try {
            SnsEventResponseDTO.InstagramMediaResponse response = restTemplate.getForObject(url, SnsEventResponseDTO.InstagramMediaResponse.class);
            // 응답이 null이거나 게시물이 아예 없으면 예외 발생
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_NO_MEDIA);
            }
            return response;
        } catch (HttpClientErrorException e) {
            // 4xx 에러 코드 처리 (예: 인증 실패, 권한 부족)
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        } catch (RestClientException e) {
            // 네트워크 오류, 5xx 서버 에러 등 기타 예외 처리
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        } catch (Exception e) {
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        }
    }


    public List<SnsEventResponseDTO.Comment> getComments(
            String mediaId,
            String accessToken
    ) {
        String baseUrl = "https://graph.instagram.com/" + mediaId + "/comments";
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("fields", "from,text,timestamp")
                .queryParam("access_token", accessToken)
                .toUriString();

        // 가져오는 값 없거나 error뜨면 error처리해야됨.
        try {
            SnsEventResponseDTO.InstagramCommentResponse response = restTemplate.getForObject(url, SnsEventResponseDTO.InstagramCommentResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_NO_COMMENT);
            }
            return response.getData();
        } catch (HttpClientErrorException e) {
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        } catch (RestClientException e) {
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        } catch (Exception e) {
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        }
    }

    private int countOccurrences(
            String text,
            String keyword
    ) {
        if (text == null || keyword == null || keyword.isEmpty()) return 0;

        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    public List<String> pickWinners(Set<String> participants, int n) {
        List<String> list = new ArrayList<>(participants); // Set → List로 변환
        Collections.shuffle(list); // 무작위 섞기

        if (n >= list.size()) {
            return list; // 참가자가 n보다 적으면 전원 반환
        }

        return list.subList(0, n); // 앞에서 n개만 추출
    }

    @Override
    @Transactional
    public SnsEventResponseDTO.LinkInstagramAccountResponse getInstagramAccessTokenAndAccount(
            String code,
            Long workspaceId
    ) {
        String shortLivedAccessToken;
        String longLivedAccessToken;
        Map<String, Object> userInfo;
        try {
            // 1. Access Token 요청
            shortLivedAccessToken = instagramOauth2RestTemplate.getShortLivedAccessTokenUrl(code);
            // 2. 단기 토큰을 장기(Long-Lived) 토큰으로 교환
            longLivedAccessToken = instagramOauth2RestTemplate.getLongLivedAccessToken(shortLivedAccessToken);
            // 3. 장기 토큰으로 사용자 계정 정보 요청
            userInfo = instagramOauth2RestTemplate.getInstagramAccountInfo(longLivedAccessToken);
        } catch (Exception e) {
            log.error("Instagram OAuth2 처리 중 오류 발생: {}", e.getMessage());
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_API_ERROR);
        }
        // 4. 워크스페이스에 인스타그램 계정 정보 저장
        String instagramId = (String) userInfo.get("user_id");
        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(WORKSPACE_NOT_FOUND));
        if (foundWorkspace.getInstagramId() != null && foundWorkspace.getInstagramId().equals(instagramId)) {
            throw new SnsEventHandler(SNS_EVENT_INSTAGRAM_ALREADY_LINKED);
        }
        foundWorkspace.saveInstagramId(instagramId);
        foundWorkspace.saveInstagramAccessToken(longLivedAccessToken);
        foundWorkspace.saveInstagramAccountName((String) userInfo.get("username"));
        return SnsEventConverter.toLinkInstagramAccountResponse((String) userInfo.get("username"));
    }
  
    @Override
    @Transactional
    public void updateSnsEventTitle(
            Long userId,
            Long snsEventId,
            SnsEventRequestDTO.UpdateSnsEventRequest request
    ) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        SnsEvent foundSnsEvent = snsEventRepository.findById(snsEventId)
                .orElseThrow(() -> new SnsEventHandler(SNS_EVENT_NOT_FOUND));
        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceAndAuth(foundSnsEvent.getWorkspace(), Auth.ADMIN)
                .orElseThrow(() -> new MemberHandler(WORKSPACE_CREATOR_NOT_FOUND));
        // 수정 권한 확인 (워크스페이스 생성자 혹은 SNS 이벤트의 생성자만 수정 가능)
        if (!foundUserWorkspace.getUser().getId().equals(foundUser.getId()) || !foundSnsEvent.getCreator().getId().equals(foundUser.getId())) {
            throw new SnsEventHandler(SNS_EVENT_NO_AUTHORITY);
        }
        foundSnsEvent.updateTitle(request.getTitle());
        snsEventRepository.save(foundSnsEvent);

        // last opened title 수정
        UserDocumentId userDocumentId = new UserDocumentId(userId, snsEventId, DocumentType.SNS_EVENT_ASSISTANT);

        UserDocumentLastOpened foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId)
                .orElseThrow(() -> new UserDocumentLastOpenedHandler(ErrorStatus.USER_DOCUMENT_LAST_OPENED_NOT_FOUND));

        foundUserDocumentLastOpened.updateTitle(request.getTitle());
    }

    @Override
    @Transactional
    public void deleteSnsEvent(
            Long userId,
            Long snsEventId
    ) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        SnsEvent foundSnsEvent = snsEventRepository.findById(snsEventId)
                .orElseThrow(() -> new SnsEventHandler(SNS_EVENT_NOT_FOUND));
        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceAndAuth(foundSnsEvent.getWorkspace(), Auth.ADMIN)
                .orElseThrow(() -> new MemberHandler(WORKSPACE_CREATOR_NOT_FOUND));
        // 수정 권한 확인 (워크스페이스 생성자 혹은 SNS 이벤트의 생성자만 삭제 가능)
        if (!foundUserWorkspace.getUser().getId().equals(foundUser.getId()) || !foundSnsEvent.getCreator().getId().equals(foundUser.getId())) {
            throw new SnsEventHandler(SNS_EVENT_NO_AUTHORITY);
        }
        snsEventRepository.delete(foundSnsEvent);

        // last opened 테이블 튜플 삭제
        // last opened가 없어도 오류 X
        UserDocumentId userDocumentId = new UserDocumentId(userId, snsEventId, DocumentType.SNS_EVENT_ASSISTANT);

        Optional<UserDocumentLastOpened> foundUserDocumentLastOpened = userDocumentLastOpenedRepository.findById(userDocumentId);

        foundUserDocumentLastOpened.ifPresent(userDocumentLastOpenedRepository::delete);
    }

    @Override
    public SnsEventResponseDTO.ListDownLoadLinkResponse downloadList(
            Long userId,
            Long snsEventId,
            ListType listType,
            Format format
    ) {
        String downloadLink = "";
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        SnsEvent foundSnsEvent = snsEventRepository.findById(snsEventId)
                .orElseThrow(() -> new SnsEventHandler(SNS_EVENT_NOT_FOUND));
        String snsEventTitle = foundSnsEvent.getTitle();
        if (listType == ListType.PARTICIPANT) {
            if (format == Format.PDF) {
                String keyName = foundSnsEvent.getKeyNameParticipantPdf();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_participnat_list.pdf");
            } else if (format == Format.DOCX) {
                String keyName = foundSnsEvent.getKeyNameParticipantWord();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_participnat_list.docx");
            } else {
                throw new SnsEventHandler(SNS_EVENT_WRONG_FORMAT);
            }
        } else if (listType == ListType.WINNER) {
            if (format == Format.PDF) {
                String keyName = foundSnsEvent.getKeyNameWinnerPdf();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_winner_list.pdf");
            } else if (format == Format.DOCX) {
                String keyName = foundSnsEvent.getKeyNameWinnerWord();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_winner_list.docx");
            } else {
                throw new SnsEventHandler(SNS_EVENT_WRONG_FORMAT);
            }
        } else {
            throw new SnsEventHandler(SNS_EVENT_WRONG_LIST_TYPE);
        }
        return SnsEventResponseDTO.ListDownLoadLinkResponse.builder()
                .downloadLink(downloadLink)
                .build();
    }

    private String injectHead(String html) {
        String fontCss = """
        <style>
            body {
                font-family: 'NotoSansKR', sans-serif;
            }
        </style>
        """;
        if (html.toLowerCase().contains("<head>")) {
            // <head> 바로 뒤에 스타일 삽입
            return html.replaceFirst("(?i)<head>", "<head>" + fontCss);
        } else {
            // head가 없으면 생성
            return html.replaceFirst("(?i)<html>", "<html><head>" + fontCss + "</head>");
        }
    }

    private String injectPageMarginStyle(String html) {
        String styleBlock = """
        <style>
            @page {
                size: A4;
                margin-top: 80pt;
                margin-bottom: 80pt;
            }
            @page :first {
                margin-top: 90pt; /* 첫 페이지만 위 여백 크게 */
            }
        </style>
        """;
        String lowerHtml = html.toLowerCase();
        if (lowerHtml.contains("<head>")) {
            // <head> 태그가 있는 경우 → 바로 뒤에 스타일 삽입
            return html.replaceFirst("(?i)<head>", "<head>" + styleBlock);
        } else {
            // <head> 태그가 없는 경우 → <html> 다음에 <head> 생성 후 스타일 삽입
            return html.replaceFirst("(?i)<html>", "<html><head>" + styleBlock + "</head>");
        }
    }

    private byte[] convertHtmlToPdf(String listHtml, File reg) throws Exception {
        // Openhtmltopdf/Flying Saucer를 사용하여 PDF 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(listHtml, null); // ex) "file:/opt/app/static/" or "https://your.cdn/", // base url 설정, 직접css파일 가져오거나 프론트엔드 배포 후 적용
        builder.toStream(baos);
        // 한글 폰트 임베딩
        if (reg.exists() && reg.canRead()) {
            builder.useFont(
                    reg,
                    "NotoSansKR"
            );
        }
        builder.run();
        return baos.toByteArray();
    }

    private byte[] addPdfTitle(byte[] pdfBytes, String text, String fontPath) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        PdfStamper stamper = new PdfStamper(reader, out);
        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        int totalPages = reader.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte over = stamper.getOverContent(i);
            over.beginText();
            over.setFontAndSize(bf, 28f); // 글씨 크게 (28pt)
            // 페이지 폭 중앙 계산
            float x = reader.getPageSize(i).getWidth() / 2;
            // 페이지 상단에서 약간 내려오게 (70pt 여백)
            float y = reader.getPageSize(i).getTop() - 70f;
            over.showTextAligned(Element.ALIGN_CENTER, text, x, y, 0);
            over.endText();
        }
        stamper.close();
        reader.close();
        return out.toByteArray();
    }

    private byte[] createWord(ListType listType, String listTitle, SnsEvent snsEvent) throws Exception { // 참여자 또는 당첨자 리스트 DB에서 가져와 표로 만들어 word로 변환해서 응답주기
        List<String> list = new ArrayList<>();
        if (listType == ListType.PARTICIPANT) {
            List<Participant> participantList = participantRepository.findAllBySnsEvent(snsEvent);
            for (Participant participant : participantList) {
                list.add(participant.getNickname());
            }
            return createTable(list, listTitle);
        } else {
            List<Winner> winnerList = winnerRepository.findAllBySnsEvent(snsEvent);
            for (Winner winner : winnerList) {
                list.add(winner.getNickname());
            }
            return createTable(list, listTitle);
        }
    }

    private byte[] createTable(List<String> list, String listTitle) throws Exception {
        int page = list.size() / WORD_TABLE_SIZE + (list.size() % WORD_TABLE_SIZE == 0 ? 0 : 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XWPFDocument doc = new XWPFDocument();
        for (int p = 0; p < page; p++) {
            int pageStart = p * WORD_TABLE_SIZE;
            int pageEnd = Math.min(pageStart + WORD_TABLE_SIZE, list.size());
            // 첫 페이지에만 제목 추가
            if (p == 0) {
                addTitle(doc, listTitle, 22);
            }
            // ── 현재 페이지 테이블: (헤더 1행 + 데이터 18행) × 4열 [번호, ID, 번호, ID]
            // 열 너비를 twip 단위로 설정 (1cm ≈ 567 twip)
            // [번호, ID, 번호, ID] 순서
            int[] colWidths = {1000, 3000, 1000, 3000};
            XWPFTable table = doc.createTable(PER_COL + 1, 4); // +1은 헤더
            setColumnWidths(table, colWidths);
            // 스타일(테두리/정렬)
            table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
            table.setTableAlignment(TableRowAlign.CENTER);
            // 헤더
            setCellTextCentered(table.getRow(0).getCell(0), "번호", true);
            setCellTextCentered(table.getRow(0).getCell(1), "ID",   true);
            setCellTextCentered(table.getRow(0).getCell(2), "번호", true);
            setCellTextCentered(table.getRow(0).getCell(3), "ID",   true);
            // 데이터 채우기
            for (int i = 0; i < PER_COL; i++) {
                int rowIdx = i + 1; // 헤더 다음 줄부터
                int leftIdx  = pageStart + i;               // 왼쪽 컬럼 번호 시작
                int rightIdx = pageStart + PER_COL + i;     // 오른쪽 컬럼 번호 시작
                // 왼쪽
                if (leftIdx < pageEnd) {
                    setCellTextCentered(table.getRow(rowIdx).getCell(0), String.valueOf(leftIdx + 1), false);
                    setCellTextLeft    (table.getRow(rowIdx).getCell(1), list.get(leftIdx), false);
                } else {
                    clearCell(table.getRow(rowIdx).getCell(0));
                    clearCell(table.getRow(rowIdx).getCell(1));
                }
                // 오른쪽
                if (rightIdx < pageEnd) {
                    setCellTextCentered(table.getRow(rowIdx).getCell(2), String.valueOf(rightIdx + 1), false);
                    setCellTextLeft    (table.getRow(rowIdx).getCell(3), list.get(rightIdx), false);
                } else {
                    clearCell(table.getRow(rowIdx).getCell(2));
                    clearCell(table.getRow(rowIdx).getCell(3));
                }
                // 행 분할 금지(페이지 넘어가며 쪼개지지 않도록)
                try { table.getRow(rowIdx).setCantSplitRow(true); } catch (Throwable ignored) {}
            }
            // 마지막 페이지가 아니면 페이지 브레이크
            if (p < page - 1) {
                XWPFParagraph br = doc.createParagraph();
                XWPFRun r = br.createRun();
                r.addBreak(BreakType.PAGE);
            }
        }
        doc.write(baos);
        doc.close();
        return baos.toByteArray();
    }

    private void clearCell(XWPFTableCell cell) {
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) cell.removeParagraph(i);
        cell.addParagraph(); // 빈 문단 하나 유지
    }

    private void setCellTextCentered(XWPFTableCell cell, String text, boolean bold) {
        setCellText(cell, text, ParagraphAlignment.CENTER, bold);
    }

    private void setCellTextLeft(XWPFTableCell cell, String text, boolean bold) {
        setCellText(cell, text, ParagraphAlignment.LEFT, bold);
    }

    private void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align, boolean bold) {
        if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(align);
        XWPFRun r = p.createRun();
        r.setFontSize(11);
        r.setBold(bold);
        r.setText(text);
    }

    private void setColumnWidths(XWPFTable table, int[] colWidths) {
        // 표 전체 너비 고정
        table.setWidthType(TableWidthType.DXA);
        int totalWidth = 0;
        for (int w : colWidths) totalWidth += w;
        table.setWidth(String.valueOf(totalWidth));
        for (int col = 0; col < colWidths.length; col++) {
            for (XWPFTableRow row : table.getRows()) {
                XWPFTableCell cell = row.getCell(col);
                cell.setWidthType(TableWidthType.DXA);
                cell.setWidth(String.valueOf(colWidths[col]));
            }
        }
    }

    // 제목 추가 메소드
    private void addTitle(XWPFDocument doc, String titleText, int fontSize) {
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER); // 가운데 정렬
        XWPFRun run = title.createRun();
        run.setText(titleText);
        run.setFontSize(fontSize);  // 전달받은 크기로 설정
        run.setBold(true);             // 굵게
        run.addBreak();                // 제목과 표 사이 한 줄 띄움
    }
}
