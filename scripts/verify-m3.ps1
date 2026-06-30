param(
    [string]$UserBase = "http://localhost:8081",
    [string]$DestinationBase = "http://localhost:8082",
    [string]$ItineraryBase = "http://localhost:8083",
    [string]$ActivityBase = "http://localhost:8084",
    [string]$BookingBase = "http://localhost:8085",
    [string]$GatewayBase = "http://localhost:8080",
    [switch]$SkipDockerChecks,
    [switch]$PreserveRedis
)

$ErrorActionPreference = "Stop"

$checks = New-Object System.Collections.Generic.List[object]

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory=$true)][string]$Method,
        [Parameter(Mandatory=$true)][string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Uri = $Url
        Method = $Method
        UseBasicParsing = $true
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 30)
        $params.ContentType = "application/json"
    }

    try {
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        $text = [string]$response.Content
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
                $text = [string]$reader.ReadToEnd()
            } catch {
                $text = [string]$_.Exception.Message
            }
        } else {
            $status = 0
            $text = [string]$_.Exception.Message
        }
    }

    $json = $null
    if ($text) {
        try { $json = $text | ConvertFrom-Json } catch { }
    }
    [pscustomobject]@{ Status = $status; Body = $json; Text = $text }
}

function Short-Text {
    param([object]$Value)
    $s = ([string]$Value) -replace "\s+", " "
    if ($s.Length -gt 180) { return $s.Substring(0, 180) }
    return $s
}

function Add-Check {
    param(
        [string]$Name,
        [object]$Result,
        [scriptblock]$Predicate
    )
    $ok = $false
    try { $ok = [bool](& $Predicate $Result) } catch { $ok = $false }
    $checks.Add([pscustomobject]@{
        Name = $Name
        Status = $Result.Status
        OK = $ok
        Body = (Short-Text $Result.Text)
    }) | Out-Null
    if (-not $ok) {
        Write-Host "FAIL $Name status=$($Result.Status) body=$($Result.Text)"
    }
    return $Result
}

function Add-BooleanCheck {
    param([string]$Name, [bool]$OK, [string]$Body = "")
    $checks.Add([pscustomobject]@{
        Name = $Name
        Status = if ($OK) { 200 } else { 0 }
        OK = $OK
        Body = $Body
    }) | Out-Null
    if (-not $OK) { Write-Host "FAIL $Name body=$Body" }
}

function Is-2xx { param($r) return ($r.Status -ge 200 -and $r.Status -lt 300) }
function Is-Non2xx { param($r) return ($r.Status -lt 200 -or $r.Status -ge 300) }

function Wait-For {
    param(
        [string]$Name,
        [scriptblock]$Action,
        [scriptblock]$Predicate,
        [int]$Attempts = 30,
        [int]$SleepSeconds = 1
    )
    $last = $null
    for ($n = 1; $n -le $Attempts; $n++) {
        $last = & $Action
        $ok = $false
        try { $ok = [bool](& $Predicate $last) } catch { $ok = $false }
        if ($ok) {
            $checks.Add([pscustomobject]@{
                Name = $Name
                Status = $last.Status
                OK = $true
                Body = (Short-Text $last.Text)
            }) | Out-Null
            return $last
        }
        Start-Sleep -Seconds $SleepSeconds
    }
    Add-Check $Name $last $Predicate | Out-Null
    return $last
}

function Get-FirstId {
    param($Result)
    if ($Result.Body -is [array]) { return [long]$Result.Body[0].id }
    return [long]$Result.Body.id
}

function Auth-Headers {
    param([string]$Token)
    @{ Authorization = "Bearer $Token" }
}

