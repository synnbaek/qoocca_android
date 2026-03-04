📱 Qoocca Parent App (부모용)

Qoocca Parent App은 학원 관리 시스템과 연동되어
학부모님이 자녀의 학원비 결제 요청을 실시간 푸시 알림으로 받고,
앱 내에서 즉시 결제 및 취소를 처리할 수 있는 서비스입니다.

🏗 프로젝트 디렉토리 구조 (Directory Structure)

프로젝트는 MVVM 아키텍처를 기반으로 기능별 패키징이 되어 있습니다.

app/src/main/java/com/qoocca/parentapp/

├── data/
│   ├── network/                # API 응답 처리 및 Result 래퍼
│   ├── repository/             # 데이터 소스 제어 (FCM, Receipt 등)
│   └── model/                  # DTO 및 엔티티 클래스
│
├── presentation/
│   ├── main/                   # 메인 홈 화면 (영수증 리스트)
│   ├── login/                  # 로그인 화면 (전화번호 인증)
│   └── common/
│       ├── AppLogger.kt        # 보안 마스킹 포함 로그 도구
│       ├── NotificationRouter.kt # 알림 클릭 시 화면 이동 제어
│       └── AuthSessionManager.kt # 세션 만료 및 인증 상태 관리
│
├── ui/
│   └── theme/                  # Compose Theme, Color, Font(Paybooc)
│
├── AuthManager.kt              # SharedPreferences 기반 세션 관리자
├── AppContainer.kt             # 의존성 주입(DI) 컨테이너
├── ParentAppApplication.kt     # 애플리케이션 클래스 (FCM/DI 초기화)
├── MyFirebaseMessagingService.kt # FCM 메시지 수신 및 알림 생성 서비스
├── ReceiptDetailActivity.kt    # 결제 상세 처리 화면
└── MainActivity.kt             # 진입점 및 권한 체크
🛠 기술 스택 (Tech Stack)

UI: Jetpack Compose (Material 3)

Language: Kotlin 2.2.10

Async: Coroutines & Flow

Network: OkHttp3 & Retrofit

Push: Firebase Cloud Messaging (FCM) - Data-only Payload

Build: Gradle Kotlin DSL (Version Catalog)

🚀 주요 기능 및 워크플로우
1️⃣ 인증 및 FCM 연동

전화번호 로그인

로그인 성공 시 서버로부터 parentId 발급

토큰 자동 등록

로그인 직후 기기의 FCM 토큰을 서버 DB에 parentId와 매핑

세션 유지

AuthManager를 통해 앱 재실행 시 로그인 상태 유지

2️⃣ 실시간 결제 알림 (FCM)

Data-only 메시지

notification 필드 없이 data 필드만 포함된 푸시 사용

안정적 수신

앱 종료/백그라운드 상태에서도 onMessageReceived()에서 직접 알림 생성

중복 방지

NotificationDeduplicator로 동일 영수증 알림 차단

3️⃣ 영수증 결제 처리

화면 이동

알림 클릭 시 receiptId를 Intent로 전달하여 ReceiptDetailActivity 실행

결제 API

POST /api/receipt/{id}/pay?parentId=...

실시간 수납 완료 처리

⚙️ 설정 및 실행 방법
1️⃣ API 베이스 URL 설정

파일 경로:

app/src/debug/java/com/qoocca/parentapp/ApiConfig.kt

설정 내용:

object ApiConfig {
    const val API_BASE_URL: String = "https://qoocca-teachers.r-e.kr"
}
2️⃣ Firebase 설정

google-services.json 파일을 app/ 폴더에 위치

보안상 .gitignore에 반드시 포함

3️⃣ 알림 권한 (Android 13+)

앱 실행 시 POST_NOTIFICATIONS 권한을 반드시 허용해야 푸시 알림을 받을 수 있습니다.

⚠️ 개발자 참고 사항 (Issue & PR)
🔐 보안

로그 출력 시 AppLogger 사용

FCM 토큰 등 민감 정보 마스킹 처리

🌐 네트워크

모든 통신은 HTTPS 사용

운영 서버 도메인 엄격 준수

🎨 UI

브랜드 아이덴티티를 위해 payboocFontFamily 기본 적용

© 2024 Qoocca Team. All rights reserved.
