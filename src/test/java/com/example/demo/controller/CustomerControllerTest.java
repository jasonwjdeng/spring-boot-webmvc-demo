package com.example.demo.controller;

import com.example.demo.entity.Customer;
import com.example.demo.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("创建客户 - 成功")
    void createCustomer_Success() throws Exception {
        Customer customer = Customer.builder()
                .name("张三")
                .email("zhangsan@example.com")
                .phone("13800138000")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.email").value("zhangsan@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("查询所有客户 - 成功")
    void listCustomers_Success() throws Exception {
        customerRepository.save(Customer.builder().name("张三").email("zhangsan@example.com").phone("13800138000").build());
        customerRepository.save(Customer.builder().name("李四").email("lisi@example.com").phone("13900139000").build());

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("张三", "李四")));
    }

    @Test
    @DisplayName("根据ID查询客户 - 成功")
    void getCustomerById_Success() throws Exception {
        Customer saved = customerRepository.save(
                Customer.builder().name("张三").email("zhangsan@example.com").phone("13800138000").build());

        mockMvc.perform(get("/api/customers/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    @Test
    @DisplayName("根据ID查询客户 - 不存在")
    void getCustomerById_NotFound() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    @DisplayName("更新客户 - 成功")
    void updateCustomer_Success() throws Exception {
        Customer saved = customerRepository.save(
                Customer.builder().name("张三").email("zhangsan@example.com").phone("13800138000").build());

        Customer updated = Customer.builder()
                .name("张三丰")
                .email("zhangsanfeng@example.com")
                .phone("13700137000")
                .build();

        mockMvc.perform(put("/api/customers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("张三丰"))
                .andExpect(jsonPath("$.data.email").value("zhangsanfeng@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13700137000"));
    }

    @Test
    @DisplayName("更新客户 - 不存在")
    void updateCustomer_NotFound() throws Exception {
        Customer customer = Customer.builder()
                .name("张三")
                .email("zhangsan@example.com")
                .phone("13800138000")
                .build();

        mockMvc.perform(put("/api/customers/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }

    @Test
    @DisplayName("删除客户 - 成功")
    void deleteCustomer_Success() throws Exception {
        Customer saved = customerRepository.save(
                Customer.builder().name("张三").email("zhangsan@example.com").phone("13800138000").build());

        mockMvc.perform(delete("/api/customers/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证已被删除
        mockMvc.perform(get("/api/customers/{id}", saved.getId()))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("删除客户 - 不存在")
    void deleteCustomer_NotFound() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", 999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }
}
