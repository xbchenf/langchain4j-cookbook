package com.langchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {

    @Autowired
    private Assistant assistant;
    @Test
    public void testMemory(){
        String answer1 = assistant.chat(1,"我叫小白");
        System.out.println(answer1);
        String answer2 = assistant.chat(2,"我叫小红");
        System.out.println(answer2);
        String answer3 = assistant.chat(1,"我是谁？");
        System.out.println(answer3);
        String answer4 = assistant.chat(2,"我是谁？");
        System.out.println(answer4);

        /**
         * 输出结果：
         你好，小白！很高兴见到你。有任何我可以帮忙的地方吗？

         你好，小红！很高兴认识你！有什么我可以帮助你的吗？

         你叫小白！如果你想聊聊自己或者有其他问题，我很乐意帮助你。

         你是小红！如果你想分享更多关于你自己的信息或者有特定的问题，可以告诉我，我会尽力帮助你！
         */
    }
}
