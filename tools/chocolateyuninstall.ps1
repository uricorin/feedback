
$ErrorActionPreference = 'Stop'; # stop on all errors

# Remove files


# Remove Event log


# Remove environment variables
#Uninstall-ChocolateyEnvironmentVariable -VariableName "RDZ_SCRIPT_PATH" -VariableValue $pp.RDZ_SCRIPT_PATH -VariableType "Machine"
#Uninstall-ChocolateyEnvironmentVariable -VariableName "RDZ_PROFILES_PATH" -VariableValue $pp.RDZ_PROFILES_PATH -VariableType "Machine"


$packageName= 'feedback'
$fileLocation = "$env:RDZ_HOME\eclipsec.exe"

$packageArgs = @{
  packageName   = $packageName
  fileType      = 'exe'
  file          = $fileLocation
  silentArgs    = "-nl en -noSplash -consoleLog -application org.eclipse.equinox.p2.director -uninstallIU compilationfeedback"
  validExitCodes= @(0)
}
Uninstall-ChocolateyPackage @packageArgs