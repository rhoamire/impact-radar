package com.impactradar.service;

import java.util.List;

public interface EmbeddingProvider {

  

    List<Float> embed(String text);

    String model();

    int dimensions();
}