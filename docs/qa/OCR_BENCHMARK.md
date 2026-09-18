# OCR 품질 벤치마크

오늘도 신선의 실제 라벨 사진을 앱과 동일한 ML Kit 한국어 OCR·날짜 파서로 반복 평가하는 QA 기준이다. 측정하지 않은 값은 성과나 배포 근거로 쓰지 않는다.

> ## 2026-09-18 현재 배포 상태
>
> ONEstore에는 v2.0.0/code5가 공개 배포되어 있고, Google Play에는 같은 버전이 closed Alpha로 출시되어 있다. Play의 `>=12` opt-in gate는 확인됐지만 14일 요건과 Production 공개는 아직 진행 중이다. 현재 릴리스 상태의 정본은 [QA_RELEASE_RECORD.md](../../QA_RELEASE_RECORD.md)다.
>
> ## 🔄 2026-08-15 개정 — 아래는 2026-07-22 v1.1 QA 시점 기록이다
>
> **2026-08-15 당시 상태는 v1.0.2 Released (원스토어, 2026-08-12)였고, 그 시점까지 공개 배포 이력은 2건이었다.**
> v1.1 배포를 보류한 뒤, 정확도를 더 올리는 대신 **인식한 날짜를 사용자가 확인해야 저장되도록 구조를 바꿔**
> 잘못 읽힌 값이 저장되는 경로를 없애고 다시 출시했다. 상세는 [QA_RELEASE_RECORD.md](../../QA_RELEASE_RECORD.md) 6절.
>
> - **단위 테스트는 19개가 아니라 29개**(계측 15개 별도)다. 아래 "단위 테스트 19개"는 v1.1 QA 시점 값이다.
> - **아래 OCR 측정 수치 자체는 그대로 유효하다.** v1.0.2는 인식 정확도를 바꾼 버전이 아니라 저장 경로를 바꾼 버전이다.
> - 이 절의 측정 기록과 릴리스 판단은 시점 기록으로 보존하며 소급 수정하지 않는다.

> ## 🔄 2026-09-09 현재 상태 — QA hardening 재개
>
> 2026-07-22의 v1.1 No-Go 기록과 수치는 역사 기준선으로 보존한다. 이후 2026-09-09 QA hardening을 재개했고, Galaxy A32 동일 기기 main 기준선에 대해 bundled Korean ML Kit 전환본이 D-30 `40/55`, D-180 `38/55`, expected-candidate `44/55`로 **aggregate·샘플별 변화 0**을 확인했다. 이 A32 결과는 과거 API 36 에뮬레이터 기준선과 서로 다른 환경이므로 수치를 합치거나 대체하지 않는다.
>
> 별도 Phase 3/4 실사진 coverage는 2026-09-09 측정을 완료했다. 식품 primary set은 `YYYY년 MM월 DD일` 1장(A32 exact 1/1)을 확보했고, 생활권 추가 탐색에서도 연속 `YYYYMMDD`·`YYMMDD` 식품 라벨은 확보하지 못했다. 별도 비식품 auxiliary probe의 `YYYYMMDD` 5장은 사용자 ground-truth 확인 후 재측정에서 exact 4/5였으며 식품 정확도로 일반화하지 않는다. 55장 고정 회귀셋에는 어느 쪽도 섞지 않는다. current release/Go-No-Go 상태의 정본은 [QA_RELEASE_RECORD.md](../../QA_RELEASE_RECORD.md)다.

### 2026-09-09 Phase 3/4 실제 촬영일 coverage

- 카카오톡을 거치지 않고 기기에서 직접 가져온 JPEG 23장을 같은 날 촬영 자료로 검수했다. 시각 메타데이터는 남아 있지만 `DateTimeOriginal`은 비어 있고 `FullSizeRender.JPEG` 1장이 있어 카메라 원본 포맷이라고 단정하지 않는다. 카카오톡 전송본은 매칭만 하고 benchmark source로 사용하지 않았다.
- 식품 primary coverage: `YYYY년 MM월 DD일` 1장, 평가일은 실제 촬영일 `2026-09-09`. A32에서 exact `1/1`, expected-candidate `1/1`, failed variant `0`. 5장 미만이므로 퍼센트 성능을 주장하지 않는다.
- 식품 compact coverage: 추가 탐색까지 했지만 contiguous `YYYYMMDD`·`YYMMDD`를 확보하지 못했다. 이 두 형식의 목표 사용자 성능은 미측정이다.
- auxiliary format-only probe: 비식품 실물 `YYYYMMDD` 5장 + `MMDDYYYY` 1장을 별도 데이터셋으로 측정했다. 사람이 라벨을 재확인하기 전 잠정 ground truth 2건이 잘못돼 1차 결과는 판정에서 제외했고, 정답 수정 후 A32에서 다시 실행했다. 최종은 전체 exact `4/6`, any-candidate `4/6`, expected-candidate `4/6`; `YYYYMMDD`는 exact `4/5` (80%, n=5), `MMDDYYYY`는 `0/1`이었다.
- corrected `YYYYMMDD`의 유일한 실패는 `no_date_candidate` 1건이다. 5장 중 4장이 정확 성공해 형식 자체의 반복 parser 실패로 보지 않았다. `MMDDYYYY`는 현재 parser 지원 밖이지만 원래 primary target이 아닌 보조 1장이라 parser 확장 근거로 사용하지 않았다.
- `MM/YYYY`, `YYYYMM`처럼 day가 없는 표본은 `LocalDate` 정답을 인위적으로 만들지 않기 위해 exact-date benchmark에서 제외했다. 인쇄가 가려진 추가 `MMDDYYYY`도 제외했다.
- 따라서 이번 Phase 4에서는 parser 수정 없이 관측 결과와 대표성 한계를 문서화하고 종료했다.
## 측정 시점 상태 (2026-07-22)

