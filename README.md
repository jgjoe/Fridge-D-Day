# 오늘도 신선 (Fridge D-Day)

**유통기한을 촬영·직접 입력으로 관리하고, 인식 결과를 확인한 뒤 저장하는 로컬 우선 Android 앱**

[![ONEstore](https://img.shields.io/badge/ONEstore-v2.0.0%20public-brightgreen)](https://m.onestore.co.kr/v2/ko-kr/app/0001003331)
[![Google Play](https://img.shields.io/badge/Google%20Play-v2.0.0%20closed%20Alpha-orange?logo=googleplay&logoColor=white)](https://play.google.com/apps/testing/app.fridgedday)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

ONEstore에는 **v2.0.0 / code5**가 공개 배포되어 있습니다. Google Play에는 같은 버전이 **closed Alpha**로 출시되어 있고, `>=12` opt-in gate는 확인됐지만 14일 요건과 Production 공개는 아직 진행 중입니다.

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

## 검증된 범위

- v2.0.0/code5 exact RC: JVM unit **107/107**, lint **0 errors**
- Galaxy A32 explicit non-OCR instrumentation: **65/65**
- 55장 고정 OCR 회귀셋으로 변경 전후 퇴행 여부를 반복 확인
- 소규모 usability 검증에서 반복 마찰을 수정한 뒤 affected-user targeted retest 수행
- Release artifact에서 `INTERNET` 권한과 비공개 QA 자료 미포함을 검증
- ONEstore **v2.0.0/code5 공개 배포 완료**
- Google Play **closed Alpha 출시 완료**, Production은 아직 진행 중

> OCR 벤치마크 수치는 고정 표본·고정 평가 조건의 회귀 기준선이며 일반 사용자 전체 정확도로 해석하지 않습니다.

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
git clone https://github.com/jgjoe/Fridge-D-Day.git
cd Fridge-D-Day
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
| Google Play | **v2.0.0 / code5 closed Alpha** · `>=12` opt-in 확인 · 14일 요건 진행 중 |
| Google Play Production | 아직 미공개 |

---

**Jigwan Joe** · [GitHub @jgjoe](https://github.com/jgjoe)