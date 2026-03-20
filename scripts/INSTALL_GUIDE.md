# MimicEase — Installation Guide / 설치 가이드

---

## Files in This Package / 이 폴더의 파일

| File / 파일 | Description / 설명 |
|---|---|
| **`install.bat`** | ▶ **Run this first / 이것을 먼저 실행하세요** |
| `install.ps1` | Installer script — runs automatically via `install.bat` (do not open directly) |
| `MimicEase-v*.apk` | Android app package |
| `adb/` | Portable ADB tools (no installation required) |

> ⚠️ **Do NOT double-click `install.ps1`** — it will open in Notepad, not run the installer.
> ⚠️ **`install.ps1`를 더블클릭하지 마세요** — 메모장에서 열립니다. 반드시 `install.bat`을 사용하세요.

---

## Quick Start / 빠른 시작

**Step 1 / 1단계** — Double-click `install.bat`
**Step 2 / 2단계** — Select your language (Enter = auto-detected default)
**Step 3 / 3단계** — Follow the on-screen instructions

That's it. The installer guides you through every step.
이게 전부입니다. 나머지는 설치 프로그램이 화면에서 안내합니다.

---

## Prerequisites / 사전 준비

### Windows PC
- Windows 10 or later / Windows 10 이상
- No installation required / 별도 설치 불필요

### Android Device / Android 기기

| Connection / 연결 방식 | Requirement / 조건 |
|---|---|
| USB cable / USB 케이블 | Any Android version + USB debugging enabled |
| Wi-Fi wireless / Wi-Fi 무선 | Android 11 or later + Wireless Debugging enabled |

---

## Detailed Steps / 상세 단계

### [1] Enable Developer Options / 개발자 옵션 활성화

1. Open **Settings** on your phone / 스마트폰에서 **설정** 열기
2. Go to **About phone** → **Software information** / **휴대전화 정보** → **소프트웨어 정보**
3. Tap **Build number** 7 times / **빌드 번호**를 7번 탭
4. Enter PIN/password when prompted / PIN/비밀번호 입력

> Samsung path / 삼성 경로: `Settings > About phone > Software information > Build number`
> Google Pixel: `Settings > About phone > Build number`

---

### [2] Choose Connection Method / 연결 방식 선택

**Option A: USB Cable (Recommended)**
**옵션 A: USB 케이블 (권장)**

1. Connect your phone to PC with USB / USB로 폰 연결
2. Tap **Allow** on the "Trust this computer?" prompt / "이 컴퓨터를 신뢰합니까?" 팝업에서 **허용** 탭
3. Enable **USB Debugging** in Developer Options / 개발자 옵션에서 **USB 디버깅** 활성화
4. Tap **Allow** on the "Allow USB Debugging?" prompt / "USB 디버깅 허용?" 팝업에서 **허용** 탭

---

**Option B: Wi-Fi (Android 11+)**
**옵션 B: Wi-Fi (Android 11 이상)**

1. Enable **Wireless Debugging** in Developer Options / 개발자 옵션에서 **무선 디버깅** 활성화
2. Tap **Wireless Debugging** label → **Pair device with pairing code**
   **무선 디버깅** 이름 탭 → **페어링 코드로 기기 페어링**
3. Enter the **IP:port** and **6-digit code** shown on screen
   화면에 표시된 **IP:포트**와 **6자리 코드** 입력
4. Return to the Wireless Debugging main screen for the **connection IP:port**
   무선 디버깅 메인 화면의 **연결 IP:포트** 입력

> ⚠️ The pairing port and connection port are **different numbers**.
> ⚠️ 페어링 포트와 연결 포트는 **서로 다른 번호**입니다.

---

### [3] APK Installation / APK 설치

The installer runs `adb install` automatically.
설치 프로그램이 자동으로 `adb install`을 실행합니다.

If an installation prompt appears on your phone, tap **Allow** / **Install**.
폰 화면에 설치 허가 팝업이 나타나면 **허용** / **설치**를 탭하세요.

---

### [4] First Launch / 앱 첫 실행

1. Open **MimicEase** on your phone / 폰에서 **MimicEase** 열기
2. Follow the onboarding to grant camera permission / 온보딩에서 카메라 권한 허용
3. Go to **Settings → Accessibility → MimicEase** and enable the service
   **설정 → 접근성 → MimicEase**에서 서비스 활성화

---

## Troubleshooting / 문제 해결

| Problem / 문제 | Solution / 해결 방법 |
|---|---|
| `install.bat` closes immediately with a red error | Re-extract the ZIP completely and try again / ZIP을 완전히 재압축 해제 후 재시도 |
| No device detected (timeout) / 기기 미감지 | Check USB cable supports data transfer; try another port / 데이터 전송 지원 케이블 사용, 다른 포트 시도 |
| Device "unauthorized" / 기기 미승인 | Tap **Allow** on the USB debugging popup on your phone screen / 폰 화면에서 USB 디버깅 허용 탭 |
| Wi-Fi pairing failed / Wi-Fi 페어링 실패 | Pairing codes expire — close and reopen the pairing screen / 페어링 코드 만료, 화면 닫고 다시 열기 |
| `INSTALL_FAILED_USER_RESTRICTION` | Enable **Install unknown apps** in Android Security Settings / 안드로이드 보안 설정에서 알 수 없는 출처 허용 |
| `INSTALL_FAILED_ALREADY_EXISTS` | Uninstall the existing MimicEase app first / 기존 MimicEase 앱 먼저 삭제 |

---

## Support / 지원

GitHub Issues: <https://github.com/CrowKing63/MimicEase/issues>
