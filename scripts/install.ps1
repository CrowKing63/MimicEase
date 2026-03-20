# ============================================================
#  MimicEase Installer (PowerShell)
#  Installs MimicEase APK via ADB (USB or Wi-Fi).
#  No internet connection or admin rights required.
#  Supports: English, 한국어, 日本語, 简体中文, 繁體中文,
#            Español, Français, Deutsch, Português
# ============================================================

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AdbPath   = Join-Path $ScriptDir "adb\adb.exe"
$ApkFile   = Get-ChildItem -Path $ScriptDir -Filter "MimicEase-*.apk" -ErrorAction SilentlyContinue |
             Where-Object { $_.Name -notmatch "debug" } |
             Select-Object -First 1

# ─── Multilingual Strings ───────────────────────────────────

$AllStrings = @{

  "en" = @{
    LangName         = "English"
    SelectLang       = "Select language / 언어 선택 / 言語選択 / 选择语言 / 選擇語言 / Idioma / Langue / Sprache / Idioma"
    LangDetected     = "Detected language"
    LangPrompt       = "Enter number to change, or press Enter to continue"
    LangOptions      = @("English","한국어","日本語","简体中文","繁體中文","Español","Français","Deutsch","Português")

    AppTitle         = "MimicEase Installer"
    AppSubtitle      = "Control your phone with facial expressions"
    AppDesc          = "This installer uses ADB (Android Debug Bridge) to install MimicEase without an internet connection."
    PressEnter       = "Press Enter to continue..."
    PressEnterExit   = "Press Enter to exit..."
    Checking         = "Checking"
    Verified         = "Verified"
    Error            = "ERROR"
    Warning          = "WARNING"
    Done             = "DONE"
    Waiting          = "Waiting"

    NoAdb            = "adb.exe not found."
    NoAdbHint        = "Make sure the ZIP archive was fully extracted."
    NoApk            = "APK file not found."
    NoApkHint        = "The MimicEase-*.apk file must be in the same folder as this script."
    AdbPath          = "ADB path"
    ApkFile          = "APK file"

    Step1Title       = "Step 1: Enable Developer Options"
    Step1Desc        = "To install via ADB, Developer Options must be enabled on your Android device."
    Step1Steps       = @(
      "Open the Settings app on your phone.",
      "Go to 'About phone' or 'Device information'.",
      "Tap 'Software information' (some devices skip this step).",
      "Tap 'Build number' 7 times in a row.",
      "Enter your PIN/pattern/password. You will see 'Developer mode has been enabled'."
    )
    Step1Brands      = @(
      "Samsung: Settings > About phone > Software information > Build number",
      "Google Pixel / LG: Settings > About phone > Build number",
      "Others: Search for 'Build number' in Settings."
    )
    Step1BrandsTitle = "Path by manufacturer"
    Step1Skip        = "If Developer Options is already enabled, you can skip this step."
    Step1Done        = "Press Enter after enabling Developer Options..."

    ConnTitle        = "Choose connection method"
    ConnUsb          = "USB cable  (recommended, all Android versions)"
    ConnWifi         = "Wi-Fi wireless  (Android 11 or later required)"
    ConnPrompt       = "Enter 1 or 2"

    UsbTitle         = "Step 2 (USB): Connect your device"
    UsbSteps         = @(
      "Connect your Android device to this PC with a USB cable.",
      "If a 'Trust this computer?' prompt appears on your device, tap 'Allow'.",
      "Change the USB mode to 'File Transfer (MTP)' or 'Data transfer'."
    )
    UsbDebugTitle    = "Enable USB Debugging"
    UsbDebugSteps    = @(
      "Settings > Developer options > turn on 'USB debugging'.",
      "Tap 'OK' on the 'Allow USB debugging?' prompt."
    )
    UsbDone          = "Press Enter after completing the steps above..."
    UsbDetecting     = "Detecting device..."
    UsbFound         = "Device connected"
    UsbUnauth        = "Device detected but authorization required."
    UsbUnauthHint    = "Check your phone screen for the 'Allow USB Debugging' prompt and tap 'Allow'."
    UsbUnauthHint2   = "If no prompt appears, unplug and reconnect the USB cable."
    UsbOffline       = "Device is offline. Please check the USB cable."
    UsbTimeout       = "Could not detect a device. (60 second timeout)"
    UsbTimeoutFix    = @(
      "Make sure your USB cable supports data transfer (charge-only cables won't work).",
      "Try a different USB port.",
      "Confirm USB debugging is enabled on the device.",
      "Check that the USB driver for your phone manufacturer is installed."
    )

    WifiTitle        = "Step 2 (Wi-Fi): Wireless Debugging Setup"
    WifiReq          = "Wi-Fi installation requires Android 11 (R) or later."
    WifiReq2         = "Your device and this PC must be on the same Wi-Fi network."
    WifiEnableTitle  = "Enable Wireless Debugging"
    WifiEnableSteps  = @(
      "Settings > Developer options > turn on 'Wireless debugging'.",
      "Tap 'Allow' on the 'Allow wireless debugging on this network?' prompt."
    )
    WifiEnableDone   = "Press Enter after enabling Wireless Debugging..."
    WifiPairTitle         = "Pair device with pairing code"
    WifiAlreadyPairedQ    = "Is this PC already paired with your device? (y/N)"
    WifiAlreadyPairedSkip = "Already paired — skipping to the connection step."
    WifiPairSteps    = @(
      "Tap 'Wireless debugging' (the label, not the toggle).",
      "Tap 'Pair device with pairing code'.",
      "Note the 'IP address & Port' and the 6-digit 'Wi-Fi pairing code'."
    )
    WifiPairWarning  = "The pairing port and connection port are different numbers."
    WifiPairAddrPrompt = "Enter pairing IP:port (e.g. 192.168.1.100:37491)"
    WifiPairCodePrompt = "Enter 6-digit pairing code"
    WifiPairing      = "Pairing..."
    WifiPairOk       = "Pairing successful!"
    WifiPairFail     = "Pairing failed."
    WifiPairFixTitle = "Troubleshooting"
    WifiPairFix      = @(
      "Double-check the IP address and port number.",
      "Pairing codes expire. Close the pairing screen and open it again.",
      "Make sure both devices are on the same Wi-Fi network."
    )
    WifiConnTitle    = "Connect to device"
    WifiConnDesc     = "Now enter the 'connection IP:port' (different from the pairing port)."
    WifiConnSteps    = @(
      "Close the pairing screen and return to the Wireless Debugging main screen.",
      "Note the port number shown at the top 'IP address & Port' entry."
    )
    WifiConnAddrPrompt = "Enter connection IP:port (e.g. 192.168.1.100:41391)"
    WifiConnecting   = "Connecting..."
    WifiConnOk       = "Device connected!"
    WifiConnFail     = "Connection failed."
    WifiConnFix      = @(
      "Make sure the connection port is different from the pairing port.",
      "Check that your firewall is not blocking that port.",
      "Toggle Wireless Debugging off and on, then try again."
    )

    InstallTitle     = "Installing APK"
    InstallFile      = "File"
    InstallTarget    = "Target device"
    Installing       = "Installing, please wait..."
    InstallAllow     = "(If an installation prompt appears on your device, tap 'Allow'.)"
    InstallOk        = "Installation complete!"
    InstallFail      = "Installation failed."
    InstallErrRestriction = @(
      "Cause: Device policy blocks installation from unknown sources.",
      "Fix:   Settings > Security > Enable 'Unknown sources' or 'Install unknown apps'."
    )
    InstallErrExists = @(
      "Cause: The installed version has a different signature.",
      "Fix:   Uninstall the existing MimicEase app and try again."
    )
    InstallErrStorage = @(
      "Cause: Insufficient storage space.",
      "Fix:   Free up some space and try again."
    )
    InstallErrGeneral = @(
      "1. Restart the device and try again.",
      "2. Try a different USB cable or port.",
      "3. Report the error at GitHub Issues."
    )

    SuccessTitle     = "MimicEase has been installed successfully!"
    SuccessSteps     = @(
      "Launch the 'MimicEase' app on your phone.",
      "Follow the onboarding screen to allow camera permission.",
      "Settings > Accessibility > find MimicEase and enable the service."
    )
    SuccessHelp      = "Help"
    SuccessHelpUrl   = "GitHub Issues"
    InputEmpty       = "Input cannot be empty."
  }

  "ko" = @{
    LangName         = "한국어"
    AppTitle         = "MimicEase 설치 도우미"
    AppSubtitle      = "얼굴 표정으로 스마트폰을 제어하세요"
    AppDesc          = "이 설치 프로그램은 ADB(Android Debug Bridge)를 사용하여 인터넷 연결 없이 MimicEase를 설치합니다."
    PressEnter       = "계속하려면 Enter 키를 누르세요..."
    PressEnterExit   = "종료하려면 Enter 키를 누르세요..."
    Checking         = "확인 중"
    Verified         = "확인 완료"
    Error            = "오류"
    Warning          = "주의"
    Done             = "완료"
    Waiting          = "대기 중"

    NoAdb            = "adb.exe를 찾을 수 없습니다."
    NoAdbHint        = "ZIP 파일이 올바르게 압축 해제되었는지 확인하세요."
    NoApk            = "APK 파일을 찾을 수 없습니다."
    NoApkHint        = "MimicEase-*.apk 파일이 이 스크립트와 같은 폴더에 있어야 합니다."
    AdbPath          = "ADB 경로"
    ApkFile          = "APK 파일"

    Step1Title       = "1단계: 개발자 옵션 활성화"
    Step1Desc        = "ADB로 앱을 설치하려면 먼저 안드로이드 기기에서 개발자 옵션을 활성화해야 합니다."
    Step1Steps       = @(
      "스마트폰에서 '설정' 앱을 엽니다.",
      "'휴대전화 정보' 또는 '디바이스 정보'로 이동합니다.",
      "'소프트웨어 정보'를 탭합니다. (일부 기기는 이 단계 없음)",
      "'빌드 번호'를 7번 연속으로 탭합니다.",
      "PIN/패턴/비밀번호를 입력하면 '개발자 모드가 활성화되었습니다' 메시지가 나타납니다."
    )
    Step1Brands      = @(
      "삼성: 설정 > 휴대전화 정보 > 소프트웨어 정보 > 빌드 번호",
      "LG / 구글 픽셀: 설정 > 휴대전화 정보 > 빌드 번호",
      "기타: 설정에서 '빌드 번호'를 검색하세요."
    )
    Step1BrandsTitle = "제조사별 경로"
    Step1Skip        = "이미 개발자 옵션이 활성화되어 있다면 이 단계를 건너뛰어도 됩니다."
    Step1Done        = "개발자 옵션 활성화 후 Enter 키를 누르세요..."

    ConnTitle        = "연결 방식을 선택하세요"
    ConnUsb          = "USB 케이블 연결 (권장, 모든 Android 버전 지원)"
    ConnWifi         = "Wi-Fi 무선 연결 (Android 11 이상 필요)"
    ConnPrompt       = "1 또는 2를 입력하세요"

    UsbTitle         = "2단계 (USB): 기기 연결"
    UsbSteps         = @(
      "USB 케이블로 Android 기기를 이 컴퓨터에 연결합니다.",
      "기기 화면에 '이 컴퓨터를 신뢰하시겠습니까?' 팝업이 나타나면 '허용'을 탭합니다.",
      "연결 유형을 '파일 전송(MTP)' 또는 '데이터 전송'으로 변경합니다."
    )
    UsbDebugTitle    = "USB 디버깅 활성화"
    UsbDebugSteps    = @(
      "설정 > 개발자 옵션 > 'USB 디버깅' 스위치를 켭니다.",
      "'USB 디버깅을 허용하시겠습니까?' 팝업에서 '확인'을 탭합니다."
    )
    UsbDone          = "위 단계를 완료한 후 Enter 키를 누르세요..."
    UsbDetecting     = "기기를 감지하는 중..."
    UsbFound         = "기기 연결 확인"
    UsbUnauth        = "기기가 감지되었지만 권한 승인이 필요합니다."
    UsbUnauthHint    = "스마트폰 화면에서 'USB 디버깅 허용' 팝업을 확인하고 '허용'을 탭하세요."
    UsbUnauthHint2   = "팝업이 보이지 않으면 USB 케이블을 뺐다가 다시 연결해보세요."
    UsbOffline       = "기기가 오프라인 상태입니다. USB 케이블을 확인하세요."
    UsbTimeout       = "기기를 감지하지 못했습니다. (60초 타임아웃)"
    UsbTimeoutFix    = @(
      "USB 케이블이 데이터 전송을 지원하는지 확인하세요. (충전 전용 케이블은 사용 불가)",
      "다른 USB 포트를 사용해보세요.",
      "기기에서 USB 디버깅이 켜져 있는지 다시 확인하세요.",
      "스마트폰 제조사 USB 드라이버가 설치되어 있는지 확인하세요."
    )

    WifiTitle        = "2단계 (Wi-Fi): 무선 디버깅 설정"
    WifiReq          = "Wi-Fi 설치는 Android 11(R) 이상에서만 지원됩니다."
    WifiReq2         = "기기와 이 컴퓨터가 같은 Wi-Fi에 연결되어 있어야 합니다."
    WifiEnableTitle  = "무선 디버깅 활성화"
    WifiEnableSteps  = @(
      "설정 > 개발자 옵션 > '무선 디버깅'을 켭니다.",
      "'이 네트워크에서 무선 디버깅을 허용하시겠습니까?' 팝업에서 '허용'을 탭합니다."
    )
    WifiEnableDone   = "무선 디버깅을 켠 후 Enter 키를 누르세요..."
    WifiPairTitle         = "페어링 코드로 기기 페어링"
    WifiAlreadyPairedQ    = "이 PC와 기기가 이미 페어링되어 있나요? (y/N)"
    WifiAlreadyPairedSkip = "이미 페어링됨 — 페어링 단계를 건너뜁니다."
    WifiPairSteps    = @(
      "'무선 디버깅' 이름 부분(토글 아님)을 탭합니다.",
      "'페어링 코드로 기기 페어링'을 탭합니다.",
      "화면에 표시된 'IP 주소 및 포트'와 '6자리 페어링 코드'를 확인합니다."
    )
    WifiPairWarning  = "페어링 포트와 연결 포트는 서로 다른 번호입니다."
    WifiPairAddrPrompt = "페어링 IP:포트를 입력하세요 (예: 192.168.1.100:37491)"
    WifiPairCodePrompt = "6자리 페어링 코드를 입력하세요"
    WifiPairing      = "페어링 중..."
    WifiPairOk       = "페어링 완료!"
    WifiPairFail     = "페어링에 실패했습니다."
    WifiPairFixTitle = "해결 방법"
    WifiPairFix      = @(
      "IP 주소와 포트 번호를 다시 확인하세요.",
      "페어링 코드는 시간이 지나면 만료됩니다. 화면을 닫고 다시 열어 새 코드를 사용하세요.",
      "기기와 컴퓨터가 같은 Wi-Fi에 연결되어 있는지 확인하세요."
    )
    WifiConnTitle    = "기기에 연결"
    WifiConnDesc     = "이제 '연결용 IP:포트'를 입력합니다. (페어링 포트와 다릅니다)"
    WifiConnSteps    = @(
      "'페어링 코드로 기기 페어링' 화면을 닫고 무선 디버깅 메인 화면으로 돌아갑니다.",
      "화면 상단 'IP 주소 및 포트' 항목의 포트 번호를 확인합니다."
    )
    WifiConnAddrPrompt = "연결 IP:포트를 입력하세요 (예: 192.168.1.100:41391)"
    WifiConnecting   = "연결 중..."
    WifiConnOk       = "기기 연결 완료!"
    WifiConnFail     = "기기 연결에 실패했습니다."
    WifiConnFix      = @(
      "연결 포트가 페어링 포트와 다른 번호인지 확인하세요.",
      "방화벽이 해당 포트를 차단하고 있지 않은지 확인하세요.",
      "무선 디버깅을 껐다 켠 후 다시 시도하세요."
    )

    InstallTitle     = "APK 설치 중"
    InstallFile      = "설치 파일"
    InstallTarget    = "대상 기기"
    Installing       = "설치 중입니다. 잠시 기다려주세요..."
    InstallAllow     = "(기기 화면에서 설치 허용 팝업이 나타나면 '허용'을 탭하세요)"
    InstallOk        = "설치 완료!"
    InstallFail      = "설치에 실패했습니다."
    InstallErrRestriction = @(
      "원인: 기기 정책이 알 수 없는 출처의 앱 설치를 차단하고 있습니다.",
      "해결: 설정 > 보안 > '알 수 없는 출처' 또는 '출처를 알 수 없는 앱 설치'를 허용하세요."
    )
    InstallErrExists = @(
      "원인: 이미 설치된 버전과 서명이 다릅니다.",
      "해결: 기존 MimicEase 앱을 삭제한 후 다시 실행하세요."
    )
    InstallErrStorage = @(
      "원인: 기기 저장공간이 부족합니다.",
      "해결: 불필요한 파일이나 앱을 삭제한 후 다시 시도하세요."
    )
    InstallErrGeneral = @(
      "1. 기기를 재시작한 후 다시 시도하세요.",
      "2. 다른 USB 케이블이나 포트를 사용해보세요.",
      "3. GitHub Issues에 오류 내용을 첨부해 문의하세요."
    )

    SuccessTitle     = "MimicEase 설치가 완료되었습니다!"
    SuccessSteps     = @(
      "스마트폰에서 'MimicEase' 앱을 실행합니다.",
      "온보딩 화면의 안내에 따라 카메라 권한을 허용합니다.",
      "설정 > 접근성 > MimicEase를 찾아 서비스를 활성화합니다."
    )
    SuccessHelp      = "도움말"
    SuccessHelpUrl   = "GitHub Issues에 남겨주세요"
    InputEmpty       = "입력값이 비어 있습니다."
  }

  "ja" = @{
    LangName         = "日本語"
    AppTitle         = "MimicEase インストーラー"
    AppSubtitle      = "顔の表情でスマートフォンを操作する"
    AppDesc          = "このインストーラーはADB(Android Debug Bridge)を使って、インターネット接続なしでMimicEaseをインストールします。"
    PressEnter       = "Enterキーを押して続行..."
    PressEnterExit   = "Enterキーを押して終了..."
    Checking         = "確認中"
    Verified         = "確認完了"
    Error            = "エラー"
    Warning          = "注意"
    Done             = "完了"
    Waiting          = "待機中"

    NoAdb            = "adb.exeが見つかりません。"
    NoAdbHint        = "ZIPファイルが正しく解凍されているか確認してください。"
    NoApk            = "APKファイルが見つかりません。"
    NoApkHint        = "MimicEase-*.apkファイルをこのスクリプトと同じフォルダに置いてください。"
    AdbPath          = "ADBパス"
    ApkFile          = "APKファイル"

    Step1Title       = "ステップ1：開発者オプションを有効にする"
    Step1Desc        = "ADB経由でアプリをインストールするには、Androidデバイスで開発者オプションを有効にする必要があります。"
    Step1Steps       = @(
      "スマートフォンで「設定」アプリを開きます。",
      "「端末情報」または「デバイス情報」に移動します。",
      "「ソフトウェア情報」をタップします（一部機種はこのステップなし）。",
      "「ビルド番号」を7回連続でタップします。",
      "PIN/パターン/パスワードを入力すると「開発者モードが有効になりました」と表示されます。"
    )
    Step1Brands      = @(
      "Samsung: 設定 > 端末情報 > ソフトウェア情報 > ビルド番号",
      "Google Pixel: 設定 > 端末情報 > ビルド番号",
      "その他: 設定で「ビルド番号」を検索してください。"
    )
    Step1BrandsTitle = "メーカー別の手順"
    Step1Skip        = "すでに開発者オプションが有効な場合は、このステップをスキップできます。"
    Step1Done        = "開発者オプションを有効にしたらEnterキーを押してください..."

    ConnTitle        = "接続方法を選択してください"
    ConnUsb          = "USBケーブル接続（推奨、すべてのAndroidバージョン対応）"
    ConnWifi         = "Wi-Fi無線接続（Android 11以降が必要）"
    ConnPrompt       = "1または2を入力してください"

    UsbTitle         = "ステップ2(USB)：デバイスを接続"
    UsbSteps         = @(
      "USBケーブルでAndroidデバイスをPCに接続します。",
      "「このコンピュータを信頼しますか？」のポップアップが表示されたら「許可」をタップします。",
      "USB接続の種類を「ファイル転送(MTP)」または「データ転送」に変更します。"
    )
    UsbDebugTitle    = "USBデバッグを有効にする"
    UsbDebugSteps    = @(
      "設定 > 開発者オプション > 「USBデバッグ」をオンにします。",
      "「USBデバッグを許可しますか？」のポップアップで「OK」をタップします。"
    )
    UsbDone          = "上記の手順を完了したらEnterキーを押してください..."
    UsbDetecting     = "デバイスを検出中..."
    UsbFound         = "デバイス接続確認"
    UsbUnauth        = "デバイスが検出されましたが、認証が必要です。"
    UsbUnauthHint    = "スマートフォンの画面で「USBデバッグを許可」のポップアップを確認し「許可」をタップしてください。"
    UsbUnauthHint2   = "ポップアップが表示されない場合は、USBケーブルを抜き差ししてください。"
    UsbOffline       = "デバイスがオフラインです。USBケーブルを確認してください。"
    UsbTimeout       = "デバイスを検出できませんでした。(60秒タイムアウト)"
    UsbTimeoutFix    = @(
      "USBケーブルがデータ転送対応であることを確認してください。(充電専用ケーブルは不可)",
      "別のUSBポートを試してください。",
      "デバイスでUSBデバッグが有効になっているか再確認してください。",
      "スマートフォンメーカーのUSBドライバーがインストールされているか確認してください。"
    )

    WifiTitle        = "ステップ2(Wi-Fi)：ワイヤレスデバッグの設定"
    WifiReq          = "Wi-FiインストールはAndroid 11(R)以降でのみ対応しています。"
    WifiReq2         = "デバイスとこのPCが同じWi-Fiに接続されている必要があります。"
    WifiEnableTitle  = "ワイヤレスデバッグを有効にする"
    WifiEnableSteps  = @(
      "設定 > 開発者オプション > 「ワイヤレスデバッグ」をオンにします。",
      "「このネットワークでワイヤレスデバッグを許可しますか？」で「許可」をタップします。"
    )
    WifiEnableDone   = "ワイヤレスデバッグを有効にしたらEnterキーを押してください..."
    WifiPairTitle         = "ペアリングコードでデバイスをペアリング"
    WifiAlreadyPairedQ    = "このPCはすでにデバイスとペアリング済みですか？(y/N)"
    WifiAlreadyPairedSkip = "ペアリング済み — ペアリング手順をスキップします。"
    WifiPairSteps    = @(
      "「ワイヤレスデバッグ」の名前部分(トグルではなく)をタップします。",
      "「ペアリングコードでデバイスをペアリング」をタップします。",
      "画面に表示された「IPアドレスとポート」と「6桁のペアリングコード」を確認します。"
    )
    WifiPairWarning  = "ペアリングポートと接続ポートは異なる番号です。"
    WifiPairAddrPrompt = "ペアリングIP:ポートを入力してください (例: 192.168.1.100:37491)"
    WifiPairCodePrompt = "6桁のペアリングコードを入力してください"
    WifiPairing      = "ペアリング中..."
    WifiPairOk       = "ペアリング完了！"
    WifiPairFail     = "ペアリングに失敗しました。"
    WifiPairFixTitle = "解決方法"
    WifiPairFix      = @(
      "IPアドレスとポート番号を再確認してください。",
      "ペアリングコードには有効期限があります。画面を閉じて新しいコードを取得してください。",
      "デバイスとPCが同じWi-Fiに接続されているか確認してください。"
    )
    WifiConnTitle    = "デバイスに接続"
    WifiConnDesc     = "次に「接続用IP:ポート」を入力します（ペアリングポートとは異なります）。"
    WifiConnSteps    = @(
      "ペアリング画面を閉じ、ワイヤレスデバッグのメイン画面に戻ります。",
      "画面上部の「IPアドレスとポート」に表示されているポート番号を確認します。"
    )
    WifiConnAddrPrompt = "接続IP:ポートを入力してください (例: 192.168.1.100:41391)"
    WifiConnecting   = "接続中..."
    WifiConnOk       = "デバイス接続完了！"
    WifiConnFail     = "デバイスへの接続に失敗しました。"
    WifiConnFix      = @(
      "接続ポートがペアリングポートと異なる番号であることを確認してください。",
      "ファイアウォールがそのポートをブロックしていないか確認してください。",
      "ワイヤレスデバッグをオフにしてからオンにして再試行してください。"
    )

    InstallTitle     = "APKをインストール中"
    InstallFile      = "インストールファイル"
    InstallTarget    = "対象デバイス"
    Installing       = "インストール中です。しばらくお待ちください..."
    InstallAllow     = "（デバイスにインストール許可のポップアップが表示された場合は「許可」をタップしてください）"
    InstallOk        = "インストール完了！"
    InstallFail      = "インストールに失敗しました。"
    InstallErrRestriction = @(
      "原因: デバイスポリシーが不明なソースからのアプリインストールをブロックしています。",
      "解決: 設定 > セキュリティ > 「提供元不明のアプリ」または「不明なアプリのインストール」を許可してください。"
    )
    InstallErrExists = @(
      "原因: インストール済みのバージョンと署名が異なります。",
      "解決: 既存のMimicEaseアプリをアンインストールしてから再試行してください。"
    )
    InstallErrStorage = @(
      "原因: デバイスのストレージ容量が不足しています。",
      "解決: 不要なファイルやアプリを削除してから再試行してください。"
    )
    InstallErrGeneral = @(
      "1. デバイスを再起動して再試行してください。",
      "2. 別のUSBケーブルやポートを試してください。",
      "3. エラー内容をGitHub Issuesに報告してください。"
    )

    SuccessTitle     = "MimicEaseのインストールが完了しました！"
    SuccessSteps     = @(
      "スマートフォンで「MimicEase」アプリを起動します。",
      "オンボーディング画面の指示に従ってカメラ許可を与えます。",
      "設定 > ユーザー補助 > MimicEaseを見つけてサービスを有効にします。"
    )
    SuccessHelp      = "ヘルプ"
    SuccessHelpUrl   = "GitHub Issuesでお問い合わせください"
    InputEmpty       = "入力値が空です。"
  }

  "zh-cn" = @{
    LangName         = "简体中文"
    AppTitle         = "MimicEase 安装程序"
    AppSubtitle      = "用面部表情控制您的手机"
    AppDesc          = "此安装程序使用ADB（Android调试桥）在无需互联网连接的情况下安装MimicEase。"
    PressEnter       = "按Enter键继续..."
    PressEnterExit   = "按Enter键退出..."
    Checking         = "检查中"
    Verified         = "验证完成"
    Error            = "错误"
    Warning          = "警告"
    Done             = "完成"
    Waiting          = "等待中"

    NoAdb            = "找不到adb.exe。"
    NoAdbHint        = "请确认ZIP文件已完整解压缩。"
    NoApk            = "找不到APK文件。"
    NoApkHint        = "MimicEase-*.apk文件必须与此脚本在同一文件夹中。"
    AdbPath          = "ADB路径"
    ApkFile          = "APK文件"

    Step1Title       = "第1步：启用开发者选项"
    Step1Desc        = "要通过ADB安装应用，需要先在Android设备上启用开发者选项。"
    Step1Steps       = @(
      "在手机上打开「设置」应用。",
      "进入「关于手机」或「关于设备」。",
      "点击「软件信息」（部分设备无此步骤）。",
      "连续点击「版本号」7次。",
      "输入PIN/图案/密码后，会显示「已启用开发者模式」。"
    )
    Step1Brands      = @(
      "三星: 设置 > 关于手机 > 软件信息 > 版本号",
      "谷歌Pixel: 设置 > 关于手机 > 版本号",
      "其他: 在设置中搜索「版本号」。"
    )
    Step1BrandsTitle = "各品牌路径"
    Step1Skip        = "如果已启用开发者选项，可以跳过此步骤。"
    Step1Done        = "启用开发者选项后，请按Enter键..."

    ConnTitle        = "请选择连接方式"
    ConnUsb          = "USB数据线连接（推荐，支持所有Android版本）"
    ConnWifi         = "Wi-Fi无线连接（需要Android 11或更高版本）"
    ConnPrompt       = "请输入1或2"

    UsbTitle         = "第2步(USB)：连接设备"
    UsbSteps         = @(
      "用USB数据线将Android设备连接到此电脑。",
      "如果设备屏幕出现「信任此计算机？」提示，请点击「允许」。",
      "将USB连接类型更改为「文件传输(MTP)」或「数据传输」。"
    )
    UsbDebugTitle    = "启用USB调试"
    UsbDebugSteps    = @(
      "设置 > 开发者选项 > 开启「USB调试」。",
      "在「允许USB调试？」提示中点击「确定」。"
    )
    UsbDone          = "完成上述步骤后，请按Enter键..."
    UsbDetecting     = "正在检测设备..."
    UsbFound         = "设备连接确认"
    UsbUnauth        = "已检测到设备，但需要授权。"
    UsbUnauthHint    = "请在手机屏幕上查看「允许USB调试」提示并点击「允许」。"
    UsbUnauthHint2   = "如果没有提示，请拔出并重新插入USB数据线。"
    UsbOffline       = "设备处于离线状态，请检查USB数据线。"
    UsbTimeout       = "未能检测到设备。（60秒超时）"
    UsbTimeoutFix    = @(
      "确认USB数据线支持数据传输（仅充电线不可用）。",
      "尝试使用其他USB端口。",
      "再次确认设备上已启用USB调试。",
      "检查是否已安装手机品牌的USB驱动程序。"
    )

    WifiTitle        = "第2步(Wi-Fi)：无线调试设置"
    WifiReq          = "Wi-Fi安装仅支持Android 11(R)及更高版本。"
    WifiReq2         = "设备和此电脑必须连接到同一Wi-Fi网络。"
    WifiEnableTitle  = "启用无线调试"
    WifiEnableSteps  = @(
      "设置 > 开发者选项 > 开启「无线调试」。",
      "在「是否允许在此网络上进行无线调试？」提示中点击「允许」。"
    )
    WifiEnableDone   = "启用无线调试后，请按Enter键..."
    WifiPairTitle         = "使用配对码配对设备"
    WifiAlreadyPairedQ    = "此PC是否已与设备配对？(y/N)"
    WifiAlreadyPairedSkip = "已配对 — 跳过配对步骤。"
    WifiPairSteps    = @(
      "点击「无线调试」的名称部分（不是开关）。",
      "点击「使用配对码配对设备」。",
      "记下屏幕上显示的「IP地址和端口」以及「6位配对码」。"
    )
    WifiPairWarning  = "配对端口和连接端口是不同的号码。"
    WifiPairAddrPrompt = "请输入配对IP:端口 (例: 192.168.1.100:37491)"
    WifiPairCodePrompt = "请输入6位配对码"
    WifiPairing      = "配对中..."
    WifiPairOk       = "配对成功！"
    WifiPairFail     = "配对失败。"
    WifiPairFixTitle = "解决方法"
    WifiPairFix      = @(
      "请再次确认IP地址和端口号。",
      "配对码有时效性。请关闭配对屏幕后重新打开获取新码。",
      "确认设备和电脑连接到同一Wi-Fi网络。"
    )
    WifiConnTitle    = "连接到设备"
    WifiConnDesc     = "现在输入「连接IP:端口」（与配对端口不同）。"
    WifiConnSteps    = @(
      "关闭配对屏幕，返回无线调试主界面。",
      "查看屏幕顶部「IP地址和端口」项目的端口号。"
    )
    WifiConnAddrPrompt = "请输入连接IP:端口 (例: 192.168.1.100:41391)"
    WifiConnecting   = "连接中..."
    WifiConnOk       = "设备连接成功！"
    WifiConnFail     = "连接设备失败。"
    WifiConnFix      = @(
      "确认连接端口与配对端口是不同的号码。",
      "检查防火墙是否阻止了该端口。",
      "关闭无线调试后重新开启，再试一次。"
    )

    InstallTitle     = "正在安装APK"
    InstallFile      = "安装文件"
    InstallTarget    = "目标设备"
    Installing       = "安装中，请稍候..."
    InstallAllow     = "（如果设备屏幕出现安装许可提示，请点击「允许」）"
    InstallOk        = "安装完成！"
    InstallFail      = "安装失败。"
    InstallErrRestriction = @(
      "原因: 设备策略阻止安装来自未知来源的应用。",
      "解决: 设置 > 安全 > 启用「未知来源」或「安装未知应用」。"
    )
    InstallErrExists = @(
      "原因: 已安装版本的签名不同。",
      "解决: 先卸载现有的MimicEase应用，然后重试。"
    )
    InstallErrStorage = @(
      "原因: 设备存储空间不足。",
      "解决: 删除不需要的文件或应用后重试。"
    )
    InstallErrGeneral = @(
      "1. 重启设备后重试。",
      "2. 尝试使用其他USB数据线或端口。",
      "3. 在GitHub Issues中报告错误详情。"
    )

    SuccessTitle     = "MimicEase安装成功！"
    SuccessSteps     = @(
      "在手机上启动「MimicEase」应用。",
      "按照引导界面的说明授予摄像头权限。",
      "设置 > 无障碍 > 找到MimicEase并启用服务。"
    )
    SuccessHelp      = "帮助"
    SuccessHelpUrl   = "请在GitHub Issues中留言"
    InputEmpty       = "输入不能为空。"
  }

  "zh-tw" = @{
    LangName         = "繁體中文"
    AppTitle         = "MimicEase 安裝程式"
    AppSubtitle      = "用臉部表情控制您的手機"
    AppDesc          = "此安裝程式使用ADB（Android偵錯橋接器）在無需網際網路連線的情況下安裝MimicEase。"
    PressEnter       = "按Enter鍵繼續..."
    PressEnterExit   = "按Enter鍵結束..."
    Checking         = "確認中"
    Verified         = "確認完成"
    Error            = "錯誤"
    Warning          = "注意"
    Done             = "完成"
    Waiting          = "等待中"

    NoAdb            = "找不到adb.exe。"
    NoAdbHint        = "請確認ZIP檔案已完整解壓縮。"
    NoApk            = "找不到APK檔案。"
    NoApkHint        = "MimicEase-*.apk檔案必須與此指令碼在同一個資料夾。"
    AdbPath          = "ADB路徑"
    ApkFile          = "APK檔案"

    Step1Title       = "第1步：啟用開發人員選項"
    Step1Desc        = "若要透過ADB安裝應用程式，需先在Android裝置上啟用開發人員選項。"
    Step1Steps       = @(
      "在手機上開啟「設定」應用程式。",
      "前往「關於手機」或「關於裝置」。",
      "點選「軟體資訊」（部分裝置無此步驟）。",
      "連續點選「組建編號」7次。",
      "輸入PIN/圖案/密碼後，將顯示「已啟用開發人員模式」。"
    )
    Step1Brands      = @(
      "三星: 設定 > 關於手機 > 軟體資訊 > 組建編號",
      "Google Pixel: 設定 > 關於手機 > 組建編號",
      "其他: 在設定中搜尋「組建編號」。"
    )
    Step1BrandsTitle = "各品牌路徑"
    Step1Skip        = "若已啟用開發人員選項，可跳過此步驟。"
    Step1Done        = "啟用開發人員選項後，請按Enter鍵..."

    ConnTitle        = "請選擇連線方式"
    ConnUsb          = "USB連接線連線（建議，支援所有Android版本）"
    ConnWifi         = "Wi-Fi無線連線（需要Android 11或更新版本）"
    ConnPrompt       = "請輸入1或2"

    UsbTitle         = "第2步(USB)：連線裝置"
    UsbSteps         = @(
      "用USB連接線將Android裝置連接到此電腦。",
      "若裝置畫面出現「信任此電腦？」提示，請點選「允許」。",
      "將USB連線類型更改為「檔案傳輸(MTP)」或「資料傳輸」。"
    )
    UsbDebugTitle    = "啟用USB偵錯"
    UsbDebugSteps    = @(
      "設定 > 開發人員選項 > 開啟「USB偵錯」。",
      "在「允許USB偵錯？」提示中點選「確定」。"
    )
    UsbDone          = "完成上述步驟後，請按Enter鍵..."
    UsbDetecting     = "正在偵測裝置..."
    UsbFound         = "裝置連線確認"
    UsbUnauth        = "已偵測到裝置，但需要授權。"
    UsbUnauthHint    = "請在手機畫面查看「允許USB偵錯」提示並點選「允許」。"
    UsbUnauthHint2   = "若無提示，請拔出並重新插入USB連接線。"
    UsbOffline       = "裝置處於離線狀態，請檢查USB連接線。"
    UsbTimeout       = "無法偵測到裝置。（60秒逾時）"
    UsbTimeoutFix    = @(
      "確認USB連接線支援資料傳輸（僅充電線不可用）。",
      "嘗試使用其他USB連接埠。",
      "再次確認裝置已啟用USB偵錯。",
      "檢查是否已安裝手機品牌的USB驅動程式。"
    )

    WifiTitle        = "第2步(Wi-Fi)：無線偵錯設定"
    WifiReq          = "Wi-Fi安裝僅支援Android 11(R)及更新版本。"
    WifiReq2         = "裝置和此電腦必須連線到相同的Wi-Fi網路。"
    WifiEnableTitle  = "啟用無線偵錯"
    WifiEnableSteps  = @(
      "設定 > 開發人員選項 > 開啟「無線偵錯」。",
      "在「是否允許在此網路上進行無線偵錯？」提示中點選「允許」。"
    )
    WifiEnableDone   = "啟用無線偵錯後，請按Enter鍵..."
    WifiPairTitle         = "使用配對碼配對裝置"
    WifiAlreadyPairedQ    = "此PC是否已與裝置配對？(y/N)"
    WifiAlreadyPairedSkip = "已配對 — 跳過配對步驟。"
    WifiPairSteps    = @(
      "點選「無線偵錯」的名稱部分（不是開關）。",
      "點選「使用配對碼配對裝置」。",
      "記下畫面上顯示的「IP位址和連接埠」以及「6位配對碼」。"
    )
    WifiPairWarning  = "配對連接埠和連線連接埠是不同的號碼。"
    WifiPairAddrPrompt = "請輸入配對IP:連接埠 (例: 192.168.1.100:37491)"
    WifiPairCodePrompt = "請輸入6位配對碼"
    WifiPairing      = "配對中..."
    WifiPairOk       = "配對成功！"
    WifiPairFail     = "配對失敗。"
    WifiPairFixTitle = "解決方法"
    WifiPairFix      = @(
      "請再次確認IP位址和連接埠號碼。",
      "配對碼有時效性，請關閉配對畫面後重新開啟取得新碼。",
      "確認裝置和電腦連線到相同的Wi-Fi網路。"
    )
    WifiConnTitle    = "連線到裝置"
    WifiConnDesc     = "現在輸入「連線IP:連接埠」（與配對連接埠不同）。"
    WifiConnSteps    = @(
      "關閉配對畫面，返回無線偵錯主畫面。",
      "查看畫面頂端「IP位址和連接埠」項目的連接埠號碼。"
    )
    WifiConnAddrPrompt = "請輸入連線IP:連接埠 (例: 192.168.1.100:41391)"
    WifiConnecting   = "連線中..."
    WifiConnOk       = "裝置連線成功！"
    WifiConnFail     = "裝置連線失敗。"
    WifiConnFix      = @(
      "確認連線連接埠與配對連接埠是不同的號碼。",
      "檢查防火牆是否封鎖了該連接埠。",
      "關閉無線偵錯後重新開啟，再試一次。"
    )

    InstallTitle     = "正在安裝APK"
    InstallFile      = "安裝檔案"
    InstallTarget    = "目標裝置"
    Installing       = "安裝中，請稍候..."
    InstallAllow     = "（若裝置畫面出現安裝許可提示，請點選「允許」）"
    InstallOk        = "安裝完成！"
    InstallFail      = "安裝失敗。"
    InstallErrRestriction = @(
      "原因: 裝置原則封鎖了來自未知來源的應用程式安裝。",
      "解決: 設定 > 安全性 > 啟用「未知來源」或「安裝不明應用程式」。"
    )
    InstallErrExists = @(
      "原因: 已安裝版本的簽章不同。",
      "解決: 先解除安裝現有的MimicEase應用程式，然後重試。"
    )
    InstallErrStorage = @(
      "原因: 裝置儲存空間不足。",
      "解決: 刪除不需要的檔案或應用程式後重試。"
    )
    InstallErrGeneral = @(
      "1. 重新啟動裝置後重試。",
      "2. 嘗試使用其他USB連接線或連接埠。",
      "3. 在GitHub Issues回報錯誤詳情。"
    )

    SuccessTitle     = "MimicEase安裝成功！"
    SuccessSteps     = @(
      "在手機上啟動「MimicEase」應用程式。",
      "依照引導畫面的指示授予相機權限。",
      "設定 > 協助工具 > 找到MimicEase並啟用服務。"
    )
    SuccessHelp      = "說明"
    SuccessHelpUrl   = "請在GitHub Issues留言"
    InputEmpty       = "輸入不能為空。"
  }

  "es" = @{
    LangName         = "Español"
    AppTitle         = "Instalador de MimicEase"
    AppSubtitle      = "Controla tu teléfono con expresiones faciales"
    AppDesc          = "Este instalador usa ADB (Android Debug Bridge) para instalar MimicEase sin conexión a internet."
    PressEnter       = "Pulsa Enter para continuar..."
    PressEnterExit   = "Pulsa Enter para salir..."
    Checking         = "Verificando"
    Verified         = "Verificado"
    Error            = "ERROR"
    Warning          = "AVISO"
    Done             = "LISTO"
    Waiting          = "Esperando"

    NoAdb            = "No se encontró adb.exe."
    NoAdbHint        = "Asegúrate de que el archivo ZIP se descomprimió correctamente."
    NoApk            = "No se encontró el archivo APK."
    NoApkHint        = "El archivo MimicEase-*.apk debe estar en la misma carpeta que este script."
    AdbPath          = "Ruta ADB"
    ApkFile          = "Archivo APK"

    Step1Title       = "Paso 1: Activar Opciones de desarrollador"
    Step1Desc        = "Para instalar mediante ADB, debes activar las Opciones de desarrollador en tu dispositivo Android."
    Step1Steps       = @(
      "Abre la app 'Ajustes' en tu teléfono.",
      "Ve a 'Información del teléfono' o 'Acerca del dispositivo'.",
      "Pulsa 'Información de software' (algunos dispositivos omiten este paso).",
      "Pulsa 'Número de compilación' 7 veces seguidas.",
      "Ingresa tu PIN/patrón/contraseña. Verás el mensaje 'Se ha activado el modo de desarrollador'."
    )
    Step1Brands      = @(
      "Samsung: Ajustes > Información del teléfono > Información de software > Número de compilación",
      "Google Pixel: Ajustes > Información del teléfono > Número de compilación",
      "Otros: Busca 'Número de compilación' en Ajustes."
    )
    Step1BrandsTitle = "Ruta según fabricante"
    Step1Skip        = "Si las Opciones de desarrollador ya están activadas, puedes saltar este paso."
    Step1Done        = "Pulsa Enter cuando hayas activado las Opciones de desarrollador..."

    ConnTitle        = "Elige el método de conexión"
    ConnUsb          = "Cable USB (recomendado, compatible con todas las versiones de Android)"
    ConnWifi         = "Wi-Fi inalámbrico (requiere Android 11 o superior)"
    ConnPrompt       = "Introduce 1 o 2"

    UsbTitle         = "Paso 2 (USB): Conectar el dispositivo"
    UsbSteps         = @(
      "Conecta tu dispositivo Android al PC con un cable USB.",
      "Si aparece '¿Confiar en este ordenador?' en tu dispositivo, pulsa 'Aceptar'.",
      "Cambia el tipo de conexión USB a 'Transferencia de archivos (MTP)'."
    )
    UsbDebugTitle    = "Activar depuración USB"
    UsbDebugSteps    = @(
      "Ajustes > Opciones de desarrollador > activa 'Depuración USB'.",
      "Pulsa 'Aceptar' en el aviso '¿Permitir depuración USB?'."
    )
    UsbDone          = "Pulsa Enter cuando hayas completado los pasos anteriores..."
    UsbDetecting     = "Detectando dispositivo..."
    UsbFound         = "Dispositivo conectado"
    UsbUnauth        = "Dispositivo detectado pero requiere autorización."
    UsbUnauthHint    = "Comprueba si aparece el aviso 'Permitir depuración USB' en tu teléfono y pulsa 'Permitir'."
    UsbUnauthHint2   = "Si no aparece el aviso, desconecta y vuelve a conectar el cable USB."
    UsbOffline       = "El dispositivo está desconectado. Comprueba el cable USB."
    UsbTimeout       = "No se pudo detectar el dispositivo. (60 segundos de tiempo límite)"
    UsbTimeoutFix    = @(
      "Asegúrate de que el cable USB admite transferencia de datos (los cables solo de carga no sirven).",
      "Prueba con un puerto USB diferente.",
      "Confirma que la depuración USB está activada en el dispositivo.",
      "Comprueba que el controlador USB del fabricante está instalado."
    )

    WifiTitle        = "Paso 2 (Wi-Fi): Configurar depuración inalámbrica"
    WifiReq          = "La instalación por Wi-Fi solo es compatible con Android 11 (R) o superior."
    WifiReq2         = "El dispositivo y el PC deben estar en la misma red Wi-Fi."
    WifiEnableTitle  = "Activar depuración inalámbrica"
    WifiEnableSteps  = @(
      "Ajustes > Opciones de desarrollador > activa 'Depuración inalámbrica'.",
      "Pulsa 'Permitir' en el aviso '¿Permitir depuración inalámbrica en esta red?'."
    )
    WifiEnableDone   = "Pulsa Enter cuando hayas activado la depuración inalámbrica..."
    WifiPairTitle         = "Emparejar dispositivo con código de emparejamiento"
    WifiAlreadyPairedQ    = "¿Este PC ya está emparejado con el dispositivo? (y/N)"
    WifiAlreadyPairedSkip = "Ya emparejado — se omite el paso de emparejamiento."
    WifiPairSteps    = @(
      "Pulsa el texto 'Depuración inalámbrica' (no el interruptor).",
      "Pulsa 'Emparejar dispositivo con código de emparejamiento'.",
      "Anota la 'Dirección IP y puerto' y el 'código de emparejamiento de 6 dígitos'."
    )
    WifiPairWarning  = "El puerto de emparejamiento y el puerto de conexión son diferentes."
    WifiPairAddrPrompt = "Introduce IP:puerto de emparejamiento (ej: 192.168.1.100:37491)"
    WifiPairCodePrompt = "Introduce el código de emparejamiento de 6 dígitos"
    WifiPairing      = "Emparejando..."
    WifiPairOk       = "¡Emparejamiento completado!"
    WifiPairFail     = "Error de emparejamiento."
    WifiPairFixTitle = "Solución de problemas"
    WifiPairFix      = @(
      "Vuelve a comprobar la dirección IP y el número de puerto.",
      "Los códigos de emparejamiento caducan. Cierra la pantalla y ábrela de nuevo.",
      "Asegúrate de que el dispositivo y el PC están en la misma red Wi-Fi."
    )
    WifiConnTitle    = "Conectar al dispositivo"
    WifiConnDesc     = "Ahora introduce la 'IP:puerto de conexión' (diferente del puerto de emparejamiento)."
    WifiConnSteps    = @(
      "Cierra la pantalla de emparejamiento y vuelve a la pantalla principal de depuración inalámbrica.",
      "Anota el número de puerto que aparece en 'Dirección IP y puerto'."
    )
    WifiConnAddrPrompt = "Introduce IP:puerto de conexión (ej: 192.168.1.100:41391)"
    WifiConnecting   = "Conectando..."
    WifiConnOk       = "¡Dispositivo conectado!"
    WifiConnFail     = "Error al conectar con el dispositivo."
    WifiConnFix      = @(
      "Asegúrate de que el puerto de conexión es diferente del puerto de emparejamiento.",
      "Comprueba que el firewall no está bloqueando ese puerto.",
      "Desactiva la depuración inalámbrica, vuelve a activarla e inténtalo de nuevo."
    )

    InstallTitle     = "Instalando APK"
    InstallFile      = "Archivo"
    InstallTarget    = "Dispositivo destino"
    Installing       = "Instalando, por favor espera..."
    InstallAllow     = "(Si aparece un aviso de instalación en tu dispositivo, pulsa 'Permitir'.)"
    InstallOk        = "¡Instalación completada!"
    InstallFail      = "Error de instalación."
    InstallErrRestriction = @(
      "Causa: La política del dispositivo bloquea la instalación de fuentes desconocidas.",
      "Solución: Ajustes > Seguridad > Habilita 'Fuentes desconocidas' o 'Instalar apps desconocidas'."
    )
    InstallErrExists = @(
      "Causa: La versión instalada tiene una firma diferente.",
      "Solución: Desinstala la app MimicEase existente e inténtalo de nuevo."
    )
    InstallErrStorage = @(
      "Causa: Espacio de almacenamiento insuficiente.",
      "Solución: Libera espacio y vuelve a intentarlo."
    )
    InstallErrGeneral = @(
      "1. Reinicia el dispositivo e inténtalo de nuevo.",
      "2. Prueba con un cable USB o puerto diferente.",
      "3. Informa del error en GitHub Issues."
    )

    SuccessTitle     = "¡MimicEase se ha instalado correctamente!"
    SuccessSteps     = @(
      "Abre la app 'MimicEase' en tu teléfono.",
      "Sigue las instrucciones de incorporación para permitir el acceso a la cámara.",
      "Ajustes > Accesibilidad > encuentra MimicEase y activa el servicio."
    )
    SuccessHelp      = "Ayuda"
    SuccessHelpUrl   = "Deja un mensaje en GitHub Issues"
    InputEmpty       = "El campo no puede estar vacío."
  }

  "fr" = @{
    LangName         = "Français"
    AppTitle         = "Installateur MimicEase"
    AppSubtitle      = "Contrôlez votre téléphone avec vos expressions faciales"
    AppDesc          = "Cet installateur utilise ADB (Android Debug Bridge) pour installer MimicEase sans connexion internet."
    PressEnter       = "Appuyez sur Entrée pour continuer..."
    PressEnterExit   = "Appuyez sur Entrée pour quitter..."
    Checking         = "Vérification"
    Verified         = "Vérifié"
    Error            = "ERREUR"
    Warning          = "ATTENTION"
    Done             = "TERMINÉ"
    Waiting          = "En attente"

    NoAdb            = "adb.exe est introuvable."
    NoAdbHint        = "Assurez-vous que l'archive ZIP a été entièrement décompressée."
    NoApk            = "Fichier APK introuvable."
    NoApkHint        = "Le fichier MimicEase-*.apk doit se trouver dans le même dossier que ce script."
    AdbPath          = "Chemin ADB"
    ApkFile          = "Fichier APK"

    Step1Title       = "Étape 1 : Activer les options développeur"
    Step1Desc        = "Pour installer via ADB, les options développeur doivent être activées sur votre appareil Android."
    Step1Steps       = @(
      "Ouvrez l'application Paramètres sur votre téléphone.",
      "Allez dans 'À propos du téléphone' ou 'Informations sur l'appareil'.",
      "Appuyez sur 'Informations sur le logiciel' (certains appareils n'ont pas cette étape).",
      "Appuyez 7 fois de suite sur 'Numéro de build'.",
      "Saisissez votre PIN/schéma/mot de passe. Un message 'Vous êtes maintenant développeur' apparaîtra."
    )
    Step1Brands      = @(
      "Samsung : Paramètres > À propos du téléphone > Informations logicielles > Numéro de build",
      "Google Pixel : Paramètres > À propos du téléphone > Numéro de build",
      "Autres : Recherchez 'Numéro de build' dans les Paramètres."
    )
    Step1BrandsTitle = "Chemin selon le fabricant"
    Step1Skip        = "Si les options développeur sont déjà activées, vous pouvez sauter cette étape."
    Step1Done        = "Appuyez sur Entrée après avoir activé les options développeur..."

    ConnTitle        = "Choisissez la méthode de connexion"
    ConnUsb          = "Câble USB (recommandé, compatible toutes versions Android)"
    ConnWifi         = "Wi-Fi sans fil (nécessite Android 11 ou supérieur)"
    ConnPrompt       = "Saisissez 1 ou 2"

    UsbTitle         = "Étape 2 (USB) : Connecter l'appareil"
    UsbSteps         = @(
      "Connectez votre appareil Android au PC avec un câble USB.",
      "Si 'Faire confiance à cet ordinateur ?' apparaît, appuyez sur 'Autoriser'.",
      "Changez le type de connexion USB en 'Transfert de fichiers (MTP)'."
    )
    UsbDebugTitle    = "Activer le débogage USB"
    UsbDebugSteps    = @(
      "Paramètres > Options développeur > activez 'Débogage USB'.",
      "Appuyez sur 'OK' dans l'invite 'Autoriser le débogage USB ?'."
    )
    UsbDone          = "Appuyez sur Entrée après avoir effectué les étapes ci-dessus..."
    UsbDetecting     = "Détection de l'appareil..."
    UsbFound         = "Appareil connecté"
    UsbUnauth        = "Appareil détecté mais autorisation requise."
    UsbUnauthHint    = "Vérifiez l'invite 'Autoriser le débogage USB' sur votre téléphone et appuyez sur 'Autoriser'."
    UsbUnauthHint2   = "Si aucune invite n'apparaît, débranchez et rebranchez le câble USB."
    UsbOffline       = "L'appareil est hors ligne. Vérifiez le câble USB."
    UsbTimeout       = "Impossible de détecter l'appareil. (délai de 60 secondes)"
    UsbTimeoutFix    = @(
      "Assurez-vous que le câble USB prend en charge le transfert de données (les câbles de charge seule ne fonctionnent pas).",
      "Essayez un autre port USB.",
      "Vérifiez que le débogage USB est activé sur l'appareil.",
      "Vérifiez que le pilote USB du fabricant est installé."
    )

    WifiTitle        = "Étape 2 (Wi-Fi) : Configuration du débogage sans fil"
    WifiReq          = "L'installation Wi-Fi nécessite Android 11 (R) ou supérieur."
    WifiReq2         = "L'appareil et le PC doivent être sur le même réseau Wi-Fi."
    WifiEnableTitle  = "Activer le débogage sans fil"
    WifiEnableSteps  = @(
      "Paramètres > Options développeur > activez 'Débogage sans fil'.",
      "Appuyez sur 'Autoriser' dans l'invite 'Autoriser le débogage sans fil sur ce réseau ?'."
    )
    WifiEnableDone   = "Appuyez sur Entrée après avoir activé le débogage sans fil..."
    WifiPairTitle         = "Associer l'appareil avec un code d'association"
    WifiAlreadyPairedQ    = "Ce PC est-il déjà associé à l'appareil ? (y/N)"
    WifiAlreadyPairedSkip = "Déjà associé — étape d'association ignorée."
    WifiPairSteps    = @(
      "Appuyez sur le texte 'Débogage sans fil' (pas le bouton bascule).",
      "Appuyez sur 'Associer l'appareil avec un code d'association'.",
      "Notez 'Adresse IP et port' et le 'code d'association à 6 chiffres'."
    )
    WifiPairWarning  = "Le port d'association et le port de connexion sont des numéros différents."
    WifiPairAddrPrompt = "Saisissez l'IP:port d'association (ex: 192.168.1.100:37491)"
    WifiPairCodePrompt = "Saisissez le code d'association à 6 chiffres"
    WifiPairing      = "Association en cours..."
    WifiPairOk       = "Association réussie !"
    WifiPairFail     = "Échec de l'association."
    WifiPairFixTitle = "Résolution de problèmes"
    WifiPairFix      = @(
      "Vérifiez à nouveau l'adresse IP et le numéro de port.",
      "Les codes d'association expirent. Fermez l'écran et rouvrez-le pour en obtenir un nouveau.",
      "Assurez-vous que l'appareil et le PC sont sur le même réseau Wi-Fi."
    )
    WifiConnTitle    = "Connexion à l'appareil"
    WifiConnDesc     = "Saisissez maintenant 'l'IP:port de connexion' (différent du port d'association)."
    WifiConnSteps    = @(
      "Fermez l'écran d'association et revenez à l'écran principal du débogage sans fil.",
      "Notez le numéro de port affiché dans 'Adresse IP et port'."
    )
    WifiConnAddrPrompt = "Saisissez l'IP:port de connexion (ex: 192.168.1.100:41391)"
    WifiConnecting   = "Connexion en cours..."
    WifiConnOk       = "Appareil connecté !"
    WifiConnFail     = "Échec de la connexion à l'appareil."
    WifiConnFix      = @(
      "Assurez-vous que le port de connexion est différent du port d'association.",
      "Vérifiez que le pare-feu ne bloque pas ce port.",
      "Désactivez puis réactivez le débogage sans fil et réessayez."
    )

    InstallTitle     = "Installation de l'APK"
    InstallFile      = "Fichier"
    InstallTarget    = "Appareil cible"
    Installing       = "Installation en cours, veuillez patienter..."
    InstallAllow     = "(Si une invite d'installation apparaît sur votre appareil, appuyez sur 'Autoriser'.)"
    InstallOk        = "Installation terminée !"
    InstallFail      = "Échec de l'installation."
    InstallErrRestriction = @(
      "Cause : La politique de l'appareil bloque l'installation depuis des sources inconnues.",
      "Solution : Paramètres > Sécurité > Activez 'Sources inconnues' ou 'Installer des applis inconnues'."
    )
    InstallErrExists = @(
      "Cause : La version installée a une signature différente.",
      "Solution : Désinstallez l'application MimicEase existante et réessayez."
    )
    InstallErrStorage = @(
      "Cause : Espace de stockage insuffisant.",
      "Solution : Libérez de l'espace et réessayez."
    )
    InstallErrGeneral = @(
      "1. Redémarrez l'appareil et réessayez.",
      "2. Essayez un autre câble USB ou port.",
      "3. Signalez l'erreur sur GitHub Issues."
    )

    SuccessTitle     = "MimicEase a été installé avec succès !"
    SuccessSteps     = @(
      "Lancez l'application 'MimicEase' sur votre téléphone.",
      "Suivez l'écran d'intégration pour autoriser l'accès à la caméra.",
      "Paramètres > Accessibilité > trouvez MimicEase et activez le service."
    )
    SuccessHelp      = "Aide"
    SuccessHelpUrl   = "Laissez un message sur GitHub Issues"
    InputEmpty       = "Le champ ne peut pas être vide."
  }

  "de" = @{
    LangName         = "Deutsch"
    AppTitle         = "MimicEase Installationsprogramm"
    AppSubtitle      = "Steuern Sie Ihr Smartphone mit Gesichtsausdrücken"
    AppDesc          = "Dieses Installationsprogramm verwendet ADB (Android Debug Bridge), um MimicEase ohne Internetverbindung zu installieren."
    PressEnter       = "Drücken Sie Enter zum Fortfahren..."
    PressEnterExit   = "Drücken Sie Enter zum Beenden..."
    Checking         = "Überprüfung"
    Verified         = "Bestätigt"
    Error            = "FEHLER"
    Warning          = "WARNUNG"
    Done             = "FERTIG"
    Waiting          = "Warten"

    NoAdb            = "adb.exe wurde nicht gefunden."
    NoAdbHint        = "Stellen Sie sicher, dass das ZIP-Archiv vollständig entpackt wurde."
    NoApk            = "APK-Datei nicht gefunden."
    NoApkHint        = "Die Datei MimicEase-*.apk muss sich im selben Ordner wie dieses Skript befinden."
    AdbPath          = "ADB-Pfad"
    ApkFile          = "APK-Datei"

    Step1Title       = "Schritt 1: Entwickleroptionen aktivieren"
    Step1Desc        = "Für die Installation über ADB müssen die Entwickleroptionen auf Ihrem Android-Gerät aktiviert sein."
    Step1Steps       = @(
      "Öffnen Sie die App 'Einstellungen' auf Ihrem Smartphone.",
      "Gehen Sie zu 'Über das Telefon' oder 'Geräteinformationen'.",
      "Tippen Sie auf 'Softwareinformationen' (einige Geräte überspringen diesen Schritt).",
      "Tippen Sie 7 Mal hintereinander auf 'Build-Nummer'.",
      "Geben Sie Ihre PIN/Muster/Kennwort ein. Es erscheint 'Entwicklermodus wurde aktiviert'."
    )
    Step1Brands      = @(
      "Samsung: Einstellungen > Über das Telefon > Softwareinformationen > Build-Nummer",
      "Google Pixel: Einstellungen > Über das Telefon > Build-Nummer",
      "Andere: Suchen Sie in den Einstellungen nach 'Build-Nummer'."
    )
    Step1BrandsTitle = "Pfad nach Hersteller"
    Step1Skip        = "Wenn die Entwickleroptionen bereits aktiviert sind, können Sie diesen Schritt überspringen."
    Step1Done        = "Drücken Sie Enter, nachdem Sie die Entwickleroptionen aktiviert haben..."

    ConnTitle        = "Verbindungsmethode wählen"
    ConnUsb          = "USB-Kabel (empfohlen, alle Android-Versionen)"
    ConnWifi         = "WLAN-Drahtlosverbindung (Android 11 oder höher erforderlich)"
    ConnPrompt       = "Bitte 1 oder 2 eingeben"

    UsbTitle         = "Schritt 2 (USB): Gerät verbinden"
    UsbSteps         = @(
      "Verbinden Sie Ihr Android-Gerät mit einem USB-Kabel mit diesem PC.",
      "Wenn 'Diesem Computer vertrauen?' erscheint, tippen Sie auf 'Zulassen'.",
      "Ändern Sie den USB-Verbindungstyp auf 'Dateiübertragung (MTP)'."
    )
    UsbDebugTitle    = "USB-Debugging aktivieren"
    UsbDebugSteps    = @(
      "Einstellungen > Entwickleroptionen > 'USB-Debugging' einschalten.",
      "Tippen Sie bei 'USB-Debugging zulassen?' auf 'OK'."
    )
    UsbDone          = "Drücken Sie Enter, nachdem Sie die obigen Schritte abgeschlossen haben..."
    UsbDetecting     = "Gerät wird erkannt..."
    UsbFound         = "Gerät verbunden"
    UsbUnauth        = "Gerät erkannt, aber Autorisierung erforderlich."
    UsbUnauthHint    = "Überprüfen Sie die Eingabeaufforderung 'USB-Debugging zulassen' auf Ihrem Telefon und tippen Sie auf 'Zulassen'."
    UsbUnauthHint2   = "Wenn keine Aufforderung erscheint, ziehen Sie das USB-Kabel ab und schließen Sie es wieder an."
    UsbOffline       = "Das Gerät ist offline. Überprüfen Sie das USB-Kabel."
    UsbTimeout       = "Gerät konnte nicht erkannt werden. (60 Sekunden Timeout)"
    UsbTimeoutFix    = @(
      "Stellen Sie sicher, dass das USB-Kabel Datenübertragung unterstützt (reine Ladekabel funktionieren nicht).",
      "Versuchen Sie einen anderen USB-Anschluss.",
      "Bestätigen Sie, dass USB-Debugging auf dem Gerät aktiviert ist.",
      "Prüfen Sie, ob der USB-Treiber des Geräteherstellers installiert ist."
    )

    WifiTitle        = "Schritt 2 (WLAN): Drahtloses Debugging einrichten"
    WifiReq          = "WLAN-Installation erfordert Android 11 (R) oder höher."
    WifiReq2         = "Gerät und PC müssen mit demselben WLAN-Netzwerk verbunden sein."
    WifiEnableTitle  = "Drahtloses Debugging aktivieren"
    WifiEnableSteps  = @(
      "Einstellungen > Entwickleroptionen > 'Drahtloses Debugging' einschalten.",
      "Tippen Sie auf 'Zulassen' bei 'Drahtloses Debugging in diesem Netzwerk zulassen?'."
    )
    WifiEnableDone   = "Drücken Sie Enter, nachdem Sie drahtloses Debugging aktiviert haben..."
    WifiPairTitle         = "Gerät mit Kopplungscode koppeln"
    WifiAlreadyPairedQ    = "Ist dieser PC bereits mit dem Gerät gekoppelt? (j/N)"
    WifiAlreadyPairedSkip = "Bereits gekoppelt — Kopplungsschritt wird übersprungen."
    WifiPairSteps    = @(
      "Tippen Sie auf den Text 'Drahtloses Debugging' (nicht den Schalter).",
      "Tippen Sie auf 'Gerät mit Kopplungscode koppeln'.",
      "Notieren Sie die angezeigte 'IP-Adresse und Port' und den '6-stelligen Kopplungscode'."
    )
    WifiPairWarning  = "Kopplungsport und Verbindungsport sind unterschiedliche Nummern."
    WifiPairAddrPrompt = "Kopplungs-IP:Port eingeben (z.B. 192.168.1.100:37491)"
    WifiPairCodePrompt = "6-stelligen Kopplungscode eingeben"
    WifiPairing      = "Kopplung läuft..."
    WifiPairOk       = "Kopplung erfolgreich!"
    WifiPairFail     = "Kopplung fehlgeschlagen."
    WifiPairFixTitle = "Fehlerbehebung"
    WifiPairFix      = @(
      "Überprüfen Sie die IP-Adresse und Portnummer erneut.",
      "Kopplungscodes laufen ab. Schließen Sie den Bildschirm und öffnen Sie ihn erneut.",
      "Stellen Sie sicher, dass Gerät und PC im selben WLAN-Netzwerk sind."
    )
    WifiConnTitle    = "Mit Gerät verbinden"
    WifiConnDesc     = "Geben Sie nun die 'Verbindungs-IP:Port' ein (anders als der Kopplungsport)."
    WifiConnSteps    = @(
      "Schließen Sie den Kopplungsbildschirm und kehren Sie zum Hauptbildschirm für drahtloses Debugging zurück.",
      "Notieren Sie die Portnummer in 'IP-Adresse und Port'."
    )
    WifiConnAddrPrompt = "Verbindungs-IP:Port eingeben (z.B. 192.168.1.100:41391)"
    WifiConnecting   = "Verbindung wird hergestellt..."
    WifiConnOk       = "Gerät verbunden!"
    WifiConnFail     = "Verbindung zum Gerät fehlgeschlagen."
    WifiConnFix      = @(
      "Stellen Sie sicher, dass der Verbindungsport sich vom Kopplungsport unterscheidet.",
      "Prüfen Sie, ob eine Firewall den Port blockiert.",
      "Schalten Sie drahtloses Debugging aus und wieder ein und versuchen Sie es erneut."
    )

    InstallTitle     = "APK wird installiert"
    InstallFile      = "Datei"
    InstallTarget    = "Zielgerät"
    Installing       = "Installation läuft, bitte warten..."
    InstallAllow     = "(Wenn eine Installationsaufforderung auf Ihrem Gerät erscheint, tippen Sie auf 'Zulassen'.)"
    InstallOk        = "Installation abgeschlossen!"
    InstallFail      = "Installation fehlgeschlagen."
    InstallErrRestriction = @(
      "Ursache: Geräterichtlinie blockiert die Installation aus unbekannten Quellen.",
      "Lösung: Einstellungen > Sicherheit > 'Unbekannte Quellen' oder 'Unbekannte Apps installieren' aktivieren."
    )
    InstallErrExists = @(
      "Ursache: Die installierte Version hat eine andere Signatur.",
      "Lösung: Deinstallieren Sie die vorhandene MimicEase-App und versuchen Sie es erneut."
    )
    InstallErrStorage = @(
      "Ursache: Nicht genügend Speicherplatz.",
      "Lösung: Geben Sie Speicherplatz frei und versuchen Sie es erneut."
    )
    InstallErrGeneral = @(
      "1. Starten Sie das Gerät neu und versuchen Sie es erneut.",
      "2. Versuchen Sie ein anderes USB-Kabel oder einen anderen Port.",
      "3. Melden Sie den Fehler unter GitHub Issues."
    )

    SuccessTitle     = "MimicEase wurde erfolgreich installiert!"
    SuccessSteps     = @(
      "Starten Sie die 'MimicEase'-App auf Ihrem Smartphone.",
      "Folgen Sie dem Einrichtungsbildschirm, um die Kameraberechtigung zu erteilen.",
      "Einstellungen > Barrierefreiheit > finden Sie MimicEase und aktivieren Sie den Dienst."
    )
    SuccessHelp      = "Hilfe"
    SuccessHelpUrl   = "Hinterlassen Sie eine Nachricht auf GitHub Issues"
    InputEmpty       = "Das Eingabefeld darf nicht leer sein."
  }

  "pt" = @{
    LangName         = "Português"
    AppTitle         = "Instalador do MimicEase"
    AppSubtitle      = "Controle seu telefone com expressões faciais"
    AppDesc          = "Este instalador usa ADB (Android Debug Bridge) para instalar o MimicEase sem conexão com a internet."
    PressEnter       = "Pressione Enter para continuar..."
    PressEnterExit   = "Pressione Enter para sair..."
    Checking         = "Verificando"
    Verified         = "Verificado"
    Error            = "ERRO"
    Warning          = "AVISO"
    Done             = "CONCLUÍDO"
    Waiting          = "Aguardando"

    NoAdb            = "adb.exe não encontrado."
    NoAdbHint        = "Certifique-se de que o arquivo ZIP foi extraído corretamente."
    NoApk            = "Arquivo APK não encontrado."
    NoApkHint        = "O arquivo MimicEase-*.apk deve estar na mesma pasta deste script."
    AdbPath          = "Caminho do ADB"
    ApkFile          = "Arquivo APK"

    Step1Title       = "Passo 1: Ativar Opções do desenvolvedor"
    Step1Desc        = "Para instalar via ADB, as Opções do desenvolvedor precisam estar ativadas no seu dispositivo Android."
    Step1Steps       = @(
      "Abra o app 'Configurações' no seu celular.",
      "Vá em 'Sobre o telefone' ou 'Informações do dispositivo'.",
      "Toque em 'Informações do software' (alguns dispositivos não têm esta etapa).",
      "Toque em 'Número da versão' 7 vezes seguidas.",
      "Digite seu PIN/padrão/senha. Você verá 'Você agora é um desenvolvedor!'."
    )
    Step1Brands      = @(
      "Samsung: Configurações > Sobre o telefone > Informações do software > Número da versão",
      "Google Pixel: Configurações > Sobre o telefone > Número da versão",
      "Outros: Pesquise 'Número da versão' nas Configurações."
    )
    Step1BrandsTitle = "Caminho por fabricante"
    Step1Skip        = "Se as Opções do desenvolvedor já estiverem ativas, você pode pular esta etapa."
    Step1Done        = "Pressione Enter após ativar as Opções do desenvolvedor..."

    ConnTitle        = "Escolha o método de conexão"
    ConnUsb          = "Cabo USB (recomendado, compatível com todas as versões do Android)"
    ConnWifi         = "Wi-Fi sem fio (requer Android 11 ou superior)"
    ConnPrompt       = "Digite 1 ou 2"

    UsbTitle         = "Passo 2 (USB): Conectar o dispositivo"
    UsbSteps         = @(
      "Conecte seu dispositivo Android ao PC com um cabo USB.",
      "Se aparecer 'Confiar neste computador?' no dispositivo, toque em 'Permitir'.",
      "Altere o tipo de conexão USB para 'Transferência de arquivos (MTP)'."
    )
    UsbDebugTitle    = "Ativar Depuração USB"
    UsbDebugSteps    = @(
      "Configurações > Opções do desenvolvedor > ative 'Depuração USB'.",
      "Toque em 'OK' no aviso 'Permitir depuração USB?'."
    )
    UsbDone          = "Pressione Enter após concluir as etapas acima..."
    UsbDetecting     = "Detectando dispositivo..."
    UsbFound         = "Dispositivo conectado"
    UsbUnauth        = "Dispositivo detectado, mas autorização necessária."
    UsbUnauthHint    = "Verifique o aviso 'Permitir depuração USB' na tela do celular e toque em 'Permitir'."
    UsbUnauthHint2   = "Se nenhum aviso aparecer, desconecte e reconecte o cabo USB."
    UsbOffline       = "O dispositivo está offline. Verifique o cabo USB."
    UsbTimeout       = "Não foi possível detectar o dispositivo. (tempo limite de 60 segundos)"
    UsbTimeoutFix    = @(
      "Certifique-se de que o cabo USB suporta transferência de dados (cabos só de carregamento não funcionam).",
      "Tente uma porta USB diferente.",
      "Confirme que a Depuração USB está ativada no dispositivo.",
      "Verifique se o driver USB do fabricante do celular está instalado."
    )

    WifiTitle        = "Passo 2 (Wi-Fi): Configurar Depuração sem fio"
    WifiReq          = "A instalação por Wi-Fi requer Android 11 (R) ou superior."
    WifiReq2         = "O dispositivo e o PC devem estar na mesma rede Wi-Fi."
    WifiEnableTitle  = "Ativar Depuração sem fio"
    WifiEnableSteps  = @(
      "Configurações > Opções do desenvolvedor > ative 'Depuração sem fio'.",
      "Toque em 'Permitir' no aviso 'Permitir depuração sem fio nesta rede?'."
    )
    WifiEnableDone   = "Pressione Enter após ativar a Depuração sem fio..."
    WifiPairTitle         = "Parear dispositivo com código de pareamento"
    WifiAlreadyPairedQ    = "Este PC já está pareado com o dispositivo? (y/N)"
    WifiAlreadyPairedSkip = "Já pareado — etapa de pareamento ignorada."
    WifiPairSteps    = @(
      "Toque no texto 'Depuração sem fio' (não no botão de alternância).",
      "Toque em 'Parear dispositivo com código de pareamento'.",
      "Anote o 'Endereço IP e porta' e o 'código de pareamento de 6 dígitos'."
    )
    WifiPairWarning  = "A porta de pareamento e a porta de conexão são números diferentes."
    WifiPairAddrPrompt = "Digite o IP:porta de pareamento (ex: 192.168.1.100:37491)"
    WifiPairCodePrompt = "Digite o código de pareamento de 6 dígitos"
    WifiPairing      = "Pareando..."
    WifiPairOk       = "Pareamento concluído!"
    WifiPairFail     = "Falha no pareamento."
    WifiPairFixTitle = "Solução de problemas"
    WifiPairFix      = @(
      "Verifique novamente o endereço IP e o número da porta.",
      "Códigos de pareamento expiram. Feche a tela e abra novamente para obter um novo.",
      "Certifique-se de que o dispositivo e o PC estão na mesma rede Wi-Fi."
    )
    WifiConnTitle    = "Conectar ao dispositivo"
    WifiConnDesc     = "Agora digite o 'IP:porta de conexão' (diferente da porta de pareamento)."
    WifiConnSteps    = @(
      "Feche a tela de pareamento e volte para a tela principal de Depuração sem fio.",
      "Anote o número da porta mostrado em 'Endereço IP e porta'."
    )
    WifiConnAddrPrompt = "Digite o IP:porta de conexão (ex: 192.168.1.100:41391)"
    WifiConnecting   = "Conectando..."
    WifiConnOk       = "Dispositivo conectado!"
    WifiConnFail     = "Falha ao conectar ao dispositivo."
    WifiConnFix      = @(
      "Certifique-se de que a porta de conexão é diferente da porta de pareamento.",
      "Verifique se o firewall não está bloqueando essa porta.",
      "Desative a Depuração sem fio, ative novamente e tente outra vez."
    )

    InstallTitle     = "Instalando APK"
    InstallFile      = "Arquivo"
    InstallTarget    = "Dispositivo alvo"
    Installing       = "Instalando, aguarde..."
    InstallAllow     = "(Se aparecer um aviso de instalação no dispositivo, toque em 'Permitir'.)"
    InstallOk        = "Instalação concluída!"
    InstallFail      = "Falha na instalação."
    InstallErrRestriction = @(
      "Causa: A política do dispositivo bloqueia a instalação de fontes desconhecidas.",
      "Solução: Configurações > Segurança > Ative 'Fontes desconhecidas' ou 'Instalar apps desconhecidos'."
    )
    InstallErrExists = @(
      "Causa: A versão instalada tem uma assinatura diferente.",
      "Solução: Desinstale o app MimicEase existente e tente novamente."
    )
    InstallErrStorage = @(
      "Causa: Espaço de armazenamento insuficiente.",
      "Solução: Libere espaço e tente novamente."
    )
    InstallErrGeneral = @(
      "1. Reinicie o dispositivo e tente novamente.",
      "2. Tente um cabo USB ou porta diferente.",
      "3. Relate o erro no GitHub Issues."
    )

    SuccessTitle     = "MimicEase foi instalado com sucesso!"
    SuccessSteps     = @(
      "Abra o app 'MimicEase' no seu celular.",
      "Siga a tela de integração para permitir a permissão da câmera.",
      "Configurações > Acessibilidade > encontre MimicEase e ative o serviço."
    )
    SuccessHelp      = "Ajuda"
    SuccessHelpUrl   = "Deixe uma mensagem no GitHub Issues"
    InputEmpty       = "O campo não pode estar vazio."
  }
}

