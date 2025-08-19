package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.lastOpened.service.UserDocumentLastOpenedService;
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
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.entity.enums.Auth;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.annotation.DeleteDocument;
import com.haru.api.global.annotation.UpdateDocumentTitle;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import com.haru.api.global.util.file.FileConvertHelper;
import com.haru.api.infra.api.restTemplate.InstagramOauth2RestTemplate;
import com.haru.api.infra.s3.AmazonS3Manager;
import com.haru.api.infra.s3.MarkdownFileUploader;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnsEventCommandServiceImpl implements SnsEventCommandService{

    private final SpringTemplateEngine templateEngine;

    private final SnsEventRepository snsEventRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final ParticipantRepository participantRepository;
    private final WinnerRepository winnerRepository;
    private final RestTemplate restTemplate;
    private final UserDocumentLastOpenedService userDocumentLastOpenedService;
    private final InstagramOauth2RestTemplate instagramOauth2RestTemplate;
    private final int WORD_TABLE_SIZE = 40; // 페이지당 총 아이디 수
    private final int PER_COL = WORD_TABLE_SIZE/ 2; // 한쪽 컬럼에 들어갈 개수
    private final AmazonS3Manager amazonS3Manager;
    private final MarkdownFileUploader markdownFileUploader;
    private final FileConvertHelper fileConvertHelper;

    @Override
    @Transactional
    public SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(
            User user,
            Workspace workspace,
            SnsEventRequestDTO.CreateSnsRequest request
    ) {

        Workspace foundWorkspace = workspaceRepository.findById(workspace.getId())
                .orElseThrow(() -> new WorkspaceHandler(WORKSPACE_NOT_FOUND));

        // SNS 이벤트 생성 및 저장
        SnsEvent createdSnsEvent = SnsEventConverter.toSnsEvent(request, user);
        createdSnsEvent.setWorkspace(foundWorkspace);

        // Instagram API 호출 후 참여자 리스트, 당첨자 리스트 생성 및 저장
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

        SnsEvent savedSnsEvent = snsEventRepository.save(createdSnsEvent);

        // PDF, DOCX파일 바이트 배열로 생성 및 썸네일 생성 & 업로드 / DB에 keyName저장
        String thumbnailKeyName = createAndUploadListFileAndThumbnail(savedSnsEvent);

        // sns event 썸네일 key name 초기화
        savedSnsEvent.initThumbnailKeyName(thumbnailKeyName);

        // sns event 생성 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에 마지막으로 연 시간은 null로하여 추가
        List<User> usersInWorkspace = userWorkspaceRepository.findUsersByWorkspaceId(foundWorkspace.getId());
        userDocumentLastOpenedService.createInitialRecordsForWorkspaceUsers(usersInWorkspace, savedSnsEvent);

        return SnsEventResponseDTO.CreateSnsEventResponse.builder()
                .snsEventId(createdSnsEvent.getId())
                .build();
    }


    @Override
    @Transactional
    public SnsEventResponseDTO.LinkInstagramAccountResponse getInstagramAccessTokenAndAccount(
            String code,
            Workspace workspace
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
        Workspace foundWorkspace = workspaceRepository.findById(workspace.getId())
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
    @UpdateDocumentTitle
    public void updateSnsEventTitle(
            User user,
            SnsEvent snsEvent,
            SnsEventRequestDTO.UpdateSnsEventRequest request
    ) {

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceAndAuth(snsEvent.getWorkspace(), Auth.ADMIN)
                .orElseThrow(() -> new MemberHandler(WORKSPACE_CREATOR_NOT_FOUND));

        // 수정 권한 확인 (워크스페이스 생성자 혹은 SNS 이벤트의 생성자만 수정 가능)
        if (!foundUserWorkspace.getUser().getId().equals(user.getId()) || !snsEvent.getCreator().getId().equals(user.getId())) {
            throw new SnsEventHandler(SNS_EVENT_NO_AUTHORITY);
        }

        snsEvent.updateTitle(request.getTitle());
        SnsEvent savedSnsEvent = snsEventRepository.save(snsEvent);

        // S3문서 제목, S3 문서내 제목, 썸네일 이미지의 제목 변경
        deleteS3FileAndThumnailImage(savedSnsEvent);

        String thumbnailKeyName = createAndUploadListFileAndThumbnail(savedSnsEvent);
        // sns event 썸네일 key name 초기화
        savedSnsEvent.initThumbnailKeyName(thumbnailKeyName);

        // sns event 생성 시 워크스페이스에 속해있는 모든 유저에 대해
        // last opened 테이블에 마지막으로 연 시간은 null로하여 추가
        List<User> usersInWorkspace = userWorkspaceRepository.findUsersByWorkspaceId(savedSnsEvent.getWorkspace().getId());
        userDocumentLastOpenedService.updateRecordsTitleAndThumbnailForWorkspaceUsers(usersInWorkspace, savedSnsEvent, request);
    }

    @Override
    @Transactional
    @DeleteDocument
    public void deleteSnsEvent(
            User user,
            SnsEvent snsEvent
    ) {

        UserWorkspace foundUserWorkspace = userWorkspaceRepository.findByWorkspaceAndAuth(snsEvent.getWorkspace(), Auth.ADMIN)
                .orElseThrow(() -> new MemberHandler(WORKSPACE_CREATOR_NOT_FOUND));

        // 수정 권한 확인 (워크스페이스 생성자 혹은 SNS 이벤트의 생성자만 삭제 가능)
        if (!foundUserWorkspace.getUser().getId().equals(user.getId()) || !snsEvent.getCreator().getId().equals(user.getId())) {
            throw new SnsEventHandler(SNS_EVENT_NO_AUTHORITY);
        }

        // S3의 문서 및 썸네일 이미지 삭제
        deleteS3FileAndThumnailImage(snsEvent);

        snsEventRepository.delete(snsEvent);

    }

    @Override
    public SnsEventResponseDTO.ListDownLoadLinkResponse downloadList(
            User user,
            SnsEvent snsEvent,
            ListType listType,
            Format format
    ) {
        String downloadLink = "";

        String snsEventTitle = snsEvent.getTitle();
        if (listType == ListType.PARTICIPANT) {
            if (format == Format.PDF) {
                String keyName = snsEvent.getKeyNameParticipantPdf();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_참여자_리스트.pdf");
            } else if (format == Format.DOCX) {
                String keyName = snsEvent.getKeyNameParticipantWord();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_참여자_리스트.docx");
            } else {
                throw new SnsEventHandler(SNS_EVENT_WRONG_FORMAT);
            }
        } else if (listType == ListType.WINNER) {
            if (format == Format.PDF) {
                String keyName = snsEvent.getKeyNameWinnerPdf();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_당첨자_리스트.pdf");
            } else if (format == Format.DOCX) {
                String keyName = snsEvent.getKeyNameWinnerWord();
                if (keyName == null || keyName.isEmpty()) {
                    throw new SnsEventHandler(SNS_EVENT_LIST_KEYNAME_NOT_FOUND);
                }
                downloadLink = amazonS3Manager.generatePresignedUrlForDownloadPdfAndWord(keyName, snsEventTitle + "_당첨자_리스트.docx");
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

    private String createAndUploadListFileAndThumbnail(SnsEvent snsEvent){
        String listHtmlParticipant = createListHtml(snsEvent, ListType.PARTICIPANT);
        String listHtmlWinner = createListHtml(snsEvent, ListType.WINNER);
        byte[] pdfBytesParticipant;
        byte[] pdfBytesWinner;
        byte[] docxBytesParticipant;
        byte[] docxBytesWinner;
        try {
            // 1) 폰트를 스트림/바이트로 읽기
            byte[] fontBytes;
            try (InputStream in = getClass().getClassLoader()
                    .getResourceAsStream("templates/NotoSansKR-Regular.ttf")) {
                if (in == null) throw new IllegalStateException("Font not found on classpath");
                fontBytes = in.readAllBytes();
            }
//            // 폰트 경로
//            URL resource = getClass().getClassLoader().getResource("templates/NotoSansKR-Regular.ttf");
//            File reg = new File(resource.toURI()); // catch에서 Exception 따로 처리해주기
            listHtmlParticipant = fileConvertHelper.injectPageMarginStyle(listHtmlParticipant);
            listHtmlWinner = fileConvertHelper.injectPageMarginStyle(listHtmlWinner);
            byte[] shiftedPdfBytesParticipant = fileConvertHelper.convertHtmlToPdf(listHtmlParticipant, fontBytes);
            byte[] shiftedPdfBytesWinner = fileConvertHelper.convertHtmlToPdf(listHtmlWinner, fontBytes);
            pdfBytesParticipant =  addPdfTitle(shiftedPdfBytesParticipant, snsEvent.getTitle() + " 참여자 리스트", fontBytes);
            pdfBytesWinner =  addPdfTitle(shiftedPdfBytesWinner, snsEvent.getTitle() + " 당첨자 리스트", fontBytes);
            docxBytesParticipant =  createWord(ListType.PARTICIPANT, snsEvent.getTitle() + " 참여자 리스트", snsEvent);
            docxBytesWinner =  createWord(ListType.WINNER, snsEvent.getTitle() + " 당첨자 리스트", snsEvent );
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

        // SNS 이벤트 당첨자 PDF의 첫페이지 썸네일로 S3에 업로드
        return markdownFileUploader.createOrUpdateThumbnailWithPdfBytes(
                pdfBytesWinner,
                "sns-event",
                null
        );
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


    private List<SnsEventResponseDTO.Comment> getComments(
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

    private List<String> pickWinners(Set<String> participants, int n) {
        List<String> list = new ArrayList<>(participants); // Set → List로 변환
        Collections.shuffle(list); // 무작위 섞기

        if (n >= list.size()) {
            return list; // 참가자가 n보다 적으면 전원 반환
        }

        return list.subList(0, n); // 앞에서 n개만 추출
    }

    private byte[] addPdfTitle(byte[] pdfBytes, String text, byte[] fontBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        PdfStamper stamper = new PdfStamper(reader, out);
        // byte[]로 폰트 임베딩 (경로 X)
        // 첫 번째 인자 name은 식별용 문자열이라 임의명 가능, 실제 폰트는 byte[]에서 읽힙니다.
        BaseFont bf = BaseFont.createFont(
                "NotoSansKR-Regular.ttf",         // internal name (아무 문자열 OK)
                BaseFont.IDENTITY_H,          // 유니코드 CJK
                BaseFont.EMBEDDED,            // 폰트 임베드
                false,                        // cached (메모리 캐시 안 함)
                fontBytes,                    // TTF 바이트
                null                          // PFB (Type1용, TTF면 null)
        );
//        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        int totalPages = reader.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte over = stamper.getOverContent(i);
            over.beginText();
            over.setFontAndSize(bf, 26f); // 글씨 크게 (28pt)
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

    private void deleteS3FileAndThumnailImage(SnsEvent snsEvent) {
        amazonS3Manager.deleteFile(snsEvent.getKeyNameParticipantPdf());
        amazonS3Manager.deleteFile(snsEvent.getKeyNameParticipantWord());
        amazonS3Manager.deleteFile(snsEvent.getKeyNameWinnerPdf());
        amazonS3Manager.deleteFile(snsEvent.getKeyNameWinnerWord());
        amazonS3Manager.deleteFile(snsEvent.getThumbnailKeyName());
    }
}
