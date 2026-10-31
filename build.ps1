[CmdletBinding()]
param(
    [string]$Version = "1.2.4",
    [string]$JavaFxVersion = "21.0.4",
    [ValidateSet("all", "win", "linux", "mac", "mac-aarch64")]
    [string]$Classifier = "all"
)

$ErrorActionPreference = "Stop"
# Reuse the single-platform build, including its dependency cache and checks.
if ($Classifier -eq "all") {
    foreach ($targetClassifier in @("win", "linux", "mac", "mac-aarch64")) {
        Write-Host "[build] Building platform $targetClassifier"
        & $PSCommandPath -Version $Version -JavaFxVersion $JavaFxVersion `
                -Classifier $targetClassifier
    }
    Write-Host "[done] All four platform presets are available in dist/"
    return
}
$Classifier = $Classifier.ToLowerInvariant()

$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$providedApiDir = Join-Path $projectRoot "provided-api"
$sourceDir = Join-Path $projectRoot "src"
$testDir = Join-Path $projectRoot "test"
$resourceDir = Join-Path $projectRoot "resources"
$metadataDir = Join-Path $projectRoot "metadata/META-INF"
$libraryDir = Join-Path $projectRoot "lib/javafx"
$buildDir = Join-Path $projectRoot "build"
$classesDir = Join-Path $buildDir "classes"
$hostClassesDir = Join-Path $buildDir "host-classes"
$hostApiJar = Join-Path $buildDir "host-api.jar"
$presetClassesJar = Join-Path $buildDir "preset-classes.jar"
$testClassesDir = Join-Path $buildDir "test-classes"
$stageDir = Join-Path $buildDir "stage"
$distDir = Join-Path $projectRoot "dist"
$artifact = Join-Path $distDir "fireflytornado-javafx-preset-$Version-$Classifier.jar"
$stagedArtifact = Join-Path $buildDir "preset.jar"

function Get-Sha256([string]$Path) {
    $stream = [IO.File]::OpenRead($Path)
    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha256.ComputeHash($stream))).
                Replace("-", "").ToLowerInvariant()
    } finally {
        $sha256.Dispose()
        $stream.Dispose()
    }
}

# Keep other platforms and versions in dist/ when building one target.
foreach ($path in @($buildDir)) {
    if (Test-Path -LiteralPath $path) {
        $resolved = [IO.Path]::GetFullPath($path)
        if (-not $resolved.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar)) {
            throw "Refusing to clean a path outside the preset project: $resolved"
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}

New-Item -ItemType Directory -Force -Path $classesDir, $hostClassesDir, $testClassesDir,
        (Join-Path $stageDir "META-INF/licenses"), (Join-Path $stageDir "runtime"),
        $libraryDir, $distDir | Out-Null

try {
$modules = @("javafx-base", "javafx-graphics", "javafx-controls")
$pinnedHashes = @{
    "21.0.4/win/javafx-base" =
            "daedb2fe921bf1c03c43f032078f01f6201d5ffa86ce353f4bac77b0d7eae346"
    "21.0.4/win/javafx-graphics" =
            "f0a0e80d0a4c75966070823d92bf9c051d34c1fa75525e9ef7687d15a918799c"
    "21.0.4/win/javafx-controls" =
            "1a958b25299bfda612475c6062b98002178124cab3cc5f76e82b6ead21cc7a6d"
    "21.0.4/linux/javafx-base" =
            "cbddc7d0127ea399c36e348a53c8361f1940a9e94c61446f075436a9947b2c68"
    "21.0.4/linux/javafx-graphics" =
            "43e6c26cb42821f68709a0c29eaa5723b609712f88e390fcbb7991ea25661967"
    "21.0.4/linux/javafx-controls" =
            "1f446fcd63cd26c9b3d5631291c9cbc14aa5240b98cf631a88ba5aa4eefa1631"
    "21.0.4/mac/javafx-base" =
            "c22dbf1dbc2b15ae891a24fb19ac5159448bbbaf592f7be8b584b8205309bec0"
    "21.0.4/mac/javafx-graphics" =
            "77c4faca3ccfe0322737c5c892ff19abef172ddba57a61e2c074db6ea5d3fab0"
    "21.0.4/mac/javafx-controls" =
            "4589696ac0e619b0741bbee886965a620de7042dd3f40311858cc31c76451a70"
    "21.0.4/mac-aarch64/javafx-base" =
            "b3a64c149b866ada621469d7f83bcbafd0731902bf445dbcf7c08d035e13a0d1"
    "21.0.4/mac-aarch64/javafx-graphics" =
            "230c75468436d4fdbcdafea03992b3595347e3681f06281c02ed5fc0fa3d629b"
    "21.0.4/mac-aarch64/javafx-controls" =
            "325f316969b8cf92e6b0f0bfc9a9ef9cf3d072f59cf0fe08228471e6d581a589"
}
$runtimeFiles = @{}
foreach ($module in $modules) {
    $fileName = "$module-$JavaFxVersion-$Classifier.jar"
    $target = Join-Path $libraryDir $fileName
    $pinKey = "$JavaFxVersion/$Classifier/$module"
    if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
        $url = "https://repo.maven.apache.org/maven2/org/openjfx/$module/$JavaFxVersion/$fileName"
        $temporary = "$target.download"
        Write-Host "[deps] Downloading $fileName from Maven Central"
        try {
            $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
            if ($null -ne $curl) {
                & $curl.Source --fail --location --retry 3 --retry-delay 2 `
                        --output $temporary $url
                if ($LASTEXITCODE -ne 0) {
                    throw "curl failed to download $fileName"
                }
            } else {
                Invoke-WebRequest -Uri $url -OutFile $temporary
            }
            if ($pinnedHashes.ContainsKey($pinKey) -and
                    (Get-Sha256 $temporary) -ne $pinnedHashes[$pinKey]) {
                throw "SHA-256 mismatch for downloaded $fileName"
            }
            Move-Item -LiteralPath $temporary -Destination $target
        } finally {
            if (Test-Path -LiteralPath $temporary) {
                Remove-Item -LiteralPath $temporary -Force
            }
        }
    }
    if ($pinnedHashes.ContainsKey($pinKey)) {
        $actualHash = Get-Sha256 $target
        if ($actualHash -ne $pinnedHashes[$pinKey]) {
            throw "SHA-256 mismatch for $fileName"
        }
        Write-Host "[deps] Verified $fileName"
    } else {
        Write-Warning "No pinned SHA-256 is configured for $fileName"
    }
    $runtimeFiles[$module] = $target
}

