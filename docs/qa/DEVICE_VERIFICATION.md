# 실기기 검증 기록 — 문자 인식 모듈 미설치 시나리오

`QA_RELEASE_RECORD.md` 8절이 남겨 둔 한계 **"v1.0.2 검증은 에뮬레이터에서만 했고 실기기 검증은 하지 않았다"**를 닫기 위한 기록이다. 측정하지 않은 값은 적지 않는다.

## 왜 에뮬레이터로는 할 수 없었나

`OCR_BENCHMARK.md`와 `QA_RELEASE_RECORD.md`에 적힌 대로, 에뮬레이터의 Google Play services에는 **한국어 OCR 모듈이 이미 설치돼 있어** 「모듈 미보유 + 네트워크 없음」 조합을 만들 수 없었다. 코드에는 `catch` 폴백이 있었으나 **그 경로를 실제로 밟아본 적이 없었다.**

앱을 한 번도 실행한 적 없는 실기기가 그 상태에 해당한다.

## 검증 환경

| 항목 | 값 |
|---|---|
| 기기 | Galaxy A32 (SM-A325N), `a32` / `a32ks` |
| OS | **Android 13 (API 33)**, `TP1A.220624.014.A325NKSS9DYA1` |
| Google Play services | `26.32.34 (190400-968093310)` |
| 검증 일자 | 2026-08-23 |
| 대상 빌드 (1차) | `app/release/app-release.apk` — v1.0.2, versionCode 4, 로컬 서명본 |
| 대상 빌드 (2차) | `app-debug.apk` — 아래 수정 반영본 |

> ⚠️ **에뮬레이터 검증은 API 36에서 했다. 이 검증은 API 33이다.** 두 결과를 같은 조건으로 취급하지 않는다.
> ⚠️ 스토어 설치본이 아니라 `adb install` 이다. `base.dm`이 없어 **baseline profile이 적용되지 않은 조건**이므로, 이 조건에서 측정된 시작 시간(2.28초)을 스토어 설치본 값으로 일반화하지 않는다.

## 전제 조건 확립

모듈 미설치 상태를 "새 기기니까 없을 것"으로 추정하지 않고 관측으로 확인했다.

1. 네트워크 차단을 설정값이 아니라 도달 가능성으로 확인했다. 삼성 기기는 비행기 모드에서도 Wi-Fi를 개별로 켤 수 있어 `settings get global` 값만으로는 판단할 수 없다.
   - `ping -c 3 -W 3 8.8.8.8` → `connect: Network is unreachable`
   - `dumpsys wifi` → `Wi-Fi is disabled`
2. 앱 설치 이력이 없음을 확인했다. `pm list packages | grep fridgedday` → 결과 없음.
3. 모듈 부재는 **실패 로그로 확인**했다. 비루팅 기기에서는 Play services의 선택 모듈 목록을 직접 열람할 수 없다.

```
W/DynamiteModule: Local module descriptor class for com.google.mlkit.dynamite.text.korean not found.
W/DynamiteModule: Local module descriptor class for com.google.android.gms.mlkit_ocr_korean not found.
W/ProviderHelper: Unknown dynamite feature mlkit_ocr_korean
E/MobileVisionBase: com.google.mlkit.common.MlKitException:
                    Waiting for the text optional module to be downloaded. Please wait.
```

## 1차 결과 — v1.0.2 릴리스 빌드

**크래시 없음.** `FATAL` / `AndroidRuntime` 0건. `catch` 폴백이 실물에서 동작했고 수동 입력 경로로 저장까지 이어졌다. 알림 권한(API 33 런타임 권한) 요청도 정상 표시됐다.

촬영 3회(20:54:49 / 20:54:59 / 20:55:14) 모두 동일하게 동작했다. 시도마다 모듈 조회가 4~5회 발생하는데 이는 원본·반전 × 0도·90도 4패스 구조와 일치한다. **ML Kit 실패 자체는 약 0.23초로 빠르다.**

### 발견 1 — 실패 안내와 진행 표시가 동시에 노출된다

실패 문구가 뜬 뒤에도 "이미지 분석 및 날짜 인식 중..." 텍스트와 진행 표시가 약 5초 더 유지됐다.

원인은 `AddEditScreen.kt`의 카메라·갤러리 두 경로 모두에서 동일했다.

```kotlin
snackbarHostState.showSnackbar("날짜를 찾지 못했습니다. ...")  // 스낵바가 닫힐 때까지 반환하지 않는다
} finally {
    isProcessingOCR = false                                   // 그 뒤에야 진행 표시가 꺼진다
}
```

`showSnackbar`는 스낵바가 사라질 때까지 suspend 되는 호출이다. 기본 지속시간(Short) 약 4초 + 애니메이션이 관측된 5초와 일치한다. 로그에서도 실패 시각(`20:54:49.451`) 이후 약 5초간 앱이 1초 간격으로 프레임을 그린다.

