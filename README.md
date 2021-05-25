# ScavengeNerd 2021 Back End

Spring Boot based REST API for the 2021 ScavengeNerd season

## Build and deploy for production
- Create .zip file with the `.platform` folder at the root
- Run Gradle task `bootJar`
- Add .jar file from the `bootJar` task (located in the `build>libs` folder) also to the root of the .zip file
- Upload the .zip file to Elastic Beanstalk

## Data Loading
Initial data load is in data.sql

## TODO
- list items
  - allow sort (status, name, tier)
  - allow filter by name