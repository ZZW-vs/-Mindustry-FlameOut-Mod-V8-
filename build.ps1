$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "d:\mc\Mindustry\mod参考\FlameOut熄灭\熄灭修复"
.\gradlew.bat jar
