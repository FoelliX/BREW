cd target/build
mkdir answers
mkdir output
cd data
mkdir storage
cd ..

"%JAVA_HOME%\bin\jar.exe" uf %1 tool.properties
del tool.properties