# ─── Language Detection & Selection ──────────────────────────

$LangCodes   = @("en","ko","ja","zh-cn","zh-tw","es","fr","de","pt")
$LangNumbers = @("1","2","3","4","5","6","7","8","9")

function Get-DefaultLang {
    try {
        $culture = (Get-UICulture).TwoLetterISOLanguageName.ToLower()
        switch ($culture) {
            "ko" { return "ko" }
            "ja" { return "ja" }
            "zh" {
                $full = (Get-UICulture).Name.ToLower()
                if ($full -match "tw|hk|mo") { return "zh-tw" }
                return "zh-cn"
            }
            "es" { return "es" }
            "fr" { return "fr" }
            "de" { return "de" }
            "pt" { return "pt" }
            default { return "en" }
        }
    } catch {
        return "en"
    }
}

function Select-Language {
    $defaultLang = Get-DefaultLang
    $defaultIdx  = $LangCodes.IndexOf($defaultLang) + 1

    Clear-Host
    Write-Host ""
    Write-Host "  ══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "    $($AllStrings['en'].SelectLang)" -ForegroundColor Cyan
    Write-Host "  ══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""

    for ($i = 0; $i -lt $LangCodes.Count; $i++) {
        $num  = $i + 1
        $code = $LangCodes[$i]
        $name = $AllStrings[$code].LangName
        if ($num -eq $defaultIdx) {
            Write-Host "  [$num] $name  ◀ default" -ForegroundColor Green
        } else {
            Write-Host "  [$num] $name" -ForegroundColor White
        }
    }

    Write-Host ""
    Write-Host "  $($AllStrings['en'].LangDetected): $($AllStrings[$defaultLang].LangName)" -ForegroundColor DarkGray
    Write-Host "  $($AllStrings['en'].LangPrompt)" -ForegroundColor DarkGray
    Write-Host ""

    $input = Read-Host "  >"
    $input = $input.Trim()

    if ([string]::IsNullOrEmpty($input)) {
        return $defaultLang
    }
    if ($LangNumbers -contains $input) {
        return $LangCodes[[int]$input - 1]
    }
    return $defaultLang
}