> **이 결함은 모듈 미설치와 무관하다.** OCR이 실패하는 모든 경우에 발생하며 에뮬레이터에서도 재현됐을 것이다.
> 계측 테스트 15건이 이 경로의 UI 상태 타이밍을 검증하지 않아 드러나지 않았다.
> "실기기라서 잡혔다"가 아니라 **"실패 경로를 사람이 직접 밟아서 잡혔다"**가 정확한 서술이다.

### 발견 2 — 실패 원인을 구분하지 않고 로그도 남기지 않았다

`TextRecognitionHelper.processBitmap`의 `catch (e: Exception)`이 예외를 로그 없이 삼켰다. 그 결과 앱 프로세스가 남긴 로그는 전부 프레임워크 태그(`ViewRootImpl`, `CameraManagerGlobal` 등)뿐이고, 실패의 유일한 흔적은 ML Kit 내부의 `E/MobileVisionBase`였다.

사용자 입장에서는 두 상황이 같은 문구로 보인다.

| 실제 상황 | 노출되던 안내 |
|---|---|
| 모듈이 없어 인식 자체가 불가능 | "날짜를 찾지 못했습니다" |
| 인식은 됐으나 사진에 날짜가 없음 | "날짜를 찾지 못했습니다" |

전자는 무엇을 촬영해도 실패한다. 그런데 안내가 사진 탓으로 읽혀 **검증 중 실제로 3회 재촬영이 일어났다.** 이것이 이 결함의 실증이다.

## 수정

| 파일 | 변경 |
|---|---|
| `ui/addedit/AddEditScreen.kt` | 카메라·갤러리 두 경로 모두 `isProcessingOCR = false`를 `showSnackbar` 이전으로 옮겼다. `catch` 분기에도 동일하게 적용했다 |
| `ui/addedit/AddEditScreen.kt` | `extractExpiryDate` 대신 `evaluateExpiryDate`를 호출해, `processedVariantCount == 0 && failedVariantCount > 0`일 때 별도 안내를 노출한다 |
| `util/ocr/TextRecognitionHelper.kt` | `catch`에서 예외를 `Log.w(TAG, ...)`로 남긴다. 태그 `OcrHelper` |

새 안내 문구: `문자 인식을 준비하지 못했습니다. 인터넷 연결 후 다시 시도하거나 날짜를 직접 선택해주세요.`

> 모델을 내려받는 주체는 Google Play services이며 **앱은 여전히 `INTERNET` 권한을 갖지 않는다.** 안내의 "인터넷 연결"은 앱의 통신을 뜻하지 않는다.

## 2차 결과 — 수정 반영 debug 빌드, 동일 조건

네트워크를 연결하지 않은 채(모듈 여전히 부재) 재검증했다. `app.fridgedday.debug`로 별도 설치했다.

| 확인 항목 | 결과 |
|---|---|
| 문구 노출 즉시 진행 표시 종료 | **확인** |
| 모듈 미준비 전용 안내 노출 | **확인** |
| 수동 입력 저장 동작 | **확인** |

로그에서 수정 3이 확인된다. 4패스 각각에 대해 기록이 남는다.

```
21:18:03.796 W/OcrHelper: OCR variant failed: MlKitException: Waiting for the text optional module to be downloaded. Please wait.
21:18:03.856 W/OcrHelper: OCR variant failed: MlKitException: ...
21:18:03.908 W/OcrHelper: OCR variant failed: MlKitException: ...
21:18:03.971 W/OcrHelper: OCR variant failed: MlKitException: ...
```

전 패스 실패(`processedVariantCount == 0`)가 성립해 새 분기가 의도대로 선택됐다. 촬영 1회의 OCR 구간 소요는 약 305ms다.

## 3차 결과 — 네트워크 복구 시나리오

비행기 모드를 해제하고 Wi-Fi를 연결한 뒤, 동일 debug 빌드로 같은 라벨을 촬영했다.

| 시각 | 사건 |
|---|---|
| `21:23:08.107` ~ `.569` | **1회차 실패.** 네트워크는 연결됐으나 모듈은 아직 부재. `Local module descriptor ... not found` 5회, `W/OcrHelper: OCR variant failed` 4회. **462ms 만에 실패 판정**, 모듈 미준비 안내 노출 |
| 약 10.4초 | Play services가 백그라운드로 모듈을 내려받음 |
| `21:23:18.945` | **2회차 성공.** `Selected remote version of com.google.android.gms.mlkit_ocr_korean, version >= 263234001`. `OcrHelper` 실패 로그 0건. 인식 2~3초 후 성공, 날짜 정확 |

