# 프로젝트에 대한 간단한 설명
## 📌Project Overview

- HaRu는 소규모 팀을 위한 All-In-One 운영 관리 플랫폼입니다.,
- 보다 효율적인 팀 운영을 위한 고민에서 시작된 HaRu는, 각자의 자리에서 치열하게 움직이는 소규모 팀들의 하루를 돕고자 합니다.,
- 회의 진행 보조, SNS 이벤트 진행 관리, 팀 분위기 체크까지.HaRu는 모든 팀이 더 가치 있는 순간에 집중할 수 있도록 돕습니다.,

## 🚀HaRu Key Features,

- **AI 회의 진행 매니저**,
    - 실시간 STT
    - HaRu AI 회의 진행 질문 추천
    - 회의록 자동 생성
- **SNS 이벤트 어시스턴트**,
    - Instagram 계정 연동
    - 이벤트 URL 등록
    - 이벤트 참여자 및 당첨자 리스트 추출 및 pdf, word 다운로드
- **팀 분위기 트래커**,
    - 설문지 작성 및 배포
    - 팀 분위기 리포트 자동 생성
    - 운영자 맞춤 HaRu 인사이트 제공
---
## ⚒️Technical Overview,

- **FrontEnd**: Next.js · React · TypeScript · Tailwind CSS · Storybook · Vercel,
- **BackEnd**: Spring · FastAPI · Docker · MySQL · AWS(EC2, S3, RDS) · Redis
---

# 사용한 브랜치 전략 및 기술 스택, 프로젝트 구조 등

## 브랜치 전략

<img width="1080" height="536" alt="github flow" src="https://github.com/user-attachments/assets/13defd1d-771e-4409-a26d-84234f269470" />

- 위 github flow를 따름
    
- 브랜치
    - `main` - 배포용 브랜치
    - `dev` - 개발용 브랜치
    - `feat/*` - 개발 피쳐별 브랜치 (새 기능)
    - `fix/*` - 버그 수정 피쳐별 브랜치 (버그 수정)
    - `refactor/*` - 리팩토링 브랜치
- 이슈마다 브랜치 생성하고 커밋 ex) feat/#10-login
- 이슈 해결되면 이슈 close하고 해당 브랜치 삭제
- **main, dev로 나누고, 개발 된 것은 dev에 merge. main에 merge되면 CI/CD**

### 브랜치명 컨벤션

- {태그}/#{이슈번호}-{작업내용}
    - 작업내용 : `kebab-case` , 띄워쓰기는 "-"로 구분
        - kebab-case: 모두 소문자로 표현하며, 단어와 단어 사이에는 하이픈(-)를 사용합니다.
    
    ex) feat/#0-project-init
    

---

## 서버 아키텍처
<img width="1377" height="780" alt="HaRu drawio_1" src="https://github.com/user-attachments/assets/f8131cbf-0dcc-4348-929d-839d87466dc7" />


## 기술 스택
<img width="491" height="321" alt="제목 없는 다이어그램 drawio (1) (1)" src="https://github.com/user-attachments/assets/cdb2e7e2-cbe0-4d92-a216-10d068aa905d" />


| Programming Languages | Java (17) / Python |
| --- | --- |
| Frameworks | SpringBoot / FastAPI |
| Version Control | Git |
| Cloud Services | AWS Route53 / EC2 / RDS(MySQL) / S3 / Docker |
| Database & Caches | MySQL / Redis |
| Deployment Tools | Nginx (프록시 서버) / Github Actions |
| Extra Library | Swagger / WebSockets / elevenlabs |

---

## 프로젝트 구조

### DDD

### 계층 구조
<img width="404" height="399" alt="image (1)" src="https://github.com/user-attachments/assets/f4ca8234-b010-43e6-a824-208b65c96f3c" />


<aside>
📢

**규칙**

1. 위의 계층에서 아래 계층에는 접근 가능 / 아래 계층에서 위 계층 접근 불가능!
2. 한 계층의 관심사와 관련된 그 어떤 것도 다른 계층에 배치되어서는 안됨!
    - 각각의 도메인은 서로 철저히 분리되어야 함.
    - 각각의 Layer는 하나의 관심사에만 집중할 수 있도록.
</aside>

<aside>

- 도메인 종류
    1. User - 회원
    2. Workspace - 워크스페이스
    3. Meeting - 회의록
    4. SnsEvent - SNS 이벤트
    5. MoodTracker - 분위기 트래커
</aside>

<aside>

