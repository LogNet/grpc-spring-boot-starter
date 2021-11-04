#!/bin/bash

if [[ ${TRAVIS_PULL_REQUEST} == 'false' ]] && [[ ${TRAVIS_BRANCH} == 'master' ]] && [[ ${TRAVIS_TAG} == '' ]]; then
 ./gradlew -S publishToSonatype  closeAndReleaseRepository
 ./gradlew -S -p grpc-spring-boot-starter-gradle-plugin build publishToSonatype   closeAndReleaseRepository publishPlugins
fi