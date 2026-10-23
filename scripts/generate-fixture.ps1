$ErrorActionPreference = 'Stop'
$taskDestination = Join-Path (Split-Path -Parent $PSScriptRoot) 'app/src/androidTest/assets/base.pdf'
$taskDirectory = Split-Path -Parent $taskDestination
New-Item -ItemType Directory -Force -Path $taskDirectory | Out-Null
$taskEncoding = [System.Text.Encoding]::ASCII
$taskContent = "BT /F1 14 Tf 30 340 Td (Original text preserved.) Tj ET`nq 40 0 0 40 30 240 cm /Im1 Do Q`n"
$taskSecondContent = "BT /F1 14 Tf 30 240 Td (Original rotated page.) Tj ET`nq 40 0 0 40 30 150 cm /Im1 Do Q`n"
$taskImage = 'FF000000FF000000FFFFFFFF>'
$taskObjects = @(
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R 7 0 R] /Count 2 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /Resources << /Font << /F1 5 0 R >> /XObject << /Im1 6 0 R >> >> /Contents 4 0 R /Annots [9 0 R] >>',
    "<< /Length $($taskEncoding.GetByteCount($taskContent)) >>`nstream`n${taskContent}endstream",
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    "<< /Type /XObject /Subtype /Image /Width 2 /Height 2 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /ASCIIHexDecode /Length $($taskImage.Length) >>`nstream`n${taskImage}`nendstream",
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 300] /CropBox [10 10 390 290] /Rotate 90 /Resources << /Font << /F1 5 0 R >> /XObject << /Im1 6 0 R >> >> /Contents 8 0 R >>',
    "<< /Length $($taskEncoding.GetByteCount($taskSecondContent)) >>`nstream`n${taskSecondContent}endstream",
    '<< /Type /Annot /Subtype /Text /Rect [250 250 270 270] /Contents (Original note) /NM (foreign-original) >>'
)
$taskBuilder = [System.Text.StringBuilder]::new("%PDF-1.7`n")
$taskOffsets = [System.Collections.Generic.List[int]]::new()
for ($taskIndex = 0; $taskIndex -lt $taskObjects.Count; $taskIndex++) {
    $taskOffsets.Add($taskEncoding.GetByteCount($taskBuilder.ToString()))
    [void]$taskBuilder.Append("$($taskIndex + 1) 0 obj`n$($taskObjects[$taskIndex])`nendobj`n")
}
$taskXref = $taskEncoding.GetByteCount($taskBuilder.ToString())
[void]$taskBuilder.Append("xref`n0 $($taskObjects.Count + 1)`n0000000000 65535 f `n")
foreach ($taskOffset in $taskOffsets) { [void]$taskBuilder.Append($taskOffset.ToString('0000000000') + " 00000 n `n") }
[void]$taskBuilder.Append("trailer`n<< /Size $($taskObjects.Count + 1) /Root 1 0 R >>`nstartxref`n$taskXref`n%%EOF`n")
[System.IO.File]::WriteAllBytes($taskDestination, $taskEncoding.GetBytes($taskBuilder.ToString()))
