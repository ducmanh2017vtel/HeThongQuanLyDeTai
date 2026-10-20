# Convert old-style templates to new layout pattern
# OLD pattern: <div th:replace="~{fragments/layout.html :: sidebar}">
# NEW pattern: <section class="page"> content </section>
#   + template: th:replace="~{fragments/layout :: layout('Title', 'active', ~{::section.page})}"
#   + body: <section class="page">...</section>

$ErrorActionPreference = 'SilentlyContinue'
$base = 'D:\Student Project Registration Page\src\main\resources\templates'

# Templates that ALREADY use the new pattern (no conversion needed)
$alreadyConverted = @(
    'dashboard.html',
    'login.html',
    'admin\periods\list.html',
    'admin\periods\form.html'
)

# Templates needing conversion (groups 1-4)
$toConvert = @(
    # Admin periods detail
    'admin\periods\detail.html',
    # Topics (lecturer)
    'topics\list.html',
    'topics\form.html',
    'topics\detail.html',
    # Admin topics
    'admin\topics\list.html',
    'admin\topics\detail.html',
    # Registrations (approval)
    'registrations\list.html',
    'registrations\detail.html',
    # Groups
    'groups\list.html',
    'groups\form.html',
    'groups\detail.html',
    # Reports
    'reports\index.html',
    'reports\list.html',
    'reports\upload.html',
    # Councils
    'councils\list.html',
    'councils\form.html',
    'councils\detail.html',
    # Assignments
    'assignments\list.html',
    # Evaluations
    'evaluations\list.html',
    'evaluations\form.html',
    # Results
    'results\me.html',
    'results\manage.html',
    # Notices
    'notices\list.html',
    'notices\form.html',
    'notices\detail.html',
    'notices\manage.html',
    # Admin users
    'admin\users\list.html',
    'admin\users\form.html',
    # Admin roles
    'admin\roles\list.html',
    'admin\roles\form.html',
    # Admin permissions
    'admin\permissions\list.html',
    'admin\permissions\assign.html',
    # Me
    'me\profile.html',
    'me\change-password.html',
    # Errors
    'error\403.html',
    'error\404.html',
    'error\500.html'
)

# Templates that DON'T need the layout (standalone or error pages)
$standalone = @(
    'error\403.html',
    'error\404.html',
    'error\500.html'
)

$converted = 0
$skipped = 0
$errors = @()

foreach ($relPath in $toConvert) {
    $fullPath = Join-Path $base $relPath
    
    if (-not (Test-Path $fullPath)) {
        Write-Host "SKIP (not found): $relPath" -ForegroundColor DarkGray
        $skipped++
        continue
    }
    
    $content = Get-Content $fullPath -Raw -ErrorAction SilentlyContinue
    if ($content -match 'th:replace="~{fragments/layout :: layout') {
        Write-Host "SKIP (already new pattern): $relPath" -ForegroundColor DarkGray
        $skipped++
        continue
    }
    
    # Extract page title from controller or guess from path
    $fileName = Split-Path $relPath -Leaf
    $sectionName = $fileName -replace '\.html$', ''
    
    # Determine active menu from path
    $active = switch -Regex ($relPath) {
        'admin\\periods' { 'periods' }
        'topics' { 'topics' }
        'registrations' { 'groups' }
        'groups' { 'groups' }
        'reports' { 'reports' }
        'councils' { 'councils' }
        'assignments' { 'assignments' }
        'evaluations' { 'evaluations' }
        'results' { 'results-manage' }
        'notices' { 'notices' }
        'admin\\users' { 'users' }
        'admin\\roles' { 'roles' }
        'admin\\permissions' { 'permissions' }
        'me\\' { '' }
        default { '' }
    }
    
    # Determine title
    $title = switch -Regex ($relPath) {
        'detail\.html' { 'Chi tiết' }
        'form\.html' { 'Biểu mẫu' }
        'list\.html' { 'Danh sách' }
        'manage\.html' { 'Quản lý' }
        'profile\.html' { 'Hồ sơ' }
        'change-password\.html' { 'Đổi mật khẩu' }
        default { '' }
    }
    
    $dirName = ($relPath -split '\\')[0] -replace 'admin\\', ''
    if ($dirName) {
        $title = $dirName -replace 'admin\\', '' -replace 's$', '' -replace '_', ' ' + ' ' + $title
        $title = (Get-Culture).TextInfo.ToTitleCase($title.Trim())
    }
    
    # Read the original file to find its content structure
    $lines = Get-Content $fullPath
    
    # Find body content (between <body> and </body>)
    $bodyStart = -1
    $bodyEnd = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '<body[^>]*>') { $bodyStart = $i }
        if ($lines[$i] -match '</body>') { $bodyEnd = $i; break }
    }
    
    if ($bodyStart -ge 0 -and $bodyEnd -gt $bodyStart) {
        $bodyContent = $lines[($bodyStart + 1)..($bodyEnd - 1)] -join "`n"
        
        # Check for app-layout vs other layouts
        $usesAppLayout = $bodyContent -match 'app-layout|class="app-layout"'
        
        if ($usesAppLayout) {
            # Replace app-layout div structure with section.page
            $newBody = $bodyContent
            $newBody = $newBody -replace '<div class="app-layout">', '<section class="page">'
            $newBody = $newBody -replace '</div>\s*</div>\s*</body>', '</section>`n</body>'
            $newBody = $newBody -replace '<div class="main">', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: (sidebar|topbar|flash|page-header|badge|pagination|scripts)}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: topbar}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: flash}"[^>]*>', ''
            $newBody = $newBody -replace '<main class="content">', ''
            $newBody = $newBody -replace '</main>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: page-header}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: sidebar}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: scripts}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: pagination\([^)]+\)}"[^>]*>', ''
            
            # Build new template
            $headBlock = '<head th:replace="~{fragments/layout :: head}"></head>'
            $sectionBlock = "<section class=`"page`">`n" + $newBody.Trim() + "`n</section>"
            $newContent = "<!DOCTYPE html>`n<html lang=`"vi`" xmlns:th=`"http://www.thymeleaf.org`"`n" +
                "      th:replace=`"~{fragments/layout :: layout('$title', '$active', ~{{::section.page}})}`"`n" +
                "<body>`n$sectionBlock`n</body>`n</html>"
        } else {
            # Generic approach: wrap everything in body with section.page
            $newBody = $bodyContent
            
            # Remove old fragment replaces
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: (sidebar|topbar|flash|page-header|badge|pagination|scripts)}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: topbar}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: flash}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: sidebar}"[^>]*>', ''
            $newBody = $newBody -replace '<div th:replace="~{fragments/layout.html :: scripts}"[^>]*>', ''
            
            $sectionBlock = "<section class=`"page`">`n" + $newBody.Trim() + "`n</section>"
            $newContent = "<!DOCTYPE html>`n<html lang=`"vi`" xmlns:th=`"http://www.thymeleaf.org`"`n" +
                "      th:replace=`"~{fragments/layout :: layout('$title', '$active', ~{{::section.page}})}`"`n" +
                "<body>`n$sectionBlock`n</body>`n</html>"
        }
        
        Set-Content -Path $fullPath -Value $newContent -NoNewline -ErrorAction SilentlyContinue
        Write-Host "CONVERTED: $relPath" -ForegroundColor Cyan
        $converted++
    } else {
        Write-Host "ERROR (no body): $relPath" -ForegroundColor Red
        $errors += $relPath
    }
}

Write-Host "`nTotal: converted=$converted skipped=$skipped errors=$($errors.Count)" -ForegroundColor Green
if ($errors.Count -gt 0) {
    Write-Host "Errors:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
}