**당시 판정: v1.0 Released / v1.1 QA No-Go.** 아래 수치와 자동화는 출시 후 v1.1 후보를 평가해 기준 미달 배포를 차단한 종료 증거다. 추가 표본 수집, OCR 개선, 사용자 베타, v1.1 출시 또는 스토어 업데이트는 활성 계획이 아니다.

- 독립 한국 라벨 사진 55장(`existing_20=20`, `new_30_plus=35`)으로 v1.1 릴리스 기준선을 고정했다.
- 수정 전(D-30): 정확 일치 37/55(67.27%), 아무 후보 탐지 54/55(98.18%).
- 수정 후(D-30): 정확 일치 40/55(72.73%), 아무 후보 탐지 54/55(98.18%), 정답 후보 재현 43/55(78.18%).
- 평가일 민감도(D-180): 정확 일치 39/55(70.91%), 아무 후보 탐지 54/55(98.18%), 정답 후보 재현 43/55(78.18%). D-30보다 1장 낮다.
- 최대 실패 유형: `wrong_date` 14건 → 11건. 동일 입력 회귀의 정확 일치 +3, 탐지 변화 0이다.
- Android 검증은 단위 테스트 19개, `lintDebug`, `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest`, D-30/D-180 instrumentation 및 새 스키마의 동일 55장 샘플별 회귀 게이트가 모두 통과했다. 두 시나리오를 한 명령으로 강제하는 릴리스 회귀도 D-30 8분 24초·D-180 8분 6초에 연속 통과했다.
- 종료 점검에서 ML Kit 전이 manifest가 추가하던 `INTERNET` 권한을 명시적으로 제거했다. 재빌드한 Release APK의 권한 목록에 `INTERNET`가 없음을 확인하고 동일 55장 릴리스 회귀를 다시 실행해 D-30 8분 21초·D-180 8분 3초, 기준선 대비 전체·샘플별 변화 0으로 통과했다.
- v1.1 배포 가능 여부: **No-Go**. 자동화·회귀 게이트는 통과했지만 15/55(27.27%)가 여전히 오답이고, 한글 `YYYY년 MM월 DD일`과 연속 숫자 `YYYYMMDD`·`YYMMDD` 실사진이 표본에 없다.

**(2026-07-22 기준)** 이 QA 작업 중 Google Play·원스토어 제출은 수행하지 않았다. 기존 원스토어 v1.0 공개 상태는 유지하며 v1.1 후보는 제출하지 않았다. 문서상 프로젝트만 Archived로 종료하고 GitHub 저장소 Archive 설정이나 스토어 상태는 변경하지 않았다.

> 이후 2026-08-12에 v1.0.2를 원스토어에 배포해 위 `Archived` 판정은 더 이상 현재 상태가 아니다.
> 2026-08-12 당시까지 공개 배포 이력은 v1.0 → v1.0.2 2건이었다. 현재 상태의 정본은 [QA_RELEASE_RECORD.md](../../QA_RELEASE_RECORD.md)다.

## 독립 한국 라벨 55장 v1.1 기준선 (2026-07-22)

- 코딩 정본/브랜치/수정 전 코드: `<로컬 저장소 루트>\Fridge-D-Day` / `codex/fridge-ocr-qa-baseline` / `5cd1402`.
- 로컬 입력: `qa-private/korean-labels-55`. 실제 사진·manifest·sample별 결과는 `.gitignore`의 `qa-private/`에만 있다.
- 기존 20장과 신규 HEIC 35장은 각각 SHA-256이 모두 고유하다. 변환본을 합친 55장도 해시가 모두 고유하고, 육안으로 서로 다른 제품·포장을 확인했다. 16×16 평균 해시의 최근접 쌍도 256비트 중 해밍 거리 54로 근접 중복 징후가 없었다.
- HEIC는 원본을 보존한 채 `heif-convert 1.23.0` 품질 95 → FFmpeg `2025-10-05` JPEG q2로 변환했다. 현재 55장은 `baseline-v1-width` 규격으로 폭을 2048px에 맞췄고, 세로 사진 33장은 긴 변이 2048px를 초과하며 최대 2730px다. 원본/변환 SHA 매핑과 도구 버전은 로컬 `conversion-manifest.csv`, `conversion-metadata.txt`에 있다.
- `IMG_2375`는 사진의 `2026.08.20`과 기대값이 일치해 메모의 누락된 일자만 교정했다. `IMG_2388`은 사진에 `유통: 2023.12.23까지`가 명확해 제공된 `2022-12-13`을 `2023-12-23`으로 교정했다.
- 누락 표기: 한글 `YYYY년 MM월 DD일`과 연속 숫자 `YYYYMMDD`·`YYMMDD` 실사진은 여전히 없다.
- 대표성 주의: 기존 `korean_012`는 설명 오버레이가 있고 `korean_017`은 여러 제품이 함께 보이는 가격표형 사진이다.