if (-not $SkipDockerChecks) {
    $urls = @(
        (& docker compose exec -T user-service printenv SPRING_DATASOURCE_URL),
        (& docker compose exec -T destination-service printenv SPRING_DATASOURCE_URL),
        (& docker compose exec -T itinerary-service printenv SPRING_DATASOURCE_URL),
        (& docker compose exec -T activity-service printenv SPRING_DATASOURCE_URL),
        (& docker compose exec -T booking-service printenv SPRING_DATASOURCE_URL)
    )
    Add-BooleanCheck "M3-DB user-service isolated datasource" ($urls[0] -eq "jdbc:postgresql://user-postgres:5432/tripdb-users") $urls[0]
    Add-BooleanCheck "M3-DB destination-service isolated datasource" ($urls[1] -eq "jdbc:postgresql://destination-postgres:5432/tripdb-destinations") $urls[1]
    Add-BooleanCheck "M3-DB itinerary-service isolated datasource" ($urls[2] -eq "jdbc:postgresql://itinerary-postgres:5432/tripdb-itineraries") $urls[2]
    Add-BooleanCheck "M3-DB activity-service isolated datasource" ($urls[3] -eq "jdbc:postgresql://activity-postgres:5432/tripdb-activities") $urls[3]
    Add-BooleanCheck "M3-DB booking-service isolated datasource" ($urls[4] -eq "jdbc:postgresql://booking-postgres:5432/tripdb-bookings") $urls[4]

    if (-not $PreserveRedis) {
        $flush = (& docker compose exec -T redis redis-cli --no-auth-warning -a redispass FLUSHDB 2>&1) -join " "
        Add-BooleanCheck "M3 cache clean start" ($flush -match "OK") $flush
    }
}

