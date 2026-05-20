package com.langchain4j.agentic._06_AIAgent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.HashMap;
import java.util.Map;

/**
 * 银行账户工具类
 *
 * 模拟银行核心操作：开户、查询余额、存款、取款。
 * 被 WithdrawAgent 和 CreditAgent 作为共享工具使用。
 *
 * 注意：这里用内存 Map 模拟账户数据，实际项目中应替换为数据库操作。
 */
public class BankTool {

    private final Map<String, Double> accounts = new HashMap<>();

    /**
     * 创建银行账户并设置初始余额
     */
    public void createAccount(String user, Double initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("用户 " + user + " 的账户已存在，无法重复开户");
        }
        accounts.put(user, initialBalance);
    }

    /**
     * 查询账户余额
     */
    public double getBalance(String user) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("未找到用户 " + user + " 的账户信息");
        }
        return balance;
    }

    @Tool("向指定用户的账户存入指定金额，操作成功后返回新的账户余额")
    public Double credit(
            @P("收款用户的姓名") String user,
            @P("存入的金额（美元）") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("未找到用户 " + user + " 的账户信息");
        }
        Double newBalance = balance + amount;
        accounts.put(user, newBalance);
        System.out.println("  [银行系统] 向 " + user + " 账户存入 $" + amount
                + "，余额 $" + balance + " → $" + newBalance);
        return newBalance;
    }

    @Tool("从指定用户的账户中取出指定金额，操作成功后返回新的账户余额")
    public Double withdraw(
            @P("付款用户的姓名") String user,
            @P("取出的金额（美元）") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("未找到用户 " + user + " 的账户信息");
        }
        if (balance < amount) {
            throw new RuntimeException("用户 " + user + " 的账户余额不足！余额 $" + balance
                    + "，尝试取出 $" + amount);
        }
        Double newBalance = balance - amount;
        accounts.put(user, newBalance);
        System.out.println("  [银行系统] 从 " + user + " 账户取出 $" + amount
                + "，余额 $" + balance + " → $" + newBalance);
        return newBalance;
    }
}
