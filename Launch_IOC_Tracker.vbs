' Indian Oil Bill Tracker - Silent Launcher
' This VBScript launches the application completely silently without any terminal windows

Set objShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

' Get the directory where this script is located
strScriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)

' Change to the application directory
objShell.CurrentDirectory = strScriptDir

' Build the command to run the Java application
strCommand = "javaw -cp ""target\classes;lib\jcalendar-1.4.jar"" com.login.LoginApp_Fixed"

' Run the command hidden (0 = hidden window, False = don't wait for completion)
objShell.Run strCommand, 0, False

' Clean up objects
Set objShell = Nothing
Set objFSO = Nothing
