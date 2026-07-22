package com.langchain4j.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识检索工具集
 *
 * 通过 RAG（检索增强生成）从公司政策文档中检索相关信息。
 * 四个工具分别对应不同的政策领域，但底层共用同一个 ContentRetriever。
 *
 * 工具分组的意义：
 * - TransactionTools 操作 MySQL 结构化数据
 * - KnowledgeTools 检索非结构化政策文档
 * 两组工具的能力边界完全不重叠，LLM 根据用户问题自主选择调用哪组工具。
 */
@Component("knowledgeTools")
@Slf4j
public class KnowledgeTools {

    @Autowired
    private ContentRetriever contentRetriever;

    /**
     * 通用检索方法：调用 ContentRetriever 执行向量相似度搜索
     */
    private List<String> retrieve(String query) {
        log.info("RAG 检索: {}", query);
        List<String> results = contentRetriever.retrieve(new Query(query))
                .stream()
                .map(content -> content.textSegment().text())
                .toList();
        log.info("RAG 检索返回 {} 条结果", results.size());
        return results;
    }

    @Tool("查询退换货政策：包括退货条件、期限、流程、退款规则等")
    public List<String> searchReturnPolicy(
            @P("用户关于退换货的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询保修政策：包括保修期限、保修范围、不保修的情况、延保服务等")
    public List<String> searchWarrantyPolicy(
            @P("用户关于保修的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询运费政策：包括退货运费承担规则、运费标准、包邮条件等")
    public List<String> searchShippingPolicy(
            @P("用户关于运费的问题") String query) {
        return retrieve(query);
    }

    @Tool("查询常见问题：包括如何申请退货、需要准备什么材料、退款多久到账等操作性问题")
    public List<String> searchFAQ(
            @P("用户的常见操作性问题") String query) {
        return retrieve(query);
    }
}
