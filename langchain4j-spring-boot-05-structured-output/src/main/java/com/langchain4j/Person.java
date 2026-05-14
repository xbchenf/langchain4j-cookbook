package com.langchain4j;

import jdk.jfr.Description;
import lombok.Data;

@Data
public class Person {
    @Description("姓名")
    private String name;

    @Description("年龄")
    private int age;
}
