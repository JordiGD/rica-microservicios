package com.rica.investigadores_service.infraestructura.salida.mensajeria;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicosConfig {

    @Bean
    NewTopic investigadoresRegistrados() {
        return TopicBuilder.name("investigadores.registrados").partitions(1).replicas(1).build();
    }

    @Bean 
    NewTopic investigadoresEnEliminacion() { 
        return TopicBuilder.name("investigadores.en-eliminacion").partitions(1).replicas(1).build(); 
    }

    @Bean 
    NewTopic publicacionesMarcadasHuerfanas() { 
        return TopicBuilder.name("publicaciones.marcadas-huerfanas").partitions(1).replicas(1).build(); 
    }

}