# ─── Helper Functions ─────────────────────────────────────────

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "  ══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "    $Text" -ForegroundColor Cyan
    Write-Host "  ══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step {
    param([int]$Num, [string]$Text)
    Write-Host "    [$Num] $Text" -ForegroundColor Yellow
}

function Write-BulletList {
    param([string[]]$Items)
    foreach ($item in $Items) {
        Write-Host "    • $item" -ForegroundColor DarkGray
    }
}

function Write-NumberedList {
    param([string[]]$Items)
    for ($i = 0; $i -lt $Items.Count; $i++) {
        Write-Host "    $($i+1). $($Items[$i])" -ForegroundColor DarkGray
    }
}

function Write-Ok { param([string]$T); Write-Host "  [$($S.Done)] $T" -ForegroundColor Green }
function Write-Err { param([string]$T); Write-Host "  [$($S.Error)] $T" -ForegroundColor Red }
function Write-Warn { param([string]$T); Write-Host "  [$($S.Warning)] $T" -ForegroundColor Yellow }
function Write-Info { param([string]$T); Write-Host "  $T" -ForegroundColor DarkGray }

function Pause-Enter {
    param([string]$Msg = "")
    if ([string]::IsNullOrEmpty($Msg)) { $Msg = $S.PressEnter }
    Write-Host ""
    Write-Host "  >> $Msg" -ForegroundColor DarkGray
    Read-Host | Out-Null
}

