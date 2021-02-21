# kafka-to-postgres-projections
### Test
    ./gradlew clean test

### Run

Add alias to localhost for kafka

    sudo sh -c 'echo "127.0.0.1   foo" >> /etc/hosts'

Run Kafka and Postgres in Docker

    docker-compose up -d kafka postgres

Run application

    ./gradle run
