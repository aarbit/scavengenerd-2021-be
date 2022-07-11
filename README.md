# ScavengeNerd 2022 Back End

Spring Boot based REST API for the 2022 ScavengeNerd season

## Build and deploy for production
- Create .zip file with the `.platform` folder at the root
- Run Gradle task `bootJar`
- Add .jar file from the `bootJar` task (located in the `build>libs` folder) also to the root of the .zip file
- Upload the .zip file to Elastic Beanstalk

## Data Loading
Initial test data load is in import.sql