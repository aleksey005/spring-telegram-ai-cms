// /src/main/java/ru/simshp/qdrant/bootstrap/QdrantBootstrap.java
package ru.simshp.qdrant.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.simshp.qdrant.qdrant.QdrantClient;

@Component
@RequiredArgsConstructor
public class QdrantBootstrap implements ApplicationRunner {

    private final QdrantClient qdrantClient;

    @Override
    public void run(ApplicationArguments args) {
        qdrantClient.ensureCollection();
    }
}
