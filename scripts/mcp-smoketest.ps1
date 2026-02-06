param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipCertValidation
)

# Prefer TLS 1.2+ for Okta/public endpoints
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13

if ($SkipCertValidation) {
    Write-Warning "Skipping certificate validation for this session."
    add-type @"
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
public static class TrustAllCertsPolicy {
    public static bool TrustAll(object sender, X509Certificate cert, X509Chain chain, SslPolicyErrors errors) { return true; }
}
"@
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = [System.Net.Security.RemoteCertificateValidationCallback]::new({ param($sender,$cert,$chain,$errors) return $true })
}

Write-Host "Target BaseUrl: $BaseUrl"

$Token = Read-Host "Enter Okta access token"

$discovery = "$BaseUrl/.well-known/oauth-protected-resource"
$sse = "$BaseUrl/sse"

Write-Host "`n1) Discovery doc" -ForegroundColor Cyan
try {
    $resp = Invoke-WebRequest -Uri $discovery -UseBasicParsing
    Write-Host "Status: $($resp.StatusCode)"
    Write-Host "Body:"
    $resp.Content
} catch {
    Write-Warning $_
}

Write-Host "`n2) SSE without token (should be 401 with WWW-Authenticate)" -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $sse -UseBasicParsing -Method GET -Headers @{ Accept = "text/event-stream" }
} catch {
    if ($_.Exception.Response) {
        Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
        Write-Host "Headers:"
        $_.Exception.Response.Headers | ForEach-Object { "$($_.Key): $($_.Value -join ',')" }
    } else {
        Write-Warning $_
    }
}

Write-Host "`n3) SSE with Bearer token (should be 200 and stream)" -ForegroundColor Cyan
try {
    $resp = Invoke-WebRequest -Uri $sse -UseBasicParsing -Method GET -Headers @{
        Authorization = "Bearer $Token"
        Accept        = "text/event-stream"
    }
    Write-Host "Status: $($resp.StatusCode)"
    Write-Host "First 200 chars of stream:"
    if ($resp.Content) {
        $resp.Content.Substring(0, [Math]::Min(200, $resp.Content.Length))
    } else {
        Write-Host "[no content returned]"
    }
} catch {
    Write-Warning $_
}
