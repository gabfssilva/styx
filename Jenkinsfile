node {
    stage('Clone sources') {
        git url: 'https://github.com/gabfssilva/styx'
    }

    stage('Gradle build') {
         sh './gradlew clean build'
    }
}