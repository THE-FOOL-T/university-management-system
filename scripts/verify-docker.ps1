param([string]$BaseUrl = 'http://localhost:8080')
# Creates only uniquely named temporary profiles; never removes the PostgreSQL volume.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
Push-Location (Split-Path $PSScriptRoot -Parent)
$checks = [System.Collections.Generic.List[string]]::new()
function Assert-Status($Response, [int]$Expected, [string]$Label) {
    $actual = [int]$Response.StatusCode
    if ($actual -ne $Expected) { throw "$Label expected $Expected, received $actual" }
    $checks.Add("PASS: $Label ($actual)")
}
function New-SessionClient {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.CookieContainer = [System.Net.CookieContainer]::new()
    $handler.AllowAutoRedirect = $false
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(15)
    return $client
}
function Send-Request($Client, [string]$Method, [string]$Path, $Body = $null, [string]$Token = '') {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    if ($Token) { $request.Headers.Add('X-CSRF-TOKEN', $Token) }
    if ($null -ne $Body) {
        $request.Content = [System.Net.Http.StringContent]::new(($Body | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json')
    }
    return $Client.SendAsync($request).GetAwaiter().GetResult()
}
function Read-Json($Response) { return $Response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json }
function Get-Token($Client) { return (Read-Json (Send-Request $Client GET '/api/auth/csrf')).token }
function Login($Client, [string]$Email, [string]$Password) {
    $token = Get-Token $Client
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$BaseUrl/login")
    $request.Headers.Add('X-CSRF-TOKEN', $token)
    $body = "email=$([Uri]::EscapeDataString($Email))&password=$([Uri]::EscapeDataString($Password))"
    $request.Content = [System.Net.Http.StringContent]::new($body, [Text.Encoding]::UTF8, 'application/x-www-form-urlencoded')
    $response = $Client.SendAsync($request).GetAwaiter().GetResult()
    Assert-Status $response 302 'Login redirect'
    if (-not $response.Headers.Location.ToString().EndsWith('/dashboard')) { throw 'Login failed' }
    return Get-Token $Client
}
function Wait-Ready($Client) {
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try { if ([int](Send-Request $Client GET '/actuator/health').StatusCode -eq 200) { return } } catch {}
        Start-Sleep -Seconds 2
    }
    throw 'Application did not become healthy in four minutes'
}
try {
    docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) { throw 'Docker engine is unavailable' }
    $configText = docker compose config --format json
    if ($LASTEXITCODE -ne 0) { throw 'Compose configuration failed' }
    $config = $configText | ConvertFrom-Json
    $email = $config.services.app.environment.BOOTSTRAP_ADMIN_EMAIL
    $password = $config.services.app.environment.BOOTSTRAP_ADMIN_PASSWORD
    if (-not $email -or -not $password) { throw 'Keep the current bootstrap admin credentials in local .env for this script' }
    docker compose up --build -d
    if ($LASTEXITCODE -ne 0) { throw 'Compose build/start failed' }
    $admin = New-SessionClient
    Wait-Ready $admin
    $token = Login $admin $email $password
    $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $temporaryPassword = [Guid]::NewGuid().ToString('N') + '-Lab9!'
    $studentBody = @{name='Docker verification';roll="VERIFY-$suffix";department='CSE';semester=4;email="verify.s.$suffix@example.test";password=$temporaryPassword}
    $created = Send-Request $admin POST '/api/students' $studentBody $token
    Assert-Status $created 201 'Create student'
    $student = Read-Json $created
    Assert-Status (Send-Request $admin GET "/api/students/$($student.id)") 200 'Read student'
    $studentBody.name = 'Updated Docker verification'
    $studentBody.password = ''
    Assert-Status (Send-Request $admin PUT "/api/students/$($student.id)" $studentBody $token) 200 'Update student'
    $teacherBody = @{name='Docker teacher';employeeId="VERIFY-$suffix";department='CSE';designation='Lecturer';email="verify.t.$suffix@example.test";password=$temporaryPassword}
    $teacherResponse = Send-Request $admin POST '/api/teachers' $teacherBody $token
    Assert-Status $teacherResponse 201 'Create teacher login'
    $teacher = Read-Json $teacherResponse
    $teacherClient = New-SessionClient
    $teacherToken = Login $teacherClient $teacherBody.email $temporaryPassword
    Assert-Status (Send-Request $teacherClient GET '/api/teachers') 200 'Teacher allowed teacher directory'
    Assert-Status (Send-Request $teacherClient GET '/api/students') 403 'Teacher forbidden student directory'
    Assert-Status (Send-Request $teacherClient POST '/api/admins' @{name='Denied';designation='Registrar';email='denied@example.test';password=$temporaryPassword} $teacherToken) 403 'Teacher cannot create admin with valid CSRF'
    $studentClient = New-SessionClient
    $studentToken = Login $studentClient $studentBody.email $temporaryPassword
    Assert-Status (Send-Request $studentClient GET '/api/students') 200 'Student allowed student directory'
    Assert-Status (Send-Request $studentClient PUT "/api/teachers/$($teacher.id)" $teacherBody $studentToken) 403 'Student cannot update teacher with valid CSRF'
    Assert-Status (Send-Request $studentClient POST '/api/students' $studentBody) 403 'Missing CSRF blocked'
    Assert-Status (Send-Request $studentClient POST '/logout' $null $studentToken) 302 'Logout'
    Assert-Status (Send-Request $studentClient GET '/api/auth/me') 401 'Logout revoked session'
    docker compose restart
    if ($LASTEXITCODE -ne 0) { throw 'Restart failed' }
    Start-Sleep -Seconds 3
    Wait-Ready $admin
    $token = Login $admin $email $password
    $persisted = Send-Request $admin GET "/api/students/$($student.id)"
    Assert-Status $persisted 200 'Student survives container restart'
    if ((Read-Json $persisted).name -ne 'Updated Docker verification') { throw 'Persisted value changed' }
    Assert-Status (Send-Request $admin DELETE "/api/students/$($student.id)" $null $token) 204 'Delete student and account'
    Assert-Status (Send-Request $admin DELETE "/api/teachers/$($teacher.id)" $null $token) 204 'Delete temporary teacher and account'
    Assert-Status (Send-Request $admin GET "/api/students/$($student.id)") 404 'Deleted student is absent'
    $checks.Add('PASS: Docker end-to-end verification completed')
} catch {
    $checks.Add("FAIL: $($_.Exception.Message)")
    throw
} finally {
    New-Item -ItemType Directory -Path 'target/verification' -Force | Out-Null
    $checks | Set-Content -Encoding UTF8 'target/verification/docker-verification.txt'
    $checks | ForEach-Object { Write-Host $_ }
    foreach ($client in @($admin, $teacherClient, $studentClient)) { if ($client) { $client.Dispose() } }
    Pop-Location
}
