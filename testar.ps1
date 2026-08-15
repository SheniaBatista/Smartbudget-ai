param(
    [string]$BaseUrl = "http://localhost:8081/api/v1"
)

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }

$Ink    = "Gray"
$Muted  = "DarkGray"
$Accent = "Cyan"
$Good   = "Green"
$Bad    = "Red"

function Get-Json([string]$Path) {
    $req = [Net.HttpWebRequest]::Create("$BaseUrl$Path")
    $req.Timeout = 15000
    $resp = $req.GetResponse()
    $text = (New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
    $resp.Close()
    return $text | ConvertFrom-Json
}

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
        $text = (New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        $resp.Close()
        return [PSCustomObject]@{ Ok = $true; Texto = ($text | ConvertFrom-Json).message }
    }
    catch {
        $resposta = $_.Exception.Response
        if ($null -eq $resposta) {
            return [PSCustomObject]@{ Ok = $false; Texto = "A aplicacao nao respondeu. Ela esta rodando? (.\gradlew.bat bootRun)" }
        }
        $corpo = (New-Object IO.StreamReader($resposta.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
        $erro = try { ($corpo | ConvertFrom-Json).message } catch { $corpo }
        return [PSCustomObject]@{ Ok = $false; Texto = $erro }
    }
}

function Format-Moeda([decimal]$Valor) {
    return "R$ " + $Valor.ToString("N2", [Globalization.CultureInfo]::GetCultureInfo("pt-BR"))
}

function Get-LarguraConsole {
    try { return [Console]::WindowWidth } catch { return 100 }
}

function Write-Wrapped([string]$Texto, [string]$Prefixo, [ConsoleColor]$Cor) {
    $largura = [Math]::Min((Get-LarguraConsole) - $Prefixo.Length - 2, 96)
    if ($largura -lt 30) { $largura = 30 }
    $recuo = " " * $Prefixo.Length
    $primeira = $true

    foreach ($paragrafo in ($Texto -split "`n")) {
        if ($paragrafo.Trim() -eq "") {
            Write-Host ""
            continue
        }
        $linha = ""
        foreach ($palavra in ($paragrafo -split "\s+")) {
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
            $primeira = $false
        }
    }
}

function Show-Saldo {
    try {
        $b = Get-Json "/finance/balance"
        $s = Get-Json "/finance/summary"

        $corSaldo = if ($b.netBalance -lt 0) { $Bad } else { $Good }

        Write-Host ""
        Write-Host "  Receitas  " -NoNewline -ForegroundColor $Muted
        Write-Host (Format-Moeda $b.totalIncome).PadLeft(16) -ForegroundColor $Ink
        Write-Host "  Despesas  " -NoNewline -ForegroundColor $Muted
        Write-Host (Format-Moeda $b.totalExpense).PadLeft(16) -ForegroundColor $Ink
        Write-Host "  Saldo     " -NoNewline -ForegroundColor $Muted
        Write-Host (Format-Moeda $b.netBalance).PadLeft(16) -ForegroundColor $corSaldo
        Write-Host "            $($s.transactionCount) transações em $($s.period)" -ForegroundColor $Muted
        Write-Host ""
        return $true
    }
    catch {
        Write-Host ""
        Write-Host "  API indisponível. A aplicação está rodando?" -ForegroundColor $Bad
        Write-Host "  .\gradlew.bat bootRun" -ForegroundColor $Muted
        Write-Host ""
        return $false
    }
}

function Show-Categorias {
    try {
        $s = Get-Json "/finance/summary"
        if (-not $s.expensesByCategory -or $s.expensesByCategory.Count -eq 0) {
            Write-Host "`n  Nenhuma despesa registrada neste mês.`n" -ForegroundColor $Muted
            return
        }
        $maior = ($s.expensesByCategory | Measure-Object -Property total -Maximum).Maximum
        Write-Host ""
        foreach ($c in $s.expensesByCategory) {
            $largura = [int]([Math]::Round(($c.total / $maior) * 26))
            if ($largura -lt 1) { $largura = 1 }
            Write-Host ("  {0,-16}" -f $c.categoryLabel) -NoNewline -ForegroundColor $Ink
            Write-Host ("█" * $largura).PadRight(27) -NoNewline -ForegroundColor $Accent
            Write-Host ("{0,14}" -f (Format-Moeda $c.total)) -NoNewline -ForegroundColor $Ink
            Write-Host ("{0,9:N1}%" -f $c.percentageOfExpenses) -ForegroundColor $Muted
        }
        Write-Host ""
    }
    catch {
        Write-Host "`n  Não foi possível carregar as categorias.`n" -ForegroundColor $Bad
    }
}

function Show-Transacoes {
    try {
        $lista = Get-Json "/transactions?limit=10"
        if ($lista.Count -eq 0) {
            Write-Host "`n  Nenhuma transação registrada ainda.`n" -ForegroundColor $Muted
            return
        }
        Write-Host ""
        Write-Host ("  {0,-26} {1,-16} {2,-7} {3,14}" -f "DESCRIÇÃO", "CATEGORIA", "DATA", "VALOR") -ForegroundColor $Muted
        Write-Host ("  " + ("─" * 66)) -ForegroundColor $Muted
        foreach ($t in $lista) {
            $desc = if ($t.description.Length -gt 25) { $t.description.Substring(0, 24) + "…" } else { $t.description }
            $data = ([datetime]$t.occurredAt).ToString("dd/MM")
            $sinal = if ($t.type -eq "INCOME") { "+" } else { "−" }
            $cor = if ($t.type -eq "INCOME") { $Good } else { $Bad }
            Write-Host ("  {0,-26} " -f $desc) -NoNewline -ForegroundColor $Ink
            Write-Host ("{0,-16} " -f $t.categoryLabel) -NoNewline -ForegroundColor $Muted
            Write-Host ("{0,-7} " -f $data) -NoNewline -ForegroundColor $Muted
            Write-Host ("{0,13}" -f "$sinal $(Format-Moeda $t.amount)") -ForegroundColor $cor
        }
        Write-Host ""
    }
    catch {
        Write-Host "`n  Não foi possível carregar as transações.`n" -ForegroundColor $Bad
    }
}

$CAIXA = 48

function Write-BoxLine([string]$Forte, [string]$Fraco) {
    $pad = $CAIXA - 2 - $Forte.Length - $Fraco.Length
    if ($pad -lt 0) { $pad = 0 }
    Write-Host "  │  " -NoNewline -ForegroundColor $Accent
    if ($Forte) { Write-Host $Forte -NoNewline -ForegroundColor White }
    if ($Fraco) { Write-Host $Fraco -NoNewline -ForegroundColor $Muted }
    Write-Host ((" " * $pad) + "│") -ForegroundColor $Accent
}

function Show-Cabecalho {
    Clear-Host
    Write-Host ""
    Write-Host ("  ┌" + ("─" * $CAIXA) + "┐") -ForegroundColor $Accent
    Write-BoxLine "SmartBudget AI" ""
    Write-BoxLine "" "Assistente financeiro por texto e voz"
    Write-Host ("  └" + ("─" * $CAIXA) + "┘") -ForegroundColor $Accent
}

function Show-Ajuda {
    Write-Host "  Escreva um comando em português. Exemplos:" -ForegroundColor $Muted
    Write-Host "    Registre uma despesa de 85 reais com Uber" -ForegroundColor $Muted
    Write-Host "    Quanto gastei com alimentação?" -ForegroundColor $Muted
    Write-Host "    Faça um resumo financeiro deste mês" -ForegroundColor $Muted
    Write-Host ""
    Write-Host "  Comandos:  " -NoNewline -ForegroundColor $Muted
    foreach ($c in @("ajuda","saldo","categorias","extrato","limpar","sair")) {
        Write-Host $c -NoNewline -ForegroundColor $Ink
        if ($c -ne "sair") { Write-Host " · " -NoNewline -ForegroundColor $Muted }
    }
    Write-Host ""
    Write-Host ""
}

function Write-Topico([string]$Titulo) {
    Write-Host ""
    Write-Host "  $Titulo" -ForegroundColor $Accent
    Write-Host ("  " + ("─" * 62)) -ForegroundColor $Muted
}

function Write-Par([string]$Esquerda, [string]$Direita) {
    Write-Host ("    {0,-42}" -f $Esquerda) -NoNewline -ForegroundColor $Ink
    Write-Host $Direita -ForegroundColor $Muted
}

function Show-Guia {
    Write-Host ""
    Write-Host "  ┌────────────────────────────────────────────────┐" -ForegroundColor $Accent
    Write-Host "  │  " -NoNewline -ForegroundColor $Accent
    Write-Host "Guia de uso" -NoNewline -ForegroundColor White
    Write-Host "                                   │" -ForegroundColor $Accent
    Write-Host "  └────────────────────────────────────────────────┘" -ForegroundColor $Accent

    Write-Topico "O QUE ACONTECE QUANDO VOCÊ PEDE ALGO"
    Write-Host "    O modelo interpreta a intenção e escolhe qual ferramenta chamar." -ForegroundColor $Muted
    Write-Host "    A ferramenta executa o caso de uso, que grava ou consulta o MySQL." -ForegroundColor $Muted
    Write-Host ""
    Write-Host "    você " -NoNewline -ForegroundColor $Ink
    Write-Host "→ " -NoNewline -ForegroundColor $Accent
    Write-Host "modelo " -NoNewline -ForegroundColor $Ink
    Write-Host "→ " -NoNewline -ForegroundColor $Accent
    Write-Host "ferramenta " -NoNewline -ForegroundColor $Ink
    Write-Host "→ " -NoNewline -ForegroundColor $Accent
    Write-Host "caso de uso " -NoNewline -ForegroundColor $Ink
    Write-Host "→ " -NoNewline -ForegroundColor $Accent
    Write-Host "MySQL " -NoNewline -ForegroundColor $Ink
    Write-Host "→ " -NoNewline -ForegroundColor $Accent
    Write-Host "resposta" -ForegroundColor $Ink
    Write-Host ""
    Write-Host "    O modelo não calcula nada. Somas e percentuais vêm prontos do SQL." -ForegroundColor $Muted

    Write-Topico "REGISTRAR DINHEIRO"
    Write-Par "Registre uma despesa de 85 reais com Uber" "grava despesa · Transporte"
    Write-Par "Gastei 45 reais no McDonalds" "deduz despesa · Alimentação"
    Write-Par "Recebi meu salário de 5000 reais" "deduz receita · Salário"
    Write-Par "Paguei 1580 de aluguel no dia 05" "entende a data informada"

    Write-Topico "CONSULTAR"
    Write-Par "Qual é o meu saldo?" "receitas menos despesas"
    Write-Par "Quanto eu gastei hoje?" "total do dia"
    Write-Par "Quanto gastei com alimentação?" "total da categoria"
    Write-Par "Onde eu mais gastei neste mês?" "distribuição por categoria"
    Write-Par "Qual foi a minha maior despesa?" "maior lançamento"
    Write-Par "Faça um resumo financeiro deste mês" "consolidado do mês"

    Write-Topico "COMANDOS DESTE CONSOLE"
    Write-Par "saldo" "receitas, despesas e saldo"
    Write-Par "categorias" "distribuição das despesas"
    Write-Par "extrato" "últimas movimentações"
    Write-Par "ajuda" "mostra este guia"
    Write-Par "limpar" "limpa a tela"
    Write-Par "sair" "encerra"

    Write-Topico "O QUE ELE NÃO FAZ"
    Write-Host "    Não inventa valores. Peça " -NoNewline -ForegroundColor $Muted
    Write-Host "Registre uma despesa de Uber" -NoNewline -ForegroundColor $Ink
    Write-Host " sem dizer" -ForegroundColor $Muted
    Write-Host "    quanto foi: ele pergunta o valor e nada é gravado no banco." -ForegroundColor $Muted

    Write-Topico "TAMBÉM DISPONÍVEL"
    Write-Par "http://localhost:8081" "mesma coisa no navegador, com voz"
    Write-Par ".\falar.ps1" "enviar um arquivo de áudio"
    Write-Par ".\falar.ps1 -Gravar" "abrir o Gravador de Voz do Windows"
    Write-Par "requests.http" "coleção de requisições REST"
    Write-Host ""
}

Show-Cabecalho
$online = Show-Saldo
Show-Ajuda

while ($true) {
    Write-Host "você " -NoNewline -ForegroundColor $Good
    Write-Host "> " -NoNewline -ForegroundColor $Muted
    $entrada = Read-Host

    if ([string]::IsNullOrWhiteSpace($entrada)) { continue }

    switch -Regex ($entrada.Trim().ToLower()) {
        '^(sair|exit|quit)$'   { Write-Host ""; return }
        '^(limpar|clear|cls)$' { Show-Cabecalho; Show-Saldo | Out-Null; Show-Ajuda; continue }
        '^(ajuda|help|guia|\?)$' { Show-Guia; continue }
        '^saldo$'              { Show-Saldo | Out-Null; continue }
        '^categorias$'         { Show-Categorias; continue }
        '^(extrato|banco|transacoes|transações)$' { Show-Transacoes; continue }
    }

    Write-Host " IA  " -NoNewline -ForegroundColor $Accent
    Write-Host "> " -NoNewline -ForegroundColor $Muted
    Write-Host "pensando" -NoNewline -ForegroundColor $Muted

    $resultado = Invoke-Assistant $entrada

    Write-Host ("`r" + (" " * 24) + "`r") -NoNewline

    if ($resultado.Ok) {
        Write-Wrapped $resultado.Texto " IA  > " $Accent
    }
    else {
        Write-Wrapped $resultado.Texto " erro > " $Bad
    }
    Write-Host ""
}
