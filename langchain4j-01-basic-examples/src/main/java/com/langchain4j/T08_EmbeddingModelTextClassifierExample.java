package com.langchain4j;

import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.TextClassifier;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.langchain4j.T08_EmbeddingModelTextClassifierExample.CustomerServiceCategory.*;
import static java.util.Arrays.asList;

/**
 * 基于嵌入模型的文本分类器示例
 *
 * 本示例演示如何使用 EmbeddingModelTextClassifier 对客户服务咨询进行自动分类。
 * 核心原理：
 * 1. 为每个类别提供多个示例文本
 * 2. 使用嵌入模型将文本转换为向量
 * 3. 通过计算待分类文本与各示例的向量相似度，确定最匹配的类别
 *
 * 优势：
 * - 无需训练复杂的机器学习模型
 * - 只需提供少量示例即可工作
 * - 支持多标签分类（一个文本可能属于多个类别）
 */
public class T08_EmbeddingModelTextClassifierExample {

    /**
     * 客户服务分类枚举
     * 定义了7种常见的客户咨询类型
     */
    enum CustomerServiceCategory {
        BILLING_AND_PAYMENTS,        // 账单与支付
        TECHNICAL_SUPPORT,           // 技术支持
        ACCOUNT_MANAGEMENT,          // 账户管理
        PRODUCT_INFORMATION,         // 产品信息
        ORDER_STATUS,                // 订单状态
        RETURNS_AND_EXCHANGES,       // 退货与换货
        FEEDBACK_AND_COMPLAINTS      // 反馈与投诉
    }