# ─── Prerequisite Check ───────────────────────────────────────

function Assert-Prerequisites {
    Write-Host "  $($S.Checking) ADB..." -ForegroundColor DarkGray
    if (-not (Test-Path $AdbPath)) {
        Write-Err $S.NoAdb
        Write-Info $S.NoAdbHint
        Write-Info "  $($S.AdbPath): $AdbPath"
        Pause-Enter $S.PressEnterExit
        exit 1
    }
    Write-Ok "$($S.AdbPath): $AdbPath"

    Write-Host "  $($S.Checking) APK..." -ForegroundColor DarkGray
    if ($null -eq $ApkFile) {
        Write-Err $S.NoApk
        Write-Info $S.NoApkHint
        Pause-Enter $S.PressEnterExit
        exit 1
    }
    Write-Ok "$($S.ApkFile): $($ApkFile.Name)"
}

# ─── Welcome Screen ───────────────────────────────────────────

function Show-Welcome {
    Clear-Host
    Write-Host ""
    Write-Host "  ╔══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "  ║                                                      ║" -ForegroundColor Cyan
    Write-Host ("  ║   {0,-52}║" -f "  $($S.AppTitle)") -ForegroundColor Cyan
    Write-Host ("  ║   {0,-52}║" -f "  $($S.AppSubtitle)") -ForegroundColor DarkGray
    Write-Host "  ║                                                      ║" -ForegroundColor Cyan
    Write-Host "  ╚══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Info $S.AppDesc
    Write-Host ""

    Assert-Prerequisites
    Pause-Enter
}

