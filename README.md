# 오늘도 신선 (Fridge D-Day)

**유통기한을 촬영·직접 입력으로 관리하고, 인식 결과를 확인한 뒤 저장하는 로컬 우선 Android 앱**

[![ONEstore](https://img.shields.io/badge/ONEstore-v2.0.0%20public-brightgreen)](https://m.onestore.co.kr/v2/ko-kr/app/0001003331)
[![Google Play](https://img.shields.io/badge/Google%20Play-%EC%A0%95%EC%8B%9D%20%EC%B6%9C%EC%8B%9C%20%EC%8B%AC%EC%82%AC%20%EC%A4%91-orange?logo=googleplay&logoColor=white)](https://play.google.com/apps/testing/app.fridgedday)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

ONEstore에 **v2.0.0**을 공개 배포했고, Google Play는 비공개 테스트를 마치고 **정식 출시 심사 중**입니다.

<p align="center">
  <img src="docs/images/today-fresh-1.png" width="31%" alt="오늘도 신선 v2 Today 화면" />
  <img src="docs/images/today-fresh-2.png" width="31%" alt="오늘도 신선 v2 Scan 화면" />
  <img src="docs/images/today-fresh-3.png" width="31%" alt="오늘도 신선 v2 Record 화면" />
</p>

## 무엇을 하는 앱인가

- **Today** — 식품별 D-Day와 임박 상태를 한눈에 확인
- **Scan** — CameraX + ML Kit OCR로 날짜 후보를 추출하고 사용자 확인 후 저장
- **직접 입력** — OCR 없이도 식품명·보관 위치·유통기한을 등록
- **Record** — 등록·소비 완료·기한 경과 기록을 날짜 흐름으로 확인
- **알림·위젯** — WorkManager 알림과 홈 화면 위젯으로 임박 항목 확인
- **백업·복원** — Android SAF를 이용한 로컬 JSON 내보내기·가져오기

## 핵심 설계

### 로컬 우선 데이터 처리

식품 기록은 Room/DataStore에 저장하고 앱 자체에는 `INTERNET` 권한이 없습니다. 광고·분석·추적 SDK나 계정 기능도 두지 않았습니다.

### OCR을 신뢰하지 않는 저장 흐름

OCR은 후보를 제안할 뿐 자동 저장하지 않습니다. 인식한 날짜는 사용자가 확인해야 저장되며, 미확정 상태에서는 저장을 막습니다.

### 단순한 v2 UI 정책

v2는 **light-only**로 고정했고 시스템 night mode와 무관하게 같은 제품 색상 체계를 사용합니다. Navigation Compose의 화면 간 route 전환은 장식 애니메이션 없이 즉시 전환합니다.

## 검증 결과

- 출시 후보(v2.0.0)에서 단위 테스트 **107/107**, lint 오류 **0건**
- Galaxy A32 실기기에서 문자인식 외 기능 테스트 **65/65**
- 55장 고정 OCR 회귀셋으로 변경 전후 퇴행 여부를 반복 확인
- 사용성 테스트에서 찾은 반복 마찰을 수정하고, 해당 사용자에게 수정 흐름을 다시 검증
- 릴리스 빌드에 `INTERNET` 권한과 비공개 QA 자료가 들어가지 않았음을 자동 검증
- ONEstore **v2.0.0 공개 배포**, Google Play 비공개 테스트 완료

## 기술 스택

| 영역 | 기술 |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| 상태·구조 | ViewModel, StateFlow, MVVM, Repository |
| 데이터 | Room, DataStore |
| 카메라·OCR | CameraX, Google ML Kit Text Recognition |
| 백그라운드 | WorkManager, Android Notification |
| 위젯·백업 | Glance App Widget, Android SAF, JSON |
| 품질·빌드 | JUnit, Android Instrumentation, GitHub Actions, Gradle Kotlin DSL |

## 실행

요구사항: Android Studio · JDK 17 · Android SDK 36

```bash
git clone https://github.com/jgjoe/fridge-d-day.git
cd fridge-d-day
./gradlew assembleDebug
```

기본 품질 게이트:

```bash
./gradlew test lintDebug assembleRelease
```

## 상세 검증 문서

- [QA_RELEASE_RECORD.md](QA_RELEASE_RECORD.md) — 릴리스 판단과 v1→v2 검증 요약
- [docs/qa/OCR_BENCHMARK.md](docs/qa/OCR_BENCHMARK.md) — OCR 측정 조건·회귀 기준
- [docs/qa/DEVICE_VERIFICATION.md](docs/qa/DEVICE_VERIFICATION.md) — Galaxy A32 실기기 검증
- [개인정보 처리방침](https://jgjoe.github.io/fresh-today-privacy/privacy_policy.html)

## 배포 상태

| 채널 | 상태 |
|---|---|
| ONEstore | **v2.0.0 / code5 공개 배포** |
| Google Play | 비공개 테스트 완료 · **정식 출시 심사 중** |

## 만든 사람

**Jigwan Joe** — Android · Backend

- GitHub: [@jgjoe](https://github.com/jgjoe)
- Email: jigwan.joe@gmail.com