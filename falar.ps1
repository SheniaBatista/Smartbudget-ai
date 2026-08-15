param(
    [string]$Arquivo,
    [switch]$Gravar,
    [switch]$SemVoz,
    [string]$BaseUrl = "http://localhost:8081/api/v1"
)

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }

$Ink    = "Gray"
$Muted  = "DarkGray"
$Accent = "Cyan"
$Good   = "Green"
$Bad    = "Red"

$FORMATOS = @(".mp3", ".mp4", ".mpeg", ".mpga", ".m4a", ".wav", ".webm", ".ogg", ".flac")

function Find-AudioMaisRecente {
    Get-ChildItem -File |
        Where-Object { $FORMATOS -contains $_.Extension.ToLower() -and $_.Name -notlike "resposta*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Get-LarguraConsole {
    try { return [Console]::WindowWidth } catch { return 100 }
}

function Write-Wrapped([string]$Texto, [string]$Prefixo, [ConsoleColor]$Cor) {
    $largura = [Math]::Min((Get-LarguraConsole) - $Prefixo.Length - 2, 96)
    if ($largura -lt 30) { $largura = 30 }
    $recuo = " " * $Prefixo.Length
    $primeira = $true
    $linha = ""

    foreach ($palavra in ($Texto -split "\s+")) {
        if ($linha.Length -gt 0 -and ($linha.Length + 1 + $palavra.Length) -gt $largura) {
            Write-Host $(if ($primeira) { $Prefixo } else { $recuo }) -NoNewline -ForegroundColor $Cor
            Write-Host $linha
            $primeira = $false
            $linha = $palavra
        }
        elseif ($linha.Length -eq 0) { $linha = $palavra }
        else { $linha += " " + $palavra }
    }
    if ($linha.Length -gt 0) {
        Write-Host $(if ($primeira) { $Prefixo } else { $recuo }) -NoNewline -ForegroundColor $Cor
        Write-Host $linha
    }
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
        $texto = (New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        $resp.Close()
        return $texto | ConvertFrom-Json
    }
    catch {
        $r = $_.Exception.Response
        if ($null -eq $r) { throw "A aplicação não respondeu. Ela está rodando? (.\gradlew.bat bootRun)" }
        $corpo = (New-Object IO.StreamReader($r.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        $erro = try { ($corpo | ConvertFrom-Json).message } catch { $corpo }
        throw $erro
    }
}

$CAIXA = 48

function Write-BoxLine([string]$Forte, [string]$Fraco) {
    $pad = $CAIXA - 2 - $Forte.Length - $Fraco.Length
    if ($pad -lt 0) { $pad = 0 }
    Write-Host "  │  " -NoNewline -ForegroundColor $Accent
    Write-Host $Forte -NoNewline -ForegroundColor White
    Write-Host $Fraco -NoNewline -ForegroundColor $Muted
    Write-Host ((" " * $pad) + "│") -ForegroundColor $Accent
}

Write-Host ""
Write-Host ("  ┌" + ("─" * $CAIXA) + "┐") -ForegroundColor $Accent
Write-BoxLine "SmartBudget AI" "  ·  teste por voz"
Write-Host ("  └" + ("─" * $CAIXA) + "┘") -ForegroundColor $Accent
Write-Host ""

if ($Gravar) {
    Write-Host "  Grave seu comando, salve nesta pasta e rode " -NoNewline -ForegroundColor $Muted
    Write-Host ".\falar.ps1" -ForegroundColor $Ink
    Write-Host "  Pasta: $(Get-Location)" -ForegroundColor $Muted
    Write-Host ""
    try { Start-Process "ms-voicerecorder:" } catch { Start-Process "soundrecorder.exe" -ErrorAction SilentlyContinue }
    return
}

if ($Arquivo) {
    if (-not (Test-Path $Arquivo)) { throw "Arquivo não encontrado: $Arquivo" }
    $entrada = (Resolve-Path $Arquivo).Path
}
else {
    $encontrado = Find-AudioMaisRecente
    if (-not $encontrado) {
        Write-Host "  Nenhum arquivo de áudio nesta pasta." -ForegroundColor $Bad
        Write-Host ""
        Write-Host "    .\falar.ps1 -Gravar   " -NoNewline -ForegroundColor $Ink
        Write-Host "abre o Gravador de Voz do Windows" -ForegroundColor $Muted
        Write-Host "    http://localhost:8081 " -NoNewline -ForegroundColor $Ink
        Write-Host "console web com microfone" -ForegroundColor $Muted
        Write-Host ""
        Write-Host "  Formatos aceitos: $($FORMATOS -join ' ')" -ForegroundColor $Muted
        Write-Host ""
        return
    }
    $entrada = $encontrado.FullName
}

$tamanho = [math]::Round((Get-Item $entrada).Length / 1KB)
Write-Host "  arquivo  " -NoNewline -ForegroundColor $Muted
Write-Host "$([IO.Path]::GetFileName($entrada)) ($tamanho KB)" -ForegroundColor $Ink
Write-Host "  enviando" -NoNewline -ForegroundColor $Muted

try {
    $resultado = Send-Audio $entrada
}
catch {
    Write-Host ("`r" + (" " * 24) + "`r") -NoNewline
    Write-Wrapped $_.Exception.Message "  erro   " $Bad
    Write-Host ""
    return
}

Write-Host ("`r" + (" " * 24) + "`r") -NoNewline

Write-Host ""
Write-Wrapped $resultado.transcription "  você > " $Good
Write-Host ""
Write-Wrapped $resultado.message "  IA   > " $Accent
Write-Host ""

if ($resultado.audioBase64) {
    $saida = Join-Path (Get-Location) "resposta.mp3"
    [IO.File]::WriteAllBytes($saida, [Convert]::FromBase64String($resultado.audioBase64))
    Write-Host "  tocando resposta.mp3" -ForegroundColor $Muted
    Write-Host ""
    Start-Process $saida
}