$providedApiFiles = @(Get-ChildItem -LiteralPath $providedApiDir -Recurse -Filter "*.java" |
        ForEach-Object { $_.FullName })
if ($providedApiFiles.Count -eq 0) {
    throw "No provided updater API sources found in $providedApiDir"
}
Write-Host "[api] Compiling the repository-local updater API contract"
& javac --release 17 -encoding UTF-8 -d $hostClassesDir @providedApiFiles
if ($LASTEXITCODE -ne 0) {
    throw "Provided updater API compilation failed"
}
& jar cf $hostApiJar -C $hostClassesDir .
if ($LASTEXITCODE -ne 0) {
    throw "Unable to package the temporary updater API classpath"
}
# javac on Windows is unreliable with a class-directory path containing spaces;
# the equivalent temporary JAR is deterministic and remains inside build/.
$hostClasspath = $hostApiJar

$sourceFiles = @(Get-ChildItem -LiteralPath $sourceDir -Recurse -Filter "*.java" |
        ForEach-Object { $_.FullName })
if ($sourceFiles.Count -eq 0) {
    throw "No preset Java sources found"
}

$compileClasspath = (@($hostClasspath) + @($modules | ForEach-Object {
    $runtimeFiles[$_]
})) -join [IO.Path]::PathSeparator
Write-Host "[build] Compiling preset sources"
& javac --release 17 -encoding UTF-8 -cp $compileClasspath -d $classesDir @sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Preset compilation failed"
}
& jar cf $presetClassesJar -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "Unable to package the temporary preset test classpath"
}

