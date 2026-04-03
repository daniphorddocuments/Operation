package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET all users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // GET user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE user and assign to shop
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, @RequestParam Long shopId) {
        try {
            return ResponseEntity.ok(userService.createUser(user, shopId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // UPDATE user
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser, @RequestParam(required = false) Long shopId) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, updatedUser, shopId));
        } catch (Exception e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.deleteUser(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
