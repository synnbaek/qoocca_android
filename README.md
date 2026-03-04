# 📱 Qoocca Parent App (부모용)

**Qoocca Parent App**은 학원 관리 시스템과 연동되어 학부모님이 자녀의 학원비 결제 요청을 실시간 푸시 알림으로 받고, 앱 내에서 즉시 결제 및 취소를 처리할 수 있는 서비스입니다.

---

## 🏗 프로젝트 디렉토리 구조 (Directory Structure)

프로젝트는 **MVVM 아키텍처**를 기반으로 기능별 패키징이 되어 있습니다.

text app/src/main/java/com/qoocca/parentapp/ ├── data/                       # 데이터 계층 (Network & Local Storage) │   ├── network/                # API 응답 처리 및 Result 래퍼 │   ├── repository/             # 데이터 소스 제어 (FCM, Receipt 등) │   └── model/                  # DTO 및 엔티티 클래스 ├── presentation/               # UI 계층 (MVVM) │   ├── main/                   # 메인 홈 화면 (영수증 리스트) │   ├── login/                  # 로그인 화면 (전화번호 인증) │   └── common/                 # 공통 유틸리티 │       ├── AppLogger.kt        # 보안 마스킹 포함 로그 도구 │       ├── NotificationRouter.kt # 알림 클릭 시 화면 이동 제어 │       └── AuthSessionManager.kt # 세션 만료 및 인증 상태 관리 ├── ui/                         # 디자인 시스템 │   └── theme/                  # Compose Theme, Color, Font(Paybooc) 설정 ├── AuthManager.kt              # SharedPreferences 기반 세션 관리자 ├── AppContainer.kt             # 의존성 주입(DI) 컨테이너 ├── ParentAppApplication.kt     # 애플리케이션 클래스 (FCM/DI 초기화) ├── MyFirebaseMessagingService.kt # FCM 메시지 수신 및 알림 생성 서비스 ├── ReceiptDetailActivity.kt    # 결제 상세 처리 화면 └── MainActivity.kt             # 진입점 및 권한 체크
Java
---

## 🛠 기술 스택 (Tech Stack)

- **UI:** Jetpack Compose (Material 3)
- **Language:** Kotlin 2.2.10
- **Asynchronous:** Coroutines & Flow
- **Network:** OkHttp3 & Retrofit
- **Push:** Firebase Cloud Messaging (FCM) - **Data-only Payload** 방식
- **Build:** Gradle Kotlin DSL (Version Catalog)

---

## 🚀 주요 기능 및 워크플로우

### 1. 인증 및 FCM 연동
- **전화번호 로그인**: 학부모 전화번호로 로그인 시 서버로부터 `parentId`를 발급받습니다.
- **토큰 자동 등록**: 로그인 성공 직후, 기기의 고유 FCM 토큰을 추출하여 서버 DB에 `parentId`와 매핑하여 저장합니다.
- **세션 유지**: `AuthManager`를 통해 앱 재실행 시에도 로그인 상태를 유지합니다.

### 2. 실시간 결제 알림 (FCM)
- **Data-only 메시지**: 서버에서 `notification` 필드 없이 `data` 필드만 포함된 푸시를 보냅니다.
- **안정적 수신**: 앱이 종료되었거나 백그라운드 상태여도 `onMessageReceived`가 직접 알림을 생성하여 사용자에게 노출합니다.
- **중복 방지**: `NotificationDeduplicator`를 통해 동일 영수증에 대한 중복 알림을 차단합니다.

### 3. 영수증 결제 처리
- **화면 이동**: 알림 클릭 시 `receiptId`를 Intent로 전달하며 `ReceiptDetailActivity`가 실행됩니다.
- **결제 API**: `POST /api/receipt/{id}/pay?parentId=...` 호출을 통해 실시간으로 수납 처리를 완료합니다.

---

## ⚙️ 설정 및 실행 방법

### 1. API 베이스 URL 설정
운영 환경 변경에 따라 `https` 도메인을 사용합니다.
- **파일 경로:** `app/src/debug/java/com/qoocca/parentapp/ApiConfig.kt`
- **설정 내용:**
kotlin object ApiConfig { const val API_BASE_URL: String = "https://qoocca-teachers.r-e.kr" }
Kotlin
### 2. Firebase 설정
- `google-services.json` 파일을 `app/` 폴더에 위치시켜야 빌드가 가능합니다. (보안상 .gitignore 포함)

### 3. 알림 권한 (Android 13+)
- 앱 실행 시 `POST_NOTIFICATIONS` 권한 팝업이 나타나면 반드시 **[허용]**을 눌러야 푸시 알림을 받을 수 있습니다.

---

## ⚠️ 개발자 참고 사항 (Issue & PR)

- **보안**: 로그 출력 시 `AppLogger`를 사용하여 FCM 토큰 등 민감 정보를 마스킹 처리합니다.
- **네트워크**: 모든 통신은 HTTPS여야 하며, 운영 서버 도메인 주소를 엄격히 준수합니다.
- **UI 폰트**: 브랜드 아이덴티티를 위해 `payboocFontFamily`가 기본 적용되어 있습니다.

---
© 2024 Qoocca Team. All rights reserved.
