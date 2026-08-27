[Setup]
AppId={{9AA91CA2-AD9C-4BBE-992E-ARNIKA200000}
AppName=ARNIKA SESSION
AppVersion=2.0.0
AppPublisher=ARNIKA
DefaultDirName={autopf}\ARNIKA SESSION
DefaultGroupName=ARNIKA SESSION
OutputDir=installer_out
OutputBaseFilename=ARNIKA_SESSION_Windows_Setup_v2.0.0
Compression=lzma2/ultra64
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
PrivilegesRequired=lowest

[Files]
Source: "publish\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\ARNIKA SESSION"; Filename: "{app}\ARNIKA_SESSION.exe"
Name: "{autodesktop}\ARNIKA SESSION"; Filename: "{app}\ARNIKA_SESSION.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "ایجاد میانبر روی Desktop"; GroupDescription: "میانبرها:"

[Run]
Filename: "{app}\ARNIKA_SESSION.exe"; Description: "اجرای ARNIKA SESSION"; Flags: nowait postinstall skipifsilent