$null = Wait-For "health user-service" { Invoke-JsonRequest GET "$UserBase/api/users/health" } { param($r) Is-2xx $r } 45 1
$null = Wait-For "health destination-service" { Invoke-JsonRequest GET "$DestinationBase/api/destinations/health" } { param($r) Is-2xx $r } 45 1
$null = Wait-For "health itinerary-service" { Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/health" } { param($r) Is-2xx $r } 45 1
$null = Wait-For "health activity-service" { Invoke-JsonRequest GET "$ActivityBase/api/activities/health" } { param($r) Is-2xx $r } 45 1
$null = Wait-For "health booking-service" { Invoke-JsonRequest GET "$BookingBase/api/bookings/health" } { param($r) Is-2xx $r } 45 1

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$today = Get-Date
$futureStart = $today.AddDays(45).ToString("yyyy-MM-dd")
$futureEnd = $today.AddDays(49).ToString("yyyy-MM-dd")
$rangeStart = $today.AddDays(-30).ToString("yyyy-MM-dd")
$rangeEnd = $today.AddDays(120).ToString("yyyy-MM-dd")

$adminLogin = Add-Check "AUTH admin login" (Invoke-JsonRequest POST "$UserBase/api/auth/login" @{ email = "admin@tripplanning.com"; password = "Admin@12345" }) { param($r) (Is-2xx $r) -and $r.Body.token }
$admin = Auth-Headers $adminLogin.Body.token

$email1 = "m3-primary-$suffix@example.com"
$email2 = "m3-secondary-$suffix@example.com"
$email3 = "m3-deactivate-$suffix@example.com"
$email4 = "m3-clear-$suffix@example.com"

$u1Login = Add-Check "S1-F10 register primary user" (Invoke-JsonRequest POST "$UserBase/api/auth/register" @{ name = "M3 Primary $suffix"; email = $email1; password = "Traveler@12345"; phone = "551$suffix" } ) { param($r) $r.Status -eq 201 -and $r.Body.token }
$u2Login = Add-Check "S1-F10 register secondary user" (Invoke-JsonRequest POST "$UserBase/api/auth/register" @{ name = "M3 Secondary $suffix"; email = $email2; password = "Traveler@12345"; phone = "552$suffix" } ) { param($r) $r.Status -eq 201 -and $r.Body.token }
$u3Login = Add-Check "S1-F10 register active-deactivate user" (Invoke-JsonRequest POST "$UserBase/api/auth/register" @{ name = "M3 Active Block $suffix"; email = $email3; password = "Traveler@12345"; phone = "553$suffix" } ) { param($r) $r.Status -eq 201 -and $r.Body.token }
$u4Login = Add-Check "S1-F10 register clear-deactivate user" (Invoke-JsonRequest POST "$UserBase/api/auth/register" @{ name = "M3 Clear Deactivate $suffix"; email = $email4; password = "Traveler@12345"; phone = "554$suffix" } ) { param($r) $r.Status -eq 201 -and $r.Body.token }

$u1Headers = Auth-Headers $u1Login.Body.token
$u2Headers = Auth-Headers $u2Login.Body.token
$u3Headers = Auth-Headers $u3Login.Body.token
$u4Headers = Auth-Headers $u4Login.Body.token

$u1Search = Add-Check "S1-F1 primary user lookup" (Invoke-JsonRequest GET "$UserBase/api/users/search?email=$([uri]::EscapeDataString($email1))" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 }
$u2Search = Add-Check "S1-F1 secondary user lookup" (Invoke-JsonRequest GET "$UserBase/api/users/search?email=$([uri]::EscapeDataString($email2))" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 }
$u3Search = Add-Check "S1-F1 active-deactivate lookup" (Invoke-JsonRequest GET "$UserBase/api/users/search?email=$([uri]::EscapeDataString($email3))" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 }
$u4Search = Add-Check "S1-F1 clear-deactivate lookup" (Invoke-JsonRequest GET "$UserBase/api/users/search?email=$([uri]::EscapeDataString($email4))" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 }
$adminSearch = Add-Check "AUTH admin lookup" (Invoke-JsonRequest GET "$UserBase/api/users/search?email=admin%40tripplanning.com" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 }

$u1 = [long]$u1Search.Body[0].id
$u2 = [long]$u2Search.Body[0].id
$u3 = [long]$u3Search.Body[0].id
$u4 = [long]$u4Search.Body[0].id
$adminId = [long]$adminSearch.Body[0].id

Add-Check "S1-F2 update primary preferences" (Invoke-JsonRequest PUT "$UserBase/api/users/$u1/preferences" @{ travelStyle = "luxury"; currency = "EGP" } $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S1-F2 update secondary preferences" (Invoke-JsonRequest PUT "$UserBase/api/users/$u2/preferences" @{ travelStyle = "luxury"; currency = "EGP" } $u2Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S1-F5 preference search" (Invoke-JsonRequest GET "$UserBase/api/users/preferences/search?key=travelStyle&value=luxury" $null $admin) { param($r) Is-2xx $r } | Out-Null

$dest1 = Add-Check "S2 CRUD create primary destination" (Invoke-JsonRequest POST "$DestinationBase/api/destinations" @{
    name = "M3 Primary City $suffix"; country = "Egypt"; description = "M3 verifier destination"; category = "CITY"; status = "ACTIVE"; rating = 4.0; totalRatings = 1; details = @{ season = "summer"; tier = "m3" }
} $admin) { param($r) $r.Status -eq 201 -and $r.Body.id }
$dest2 = Add-Check "S2 CRUD create guard destination" (Invoke-JsonRequest POST "$DestinationBase/api/destinations" @{
    name = "M3 Guard Beach $suffix"; country = "Egypt"; description = "M3 guard destination"; category = "BEACH"; status = "ACTIVE"; rating = 3.5; totalRatings = 1; details = @{ season = "spring"; tier = "guard" }
} $admin) { param($r) $r.Status -eq 201 -and $r.Body.id }
$d1 = [long]$dest1.Body.id
$d2 = [long]$dest2.Body.id

Add-Check "S2 Feign endpoint GET destination DTO" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/$d1" $null $admin) { param($r) (Is-2xx $r) -and $r.Body.id -eq $d1 -and $r.Body.status } | Out-Null
Add-Check "S2 batch endpoint for S5-F10" (Invoke-JsonRequest POST "$DestinationBase/api/destinations/batch" @{ destinationIds = @($d1, $d2) } $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 2 } | Out-Null
Add-Check "S2-F2 details JSONB search" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/details/search?key=season&value=summer&status=ACTIVE" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F10 index destination" (Invoke-JsonRequest POST "$DestinationBase/api/destinations/$d1/index" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F11 full-text search" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/search/full-text?query=M3&status=ACTIVE" $null $admin) { param($r) Is-2xx $r } | Out-Null

$guardItin = Add-Check "S3 CRUD create guard itinerary" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries" @{
    userId = $u3; title = "M3 Guard Trip $suffix"; status = "DRAFT"; estimatedBudget = 400; metadata = @{ source = "m3-verify" }; startDate = $futureStart; endDate = $futureEnd
} $u3Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$guardItinId = [long]$guardItin.Body.id
Add-Check "S3-F2 assign active destination" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$guardItinId/assign" @{ destinationId = $d2 } $u3Headers) { param($r) (Is-2xx $r) -and $r.Body.destinationId -eq $d2 } | Out-Null
Add-Check "S3 update guard itinerary planned" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$guardItinId" @{ id = $guardItinId; userId = $u3; destinationId = $d2; title = "M3 Guard Trip $suffix"; status = "PLANNED"; estimatedBudget = 400; metadata = @{ source = "m3-verify" }; startDate = $futureStart; endDate = $futureEnd } $u3Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F4 reject INACTIVE with active itineraries" (Invoke-JsonRequest PUT "$DestinationBase/api/destinations/$d2/status" @{ status = "INACTIVE" } $admin) { param($r) $r.Status -eq 400 } | Out-Null
Add-Check "S1-F4 reject deactivate with active itinerary" (Invoke-JsonRequest PUT "$UserBase/api/users/$u3/deactivate" $null $u3Headers) { param($r) $r.Status -eq 400 } | Out-Null
Add-Check "S1-F4 deactivate user without active itinerary" (Invoke-JsonRequest PUT "$UserBase/api/users/$u4/deactivate" $null $u4Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "DEACTIVATED" } | Out-Null

$primaryItin = Add-Check "S3 CRUD create primary draft itinerary" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries" @{
    userId = $u1; title = "M3 Primary Trip $suffix"; status = "DRAFT"; estimatedBudget = 1200; metadata = @{ style = "luxury"; source = "m3-verify" }; startDate = $futureStart; endDate = $futureEnd
} $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$itin1 = [long]$primaryItin.Body.id
Add-Check "S3-F2 assign primary destination" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$itin1/assign" @{ destinationId = $d1 } $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.destinationId -eq $d1 } | Out-Null
Add-Check "S3 update primary itinerary planned" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$itin1" @{ id = $itin1; userId = $u1; destinationId = $d1; title = "M3 Primary Trip $suffix"; status = "PLANNED"; estimatedBudget = 1200; metadata = @{ style = "luxury"; source = "m3-verify" }; startDate = $futureStart; endDate = $futureEnd } $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "PLANNED" } | Out-Null
Add-Check "S3 helper GET itinerary DTO" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/$itin1" $null $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.id -eq $itin1 } | Out-Null
Add-Check "S3-F9 add itinerary day" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries/$itin1/days" @{ dayOrder = 1; date = $futureStart; title = "Arrival"; description = "Arrive"; status = "PLANNED" } $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3-F9 get itinerary days" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/$itin1/days" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3 batch endpoint for S5-F10" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries/batch" @{ itineraryIds = @($itin1) } $admin) { param($r) (Is-2xx $r) -and $r.Body.Count -ge 1 } | Out-Null
Add-Check "S3 active-count endpoint for S1-F4" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/user/$u1/active-count" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3 destination active-count endpoint for S2-F4" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/destination/$d1/active-count" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3-F12 recommendations owner allowed" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/recommendations?userId=$u1&limit=3" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3-F12 recommendations other user forbidden" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/recommendations?userId=$u1&limit=3" $null $u2Headers) { param($r) $r.Status -eq 403 } | Out-Null
Add-Check "S3-F10 analytics dashboard" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/analytics/dashboard?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3-F10 analytics dashboard cache hit path" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/analytics/dashboard?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null