# ─── Developer Options Guide ──────────────────────────────────

function Show-DeveloperOptions {
    Write-Header $S.Step1Title
    Write-Host "  $($S.Step1Desc)" -ForegroundColor White
    Write-Host ""

    for ($i = 0; $i -lt $S.Step1Steps.Count; $i++) {
        Write-Step ($i + 1) $S.Step1Steps[$i]
    }

    Write-Host ""
    Write-Host "  ── $($S.Step1BrandsTitle) ──" -ForegroundColor Yellow
    Write-BulletList $S.Step1Brands
    Write-Host ""
    Write-Warn $S.Step1Skip

    Pause-Enter $S.Step1Done
}

# ─── Connection Method Menu ───────────────────────────────────

function Show-ConnectionMenu {
    Write-Header $S.ConnTitle
    Write-Host "  [1] $($S.ConnUsb)" -ForegroundColor White
    Write-Host "  [2] $($S.ConnWifi)" -ForegroundColor White
    Write-Host ""

    do {
        $choice = Read-Host "  $($S.ConnPrompt)"
    } while ($choice.Trim() -notin @("1","2"))

    return $choice.Trim()
}

# ─── USB Installation Flow ────────────────────────────────────

function Install-ViaUsb {
    Write-Header $S.UsbTitle

    for ($i = 0; $i -lt $S.UsbSteps.Count; $i++) {
        Write-Step ($i + 1) $S.UsbSteps[$i]
    }
    Write-Host ""
    Write-Host "  ── $($S.UsbDebugTitle) ──" -ForegroundColor Yellow
    for ($i = 0; $i -lt $S.UsbDebugSteps.Count; $i++) {
        Write-Step ($i + 1) $S.UsbDebugSteps[$i]
    }

    Pause-Enter $S.UsbDone

    Write-Header $S.UsbDetecting

    $maxAttempts  = 12
    $attempt      = 0
    $deviceSerial = $null

    while ($attempt -lt $maxAttempts) {
        $attempt++
        $output  = & $AdbPath devices 2>&1
        $lines   = ($output -split "`n") | Where-Object { $_ -notmatch "^List" -and $_ -match "\t" }

        $unauthorized = $lines | Where-Object { $_ -match "unauthorized" }
        $offline      = $lines | Where-Object { $_ -match "offline" }
        $ready        = @($lines | Where-Object { $_ -match "\tdevice$" })

        if ($ready.Count -gt 0) {
            $deviceSerial = ($ready[0] -split "\t")[0].Trim()
            Write-Ok "$($S.UsbFound): $deviceSerial"
            break
        } elseif ($unauthorized) {
            Write-Warn $S.UsbUnauth
            Write-Info $S.UsbUnauthHint
            Write-Info $S.UsbUnauthHint2
        } elseif ($offline) {
            Write-Warn $S.UsbOffline
        } else {
            Write-Host "  ($attempt/$maxAttempts) $($S.Waiting)..." -ForegroundColor DarkGray
        }

        Start-Sleep -Seconds 5
    }

    if ($null -eq $deviceSerial) {
        Write-Err $S.UsbTimeout
        Write-Host ""
        Write-NumberedList $S.UsbTimeoutFix
        Pause-Enter $S.PressEnterExit
        exit 1
    }

    Install-Apk -Serial $deviceSerial
}

