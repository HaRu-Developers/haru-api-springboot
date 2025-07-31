package com.haru.api.domain.snsEvent.service;

import com.haru.api.domain.snsEvent.converter.SnsEventConverter;
import com.haru.api.domain.snsEvent.dto.SnsEventRequestDTO;
import com.haru.api.domain.snsEvent.dto.SnsEventResponseDTO;
import com.haru.api.domain.snsEvent.entity.Participant;
import com.haru.api.domain.snsEvent.entity.SnsEvent;
import com.haru.api.domain.snsEvent.entity.Winner;
import com.haru.api.domain.snsEvent.repository.ParticipantRepository;
import com.haru.api.domain.snsEvent.repository.SnsEventRepository;
import com.haru.api.domain.snsEvent.repository.WinnerRepository;
import com.haru.api.domain.user.entity.User;
import com.haru.api.domain.user.repository.UserRepository;
import com.haru.api.domain.user.security.jwt.SecurityUtil;
import com.haru.api.domain.userWorkspace.entity.UserWorkspace;
import com.haru.api.domain.userWorkspace.repository.UserWorkspaceRepository;
import com.haru.api.domain.workspace.entity.Workspace;
import com.haru.api.domain.workspace.repository.WorkspaceRepository;
import com.haru.api.global.apiPayload.exception.handler.MemberHandler;
import com.haru.api.global.apiPayload.exception.handler.SnsEventHandler;
import com.haru.api.global.apiPayload.exception.handler.WorkspaceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.haru.api.global.apiPayload.code.status.ErrorStatus.*;

@Service
@RequiredArgsConstructor
public class SnsEventCommandServiceImpl implements SnsEventCommandService{

    private final SnsEventRepository snsEventRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final ParticipantRepository participantRepository;
    private final WinnerRepository winnerRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public SnsEventResponseDTO.CreateSnsEventResponse createSnsEvent(Long workspaceId, SnsEventRequestDTO.CreateSnsRequest request) {
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
        snsEventRepository.save(createdSnsEvent);

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
        return null;
    }

    private SnsEventResponseDTO.InstagramMediaResponse fetchInstagramMedia(String accessToken) {
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


    public List<SnsEventResponseDTO.Comment> getComments(String mediaId, String accessToken) {
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

    private int countOccurrences(String text, String keyword) {
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

    public SnsEventResponseDTO.GetSnsEventListRequest getSnsEventList(Long userId, Long workspaceId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceHandler(WORKSPACE_NOT_FOUND));
        UserWorkspace foundUserWorkSapce = userWorkspaceRepository.findByUserAndWorkspace(foundUser, foundWorkspace)
                .orElseThrow(() -> new MemberHandler(NOT_BELONG_TO_WORKSPACE));
        List<SnsEvent> snsEventList = snsEventRepository.findAllByWorkspace(foundWorkspace);
        return SnsEventConverter.toGetSnsEventListRequest(snsEventList);
    }

    public SnsEventResponseDTO.GetSnsEventRequest getSnsEvent(Long userId, Long snsEventId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new MemberHandler(MEMBER_NOT_FOUND));
        SnsEvent foundSnsEvent = snsEventRepository.findById(snsEventId)
                .orElseThrow(() -> new SnsEventHandler(SNS_EVENT_NOT_FOUND));
        List<Participant> participantList = participantRepository.findAllBySnsEvent(foundSnsEvent);
        List<Winner> winnerList = winnerRepository.findAllBySnsEvent(foundSnsEvent);
        return SnsEventConverter.toGetSnsEventRequest(
                foundSnsEvent,
                participantList,
                winnerList
        );
    }
}
