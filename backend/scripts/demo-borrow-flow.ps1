param(
    [string]$BaseUrl = 'http://localhost:18080',
    [string]$ReaderEmail = 'reader@librio.local',
    [Parameter(Mandatory = $true)]
    [string]$ReaderPassword,
    [string]$LibrarianEmail = 'librarian@librio.local',
    [Parameter(Mandatory = $true)]
    [string]$LibrarianPassword,
    [long]$ResourceId = 1
)

$ErrorActionPreference = 'Stop'

function Get-Csrf($baseUrl, $session) {
    Invoke-RestMethod -Uri "$baseUrl/auth/csrf" -WebSession $session
}

function Login($baseUrl, $session, $email, $password) {
    $csrf = Get-Csrf $baseUrl $session
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/login" `
        -WebSession $session `
        -Headers @{ $csrf.headerName = $csrf.token } `
        -ContentType 'application/json' `
        -Body $body
}

function Post-WithCsrf($baseUrl, $session, $path, $body = $null) {
    $csrf = Get-Csrf $baseUrl $session
    $arguments = @{
        Method = 'Post'
        Uri = "$baseUrl$path"
        WebSession = $session
        Headers = @{ $csrf.headerName = $csrf.token }
    }
    if ($null -ne $body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = ($body | ConvertTo-Json)
    }
    Invoke-RestMethod @arguments
}

$readerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$librarianSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$readerLogin = Login $BaseUrl $readerSession $ReaderEmail $ReaderPassword
$before = Invoke-RestMethod -Uri "$BaseUrl/resources/$ResourceId"
$requested = Post-WithCsrf $BaseUrl $readerSession '/me/borrow-requests' @{
    resourceId = $ResourceId
}

$librarianLogin = Login $BaseUrl $librarianSession $LibrarianEmail $LibrarianPassword
$ready = Post-WithCsrf $BaseUrl $librarianSession "/librarian/borrow-requests/$($requested.id)/prepare"
$borrowing = Post-WithCsrf $BaseUrl $librarianSession "/librarian/borrow-requests/$($requested.id)/fulfil"
$after = Invoke-RestMethod -Uri "$BaseUrl/resources/$ResourceId"

[ordered]@{
    readerLogin = $readerLogin
    availabilityBefore = $before.physical
    requested = $requested
    librarianLogin = $librarianLogin
    ready = $ready
    borrowing = $borrowing
    availabilityAfter = $after.physical
} | ConvertTo-Json -Depth 8
