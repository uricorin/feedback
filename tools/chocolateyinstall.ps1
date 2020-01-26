
$ErrorActionPreference = 'Stop'; # stop on all errors

$packageName= 'feedback'
$toolsDir   = "$(Split-Path -Parent $MyInvocation.MyCommand.Definition)"
$fileLocation = "$env:RDZ_HOME\eclipsec.exe"
$updateSite = "jar:file:/" + (Get-Item -path $toolsDir\cloud.corin.feedback.update*.zip).FullName + "!/"

$packageArgs = @{
  packageName   = $packageName
  fileType      = 'exe'
  file          = $fileLocation
  silentArgs    = "-nl en -noSplash -consoleLog -application org.eclipse.equinox.p2.director -installIU compilationfeedback -r $updateSite"
  validExitCodes= @(0)
  softwareName  = 'FEEDBACK*'
}

Install-ChocolateyInstallPackage @packageArgs

