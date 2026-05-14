package com.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;

public class MyDocumentSplitter implements DocumentSplitter {


    // 使用两个或以上空行分割文档，保持问题和答案在同一片段中
    public static final String SPLIT_REGEX = "\\s*\\R\\s*\\R\\s*\\R\\s*";
    @Override
    public List<TextSegment> split(Document document) {
        List<TextSegment> segments = new ArrayList<>();
        String[] parts = document.text().split(SPLIT_REGEX);
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                segments.add(TextSegment.from(part));
            }
        }
        return segments;
    }
}
