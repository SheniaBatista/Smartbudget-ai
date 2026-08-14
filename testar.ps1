param(
    [string]$BaseUrl = "http://localhost:8081/api/v1"
)

$ErrorActionPreference = "Stop"

function Invoke-Assistant([string]$Mensagem) {
    $payload = @{ message = $Mensagem } | ConvertTo-Json -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($payload)

    $req = [Net.HttpWebRequest]::Create("$BaseUrl/assistant/message")
    $req.Method = "POST"
    $req.ContentType = "application/json; charset=utf-8"
    $req.Timeout = 120000
    $req.ContentLength = $bytes.Length

    $stream = $req.GetRequestStream()
    $stream.Write($bytes, 0, $bytes.Length)
    $stream.Close()

    try {
        $resp = $req.GetResponse()
        $texto = (New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        return ($texto | ConvertFrom-Json).message
    }
    catch {
        $resposta = $_.Exception.Response
        if ($null -eq $resposta) { return "A aplicacao nao respondeu. Ela esta rodando? (.\gradlew.bat bootRun)" }
        $texto = (New-Object IO.StreamReader($resposta.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        return "[erro $([int]$resposta.StatusCode)] $texto"
    }
}

function Show-Banco {
    Write-Host "`n--- transacoes gravadas ---" -ForegroundColor DarkGray
    docker exec smartbudget-mysql mysql -uapp -papp smart_budget `
        -e "SELECT description AS descricao, amount AS valor, type AS tipo, category AS categoria, occurred_at AS data FROM transactions ORDER BY created_at;" 2>$null
}

Write-Host ""
Write-Host "  SmartBudget AI" -ForegroundColor Cyan
Write-Host "  --------------" -ForegroundColor Cyan
Write-Host "  Escreva um comando em portugues e tecle Enter."
Write-Host ""
Write-Host "  Exemplos:" -ForegroundColor DarkGray
Write-Host "    Registre uma despesa de 85 reais com Uber" -ForegroundColor DarkGray
Write-Host "    Recebi meu salario de 5000 reais"          -ForegroundColor DarkGray
Write-Host "    Qual e o meu saldo?"                       -ForegroundColor DarkGray
Write-Host "    Quanto eu gastei com alimentacao?"         -ForegroundColor DarkGray
Write-Host "    Faca um resumo financeiro deste mes"       -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Comandos: 'banco' mostra a tabela | 'sair' encerra" -ForegroundColor DarkGray
Write-Host ""

while ($true) {
    Write-Host "voce > " -ForegroundColor Green -NoNewline
    $entrada = Read-Host

    if ([string]::IsNullOrWhiteSpace($entrada)) { continue }
    if ($entrada -in @("sair", "exit", "quit")) { break }
    if ($entrada -eq "banco") { Show-Banco; Write-Host ""; continue }

    Write-Host "IA   > " -ForegroundColor Cyan -NoNewline
    Write-Host (Invoke-Assistant $entrada)
    Write-Host ""
}
