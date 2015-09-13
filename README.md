# facerecognition
To use this program you must first add pictures to the database. Add pictures (preferably 1080x720) into 'common/src/main/resources/recognition/training' named in the following manner:

\<person-id>-\<person-name>_\<picture-id>.\<file-extension>

e.g.

1-Alice_1.jpg

1-Alice_2.jpg

1-Alice_3.jpg

2-Bob_1.jpg

2-Bob_2.jpg

...

### Generate Intellij Project: 
./gradlew ideaModule


### Run the REST-service: 
./gradlew service:run

###  Run the client:
./gradlew app:run


### Using jar files: 
./gradlew assemble

java -jar service/build/libs/facerecog-service.jar

java -jar app/build/libs/facerecog-app.jar


