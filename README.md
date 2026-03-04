# Introduction

This is  a simple file sharing app. It uses keycloak for registration and postgresql and a docker volume for storing files. Users can self register and share files between themselves. It is a small project to learn java and get better at managing keycloak, for example see the SPI implementation to keep a table of users exported and synced from keycloak using the EventListerner SPI. It was moderatly vibe coded, signatures of the methods, data modelling decisions etc were mine but I used claude to explore the best way to implement simple concepts that would have taken me no time in python but demanded a bit of reflection in java. Especially the collectors API.

# TODO
- integration testing, including with selenium
- caching with redis