# ─── Wi-Fi Installation Flow ──────────────────────────────────

function Install-ViaWifi {
    Write-Header $S.WifiTitle
    Write-Host "  $($S.WifiReq)" -ForegroundColor White
    Write-Host "  $($S.WifiReq2)" -ForegroundColor White
    Write-Host ""
    Write-Host "  ── $($S.WifiEnableTitle) ──" -ForegroundColor Yellow
    for ($i = 0; $i -lt $S.WifiEnableSteps.Count; $i++) {
        Write-Step ($i + 1) $S.WifiEnableSteps[$i]
    }

    Pause-Enter $S.WifiEnableDone

    Write-Header $S.WifiPairTitle
    Write-Host ""
    $alreadyPaired = (Read-Host "  $($S.WifiAlreadyPairedQ)").Trim()
    $skipPairing   = $alreadyPaired -match '^[yYjJoO]'

    if ($skipPairing) {
        Write-Ok $S.WifiAlreadyPairedSkip
        Write-Host ""
    } else {
        for ($i = 0; $i -lt $S.WifiPairSteps.Count; $i++) {
            Write-Step ($i + 1) $S.WifiPairSteps[$i]
        }
        Write-Host ""
        Write-Warn $S.WifiPairWarning
        Write-Host ""

        do {
            $pairAddr = (Read-Host "  $($S.WifiPairAddrPrompt)").Trim()
            if ([string]::IsNullOrEmpty($pairAddr)) { Write-Warn $S.InputEmpty }
        } while ([string]::IsNullOrEmpty($pairAddr))

        do {
            $pairCode = (Read-Host "  $($S.WifiPairCodePrompt)").Trim()
            if ([string]::IsNullOrEmpty($pairCode)) { Write-Warn $S.InputEmpty }
        } while ([string]::IsNullOrEmpty($pairCode))

        Write-Host ""
        Write-Info $S.WifiPairing
        $pairResult = & $AdbPath pair $pairAddr $pairCode 2>&1
        Write-Info $pairResult

        if ($LASTEXITCODE -ne 0 -or ($pairResult -join "") -match "failed|error|Failed") {
            Write-Err $S.WifiPairFail
            Write-Host ""
            Write-Host "  ── $($S.WifiPairFixTitle) ──" -ForegroundColor Yellow
            Write-NumberedList $S.WifiPairFix
            Pause-Enter $S.PressEnterExit
            exit 1
        }

        Write-Ok $S.WifiPairOk
        Write-Host ""
    }

    Write-Header $S.WifiConnTitle
    Write-Host "  $($S.WifiConnDesc)" -ForegroundColor White
    Write-Host ""
    for ($i = 0; $i -lt $S.WifiConnSteps.Count; $i++) {
        Write-Step ($i + 1) $S.WifiConnSteps[$i]
    }
    Write-Host ""

    do {
        $connAddr = (Read-Host "  $($S.WifiConnAddrPrompt)").Trim()
        if ([string]::IsNullOrEmpty($connAddr)) { Write-Warn $S.InputEmpty }
    } while ([string]::IsNullOrEmpty($connAddr))

    Write-Host ""
    Write-Info $S.WifiConnecting
    $connResult = & $AdbPath connect $connAddr 2>&1
    Write-Info $connResult

    if ($LASTEXITCODE -ne 0 -or ($connResult -join "") -match "failed|refused|cannot|error") {
        Write-Err $S.WifiConnFail
        Write-Host ""
        Write-NumberedList $S.WifiConnFix
        Pause-Enter $S.PressEnterExit
        exit 1
    }

    # $connAddr is the ADB serial for Wi-Fi devices (e.g. 192.168.1.100:41391)
    Write-Ok "$($S.WifiConnOk) [$connAddr]"

    Install-Apk -Serial $connAddr
}