$testFiles = @()
$bootstrapSmokeTest = Join-Path $testDir `
        "com/fireflytornado/mcupdate/javafx/PresetBootstrapSmokeTest.java"
if (Test-Path -LiteralPath $bootstrapSmokeTest -PathType Leaf) {
    $testFiles = @($bootstrapSmokeTest)
}
$testClasspath = "$hostClasspath$([IO.Path]::PathSeparator)$presetClassesJar"
if ($testFiles.Count -gt 0) {
    & javac --release 17 -encoding UTF-8 -cp $testClasspath -d $testClassesDir @testFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Preset smoke-test compilation failed"
    }
    & java -cp "$testClasspath$([IO.Path]::PathSeparator)$testClassesDir" `
            com.fireflytornado.mcupdate.javafx.PresetBootstrapSmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "Preset bootstrap smoke test failed"
    }
}

Copy-Item -Path (Join-Path $classesDir "*") -Destination $stageDir -Recurse
Copy-Item -LiteralPath (Join-Path $resourceDir "ui.css") -Destination $stageDir
Copy-Item -LiteralPath (Join-Path $resourceDir "images") -Destination $stageDir -Recurse
$languageFiles = @("messages.properties", "messages_zh_CN.properties",
        "messages_zh_TW.properties")
$baseLanguageKeys = @()
foreach ($languageFile in $languageFiles) {
    $languagePath = Join-Path $resourceDir "lang/$languageFile"
    # Windows PowerShell 5.1 otherwise decodes BOM-less UTF-8 property files
    # with the active ANSI code page. Some resulting control characters are
    # treated as line breaks, making valid Chinese bundles appear to lose keys.
    $languageKeys = @(Get-Content -LiteralPath $languagePath -Encoding UTF8 |
            Where-Object { $_ -match '^[^#!\s][^=]*=' } |
            ForEach-Object { ($_ -split '=', 2)[0] } |
            Sort-Object)
    if ($baseLanguageKeys.Count -eq 0) {
        $baseLanguageKeys = $languageKeys
    } elseif (Compare-Object $baseLanguageKeys $languageKeys) {
        throw "Language bundle keys do not match: $languageFile"
    }
}
Copy-Item -LiteralPath (Join-Path $resourceDir "lang") -Destination $stageDir -Recurse
Copy-Item -LiteralPath (Join-Path $metadataDir "mc-update-gui.properties") `
        -Destination (Join-Path $stageDir "META-INF/mc-update-gui.properties")
Copy-Item -LiteralPath (Join-Path $projectRoot "LICENSE") `
        -Destination (Join-Path $stageDir "META-INF/LICENSE")
Copy-Item -LiteralPath (Join-Path $projectRoot "NOTICE") `
        -Destination (Join-Path $stageDir "META-INF/NOTICE")
Copy-Item -LiteralPath (Join-Path $projectRoot "THIRD_PARTY_NOTICES.md") `
        -Destination (Join-Path $stageDir "META-INF/THIRD_PARTY_NOTICES.md")
Copy-Item -LiteralPath (Join-Path $projectRoot "licenses/openjfx") `
        -Destination (Join-Path $stageDir "META-INF/licenses") -Recurse

foreach ($module in $modules) {
    Copy-Item -LiteralPath $runtimeFiles[$module] -Destination (Join-Path $stageDir "runtime")
}

$runtimeTemplate = Get-Content -Raw -LiteralPath `
        (Join-Path $metadataDir "mc-update-runtime.properties.template")
$hashes = @{}
foreach ($module in $modules) {
    $hashes[$module] = Get-Sha256 $runtimeFiles[$module]
}
$runtimeManifest = $runtimeTemplate.Replace("21.0.4", $JavaFxVersion).
        Replace("-win.jar", "-$Classifier.jar").
        Replace("@JAVAFX_BASE_SHA256@", $hashes["javafx-base"]).
        Replace("@JAVAFX_GRAPHICS_SHA256@", $hashes["javafx-graphics"]).
        Replace("@JAVAFX_CONTROLS_SHA256@", $hashes["javafx-controls"])
