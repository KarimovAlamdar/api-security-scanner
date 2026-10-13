package com.demo;

import java.util.List;
import java.util.Map;

// Simulated annotations or Spring MVC annotations
@interface RequestMapping { String value() default ""; }
@interface GetMapping { String value() default ""; }
@interface PostMapping { String value() default ""; }
@interface PutMapping { String value() default ""; }
@interface DeleteMapping { String value() default ""; }

@RequestMapping("/users")
public class UserController {

    @GetMapping
    public List<Map<String, Object>> searchUsers(String search) {
        return List.of();
    }

    @PostMapping
    public Map<String, Object> createUser(Map<String, Object> input) {
        return Map.of("id", "101");
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(String id) {
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(String id, Map<String, Object> input) {
        return Map.of("id", id);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(String id) {
    }
}