- 도메인 별 포함되는 패키지
    1. domain (Entity)
        - 데이터베이스와 직접적으로 매핑되는 클래스
        - 각 엔티티가 어떠한 형태, 타입으로 정의되는지 정리
        - ex) User
    2. DTO
        - 통신 시의 요청, 응답 형태를 정의
        - ex) UserRequestDTO, UserResponseDTO
        - Request, Response 하나씩
        - 각 내부에 static class 사용
    3. controller
        - 클라이언트로부터 요청(DTO)을 직접 전달받는 클래스
        - Api Path가 여기서 사용됨
        - 서비스를 호출하고, 최종적으로 값(DTO)을 반환함
        - ex) UserController
    4. service
        - 비즈니스 로직을 처리하는 클래스
        - Controller에서 받은 DTO를 이용하여 비즈니스 로직을 수행
        - Repository를 이용하여 데이터베이스 작업을 수행
        - 결과를 DTO 형태로 반환
        - ex) UserService
    5. repository
        - 데이터베이스와 상호작용
        - CRUD 작업 수행
        - JPA가 여기서 사용됨
        - ex) UserRepository
    6. converter
        - DTO ↔ Entity 변환을 담당
        - Controller, Service에서 원하는 형태로 변환할 때 호출하여 사용
</aside>

---

<aside>

**각각의 레이어 및 구조 설명**

1. domain
    
    엔티티. 외부 변경에 의해 도메인 내부 변경되는 것을 막아야 함. 
    
2. infra
    
    외부와의 통신 담당 로직.
    
    ex) 카카오 인증서버 OAuth
    
3. global
    
    공통된 응답처리
    
</aside>

---

### 패키지 구조 예시

```markdown
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── haru
    │   │           └── server
    │   │               ├── ServerApplication.java
    │   │               ├── domain
    │   │               │   ├── user
    │   │               │   │   ├── controller
    │   │               │   │   ├── service
    │   │               │   │   ├── repository
    │   │               │   │   ├── converter
    │   │               │   │   ├── dto
    │   │               │   │   ├── exception
    │   │               │   │   │   ├── handler
    │   │               │   │   │   ├── validator
    │   │               │   │   │   └── annotation
    │   │               │   │   └── entity
    │   │               │   ├── ...
    │   │               │   ├── moodTracker
    │   │               │   │   ├── controller
    │   │               │   │   ├── service
    │   │               │   │   ├── repository
    │   │               │   │   ├── converter
    │   │               │   │   ├── dto
    │   │               │   │   **├──** exception
    │   │               │   │   │   ├── handler
    │   │               │   │   │   ├── validator
    │   │               │   │   │   └── annotation
    │   │               │   │   └── entity
    │   │               ├── global
    │   │               │   ├── common
    │   │               │   │   └── entity
    │   │               │   │       └── BaseEntity.java
    │   │               │   ├── config
    │   │               │   │   ├── SwaggerConfig.java
    │   │               │   │   ├── properties
    │   │               │   │   └── security
    │   │               │   │       └── SecurityConfig.java
    │   │               │   ├── apiPayload
    │   │               │   │   ├── code
    │   │               │   │   │   ├── Status
    │   │               │   │   │   │   ├── ErrorStatus
    │   │               │   │   │   │   └── SuccessStatus
    │   │               │   │   │   ├── BaseCode.java
    │   │               │   │   │   ├── BaseErrorCode.java
    │   │               │   │   │   ├── ErrorReasonDTO.java
    │   │               │   │   │   └── ReasonDTO.java
    │   │               │   │   ├── exception
    │   │               │   │   │   ├── ExceptionAdvice.java
    │   │               │   │   │   └── GeneralException.java
    │   │               │   │   └── ApiResponse.java
    │   │               │   └── util
    │   │               └── infra
    │   └── resources
    │       └── application.yml
    │       └── application-secret.yml
```

  

---

---

---

# 팀원 정보

| 이름 | 닉네임 | 파트 | 소속 | 전화번호 | GitHub |
| --- | --- | --- | --- | --- | --- |
| 황지원 | 벨라 | PM | 중앙대학교 경영학부 | 010-4139-4130 | hwangjeewon |
| 이수호 | 쏘 | Designer | 한양대학교 ERICA ICT학부 디자인테크놀로지 | 010-9455-9509 | Leesuho9509 |
| 김여진 | 조이 | Frontend Developer | 숭실대학교 IT대학 글로벌미디어학부 | 010-5001-9456 | duwlsssss |
| 박수현 | 노코 | Frontend Developer | 명지대학교 컴퓨터공학과 | 010-6631-1760 | strfunctionk |
| 손기훈 | 제트 | Frontend Developer | 숭실대학교 IT대학 글로벌미디어학부 | 010-3947-5847 | S-Gihun |
| 박경운 | 하늘 | Frontend Developer | 중앙대학교 소프트웨어대학 소프트웨어학부 | 010-9344-8561 | kyeoungwoon |
| 임동재 | 포츠 | Backend Developer | 중앙대학교 소프트웨어대학 소프트웨어학부 | 010-8986-2425 | djlim2425 |
| 김진호 | 루피 | Backend Developer | 중앙대학교 소프트웨어대학 소프트웨어학부 | 010-8828-5091 | Jinho622 |
| 이호근 | 우디 | Backend Developer | 숭실대학교 컴퓨터학부 | 010-9842-4789 | 2ghrms |
| 이석주 | 닉 | Backend Developer | 중앙대학교 소프트웨어대학 소프트웨어학부 | 010-4067-2687 | hknhj |
