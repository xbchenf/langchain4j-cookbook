package com.langchain4j.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, 
                    new com.google.gson.TypeAdapter<LocalDateTime>() {
                        @Override
                        public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws java.io.IOException {
                            if (value == null) {
                                out.nullValue();
                            } else {
                                out.value(value.toString());
                            }
                        }
                        
                        @Override
                        public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                            return LocalDateTime.parse(in.nextString());
                        }
                    })
                .create();
    }
}
