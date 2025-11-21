# デバッグ環境（サーバー、クライアント、FakeClient）を一括終了するスクリプト

Write-Host "Stopping Ghost Debug Environment..."

# 終了対象のウィンドウタイトル定義
$targets = @(
    "Ghost Debug - Server",
    "Ghost Debug - Fake Client",
    "Ghost Debug - Mod Client"
)

foreach ($title in $targets) {
    # ウィンドウタイトルが一致するプロセス（PowerShell）を取得
    $procs = Get-Process | Where-Object { $_.MainWindowTitle -eq $title }
    
    if ($procs) {
        Write-Host "Stopping window: $title"
        foreach ($proc in $procs) {
            try {
                Stop-Process -Id $proc.Id -Force -ErrorAction Stop
                Write-Host "  Closed window (PID: $($proc.Id))"
            } catch {
                Write-Warning "  Failed to close window (PID: $($proc.Id))."
            }
        }
    } else {
        Write-Host "$title is not running."
    }
}

# 念のため、Javaプロセス自体もクリーンアップ（ウィンドウなしで残っている場合など）
$javaTargets = @("GhostModServer", "FakeClientMain", "KnotClient")
$javaProcs = Get-CimInstance Win32_Process -Filter "Name LIKE 'java%.exe'"

foreach ($targetName in $javaTargets) {
    $targetProcs = $javaProcs | Where-Object { $_.CommandLine -like "*$targetName*" }
    if ($targetProcs) {
        Write-Host "Cleaning up lingering Java process: $targetName"
        foreach ($proc in $targetProcs) {
            try {
                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
                Write-Host "  Stopped Java process (PID: $($proc.ProcessId))"
            } catch {}
        }
    }
}

Write-Host "Done."
