param(
    [string]$Arquivo,
    [switch]$Gravar,
    [switch]$SemVoz,
    [string]$BaseUrl = "http://localhost:8081/api/v1"
)

$ErrorActionPreference = "Stop"

$FORMATOS = @(".mp3", ".mp4", ".mpeg", ".mpga", ".m4a", ".wav", ".webm", ".ogg", ".flac")

function Find-AudioMaisRecente {
    Get-ChildItem -File |
        Where-Object { $FORMATOS -contains $_.Extension.ToLower() -and $_.Name -notlike "resposta*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-Audio([string]$caminho) {
    $bytes = [IO.File]::ReadAllBytes($caminho)
    $nome = [IO.Path]::GetFileName($caminho)
    $boundary = [Guid]::NewGuid().ToString()
    $LF = "`r`n"

    $cabecalho = [Text.Encoding]::UTF8.GetBytes(
        "--$boundary$LF" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"$nome`"$LF" +
        "Content-Type: application/octet-stream$LF$LF")
    $rodape = [Text.Encoding]::UTF8.GetBytes("$LF--$boundary--$LF")

    $url = "$BaseUrl/assistant/audio"
    if ($SemVoz) { $url += "?speak=false" }

    $req = [Net.HttpWebRequest]::Create($url)
    $req.Method = "POST"
    $req.ContentType = "multipart/form-data; boundary=$boundary"
    $req.Timeout = 180000
    $req.ContentLength = $cabecalho.Length + $bytes.Length + $rodape.Length

    $fluxo = $req.GetRequestStream()
    $fluxo.Write($cabecalho, 0, $cabecalho.Length)
    $fluxo.Write($bytes, 0, $bytes.Length)
    $fluxo.Write($rodape, 0, $rodape.Length)
    $fluxo.Close()

    try {
        $resp = $req.GetResponse()
        return ((New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd() | ConvertFrom-Json)
    }
    catch {
        $r = $_.Exception.Response
        if ($null -eq $r) { throw "A aplicacao nao respondeu. Ela esta rodando? (.\gradlew.bat bootRun)" }
        $corpo = (New-Object IO.StreamReader($r.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        throw "Erro $([int]$r.StatusCode): $corpo"
    }
}

Write-Host ""
Write-Host "  SmartBudget AI - teste por voz" -ForegroundColor Cyan
Write-Host "  ------------------------------" -ForegroundColor Cyan

if ($Gravar) {
    Write-Host ""
    Write-Host "  Grave seu comando, salve o arquivo nesta pasta e rode: .\falar.ps1" -ForegroundColor Yellow
    Write-Host "  Pasta: $(Get-Location)"
    Write-Host ""
    try { Start-Process "ms-voicerecorder:" } catch { Start-Process "soundrecorder.exe" -ErrorAction SilentlyContinue }
    return
}

if ($Arquivo) {
    if (-not (Test-Path $Arquivo)) { throw "Arquivo nao encontrado: $Arquivo" }
    $entrada = (Resolve-Path $Arquivo).Path
}
else {
    $encontrado = Find-AudioMaisRecente
    if (-not $encontrado) {
        Write-Host ""
        Write-Host "  Nenhum arquivo de audio nesta pasta." -ForegroundColor Red
        Write-Host ""
        Write-Host "    .\falar.ps1 -Gravar        abre o Gravador de Voz do Windows"
        Write-Host "    http://localhost:8081      console web com microfone"
        Write-Host ""
        Write-Host "  Formatos aceitos: $($FORMATOS -join ', ')" -ForegroundColor DarkGray
        Write-Host ""
        return
    }
    $entrada = $encontrado.FullName
}

Write-Host "  Arquivo: $([IO.Path]::GetFileName($entrada)) ($([math]::Round((Get-Item $entrada).Length / 1KB)) KB)" -ForegroundColor DarkGray
Write-Host "  Enviando..." -ForegroundColor Yellow

$resultado = Send-Audio $entrada

Write-Host ""
Write-Host "  VOCE DISSE : " -ForegroundColor Green -NoNewline
Write-Host $resultado.transcription
Write-Host "  A IA DISSE : " -ForegroundColor Cyan -NoNewline
Write-Host $resultado.message
Write-Host ""

if ($resultado.audioBase64) {
    $saida = Join-Path (Get-Location) "resposta.mp3"
    [IO.File]::WriteAllBytes($saida, [Convert]::FromBase64String($resultado.audioBase64))
    Start-Process $saida
}

Write-Host ""
