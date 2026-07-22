package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private DataSource dataSource;

    public User getUserByName(String name) {
        try {
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            // SQL 注入风险：直接拼接用户输入
            String sql = "SELECT * FROM users WHERE name = '" + name + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Order> getUserOrders(Long userId) {
        // N+1 查询：先查订单列表，再逐条查商品
        List<Order> orders = orderDao.findByUserId(userId);
        for (Order order : orders) {
            // 每条订单发起一次查询 — N+1 问题
            List<Item> items = itemDao.findByOrderId(order.getId());
            order.setItems(items);
        }
        return orders;
    }

    public void updateUserEmail(Long userId, String email) {
        // 缺少输入校验：email 可能为空或格式错误
        User user = userDao.findById(userId);
        user.setEmail(email);
        userDao.save(user);
    }

    // 缺少依赖注入声明
    private OrderDao orderDao;
    private ItemDao itemDao;
    private UserDao userDao;
}