$booking1 = Add-Check "S5-F4 create booking for itinerary via Feign" (Invoke-JsonRequest POST "$BookingBase/api/bookings/itinerary/$itin1" @{
    userId = $u1; amount = 300.0; type = "ACCOMMODATION"; providerName = "M3 Verifier Hotel"
} $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id -and $r.Body.status -eq "CONFIRMED" }
$booking1Id = [long]$booking1.Body.id
Add-Check "S5 CRUD get booking cached" (Invoke-JsonRequest GET "$BookingBase/api/bookings/$booking1Id" $null $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.id -eq $booking1Id } | Out-Null
Add-Check "S5-F8 booking details" (Invoke-JsonRequest GET "$BookingBase/api/bookings/$booking1Id/details" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5-F3 user booking summary via user-service Feign" (Invoke-JsonRequest GET "$BookingBase/api/bookings/user/$u1/summary" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5 helper user booking total for S1-F6" (Invoke-JsonRequest GET "$BookingBase/api/bookings/user/$u1/total?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5 helper confirmed summary for S3-F4" (Invoke-JsonRequest GET "$BookingBase/api/bookings/itinerary/$itin1/confirmed-summary" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5 helper confirmed count for S3-F4" (Invoke-JsonRequest GET "$BookingBase/api/bookings/itinerary/$itin1/confirmed-count" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5 aggregate-by-itineraries batch" (Invoke-JsonRequest POST "$BookingBase/api/bookings/aggregate-by-itineraries" @{ itineraryIds = @($itin1) } $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5-F6 revenue report" (Invoke-JsonRequest GET "$BookingBase/api/bookings/reports/revenue?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5-F11 payment history" (Invoke-JsonRequest GET "$BookingBase/api/bookings/$booking1Id/payment-history?page=0&size=5" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5-F10 destination-season analytics" (Invoke-JsonRequest GET "$BookingBase/api/bookings/analytics/destination-season?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S5-F10 destination-season analytics cache hit" (Invoke-JsonRequest GET "$BookingBase/api/bookings/analytics/destination-season?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null

$activity1 = Add-Check "S4-F2 create activity via itinerary Feign check" (Invoke-JsonRequest POST "$ActivityBase/api/activities/itinerary/$itin1" @{
    name = "M3 Museum"; category = "CULTURAL"; latitude = 30.0444; longitude = 31.2357; scheduledTime = "$futureStart`T10:00:00"; metadata = @{ cost = 20; source = "m3-verify" }
} $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$activity1Id = [long]$activity1.Body.id
Add-Check "S4-F4 batch activity creation via itinerary Feign check" (Invoke-JsonRequest POST "$ActivityBase/api/activities/batch" @{
    itineraryId = $itin1
    activities = @(@{ name = "M3 Dinner"; category = "DINING"; latitude = 30.05; longitude = 31.24; scheduledTime = "$futureStart`T19:00:00"; metadata = @{ cost = 30 } })
} $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.count -ge 1 } | Out-Null
Add-Check "S4-F1 latest activity" (Invoke-JsonRequest GET "$ActivityBase/api/activities/itinerary/$itin1/latest" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F3 nearby activities" (Invoke-JsonRequest GET "$ActivityBase/api/activities/nearby?lat=30.0444&lon=31.2357&radiusKm=5" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F5 metadata search" (Invoke-JsonRequest GET "$ActivityBase/api/activities/metadata/search?key=cost&operator=gt&value=10" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F6 history date range" (Invoke-JsonRequest GET "$ActivityBase/api/activities/history?startDate=$rangeStart&endDate=$rangeEnd" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F8 activity summary" (Invoke-JsonRequest GET "$ActivityBase/api/activities/itinerary/$itin1/summary?startDate=$rangeStart&endDate=$rangeEnd" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F9 budget-friendly" (Invoke-JsonRequest GET "$ActivityBase/api/activities/budget-friendly?maxCost=50&sinceMinutes=100000" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F10 analytics admin" (Invoke-JsonRequest GET "$ActivityBase/api/activities/analytics?startDate=$rangeStart&endDate=$rangeEnd&userId=$u1" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S4-F10 analytics ownership forbidden" (Invoke-JsonRequest GET "$ActivityBase/api/activities/analytics?startDate=$rangeStart&endDate=$rangeEnd&userId=$u1" $null $u2Headers) { param($r) $r.Status -eq 403 } | Out-Null
Add-Check "S4-F11 activity lifecycle event" (Invoke-JsonRequest POST "$ActivityBase/api/activities/$activity1Id/events" @{ status = "COMPLETED"; notes = "m3 verifier" } $u1Headers) { param($r) $r.Status -eq 201 } | Out-Null
Add-Check "S4-F12 activity timeline" (Invoke-JsonRequest GET "$ActivityBase/api/activities/$activity1Id/timeline" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null

Add-Check "S3 mark primary itinerary completed for visit tests" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$itin1" @{ id = $itin1; userId = $u1; destinationId = $d1; title = "M3 Primary Trip $suffix"; status = "COMPLETED"; estimatedBudget = 1200; metadata = @{ style = "luxury"; source = "m3-verify" }; startDate = $futureStart; endDate = $futureEnd } $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "COMPLETED" } | Out-Null
Add-Check "S3-F11 record user-destination visit" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries/$itin1/record-visit" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F7 rate destination after visit" (Invoke-JsonRequest POST "$DestinationBase/api/destinations/$d1/rate" @{ itineraryId = $itin1; rating = 5 } $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.totalRatings -ge 2 } | Out-Null
Add-Check "S2-F3 destination booking revenue via itinerary-service" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/$d1/revenue?startDate=$rangeStart&endDate=$rangeEnd" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F12 destination dashboard via itinerary-service" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/$d1/dashboard" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3 destination booking-revenue helper for S2-F3" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/destination/$d1/booking-revenue" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3 destination dashboard aggregate helper for S2-F12" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/destination/$d1/dashboard-aggregate" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S1-F3 user trip summary via itinerary-service" (Invoke-JsonRequest GET "$UserBase/api/users/$u1/trip-summary" $null $u1Headers) { param($r) Is-2xx $r } | Out-Null
Add-Check "S1-F6 top travelers via booking-service" (Invoke-JsonRequest GET "$UserBase/api/users/reports/top-travelers?startDate=$rangeStart&endDate=$rangeEnd&limit=5" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S1-F9 travel-style min trips via itinerary-service" (Invoke-JsonRequest GET "$UserBase/api/users/preferences/travel-style?style=luxury&minTrips=1" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S3 completed-count endpoint for S1-F9" (Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/user/$u1/completed-count" $null $admin) { param($r) Is-2xx $r } | Out-Null

$review = Add-Check "S2 review create" (Invoke-JsonRequest POST "$DestinationBase/api/destinations/$d1/reviews" @{ type = "VISITOR"; content = "M3 verifier review"; rating = 4; visitDate = $today.AddDays(-10).ToString("yyyy-MM-dd"); metadata = @{ source = "m3-verify" } } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$reviewId = [long]$review.Body.id
Add-Check "S2-F8 verify review via user-service admin Feign" (Invoke-JsonRequest PUT "$DestinationBase/api/destinations/$d1/reviews/$reviewId/verify" @{ verifiedBy = $adminId } $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "S2-F8 reject non-admin verifier" (Invoke-JsonRequest PUT "$DestinationBase/api/destinations/$d1/reviews/$reviewId/verify" @{ verifiedBy = $u1 } $admin) { param($r) $r.Status -eq 403 } | Out-Null
Add-Check "S2-F9 low-rated reviews" (Invoke-JsonRequest GET "$DestinationBase/api/destinations/reviews/low-rated?maxRating=5" $null $admin) { param($r) Is-2xx $r } | Out-Null

$refundItin = Add-Check "S5-F12 setup refund itinerary" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries" @{ userId = $u1; destinationId = $d1; title = "M3 Refund Trip $suffix"; status = "PLANNED"; estimatedBudget = 700; metadata = @{ source = "m3-refund" }; startDate = $today.AddDays(60).ToString("yyyy-MM-dd"); endDate = $today.AddDays(64).ToString("yyyy-MM-dd") } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$refundItinId = [long]$refundItin.Body.id
$refundBooking = Add-Check "S5-F12 setup confirmed booking" (Invoke-JsonRequest POST "$BookingBase/api/bookings/itinerary/$refundItinId" @{ userId = $u1; amount = 500.0; type = "TRANSPORT"; providerName = "M3 Refund Provider" } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.status -eq "CONFIRMED" }
$refundBookingId = [long]$refundBooking.Body.id
Add-Check "S5-F12 refund cancellation strategy via itinerary Feign" (Invoke-JsonRequest POST "$BookingBase/api/bookings/$refundBookingId/refund-cancellation-tier" @{ reason = "schedule_conflict" } $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "CANCELLED" -and $r.Body.strategy } | Out-Null

$cancelItin = Add-Check "S3-F7 setup planned itinerary" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries" @{ userId = $u1; destinationId = $d1; title = "M3 Cancel Trip $suffix"; status = "PLANNED"; estimatedBudget = 100; metadata = @{ source = "m3-cancel" }; startDate = $futureStart; endDate = $futureEnd } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$cancelItinId = [long]$cancelItin.Body.id
Add-Check "S3-F7 cancel itinerary publishes event" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$cancelItinId/cancel" $null $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "CANCELLED" } | Out-Null
Add-Check "S3-F7 reject cancel completed itinerary" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$itin1/cancel" $null $u1Headers) { param($r) Is-Non2xx $r } | Out-Null

$sagaItin = Add-Check "S3-F4 setup in-progress itinerary" (Invoke-JsonRequest POST "$ItineraryBase/api/itineraries" @{ userId = $u1; destinationId = $d1; title = "M3 Saga Trip $suffix"; status = "IN_PROGRESS"; estimatedBudget = 0; metadata = @{ source = "m3-saga" }; startDate = $futureStart; endDate = $futureEnd } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.id }
$sagaItinId = [long]$sagaItin.Body.id
$sagaBooking = Add-Check "S3-F4 setup confirmed booking" (Invoke-JsonRequest POST "$BookingBase/api/bookings/itinerary/$sagaItinId" @{ userId = $u1; amount = 650.0; type = "ACTIVITY"; providerName = "M3 Saga Provider" } $u1Headers) { param($r) $r.Status -eq 201 -and $r.Body.status -eq "CONFIRMED" }
$sagaAmount = [double]$sagaBooking.Body.amount
Add-Check "S3-F4 complete itinerary starts saga" (Invoke-JsonRequest PUT "$ItineraryBase/api/itineraries/$sagaItinId/complete" $null $u1Headers) { param($r) (Is-2xx $r) -and $r.Body.status -eq "COMPLETING" -and [double]$r.Body.estimatedBudget -ge $sagaAmount } | Out-Null
$null = Wait-For "S3-F4 payment.initiated drives PAYMENT_PENDING" { Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/$sagaItinId" $null $u1Headers } { param($r) (Is-2xx $r) -and $r.Body.status -eq "PAYMENT_PENDING" } 30 1
$settlement = Wait-For "S5 settlement row available and processable" {
    Invoke-JsonRequest POST "$BookingBase/api/bookings/settlement/process" @{ itineraryId = $sagaItinId; userId = $u1; amount = $sagaAmount; simulateFailure = $false } @{ "X-User-Id" = "$u1"; Authorization = $u1Headers.Authorization }
} { param($r) (Is-2xx $r) -and $r.Body.status -eq "COMPLETED" } 30 1
Add-Check "S5 settlement idempotent repeat returns prior result" (Invoke-JsonRequest POST "$BookingBase/api/bookings/settlement/process" @{ itineraryId = $sagaItinId; userId = $u1; amount = $sagaAmount; simulateFailure = $false } @{ "X-User-Id" = "$u1"; Authorization = $u1Headers.Authorization }) { param($r) (Is-2xx $r) -and $r.Body.status -eq "COMPLETED" } | Out-Null
$null = Wait-For "S3-F4 payment.completed drives PAID" { Invoke-JsonRequest GET "$ItineraryBase/api/itineraries/$sagaItinId" $null $u1Headers } { param($r) (Is-2xx $r) -and $r.Body.status -eq "PAID" } 30 1

Add-Check "GATEWAY protected endpoint rejects missing token" (Invoke-JsonRequest GET "$GatewayBase/api/users" $null @{}) { param($r) Is-Non2xx $r } | Out-Null
Add-Check "GATEWAY protected endpoint accepts valid token" (Invoke-JsonRequest GET "$GatewayBase/api/users" $null $admin) { param($r) Is-2xx $r } | Out-Null
Add-Check "GATEWAY health" (Invoke-JsonRequest GET "$GatewayBase/actuator/health" $null @{}) { param($r) Is-2xx $r } | Out-Null

$passed = ($checks | Where-Object OK).Count
$total = $checks.Count
$checks | Format-Table -AutoSize Name,Status,OK
Write-Host "M3 verifier summary: $passed/$total passed"

if ($passed -ne $total) {
    $checks | Where-Object { -not $_.OK } | Format-List
    exit 1
}