Set-Content -LiteralPath (Join-Path $stageDir "META-INF/mc-update-runtime.properties") `
        -Value $runtimeManifest -Encoding ascii -NoNewline

$guiMetadataPath = Join-Path $stageDir "META-INF/mc-update-gui.properties"
$guiMetadata = (Get-Content -Raw -LiteralPath $guiMetadataPath) `
        -replace '(?m)^version=.*$', "version=$Version"
Set-Content -LiteralPath $guiMetadataPath -Value $guiMetadata -Encoding ascii -NoNewline

Write-Host "[build] Packaging $artifact"
Push-Location $stageDir
try {
    & jar cf $stagedArtifact .
    if ($LASTEXITCODE -ne 0) {
        throw "Preset packaging failed"
    }
} finally {
    Pop-Location
}

$entries = @(& jar tf $stagedArtifact)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect the packaged preset"
}
$requiredEntries = @(
    "META-INF/mc-update-gui.properties",
    "META-INF/mc-update-runtime.properties",
    "META-INF/LICENSE",
    "META-INF/NOTICE",
    "META-INF/THIRD_PARTY_NOTICES.md",
    "META-INF/licenses/openjfx/LICENSE",
    "META-INF/licenses/openjfx/ADDITIONAL_LICENSE_INFO",
    "META-INF/licenses/openjfx/ASSEMBLY_EXCEPTION",
    "com/fireflytornado/mcupdate/javafx/JavaFxPresetFactory.class",
    "com/fireflytornado/mcupdate/javafx/JavaFxPresetEntrypoint.class",
    "ui.css",
    "lang/messages.properties",
    "lang/messages_zh_CN.properties",
    "lang/messages_zh_TW.properties",
    "images/preparing.png",
    "runtime/javafx-base-$JavaFxVersion-$Classifier.jar",
    "runtime/javafx-graphics-$JavaFxVersion-$Classifier.jar",
    "runtime/javafx-controls-$JavaFxVersion-$Classifier.jar"
)
foreach ($entry in $requiredEntries) {
    if ($entries -notcontains $entry) {
        throw "Packaged preset is missing required entry: $entry"
    }
}
if ($entries | Where-Object { $_ -like "com/zack88604/autoupdater/gui/api/*" }) {
    throw "Preset must not bundle updater API classes"
}
$packagedRuntime = @($entries | Where-Object { $_ -like "runtime/*.jar" })
if ($packagedRuntime.Count -ne $modules.Count) {
    throw "Preset must bundle exactly the three runtime JARs for $Classifier"
}

$factoryBytecode = (& javap -classpath "$hostClasspath$([IO.Path]::PathSeparator)$stagedArtifact" `
        -verbose com.fireflytornado.mcupdate.javafx.JavaFxPresetFactory) -join "`n"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect preset bootstrap bytecode"
}
if ($factoryBytecode -match "javafx/(application|animation|beans|collections|css|event|fxml|geometry|scene|stage|util|embed|print|concurrent)/" ) {
    throw "Bootstrap factory unexpectedly references JavaFX"
}

$artifactHash = Get-Sha256 $stagedArtifact
Move-Item -LiteralPath $stagedArtifact -Destination $artifact -Force
Write-Host "[verify] Preset structure and bootstrap isolation passed"
Write-Host "[done] $artifact"
Write-Host "[sha256] $artifactHash"
} finally {
    if (Test-Path -LiteralPath $buildDir) {
        $resolvedBuild = [IO.Path]::GetFullPath($buildDir)
        if (-not $resolvedBuild.StartsWith(
                $projectRoot + [IO.Path]::DirectorySeparatorChar)) {
            throw "Refusing to clean a build path outside the preset project"
        }
        Remove-Item -LiteralPath $resolvedBuild -Recurse -Force
    }
}
