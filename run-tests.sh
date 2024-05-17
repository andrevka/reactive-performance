docker-compose down -v

mvn -f hello-client/pom.xml spring-boot:build-image &
mvn -f hello-reactive-client/pom.xml spring-boot:build-image &
mvn -f hello-reactive-server/pom.xml spring-boot:build-image &

wait

docker-compose up --build
