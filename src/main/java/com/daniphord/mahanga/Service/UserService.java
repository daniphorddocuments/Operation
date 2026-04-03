package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Shop;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.ShopRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // GET user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // CREATE user and assign to shop
    public User createUser(User user, Long shopId) throws Exception {
        Optional<Shop> shopOpt = shopRepository.findById(shopId);
        if (shopOpt.isEmpty()) {
            throw new Exception("Shop not found");
        }
        user.setShop(shopOpt.get());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // UPDATE user
    public User updateUser(Long id, User updatedUser, Long shopId) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));

        user.setUsername(updatedUser.getUsername());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        user.setRole(updatedUser.getRole());

        if (shopId != null) {
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new Exception("Shop not found"));
            user.setShop(shop);
        }

        return userRepository.save(user);
    }

    // DELETE user
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) return false;
        userRepository.deleteById(id);
        return true;
    }

    public List<User> getUsersByShopId(Long shopId) {
        if (shopId == null) {
            return getAllUsers();
        }
        return userRepository.findByShopShopId(shopId);
    }
}
