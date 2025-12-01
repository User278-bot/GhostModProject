# サーバー、クライアント、FakeClientを一括起動するスクリプト

# 1. サーバー起動
Write-Host "Starting Ghost Server..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Server'; ./gradlew :ghost-server:run"

# サーバーの起動を待つ（適宜調整）
Start-Sleep -Seconds 10

# 2. Modクライアント起動
Write-Host "Starting Ghost Mod Client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Mod Client'; ./gradlew :ghost-mod:1.19.2:runClient"

# クライアントの起動を待つ
Start-Sleep -Seconds 30

# 3. FakeClient起動
Write-Host "Starting Fake Client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Fake Client'; ./gradlew :ghost-fake_client:run --args='--uri ws://localhost:8887 --uuid 36945147-4e98-48e7-abee-23469a298984'"

Start-Sleep -Seconds 10

Write-Host "Starting Fake Client..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle = 'Ghost Debug - Fake Client'; ./gradlew :ghost-fake_client:run --args='--uri ws://localhost:8887 --uuid b56303be-03ae-4ee6-ace3-1cccc4c971a8'"

Write-Host "All processes started."