### 표본 분류

| 차원 | 분포 |
|---|---|
| 코호트 | 신규 35, 기존 20 |
| 조명 | normal 40, glare 12, dark 2, bright 1 |
| 각도 | skewed 31, upright 20, rotated_90 4 |
| 재질 | plastic_film 19, paper_carton 9, pet_bottle 8, plastic_bottle 5, plastic_tub 3, metal_can/plastic_cap/plastic_cup/plastic_tray 각 2, glass_bottle/paper_label/paper_label_on_plastic 각 1 |
| 날짜 형식 | yyyy.mm.dd 41, yy.mm.dd 8, mm.dd_no_year 2, yyyy_space_mm.dd 2, dd/mm/yyyy 1, yyyy_mm_dd_spaces 1 |
| 인쇄 품질 | clear_dot_matrix 16, clear_label_print 15, clear_inkjet 12, embossed_low_contrast 4, low_contrast 4, faded_ink 3, broken_inkjet 1 |

### 수정 전후 결과

아래 정확 일치율은 각 정답을 평가일 30일 후에 두는 D-30 시나리오의 조건부 수치다. 파서가 평가일과 가까운 후보를 선택하므로 실사용 촬영 시점 정확도로 일반화하지 않는다. D-180 민감도에서는 정확 일치가 1장 감소했다. 실제 촬영일이 없는 과거 사진이므로 두 값 모두 시나리오 측정값이다.

| 지표 | 수정 전 | 수정 후 | 변화 |
|---|---:|---:|---:|
| 정확 일치율 | 37/55 (67.27%) | 40/55 (72.73%) | +3장, +5.46%p |
| 아무 후보 탐지율 | 54/55 (98.18%) | 54/55 (98.18%) | 0 |
| 정답 후보 재현율 | 미기록 | 43/55 (78.18%) | 새 스키마에서 측정 |
| `wrong_date` | 14 | 11 | -3 |
| `candidate_out_of_window` | 3 | 3 | 0 |
| `no_text` | 1 | 1 | 0 |
| 기존 20장 실패율 | 7/20 (35%) | 6/20 (30%) | -1장 |
| 신규 35장 실패율 | 11/35 (31.43%) | 9/35 (25.71%) | -2장 |

환경별 실패율은 작은 그룹을 일반화하지 않고 관측값으로만 사용한다.

| 환경 | 표본 | 수정 전 실패율 | 수정 후 실패율 |
|---|---:|---:|---:|
| 조명-normal | 40 | 10/40 (25%) | 8/40 (20%) |
| 조명-glare | 12 | 6/12 (50%) | 5/12 (41.67%) |
| 조명-dark | 2 | 2/2 (100%) | 2/2 (100%) |
| 조명-bright | 1 | 0/1 (0%) | 0/1 (0%) |
| 각도-skewed | 31 | 11/31 (35.48%) | 8/31 (25.81%) |
| 각도-upright | 20 | 6/20 (30%) | 6/20 (30%) |
| 각도-rotated_90 | 4 | 1/4 (25%) | 1/4 (25%) |
| 재질-plastic_film | 19 | 8/19 (42.11%) | 7/19 (36.84%) |
| 재질-paper_carton | 9 | 5/9 (55.56%) | 5/9 (55.56%) |
| 재질-pet_bottle | 8 | 0/8 (0%) | 0/8 (0%) |
| 재질-plastic_bottle | 5 | 3/5 (60%) | 2/5 (40%) |
| 재질-기타 8종 | 14 | 2/14 (14.29%) | 1/14 (7.14%) |
| 형식-yyyy.mm.dd | 41 | 13/41 (31.71%) | 10/41 (24.39%) |
| 형식-yy.mm.dd | 8 | 1/8 (12.5%) | 1/8 (12.5%) |
| 형식-mm.dd_no_year | 2 | 2/2 (100%) | 2/2 (100%) |
| 형식-yyyy_space_mm.dd | 2 | 1/2 (50%) | 1/2 (50%) |
| 형식-dd/mm/yyyy | 1 | 1/1 (100%) | 1/1 (100%) |
| 형식-yyyy_mm_dd_spaces | 1 | 0/1 (0%) | 0/1 (0%) |
| 인쇄-clear_dot_matrix | 16 | 8/16 (50%) | 8/16 (50%) |
| 인쇄-clear_label_print | 15 | 3/15 (20%) | 2/15 (13.33%) |
| 인쇄-clear_inkjet | 12 | 1/12 (8.33%) | 0/12 (0%) |
| 인쇄-embossed_low_contrast | 4 | 3/4 (75%) | 3/4 (75%) |
| 인쇄-low_contrast | 4 | 2/4 (50%) | 2/4 (50%) |
| 인쇄-faded_ink | 3 | 1/3 (33.33%) | 0/3 (0%) |
| 인쇄-broken_inkjet | 1 | 0/1 (0%) | 0/1 (0%) |