**모듈 다운로드는 첫 인식 시도가 촉발한다.** 즉 네트워크 연결 직후의 첫 시도는 실패하는 것이 정상 경로이고, 이때 사용자가 무엇을 해야 하는지가 안내에 달려 있다.

- 수정 전 문구였다면 `"날짜를 찾지 못했습니다"`가 노출됐을 것이다. 사용자는 사진 문제로 이해해 **각도·조명을 바꿔가며 재촬영**하게 되지만, 실제 해법은 "잠시 후 다시 시도"다.
- 수정 후 문구는 `"인터넷 연결 후 다시 시도하거나..."`이며, 검증에서 실제로 **안내대로 재시도해 성공**했다.

이로써 발견 2의 수정 효과가 실사용 경로에서 확인됐다.

### 2026-08-23 시점 개선 여지 (당시 미착수)

첫 시도가 실패해야 다운로드가 시작되는 구조다. Play services의 `ModuleInstallClient`로 모듈 설치를 선요청하거나 진행 상태를 노출하면 첫 실패 자체를 없앨 수 있다. **이번 범위에서는 하지 않았고, 필요성 판단도 아직이다.**

## 부수 관찰

- **`Killing 2187:app.fridgedday (adj 101): stop app.fridgedday due to SPEG`** — 최초 실행 직후 앱 프로세스가 종료되고 재시작됐다. 삼성 SPEG가 이 앱을 게임으로 판정해(`identifyGamePackage`) 가상 디스플레이(`SpegVirtualDisplay`)에 띄웠다가 정리한 흐름으로 보인다. 사용자 체감상 이상은 없었다. **앱 결함으로 단정하지 않는다. 원인 규명은 하지 않았다.**
- 삼성 자체 OCR(`DeepSkyLibrary` / `SmartCapture`)의 `isVisionTextSupported false` 로그는 이 앱과 무관한 기기 기본 기능이다.

## 2026-08-23 시점 남은 한계

- **단위 테스트를 로컬에서 실행하지 못했다.** 6개 테스트 클래스가 모두 `ClassNotFoundException`으로 초기화에 실패한다. 수정 전 코드(`git stash`)에서도 동일하게 재현되므로 이번 변경과 무관한 기존 환경 문제다. `gradle.properties`의 `android.overridePathCheck=true`와 저장소가 비ASCII 경로에 있는 점이 유력한 원인이나 **확정하지 않았다.** 이번 수정은 컴파일 통과와 실기기 수동 검증까지만 확보된 상태다.
- 이번 검증은 **API 33 단일 기기**다. 다른 제조사·API 레벨로 일반화하지 않는다.
- 수정본은 **debug 빌드로만 검증했다.** 릴리스 빌드(R8 적용)에서의 동작은 별도 확인이 필요하다.
- 이 수정은 **2026-08-23 당시 아직 배포되지 않았다.** 당시 원스토어 공개본은 v1.0.2였다.
- 네트워크 복구 시나리오는 **Wi-Fi 1회, 촬영 2회 관측**이다. 모바일 데이터·저속 회선·다운로드 실패 상황은 확인하지 않았다.

## 4차 결과 — bundled Korean ML Kit 후보, 오프라인 첫 실행 (2026-09-09)

2026-09-09에는 2026-08-23의 optional-module 실패 경로를 제품 의존성 차원에서 없애기 위해 `com.google.mlkit:text-recognition-korean:16.0.1` bundled 후보를 검증했다. 공개 원스토어 v1.0.2가 아니라 `feat/bundled-mlkit` 개발 브랜치의 debug/Release 산출물 대상이다.

### 자동·동일 기기 회귀

- 로컬 품질 게이트 5종(`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest`) 통과.
- Galaxy A32(SM-A325N, API 33)에서 2026-09-08 동일 기기 main 기준선과 비교:
  - D-30 exact `40/55 → 40/55`, expected-candidate `44/55 → 44/55`
  - D-180 exact `38/55 → 38/55`, expected-candidate `44/55 → 44/55`
  - aggregate delta 0, 샘플별 exact/expected-candidate 퇴행 0
  - `Release regression passed for D-30 and D-180: bundled-mlkit-a32-20260909`

처음 실행한 회귀 1회는 스크립트 기본값인 과거 API 36 에뮬레이터 CSV와 비교해 잘못된 per-sample 경고를 냈다. 이 비교는 동일 기기 계약을 위반하므로 판정에서 제외했고, 위 결과는 올바른 A32 main 기준선으로 재실행한 값이다.

### 클린 설치 + 네트워크 없음 + 첫 OCR

