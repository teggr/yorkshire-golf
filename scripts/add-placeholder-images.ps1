# Script to add placeholder mainImageUrl to courses without images
$coursesDir = "src\main\resources\courses"
$placeholderUrl = "/images/courses/placeholder-course.jpg"
$updated = 0

Get-ChildItem "$coursesDir\*.yaml" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    
    # Check if mainImageUrl already exists
    if ($content -notmatch 'mainImageUrl:') {
        Write-Host "Adding placeholder to: $($_.Name)" -ForegroundColor Yellow
        
        # Add mainImageUrl after website field (or after region if no website)
        if ($content -match '(website:.*?\n)') {
            $content = $content -replace '(website:.*?\n)', "`$1mainImageUrl: `"$placeholderUrl`"`n"
        } elseif ($content -match '(region:\s*\n\s*name:.*?\n)') {
            $content = $content -replace '(region:\s*\n\s*name:.*?\n)', "`$1mainImageUrl: `"$placeholderUrl`"`n"
        }
        
        Set-Content -Path $_.FullName -Value $content -NoNewline
        $updated++
    }
}

Write-Host "`nUpdated $updated courses with placeholder image" -ForegroundColor Green