실패 상위 3개는 수정 전 `wrong_date` 14, `candidate_out_of_window` 3, `no_text` 1이고 수정 후 `wrong_date` 11, `candidate_out_of_window` 3, `no_text` 1이다. 가장 큰 `wrong_date`를 겨냥해 OCR 전체 숫자 결합 후보의 신뢰도를 2~3에서 1로 낮추고, 실제 연속 6/8자리 compact 날짜만 신뢰도 2~3으로 분리했다. 구분자가 있거나 실제로 연속된 날짜가 있으면 제품번호·용량·시간에서 합성된 후보보다 우선한다.

첫 수정안은 정확 일치 39/55로 개선됐지만 당시 v1 게이트의 아무 후보 탐지가 54→51로 하락해 기각됐다. 이후 리뷰에서 이 지표가 오답 후보 유지에 보상을 줄 수 있음을 확인했다. v2 게이트는 아무 후보 탐지를 진단값으로만 남기고, 샘플별 정확 일치 퇴행과 정답 후보 재현 퇴행을 차단한다. 저신뢰 fallback을 유지한 최종 파서의 D-30 결과는 정확 일치 40/55, 정답 후보 재현 43/55, `wrong_date` 11건이다.

| 로컬 원시 결과 | SHA-256 |
|---|---|
| `qa-private/results/korean-labels-55-baseline.csv` | `f3da66618c7703cd9d12308d357800fdf7fe5e0e38fb412ca5a26a9e4d087894` |
| `qa-private/results/korean-labels-55-after.csv` (기각안) | `edceb8feddac706a79e1551dffc9ba597e77d9adf3ae181898fdc10d308a7af7` |
| `qa-private/results/korean-labels-55-after-final.csv` | `7f0e3babf757193eed1801236e8ed60ec29587abfd77c0376eb194e8a78cc210` |
| `qa-private/results/korean-labels-55-v2-d30.csv` | `e7d2639b80fecc171a70691107b02c719b620fee1bc3ff52383b7254cb3fb9ff` |
| `qa-private/results/korean-labels-55-v2-d180.csv` | `d896caa5702aaa559f1ba377131b1ebeca2e29fb86f85419931dfdc6253ff2f9` |
| `qa-private/results/korean-labels-55-v2-d30-regression.csv` | `e7d2639b80fecc171a70691107b02c719b620fee1bc3ff52383b7254cb3fb9ff` |
| `qa-private/results/korean-labels-55-v3-guard-regression-d30.csv` | `e7d2639b80fecc171a70691107b02c719b620fee1bc3ff52383b7254cb3fb9ff` |
| `qa-private/results/korean-labels-55-v3-guard-regression-d180.csv` | `d896caa5702aaa559f1ba377131b1ebeca2e29fb86f85419931dfdc6253ff2f9` |

측정 환경은 `Medium_Phone_API_36.0` AVD(Android 16), `sdk_gphone64_x86_64`, 앱 `1.0.0-debug`다. 역사 기준선 instrumentation은 9분 17초, 최종 파서 회귀는 9분 1초였고, v2 스키마 D-30·D-180 측정은 각각 9분 3초·8분 33초였다. 새 스키마 D-30 동일 표본 회귀는 8분 26초에 통과했고 기준선과 결과 파일 해시까지 같았다.

러너는 새 기준선에서 다음을 추가로 보장한다.

- 파일 SHA-256과 `independence_key` 중복을 모두 거부한다.
- 50장 이상, `existing_20=20`·`new_30_plus>=30`, 실제 재질, 인쇄 품질 분류를 릴리스 입력 조건으로 검사한다.
- 결과 CSV에 이미지 SHA-256을 남기고 수정 전후 사진·정답·분류·평가일이 모두 같은지 비교한다.
- 실행 시작 이후 새로 생성된 CSV만 결과로 인정한다.
- 기준선과 새 결과가 같은 경로면 instrumentation 전에 거부하고, 기존 결과 파일은 `-OverwriteResult`를 명시하지 않으면 덮어쓰지 않는다.
- 아무 후보 탐지와 정답 후보 재현을 분리하고 OCR 실패 변형 수를 기록한다.
- 전체 합계뿐 아니라 샘플별 정확 일치와 정답 후보의 퇴행도 거부한다.
- 실패를 `ocr_error`, `no_text`, `no_date_candidate`, `candidate_out_of_window`, `wrong_date`로 분리하고 상위 3개를 출력한다.
- 아무 후보 탐지는 파서가 만든 날짜 후보가 한 개 이상인 경우다. 오답 후보도 포함하므로 품질 게이트로 쓰지 않는다. 정답 후보 재현은 `expected_date`가 후보 날짜 집합에 실제 포함된 경우다.

## 측정 데이터셋