1. A32에서 비행기 모드 ON, Wi-Fi OFF를 확인했다.
2. `ping`은 `Network is unreachable`이었다.
3. production `app.fridgedday`는 보존한 채 별도 debug 패키지 `app.fridgedday.debug`를 최초 설치했다. `firstInstallTime=2026-09-09 09:01:42`.
4. logcat을 초기화한 뒤 앱을 cold start했다.
5. 사용자가 실제 날짜 `2027.02.28`인 라벨로 **첫 OCR 시도 성공**을 확인했다. 화면의 인식 날짜 문자열 자체는 별도 로그로 수집하지 않았으므로 이 1회를 정확도 측정으로 사용하지 않는다.
6. 해당 첫 시도 구간에서 `OcrHelper: OCR variant failed`, `Waiting for the text optional module to be downloaded`, `MlKitException` 오류는 0건이었다.

### Dynamite 로그 해석

bundled dependency의 모델 자산(`Kore_ctc`, `Latn_ctc`)은 APK에 포함되어 있고, Google 공식 설치 경로 정의상 bundled 모델은 빌드 시 앱에 정적으로 포함된다. 동시에 현재 ML Kit runtime dependency graph에는 Play-services text-recognition 구성요소가 transitive로 포함되며, 이 A32에는 과거 unbundled 검증에서 설치된 `mlkit_ocr_korean`/`mlkit_ocr_common` Dynamite 모듈도 남아 있어 runtime 선택 로그가 발생했다.

따라서 이번 수락 조건은 **"Dynamite 로그가 없어야 한다"가 아니라 "bundled 모델을 패키징한 clean app install이 실제 네트워크 없이 첫 OCR을 수행하고 optional-module 대기 오류를 내지 않는다"**이다. Google Play services 전체 데이터를 지우는 파괴적 검증은 수행하지 않았다.

### 현재 남은 한계

- 이번 오프라인 수동 확인은 A32/API 33 단일 기기, 실제 라벨 1회다. 정확도 일반화 근거가 아니라 첫 실행 가용성 확인이다.
- Release APK는 R8 빌드·모델 패키징·권한·비공개자료 제외까지 확인했지만 로컬 산출물이 unsigned라 해당 APK 자체를 adb 설치하지 않았다. 기능 경로는 동일 dependency의 signed debug 패키지로 검증했다.
- bundled 변경은 2026-09-09 PR #9로 `main`에 merge했다. 원스토어에는 아직 새 버전으로 배포하지 않았다.

## 5차 결과 — Phase 3/4 format-gap A32 측정 (2026-09-09)

같은 Galaxy A32(SM-A325N, API 33)에서 실제 촬영일 기반 format-gap set을 측정했다. source는 카카오톡을 거치지 않고 기기에서 직접 가져온 JPEG를 사용했다. 2026-09-09 시각 메타데이터는 남아 있지만 `DateTimeOriginal`은 비어 있어 카메라 원본 포맷 자체로 일반화하지 않는다. 카카오톡 전송본은 benchmark source로 쓰지 않았다.

- 식품 primary: `YYYY년 MM월 DD일` 1장 → exact `1/1`, expected-candidate `1/1`, failed variant `0`.
- 생활권 추가 탐색에서도 식품 연속 `YYYYMMDD`·`YYMMDD`는 확보하지 못해 목표 사용자 성능은 미측정으로 남겼다.
- 비식품 auxiliary format-only probe: `YYYYMMDD` 5장 + `MMDDYYYY` 1장. 1차 잠정 측정은 사람이 읽은 정답 2건이 잘못돼 제외했다. 사용자 확인으로 ground truth를 수정한 뒤 다시 실행한 최종 결과는 전체 exact `4/6`, any-candidate `4/6`, expected-candidate `4/6`, failed variant `0`.
- `YYYYMMDD`만 보면 exact `4/5` (80%, n=5); 실패는 `no_date_candidate` 1건뿐이었다. 따라서 반복 parser 원인으로 판정하지 않았다.
- `MMDDYYYY` 1장은 `no_date_candidate`; 현재 compact parser 지원 밖이지만 primary target이 아닌 단일 보조 표본이라 parser 확장은 하지 않았다.
- `MM/YYYY`, `YYYYMM` 월 단위 표본과 인쇄가 가려진 추가 `MMDDYYYY`는 exact-date benchmark에서 제외했다.

이 측정은 형식 gap을 닫기 위한 소규모 실사진 관측이다. 55장 고정 회귀와 합치지 않고, 비식품 auxiliary 결과를 한국 식품 라벨 정확도로 일반화하지 않는다.
## 재현 방법

```powershell
$env:ANDROID_SERIAL = "<기기 시리얼>"
adb shell 'ping -c 3 -W 3 8.8.8.8'          # Network is unreachable 이어야 한다
adb shell pm list packages | Select-String fridgedday   # 결과가 없어야 한다
adb logcat -c; adb logcat -v time | Tee-Object -FilePath phase1.log
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

🔴 **네트워크를 한 번이라도 연결하면 모듈이 설치되어 이 시나리오는 재현 불가능해진다.** 기기 최초 상태에서 한 번만 가능하다.
