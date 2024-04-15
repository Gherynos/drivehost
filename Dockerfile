# Dockerfile for Drive Host
#
##

# base image
FROM maven:3.3.9-jdk-8-onbuild

# maintainer
MAINTAINER Gherynos <gherynos.com>

# create cache directory
RUN mkdir /var/cache/drivehost

# TCP ports
EXPOSE 8080

# run command
CMD ["java", "-jar", "target/drivehost-jar-with-dependencies.jar"]