    public static void main(String[] args) {
        // 构建分类示例数据集：每个类别对应一组典型的客户咨询文本
        Map<CustomerServiceCategory, List<String>> examples = new HashMap<>();
            
        // 账单与支付类示例：包含支付方式、退款、发票、订阅等问题
        examples.put(BILLING_AND_PAYMENTS, asList(
                "Can I pay using PayPal?",
                "Do you accept Bitcoin?",
                "Is it possible to pay via wire transfer?",
                "I keep getting an error message when I try to pay.",
                "My card was charged twice, can you help?",
                "Why was my payment declined?",
                "How can I request a refund?",
                "When will I get my refund?",
                "Can I get a refund if I cancel my subscription?",
                "Can you send me an invoice for my last order?",
                "I didn't receive a receipt for my purchase.",
                "Is the invoice sent to my email automatically?",
                "How do I upgrade my subscription?",
                "What are the differences between the Basic and Premium plans?",
                "How do I cancel my subscription?",
                "Can I switch to a monthly plan from an annual one?",
                "I want to downgrade my subscription, how do I go about it?",
                "Is there a penalty for downgrading my plan?"
        ));
            
        // 技术支持类示例：包含应用崩溃、安装问题、网络连接、性能问题等
        examples.put(TECHNICAL_SUPPORT, asList(
                "The app keeps crashing whenever I open it.",
                "I can't save changes in the settings.",
                "Why is the search function not working?",
                "The installer is stuck at 50%.",
                "I keep getting an 'Installation Failed' message.",
                "How do I install this on a Mac?",
                "I can't connect to the server.",
                "Why am I constantly getting disconnected?",
                "My Wi-Fi works, but your app says no internet connection.",
                "Why is the app so slow?",
                "I'm experiencing lag during video calls.",
                "The website keeps freezing on my browser.",
                "I get a '404 Not Found' error.",
                "What does the 'Permission Denied' error mean?",
                "Why am I seeing an 'Insufficient Storage' warning?",
                "Is this compatible with Windows 11?",
                "The app doesn't work on my Android phone.",
                "Do you have a browser extension for Safari?"
        ));
            
        // 账户管理类示例：包含密码重置、双因素认证、账户锁定、个人信息修改等
        examples.put(ACCOUNT_MANAGEMENT, asList(
                "I forgot my password, how can I reset it?",
                "I didn't receive a password reset email.",
                "Is there a way to change my password from within the app?",
                "How do I set up two-factor authentication?",
                "I lost my phone, how can I log in now?",
                "Why am I not getting the 2FA code?",
                "My account has been locked, what do I do?",
                "Is there a limit on login attempts?",
                "I've been locked out for no reason, can you help?",
                "How do I change my email address?",
                "Can I update my profile picture?",
                "How do I edit my shipping address?",
                "Can I share my account with family?",
                "How do I give admin access to my team member?",
                "Is there a guest access feature?",
                "How do I delete my account?",
                "What happens to my data if I deactivate my account?",
                "Can I reactivate my account later?"
        ));
            
        // 产品信息类示例：包含功能说明、库存查询、版本对比、兼容性、保修等
        examples.put(PRODUCT_INFORMATION, asList(
                "What does the 'Sync' feature do?",
                "How does the privacy mode work?",
                "Can you explain the real-time tracking feature?",
                "When will the new model be in stock?",
                "Do you have this item in a size medium?",
                "Are you restocking the sold-out items soon?",
                "What's the difference between version 1.0 and 2.0?",
                "Is the Pro version worth the extra cost?",
                "Do older versions support the new update?",
                "Is this product compatible with iOS?",
                "Will this work with a 220V power supply?",
                "Do you have options for USB-C?",
                "Are there any accessories included?",
                "Do you sell protective cases for this model?",
                "What add-ons would you recommend?",
                "What does the warranty cover?",
                "How do I claim the warranty?",
                "Is the warranty international?"
        ));
            
        // 订单状态类示例：包含物流追踪、配送方式、送达时间、包裹缺失等
        examples.put(ORDER_STATUS, asList(
                "Where is my order right now?",
                "Can you give me a tracking number?",
                "How do I know my order has been shipped?",
                "Can I change the shipping method?",
                "Do you offer overnight shipping?",
                "Is pickup from the store an option?",
                "When will my order arrive?",
                "Why is my delivery delayed?",
                "Can I specify a delivery date?",
                "It's past the delivery date, where is my order?",
                "Will I be notified if there's a delay?",
                "How long will the weather delay my shipment?",
                "I received my order, but an item is missing.",
                "The package was empty when it arrived.",
                "I got the wrong item, what should I do?",
                "Will all my items arrive at the same time?",
                "Why did I receive only part of my order?",
                "Is it possible to get the remaining items faster?"
        ));
            
        // 退货与换货类示例：包含退货政策、换货流程、损坏商品、错误发货等
        examples.put(RETURNS_AND_EXCHANGES, asList(
                "What's your return policy?",
                "Is the return shipping free?",
                "Do I need the original packaging to return?",
                "How do I get a return label?",
                "Do I need to call customer service for a return?",
                "Is an RMA number required?",
                "I need to exchange for a different size.",
                "Can I exchange a gift?",
                "How long does the exchange process take?",
                "My item arrived damaged, what do I do?",
                "The product doesn't work as described.",
                "There's a part missing, can you send it?",
                "I received the wrong item, how can I get it corrected?",
                "I didn't order this, why did I receive it?",
                "You sent me two of the same item by mistake.",
                "Is there a restocking fee for returns?",
                "Will I get a full refund?",
                "How much will be deducted for restocking?"
        ));
            
        // 反馈与投诉类示例：包含产品质量、服务态度、网站体验、功能建议等
        examples.put(FEEDBACK_AND_COMPLAINTS, asList(
                "The material quality is not as advertised.",
                "The product broke after a week of use.",
                "The colors faded after the first wash.",
                "The representative was rude to me.",
                "I was on hold for 30 minutes, this is unacceptable.",
                "Your customer service resolved my issue quickly, thank you!",
                "Your website is hard to navigate.",
                "The app keeps crashing, it's frustrating.",
                "The checkout process is confusing.",
                "You should offer a chat feature for quicker help.",
                "Can you add a wishlist feature?",
                "Please make a mobile-friendly version of the website.",
                "I found a bug in your software.",
                "There's a typo on your homepage.",
                "The payment page has a glitch.",
                "Can you start offering this in a gluten-free option?",
                "Please add support for Linux.",
                "I wish you had more colors to choose from."
        ));
    
        // 创建嵌入模型：使用本地运行的 all-MiniLM-L6-v2 模型（无需 API Key）
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            
        // 创建文本分类器：传入嵌入模型和示例数据集
        TextClassifier<CustomerServiceCategory> classifier = new EmbeddingModelTextClassifier<>(embeddingModel, examples);
    
        // 对新的客户咨询进行分类
        String customerQuery = "Yo where is my package?";
        List<CustomerServiceCategory> categories = classifier.classify(customerQuery);
    
        // 输出分类结果：预期为 [ORDER_STATUS]，因为用户在询问包裹位置
        System.out.println(categories); // [ORDER_STATUS]
    }
}