- 출처: Mendeley Data, [Food Packaging OCR Dataset v2](https://data.mendeley.com/datasets/3cpx2fmn3r/2), DOI `10.17632/3cpx2fmn3r.2`
- 라이선스: CC BY 4.0
- 원본 ZIP: 2,561,540,317 bytes
- 원본 SHA-256: `74b288e6da78c29b31812e3e23ff3fe19b634a3b0e659dea8b86b8de0a04e5fd`
- 선정 방식: 공개 detection split의 실제 포장 사진 중 날짜 표기와 정답을 육안으로 확인할 수 있는 61장
- 표본 분포: 조명 `normal` 55, `dark` 4, `bright` 2 / 방향 `upright` 58, `skewed` 2, `upside_down` 1
- 재질: 원 데이터가 재질 정답을 제공하지 않아 추측하지 않고 전부 `unspecified_public_dataset`으로 기록
- 평가 날짜: 과거 라벨을 현재 날짜 필터로 버리지 않도록 각 정답 날짜 30일 전으로 고정. 실제 앱의 현재 날짜 동작을 바꾼 것이 아니라 과거 표본 재현을 위한 테스트 입력이다.

원본 ZIP, 추출 파일, 선정 사진, manifest, sample별 원시 결과는 모두 `qa-private/`에만 있으며 Git에 커밋하지 않는다.

## 실제 측정 결과

| 지표 | 수정 전 | 수정 후 | 변화 |
|---|---:|---:|---:|
| 정확 일치율 | 3/61 (4.92%) | 23/61 (37.70%) | +20장, +32.78%p |
| 후보 탐지율 | 45/61 (73.77%) | 48/61 (78.69%) | +3장, +4.92%p |
| `wrong_date` | 42 | 25 | -17 |
| `no_candidate` | 16 | 13 | -3 |

환경별 실패율은 다음과 같다. 작은 환경 그룹은 일반화하지 않고 관측값으로만 기록한다.

| 환경 | 표본 | 수정 전 실패율 | 수정 후 실패율 |
|---|---:|---:|---:|
| 조명-normal | 55 | 52/55 (94.55%) | 34/55 (61.82%) |
| 조명-dark | 4 | 4/4 (100%) | 3/4 (75%) |
| 조명-bright | 2 | 2/2 (100%) | 1/2 (50%) |
| 방향-upright | 58 | 55/58 (94.83%) | 35/58 (60.34%) |
| 방향-skewed | 2 | 2/2 (100%) | 2/2 (100%) |
| 방향-upside_down | 1 | 1/1 (100%) | 1/1 (100%) |
| 재질-unspecified | 61 | 58/61 (95.08%) | 38/61 (62.30%) |

가장 큰 실패 유형 `wrong_date` 안에서 일 우선 숫자 날짜가 연 우선으로 오해되는 원인 하나만 수정했다. 개선된 20장은 `dd/mm/yyyy` 15장, `dd/mm/yy` 3장, `dd.mm.yy` 2장이며 기존 정확 일치 3장은 모두 유지됐다. 월 이름 날짜, 기울어진 사진, OCR 자체 미탐지는 이번 변경 범위에서 제외했다.

### 한국 라벨 보완 세트 20장

사용자가 제공한 JPG 18장과 WebP 2장을 육안 검수했다. 날짜 종류는 포장에 `유통기한`이라고 명시된 경우 `유통기한`, `소비기한`이라고 명시되거나 사용자가 해당 종류로 확인한 경우 `소비기한`으로 보존했다. `korean_019`는 이미지에 `2024 12 27`이 명확해 제공된 표기 원문의 `2024 12 17`만 정정했다. 사진·정답 원문·원시 결과는 `qa-private/korean-labels`와 `qa-private/results`에만 있다.

| 지표 | 한국 20장 결과 |
|---|---:|
| 정확 일치율 | 13/20 (65%) |
| 후보 탐지율 | 18/20 (90%) |
| `wrong_date` | 5 |
| `no_candidate` | 2 |

| 환경 | 표본 | 실패율 |
|---|---:|---:|
| 조명-normal | 14 | 3/14 (21.43%) |
| 조명-glare | 4 | 3/4 (75%) |
| 조명-bright | 1 | 0/1 (0%) |
| 조명-dark | 1 | 1/1 (100%) |
| 방향-upright | 10 | 4/10 (40%) |
| 방향-skewed | 8 | 3/8 (37.50%) |
| 방향-rotated_90 | 2 | 0/2 (0%) |

실패 sample_id는 `korean_001`, `008`, `009`, `010`, `011`, `012`, `020`이다. 이 중 연도 없는 `MM.DD`는 2/2 실패했고 반사 조명은 3/4 실패했다. 그룹이 매우 작으므로 원인 확정이나 일반화에는 사용하지 않는다. 이번 작업은 공개 61장에서 가장 큰 실패 원인 하나만 수정한다는 범위를 이미 완료했으므로 한국 세트 결과를 근거로 추가 파서 수정은 하지 않았다.

## 개인정보와 Git 경계

실제 사진, 정답 manifest, 원시 측정 CSV는 모두 저장소 루트의 `qa-private/` 아래에만 둔다. 이 경로 전체는 `.gitignore`에 등록되어 있다. `docs/qa/manifest.example.csv`와 `docs/qa/ocr-benchmark.csv`는 열 구조만 보여 주는 빈 예시이며 실제 측정 데이터가 아니다.

실행 전후 다음 명령으로 사진이나 실측 CSV가 Git 대상이 아닌지 확인한다.

```powershell
git status --short --ignored qa-private
git check-ignore -v qa-private/ocr-benchmark/images/sample_001.jpg
```

## 로컬 데이터 준비

```text
qa-private/
├─ ocr-benchmark/
│  ├─ manifest.csv
│  └─ images/
│     ├─ sample_001.jpg
│     └─ ...
└─ results/
   ├─ baseline-public-v2.csv
   └─ after-day-first-parser.csv
```

manifest를 만들고 사진 파일을 배치한다.

```powershell
New-Item -ItemType Directory -Force qa-private/ocr-benchmark/images
Copy-Item docs/qa/manifest.example.csv qa-private/ocr-benchmark/manifest.csv
```

manifest 열은 다음과 같다. 쉼표가 들어간 값은 허용하지 않는다.

| 열 | 의미 |
|---|---|
| `sample_id` | 사진과 결과를 연결하는 비식별 ID. 영문·숫자·`_`·`-`만 사용 |
| `image_file` | `qa-private/ocr-benchmark` 기준 상대 경로 |
| `expected_date` | 사람이 라벨을 확인한 정답, `YYYY-MM-DD` |
| `lighting` | `bright`, `dark`, `glare` 등 촬영 조명 |
| `orientation` | `upright`, `rotated_90`, `skewed` 등 라벨 방향 |
| `material` | `paper`, `plastic`, `can`, `cap` 등 재질 |
| `date_format` | 실제 인쇄 형식: `yyyy.mm.dd`, `yy.mm.dd`, `mm.dd`, `compact` 등 |
| `print_quality` | `clear`, `low_contrast`, `faded`, `broken_inkjet`, `smudged`, `embossed` 등 인쇄 상태 |
| `independence_key` | 같은 실제 제품·포장을 묶는 비식별 키. 릴리스 표본에서는 사진마다 고유해야 함 |
| `cohort` | 기존 표본은 `existing_20`, 새 독립 표본은 `new_30_plus` |
| `base_rotation` | 비트맵을 바로 세우는 회전값: `0`, `90`, `180`, `270` |
| `evaluation_date` | 연도 없는 날짜 추론을 고정할 평가 기준일, `YYYY-MM-DD` |

같은 사진은 같은 `sample_id`, 정답, 환경 라벨, `evaluation_date`를 유지한다. 실패 사진도 삭제하지 않는다.

새 사진을 받은 직후 기기 없이 입력만 검증한다.

```powershell
.\scripts\run-ocr-benchmark.ps1 `
  -DatasetDir qa-private/korean-labels-55 `
  -ReleaseBaseline `
  -ValidateOnly
```

HEIC 원본을 보존하면서 현재 55장의 파생 JPEG를 재현한다. `baseline-v1-width`는 폭 2048px인 기존 기준선 규격이므로 이름과 출력 특성을 그대로 보존한다. `heif-convert`와 `ffmpeg`가 PATH에 있어야 한다.

```powershell
.\scripts\convert-heic-ocr-dataset.ps1 `
  -SourceDir qa-private/korean-lables-new/images `
  -OutputDir qa-private/korean-labels-55/images `
  -MaxLongEdge 2048 `
  -JpegQuality 2 `
  -ConversionProfile baseline-v1-width
```

새 데이터셋은 업스케일 없이 실제 긴 변을 제한하는 v2 규격을 쓴다. 기존 55장에 이 명령을 덮어쓰면 이미지 해시가 바뀌므로 별도 출력 경로와 새 기준선을 사용한다.

```powershell
.\scripts\convert-heic-ocr-dataset.ps1 `
  -SourceDir qa-private/korean-lables-new/images `
  -OutputDir qa-private/korean-labels-v2/images `
  -MaxLongEdge 2048 `
  -JpegQuality 2 `
  -ConversionProfile max-long-edge-v2
```

## 실행 환경과 명령

요구사항은 JDK 17, Android SDK 35, 정확히 1대의 연결된 Android 기기 또는 에뮬레이터다. 2026-07-22의 55장 측정은 `Medium_Phone_API_36.0` AVD(Android 16), 기기 모델 `sdk_gphone64_x86_64`, 앱 `1.0.0-debug`에서 수행했다. Windows 한글 상위 경로에서 Gradle 8.2 테스트 클래스패스가 깨지는 문제를 피하기 위해 스크립트가 저장소를 임시 ASCII 드라이브로 매핑하고 종료 시 해제한다.

일반 Android 품질 게이트:

```powershell
.\scripts\android-quality.ps1 `
  -Tasks @("testDebugUnitTest","lintDebug","assembleDebug","assembleRelease","assembleDebugAndroidTest")
```

역사 기준선 측정 명령은 아래와 같다. 이 결과는 레거시 스키마이므로 현재 러너의 회귀 비교 입력으로 다시 사용하지 않는다.

```powershell
.\scripts\run-ocr-benchmark.ps1 `
  -RunName korean-labels-55-baseline `
  -DatasetDir qa-private/korean-labels-55 `
  -ReleaseBaseline
```

현재 파서의 D-30 새 스키마 기준선과 D-180 민감도를 측정한다.

```powershell
.\scripts\run-ocr-benchmark.ps1 `
  -RunName korean-labels-55-v2-d30 `
  -DatasetDir qa-private/korean-labels-55 `
  -ReleaseBaseline `
  -EvaluationOffsetDays 30

.\scripts\run-ocr-benchmark.ps1 `
  -RunName korean-labels-55-v2-d180 `
  -DatasetDir qa-private/korean-labels-55 `
  -ReleaseBaseline `
  -EvaluationOffsetDays 180
```

한국 보완 세트 측정:

```powershell
.\scripts\run-ocr-benchmark.ps1 `
  -RunName korean-labels-20 `
  -DatasetDir qa-private/korean-labels
```

현재 스키마에서 가장 큰 실패 유형 하나만 수정한 뒤 D-30·D-180 동일 표본을 한 명령으로 회귀한다. 대상 실패 유형의 개선은 D-30에서 요구하고, 두 시나리오 모두 샘플별 정확 일치·정답 후보와 전체 합계가 하락하면 실패한다.

```powershell
.\scripts\run-ocr-release-regression.ps1 `
  -RunNamePrefix korean-labels-55-v2-after-next-fix `
  -DatasetDir qa-private/korean-labels-55 `
  -D30BaselineCsv qa-private/results/korean-labels-55-v2-d30.csv `
  -D180BaselineCsv qa-private/results/korean-labels-55-v2-d180.csv `
  -TargetFailureType wrong_date
```

결과 회수 경로만 빠르게 확인할 때는 성과 측정에 포함하지 않는 스모크 옵션을 쓴다.

```powershell
.\scripts\run-ocr-benchmark.ps1 -RunName smoke-output-path -SampleLimit 1
```

러너는 manifest·사진 존재 여부, `sample_id`·파일 경로·SHA-256·독립성 키 중복, 연결 기기 수를 먼저 검사한다. 앱의 원본/90도·반전/반전 90도 OCR 경로를 실행하고 `qa-private/results/<run-name>.csv`를 만든다. 회귀 비교는 표본 수와 `sample_id`뿐 아니라 사진 해시·정답·모든 환경 분류·평가일·평가 시나리오까지 같아야 통과한다. 전체 정확 일치와 정답 후보 재현이 하락하거나 기존 정답/정답 후보 샘플 하나라도 퇴행하면 실패한다. `-TargetFailureType`은 수정 전 최상위 유형이어야 하고 건수가 실제로 줄어야 통과한다. 아무 후보 탐지는 진단값으로만 출력한다.

`EvaluationOffsetDays`는 0(기존 manifest 평가일 사용) 또는 1~1825일만 허용한다. instrumentation 인자가 숫자가 아니거나 `Long` 범위를 넘으면 manifest 시나리오로 조용히 대체하지 않고 명시적으로 실패한다. 같은 `RunName`의 결과가 이미 있으면 새 이름을 사용한다. 회귀 비교에 `-OverwriteResult`를 함께 지정하면 거부하며, 이 옵션은 기준선과 비교하지 않는 폐기 가능한 측정에만 사용한다. `run-ocr-benchmark.ps1`을 직접 호출한 단일 시나리오 릴리스 회귀는 불완전하다는 경고를 출력한다. `run-ocr-release-regression.ps1`은 D-30과 D-180을 순서대로 실행해 한쪽만 통과한 변경을 릴리스 회귀 성공으로 인정하지 않는다.

기준선 보호 음성 검증에서는 `RunName`이 기준선 파일명과 같은 경우, 기존 결과명이 재사용된 경우, 회귀 비교에 `-OverwriteResult`를 지정한 경우, 평가 offset이 1825일을 초과한 경우가 모두 instrumentation 전에 거부됐다. 단일 시나리오 직접 릴리스 회귀 경고도 확인했고, 20자리 offset은 instrumentation에서 `evaluationOffsetDays is too large`로 명시적으로 실패했다. 검증 전후 D-30 기준선 SHA-256은 `e7d2639b80fecc171a70691107b02c719b620fee1bc3ff52383b7254cb3fb9ff`로 동일했다.

## 지표와 실패 분류

- 정확 일치율 = `predicted_date == expected_date`인 사진 / 전체 사진
- 아무 후보 탐지율 = 정답 여부와 무관하게 날짜 후보를 하나 이상 반환한 사진 / 전체 사진
- 정답 후보 재현율 = `expected_date`가 OCR·파서 후보 날짜 집합에 포함된 사진 / 전체 사진
- OCR 실패 변형 수 = 원본·90도·반전·반전 90도 중 인식 예외가 발생한 총 변형 수
- 환경별 실패율 = 해당 조명·방향·재질·표기 그룹에서 정확 일치하지 않은 사진 / 해당 그룹 전체
- `ocr_error`: 네 가지 OCR 변형이 모두 처리 오류로 끝남
- `no_text`: OCR 처리는 성공했지만 인식 문자열이 없음
- `no_date_candidate`: 문자는 인식했지만 날짜 파서 후보가 없음
- `candidate_out_of_window`: 날짜 후보는 있으나 평가일 허용 범위를 통과한 후보가 없음
- `wrong_date`: 후보를 선택했지만 최종 날짜가 정답과 다름

2026-07-14의 공개 61장·한국 20장 역사 결과와 초기 한국 55장 결과는 종전 스키마를 사용한다. 현재 러너는 정답 후보·실패 변형·평가 시나리오 열이 없는 레거시 CSV를 회귀 입력으로 거부하며, `korean-labels-55-v2-d30.csv`부터 동일 스키마끼리만 비교한다.

표본이 50장 미만이면 스크립트가 결과를 표시하되 v1.1 릴리스 기준선으로 인정하지 않는 경고를 낸다. 50장 이상이면 정확 일치율, 아무 후보 탐지율, 정답 후보 재현율, OCR 실패 변형 수, 실패 유형 및 조명·방향·재질·표기별 실패율을 모두 출력한다.

## 수정과 배포 판정 규칙

1. 50장 이상의 기준선을 먼저 측정한다.
2. 실패 건수를 `failure_type`으로 집계해 가장 큰 유형 하나를 선택한다.
3. 그 유형만 겨냥한 최소 변경을 한다. 측정 전에는 파서 개선을 추측해 적용하지 않는다.
4. 같은 기기·같은 사진·같은 manifest로 D-30과 D-180을 모두 다시 실행한다.
5. 두 시나리오에서 전체 정확 일치·정답 후보 재현이 하락하지 않고 개별 정답·정답 후보 퇴행이 없으며, D-30에서 대상 실패 유형이 개선됐는지 CSV 차이로 확인한다.
6. 단위 테스트·lint·debug 빌드가 모두 통과하고 목표 사용자 라벨의 대표성이 확보돼야 v1.1 배포 가능으로 판정한다.

실제 전후 수치는 이 문서의 현재 상태에 기록하되, 사진·sample별 정답·원시 결과는 계속 `qa-private/`에만 둔다. Google Play 또는 원스토어 제출은 이 절차에 포함하지 않는다.

## 남은 리스크와 종료 판단

- 공개 61장은 50장 판정선을 충족하지만 영어권·일 우선 표기가 많아 한국 유통기한 라벨을 대표하지 않는다.
- 한국 55장은 독립 릴리스 기준선이지만 정확 일치가 72.73%여서 자동 입력 오답 15장을 사용자에게 그대로 노출할 위험이 남는다.
- 72.73%는 고정 55장의 D-30 조건부 결과다. D-180은 70.91%였고 두 조건 모두 실제 촬영일을 대신하지 못한다. 2026-09-09 별도 소규모 coverage는 실제 촬영일을 사용했지만 표본 규모·형식·제품군이 제한되므로 55장 수치를 실사용 정확도로 일반화하지 않는다.
- 동일 confidence 후보 중 평가일과 가장 가까운 날짜를 선택하므로 제조일자와 소비기한이 함께 인식되면 제조일자를 고를 수 있다. 이 위험은 알려진 한계로 남긴다.
- 고정 55장에는 한글 `YYYY년 MM월 DD일`, 연속 숫자 `YYYYMMDD`·`YYMMDD`가 없다. 별도 2026-09-09 coverage에서 한글 식품 1장을 측정했고, 연속 숫자 식품 라벨은 추가 탐색에서도 미관측이었다. 비식품 `YYYYMMDD` 보조 5장 결과는 식품 품질 추정에 사용하지 않는다.
- 잔여 실패는 clear_dot_matrix 8/16, embossed_low_contrast 3/4, dark 2/2, 연도 없는 `MM.DD` 2/2에 집중된다. 작은 그룹의 관측값은 원인이나 전체 성능으로 일반화하지 않는다.
- `korean_012`의 설명 오버레이와 `korean_017`의 가격표형 다중 제품 구도는 실제 카메라 촬영 대표성의 한계다.
- 사진·정답·원시 결과는 계속 `qa-private/`에만 저장하고 공개 문서에는 집계만 남긴다.
- **2026-07-22 당시** 위 한계 때문에 v1.1을 Release No-Go로 판정했고, 그 시점에는 보완 실사진 수집·추가 파서 변경·재판정을 계획하지 않았다. 2026-09-09부터 별도 QA hardening 사이클이 재개됐으며 현재 계획은 문서 상단의 2026-09-09 상태 주를 따른다.
  _(2026-08-15 주: 이후 인식 정확도를 올리는 대신 저장 전 사용자 확인 단계를 넣어 위험 경로를 제거하고 v1.0.2를 원스토어에 배포했다. 위 문장의 "원스토어 업데이트 없음"은 v1.1 후보에 대한 판단이다.)_