# ─── Common APK Install Function ──────────────────────────────

function Install-Apk {
    param([string]$Serial)

    Write-Header $S.InstallTitle
    Write-Host "  $($S.InstallFile): $($ApkFile.Name)" -ForegroundColor White
    Write-Host "  $($S.InstallTarget): $Serial" -ForegroundColor White
    Write-Host ""
    Write-Info $S.Installing
    Write-Warn $S.InstallAllow
    Write-Host ""

    $result = & $AdbPath -s $Serial install -r $ApkFile.FullName 2>&1
    Write-Info ($result -join "`n")

    if ($LASTEXITCODE -eq 0 -and ($result -join "") -match "Success") {
        Show-Success
    } else {
        Write-Err $S.InstallFail
        Write-Host ""
        $resultStr = $result -join ""

        if ($resultStr -match "INSTALL_FAILED_USER_RESTRICTION") {
            Write-BulletList $S.InstallErrRestriction
        } elseif ($resultStr -match "INSTALL_FAILED_ALREADY_EXISTS") {
            Write-BulletList $S.InstallErrExists
        } elseif ($resultStr -match "INSTALL_FAILED_INSUFFICIENT_STORAGE") {
            Write-BulletList $S.InstallErrStorage
        } else {
            Write-NumberedList $S.InstallErrGeneral
        }

        Pause-Enter $S.PressEnterExit
        exit 1
    }
}

# ─── Success Screen ───────────────────────────────────────────

function Show-Success {
    Write-Host ""
    Write-Host "  ╔══════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host ("  ║  {0,-54}║" -f $S.SuccessTitle) -ForegroundColor Green
    Write-Host "  ╚══════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""

    for ($i = 0; $i -lt $S.SuccessSteps.Count; $i++) {
        Write-Step ($i + 1) $S.SuccessSteps[$i]
    }

    Write-Host ""
    Write-Host "  ── $($S.SuccessHelp) ──" -ForegroundColor DarkGray
    Write-Host "  GitHub: https://github.com/CrowKing63/MimicEase" -ForegroundColor DarkGray
    Write-Host "  $($S.SuccessHelpUrl)" -ForegroundColor DarkGray
    Write-Host ""

    Pause-Enter $S.PressEnterExit
}

# ─── Main Execution ───────────────────────────────────────────

$selectedLang = Select-Language
$S = $AllStrings[$selectedLang]   # $S is the active string table

& $AdbPath start-server 2>&1 | Out-Null

Show-Welcome
Show-DeveloperOptions

$connChoice = Show-ConnectionMenu

if ($connChoice -eq "1") {
    Install-ViaUsb
} else {
    Install-ViaWifi
}
