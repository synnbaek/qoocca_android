# qoocca_android
Qoocca Parent App은 학원 관리 시스템과 연동되어 부모님이 자녀의 학원비 결제 요청을 실시간으로 받고, 앱 내에서 즉시 결제 및 취소를 처리할 수 있는 서비스입니다.
🛠 Tech Stack
•
Language: Kotlin 2.2.10
•
UI Framework: Jetpack Compose (Material 3)
•
Asynchronous: Coroutines & Flow
•
Network: OkHttp3 & Retrofit (예정)
•
Push Notification: Firebase Cloud Messaging (FCM)
•
Architecture: MVVM Pattern
•
Local Storage: SharedPreferences (Auth Session 관리)
🚀 주요 기능
1.
학부모 로그인
◦
전화번호 기반의 간편 로그인 기능을 제공합니다.
◦
로그인 성공 시 parentId를 확보하여 모든 통신에 사용합니다.
2.
FCM 실시간 알림 수신
◦
새로운 결제 요청(영수증 발행) 시 푸시 알림을 수신합니다.
◦
Data-only Payload 방식을 사용하여 앱의 상태(포그라운드/백그라운드/종료)와 상관없이 안정적으로 알림을 표시합니다.
3.
결제 요청 관리
◦
수신된 알림을 클릭하면 해당 영수증의 상세 페이지로 즉시 이동합니다.
◦
'결제하기' 및 '취소하기' 기능을 통해 학원비 수납을 비대면으로 처리합니다.
⚙️ 설정 및 설치 방법
1. 사전 요구 사항
•
Android Studio: Ladybug 이상 권장
•
JDK: 17 버전 이상
•
Android 기기: API Level 24 (Nougat) 이상
2. Firebase 설정
보안을 위해 google-services.json 파일은 저장소에서 제외되어 있습니다.
1.
Firebase Console에서 프로젝트를 생성합니다.
2.
Android 앱을 등록하고 com.qoocca.parentapp 패키지명을 설정합니다.
3.
다운로드한 google-services.json 파일을 app/ 디렉토리에 추가합니다.
3. API 서버 설정
현재 앱은 운영 서버와 HTTPS 통신을 수행합니다. 설정은 app/src/debug/java/com/qoocca/parentapp/ApiConfig.kt에서 관리합니다.
•
Base URL: https://qoocca-teachers.r-e.kr
📖 사용 방법
1.
로그인
◦
앱 실행 후 등록된 학부모 전화번호(예: 01012345678)를 입력하여 로그인합니다.
◦
로그인과 동시에 기기의 FCM 토큰이 서버 DB에 등록됩니다.
2.
알림 수신 및 확인
◦
학원에서 결제 요청을 보내면 상단바에 알림이 뜹니다.
◦
알림을 클릭하여 결제 상세 화면으로 진입합니다.
3.
결제 처리
◦
상세 화면에서 영수증 번호와 금액을 확인합니다.
◦
[결제하기] 버튼을 누르면 서버에 승인 요청이 전송되며 결제가 완료됩니다.
🔍 개발자 참고 (Issue & PR)
•
FCM 서비스: MyFirebaseMessagingService.kt에서 수신 로직을 담당하며, data 페이로드의 receiptId를 통해 화면 이동을 제어합니다.
•
네트워크 보안: 운영 환경 변경에 따라 모든 주소는 HTTPS를 사용해야 하며, Cleartext Traffic은 지양합니다.
•
권한 처리: Android 13 이상 기기에서는 앱 실행 시 POST_NOTIFICATIONS 권한 승인이 필요합니다.
🤝 기여 및 문의
•
Project Name: ParentApp
•
Current Version: 1.0.0
•
Main Contact: [담당자 이름 또는 이메일]
