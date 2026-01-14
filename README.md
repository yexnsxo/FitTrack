# 득근득근 🏋️‍♂️

**득근득근**은 나만의 운동 루틴을 계획하고, 타이머와 함께 수행하며, 캘린더로 꾸준함을 기록하는 안드로이드 운동 보조 앱입니다.

<br>

## ✨ 주요 기능

* **✅ 오늘의 운동 관리 (To-do)**
    * 오늘 수행할 운동 목록을 추가하고 관리할 수 있습니다.
    * 운동 종류(무게, 횟수 등)와 목표치를 설정하여 체계적인 계획을 세울 수 있습니다.
    * 오늘한 운동의 통계를 한 눈에 확인 할 수 있습니다.
<img width="810" height="576" alt="스크린샷 2026-01-14 오후 7 46 38" src="https://github.com/user-attachments/assets/7297b29c-e364-442a-8f22-51c464cdc798" />


* **⏱️ 백그라운드 운동 타이머**
    * 타이머 탭에서 세트, 휴식 시간을 관리할 수 있습니다.
    * **Foreground Service**를 활용하여 앱이 백그라운드에 있거나 화면이 꺼져도 타이머가 중단되지 않고 알림을 통해 현재 상태를 확인할 수 있습니다.

<img width="822" height="565" alt="스크린샷 2026-01-14 오후 7 46 51" src="https://github.com/user-attachments/assets/87d02aeb-3c43-436a-aeb9-a2ffde7268d0" />


* **📅 운동 기록 및 캘린더**
    * 완료된 운동은 날짜별로 기록됩니다.
    * 캘린더 뷰를 통해 언제 어떤 운동을 했는지 한눈에 파악하고 수정할 수 있습니다.
    * 전체 사진 탭을 통해 사진을 한 눈에 볼 수 있습니다.
 
<img width="807" height="523" alt="스크린샷 2026-01-14 오후 7 47 08" src="https://github.com/user-attachments/assets/fb52e02f-b014-4c3c-9fb5-5b01d3e1eead" />


본 앱은 각 탭이 유기적으로 데이터를 주고받으며 사용자에게 끊김 없는 운동 경험을 제공합니다.

<img width="807" height="447" alt="스크린샷 2026-01-14 오후 7 47 30" src="https://github.com/user-attachments/assets/7f19608a-d68c-40e9-8b2c-dcc581434a22" />

### 1️⃣ Todo ↔ Timer: 실시간 운동 수행
* **운동 정보 전달**: Todo 탭에서 설정한 운동의 **세트 정보와 무게**를 타이머 탭으로 즉시 가져와 운동을 시작할 수 있습니다.
* **결과 자동 기록**: 타이머 탭에서 운동을 종료하면, 실제로 수행한 **세트 정보와 총 소요 시간**이 Todo 탭의 해당 항목에 자동으로 기록됩니다.

### 2️⃣ Todo ↔ Record: 데이터 보존 및 조회
* **활동 저장**: Todo 탭에서 완료 처리된 오늘의 모든 운동 내역은 Record 탭의 데이터베이스로 전송됩니다.
* **히스토리 추적**: Record 탭(캘린더)에서 특정 날짜를 선택하면, 그날 Todo 탭에서 수행했던 **상세 운동 기록을 다시 불러와** 확인할 수 있습니다.

<br>

## 🛠 기술 스택

| 분류 | 기술 |
| --- | --- |
| **Language** | kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Asynchronous** | Kotlin Coroutines |
| **Navigation** | Navigation Compose |
| **Local DB** | Room |
| **Background** | Foreground Service |
| **DI** | ViewModelFactory를 사용한 수동 의존성 주입 |

## 💪 팀원
| &nbsp;&nbsp;이&nbsp;름&nbsp;&nbsp; |  학교 |                                    역할분담                         |
| :--------------------------------------: | :--------------------: | :-------------------------------------------------: |
| 양현민 | 고려대학교 컴퓨터학과 | 기획, <br/> Record 탭, Timer 탭, <br/> Todo <-> Record DB 연동 |
| 이연서 | 숙명여자대학교 컴퓨터과학 전공 | 전체적인 UI/UX 디자인,<br/> Todo 탭,<br/> Todo <-> Timer DB 연동 |


### 📚 Libraries
- `Coil`: 이미지 로딩
- `Kizitonwose Calendar`: 캘린더 UI 구현
- `Kotlinx Serialization`: 데이터 직렬화

--
