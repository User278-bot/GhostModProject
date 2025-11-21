# サーバー、クライアント、FakeClientを一括起動するスクリプト

# 1. サーバー起動
Write-Host "Starting Ghost Server..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Server'; ./gradlew :ghost-server:run"

# サーバーの起動を待つ（適宜調整）
Start-Sleep -Seconds 10

# 2. Modクライアント起動
Write-Host "Starting Ghost Mod Client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Mod Client'; ./gradlew :ghost-mod:runClient"

# クライアントの起動を待つ
Start-Sleep -Seconds 10

# 3. FakeClient起動
Write-Host "Starting Fake Client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Fake Client'; ./gradlew :ghost-fake_client:run"

Write-Host "All processes started."
