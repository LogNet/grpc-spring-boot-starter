#!/bin/bash

if [[ ${TRAVIS_PULL_REQUEST} == 'false' ]] && [[ ${TRAVIS_BRANCH} == 'master' ]] && [[ ${TRAVIS_TAG} == '' ]]; then
 #./gradlew -S publishToSonatype  closeAndReleaseRepository
 ./gradlew -S -Psigning.secretKeyRingFile="/home/travis/build/LogNet/grpc-spring-boot-starter/codesigning.asc" -p grpc-spring-boot-starter-gradle-plugin build publishToSonatype   closeAndReleaseRepository
# ./gradlew -S -p grpc-spring-boot-starter-gradle-plugin build publishPlugins
fi