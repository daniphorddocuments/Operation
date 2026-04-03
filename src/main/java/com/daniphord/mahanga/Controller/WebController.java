package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Product;
import com.daniphord.mahanga.Model.Sale;
import com.daniphord.mahanga.Model.Shop;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.ShopRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.ProductService;
import com.daniphord.mahanga.Service.SalesService;
import com.daniphord.mahanga.Service.ShopService;
import com.daniphord.mahanga.Service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final ProductService productService;
    private final SalesService salesService;
    private final ShopService shopService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    public WebController(
            ProductService productService,
            SalesService salesService,
            ShopService shopService,
            UserService userService,
            UserRepository userRepository,
            ShopRepository shopRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.productService = productService;
        this.salesService = salesService;
        this.shopService = shopService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @ModelAttribute("currentLang")
    public String currentLang() {
        return "sw";
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:" + dashboardForRole((String) session.getAttribute("role"));
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty() || userOpt.get().getPassword() == null || !passwordMatches(password, userOpt.get().getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password");
            return "redirect:/login";
        }

        User user = userOpt.get();
        user = refreshUserShop(user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", normalizedRole(user.getRole()));
        session.setAttribute("shopId", user.getShop() != null ? user.getShop().getShopId() : null);

        return "redirect:" + dashboardForRole(user.getRole());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Logged out successfully");
        return "redirect:/login";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login first");
            return "redirect:/login";
        }
        if (!passwordMatches(oldPassword, currentUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Old password is incorrect");
            return "redirect:" + dashboardForRole(currentUser.getRole());
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New password confirmation does not match");
            return "redirect:" + dashboardForRole(currentUser.getRole());
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        return "redirect:" + dashboardForRole(currentUser.getRole());
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }

        List<Shop> shops = shopService.getAllShops();
        shops = shops.stream().map(shopService::refreshBillingStatus).collect(Collectors.toList());
        List<User> users = userService.getAllUsers();
        List<Sale> sales = salesService.getAllSales();
        shops.forEach(shop -> {
            double monthlyProfit = currentMonthProfit(shop, sales);
            boolean firstMonthFree = isFirstBillingMonth(shop);
            shop.setCurrentMonthProfit(monthlyProfit);
            shop.setFirstMonthFree(firstMonthFree);
            shop.setCurrentMonthFeeDue(firstMonthFree ? 0 : monthlyProfit * 0.01);
        });
        Shop topProfitShop = mostProfitableShop(shops, sales);
        long activeShops = shops.stream().filter(Shop::isActive).count();
        long inactiveShops = shops.size() - activeShops;
        long activeUsers = users.stream().filter(this::isActiveUser).count();
        long inactiveUsers = users.size() - activeUsers;
        double currentMonthReturns = shops.stream().mapToDouble(Shop::getCurrentMonthProfit).sum();
        double currentMonthFees = shops.stream().mapToDouble(Shop::getCurrentMonthFeeDue).sum();

        model.addAttribute("shop", new Shop());
        model.addAttribute("shops", shops);
        model.addAttribute("users", users);
        model.addAttribute("roles", List.of("MANAGER", "ACCOUNTANT", "SUPER_ADMIN"));
        model.addAttribute("stats", Map.of(
                "activeShops", activeShops,
                "inactiveShops", inactiveShops,
                "activeUsers", activeUsers,
                "inactiveUsers", inactiveUsers,
                "totalSales", sales.stream().mapToDouble(Sale::getTotal).sum(),
                "totalProfit", sales.stream().mapToDouble(Sale::getProfit).sum(),
                "currentMonthReturns", currentMonthReturns,
                "currentMonthFees", currentMonthFees,
                "topProfitShop", topProfitShop != null ? topProfitShop.getName() : "No sales yet",
                "topProfitValue", shopProfit(topProfitShop, sales)
        ));
        model.addAttribute("notifications", adminNotifications(shops, users, sales));
        model.addAttribute("shopChartLabels", shops.stream().map(Shop::getName).collect(Collectors.toList()));
        model.addAttribute("shopChartProfits", shops.stream().map(shop -> shopProfit(shop, sales)).collect(Collectors.toList()));
        model.addAttribute("shopChartSales", shops.stream().map(shop -> shopSales(shop, sales)).collect(Collectors.toList()));
        model.addAttribute("overviewChartLabels", Arrays.asList("Active Users", "Inactive Users", "Active Shops", "Inactive Shops"));
        model.addAttribute("overviewChartValues", Arrays.asList(activeUsers, inactiveUsers, activeShops, inactiveShops));
        model.addAttribute("loggedInUsername", session.getAttribute("username"));
        return "admin";
    }

    @GetMapping({"/admin/users/view", "/admin/users/manage", "/admin/shops/view", "/admin/shops/manage"})
    public String adminLegacyRoutes(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }
        return "redirect:/admin";
    }

    @GetMapping({"/super-dashboard", "/superadmin/dashboard"})
    public String superAdminLegacyDashboard(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/shops")
    public String createShop(
            @RequestParam String name,
            @RequestParam(required = false) String location,
            @RequestParam String adminUsername,
            @RequestParam String adminPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }
        if (shopRepository.existsByName(name.trim())) {
            redirectAttributes.addFlashAttribute("error", "Shop name already exists");
            return "redirect:/admin";
        }
        if (userRepository.findByUsername(adminUsername.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/admin";
        }

        Shop shop = new Shop();
        shop.setName(name.trim());
        shop.setLocation(location == null ? "" : location.trim());
        shop.setCreatedDate(LocalDate.now());
        shop.setPaymentComplete(false);
        shop.setActive(true);
        Shop savedShop = shopRepository.save(shop);

        User admin = new User();
        admin.setUsername(adminUsername.trim());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("MANAGER");
        admin.setShop(savedShop);
        userRepository.save(admin);

        redirectAttributes.addFlashAttribute("success", "Shop and manager created successfully");
        return "redirect:/admin";
    }

    @PostMapping("/shop/register/save")
    public String createShopFromLegacyDashboard(
            @RequestParam String name,
            @RequestParam(required = false) String location,
            @RequestParam String adminUsername,
            @RequestParam String adminPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return createShop(name, location, adminUsername, adminPassword, session, redirectAttributes);
    }

    @PostMapping("/admin/shops/{id}/payment")
    public String updateShopPaymentStatus(
            @PathVariable Long id,
            @RequestParam boolean paymentComplete,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }
        if (!shopService.updatePaymentStatus(id, paymentComplete)) {
            redirectAttributes.addFlashAttribute("error", "Shop not found");
            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                paymentComplete ? "Monthly 1% profit fee marked as paid and shop activated" : "Monthly 1% profit fee marked as unpaid and shop deactivated"
        );
        return "redirect:/admin";
    }

    @PostMapping("/admin/shops/{id}/delete")
    public String deleteShop(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }

        List<User> linkedUsers = userRepository.findByShopShopId(id);
        if (!linkedUsers.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Delete users in this shop first");
            return "redirect:/admin";
        }
        if (!productService.getProductsByShopId(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Delete products in this shop first");
            return "redirect:/admin";
        }
        if (!salesService.getSalesByShop(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Delete sales in this shop first");
            return "redirect:/admin";
        }
        if (!shopService.deleteShop(id)) {
            redirectAttributes.addFlashAttribute("error", "Shop not found");
            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute("success", "Shop deleted");
        return "redirect:/admin";
    }

    @PostMapping("/admin/users")
    public String createUserByAdmin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) Long shopId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/admin";
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.trim().toUpperCase());

        if (shopId != null) {
            Optional<Shop> shop = shopRepository.findById(shopId);
            if (shop.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Selected shop was not found");
                return "redirect:/admin";
            }
            user.setShop(shop.get());
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User created successfully");
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUserByAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Admin access required");
            return "redirect:/login";
        }

        User currentUser = requireAuthenticatedUser(session);
        if (currentUser != null && currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete the current logged in user");
            return "redirect:/admin";
        }

        if (!userService.deleteUser(id)) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("success", "User deleted");
        return "redirect:/admin";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);

        Long shopId = currentUser.getShop() != null ? currentUser.getShop().getShopId() : null;
        List<Product> products = productService.getProductsByShopId(shopId);
        List<Sale> sales = shopId == null ? salesService.getAllSales() : salesService.getSalesByShop(shopId);

        model.addAttribute("lowStock", lowStockProducts(products));
        model.addAttribute("products", products);
        model.addAttribute("shopName", currentUser.getShop() != null ? currentUser.getShop().getName() : "All Shops");
        model.addAttribute("capital", products.stream().mapToDouble(product -> product.getCost() * product.getStock()).sum());
        model.addAttribute("profit", todayProfit(sales));
        model.addAttribute("mostSold", topProductTotals(sales, true));
        model.addAttribute("mostProfitable", topProductTotals(sales, false));
        model.addAttribute("productCount", products.size());
        model.addAttribute("lowStockCount", lowStockProducts(products).size());
        model.addAttribute("managerSalesChartLabels", topProductTotals(sales, true).stream().map(entry -> entry[0]).collect(Collectors.toList()));
        model.addAttribute("managerSalesChartValues", topProductTotals(sales, true).stream().map(entry -> entry[1]).collect(Collectors.toList()));
        model.addAttribute("managerProfitChartLabels", topProductTotals(sales, false).stream().map(entry -> entry[0]).collect(Collectors.toList()));
        model.addAttribute("managerProfitChartValues", topProductTotals(sales, false).stream().map(entry -> entry[1]).collect(Collectors.toList()));
        model.addAttribute("managerMonthlyLabels", recentMonthSales(sales).keySet().stream().toList());
        model.addAttribute("managerMonthlyValues", recentMonthSales(sales).values().stream().toList());
        model.addAttribute("loggedInUsername", currentUser.getUsername());
        addBillingNotice(model, currentUser, sales);
        model.addAttribute("currentPaymentNumber", currentUser.getShop() != null ? currentUser.getShop().getPaymentNumber() : "");
        return "manager";
    }

    @GetMapping("/manager/products")
    public String productsPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }

        List<Product> products = productService.getProductsByShopId(currentUser.getShop() != null ? currentUser.getShop().getShopId() : null);
        List<Sale> sales = currentUser.getShop() == null ? salesService.getAllSales() : salesService.getSalesByShop(currentUser.getShop().getShopId());

        model.addAttribute("products", products);
        model.addAttribute("lowStock", lowStockProducts(products));
        model.addAttribute("capital", products.stream().mapToDouble(product -> product.getCost() * product.getStock()).sum());
        model.addAttribute("profit", todayProfit(sales));
        model.addAttribute("loggedInUsername", currentUser.getUsername());
        addBillingNotice(model, currentUser, sales);
        return "product";
    }

    @GetMapping("/manager/products/add")
    public String addProductPage(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        model.addAttribute("loggedInUsername", currentUser.getUsername());
        return "add-product";
    }

    @PostMapping("/manager/products/save")
    public String saveProduct(
            @RequestParam String name,
            @RequestParam double cost,
            @RequestParam double price,
            @RequestParam int stock,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        if (currentUser.getShop() == null) {
            redirectAttributes.addFlashAttribute("error", "Manager is not assigned to a shop");
            return "redirect:/manager/products";
        }

        Product product = new Product();
        product.setName(name.trim());
        product.setCost(cost);
        product.setPrice(price);
        product.setStock(stock);
        product.setShop(currentUser.getShop());
        productService.createProduct(product);

        redirectAttributes.addFlashAttribute("success", "Product saved");
        return "redirect:/manager/dashboard";
    }

    @PostMapping("/manager/products/{id}/update")
    public String updateManagerProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam double cost,
            @RequestParam double price,
            @RequestParam int stock,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }

        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty() || !belongsToCurrentShop(productOpt.get().getShop(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/manager/dashboard";
        }

        Product product = productOpt.get();
        product.setName(name.trim());
        product.setCost(cost);
        product.setPrice(price);
        product.setStock(stock);
        productService.createProduct(product);

        redirectAttributes.addFlashAttribute("success", "Product updated");
        return "redirect:/manager/dashboard";
    }

    @PostMapping("/manager/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty() || !belongsToCurrentShop(productOpt.get().getShop(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/manager/dashboard";
        }

        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("success", "Product deleted");
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/manager/users")
    public String managerUsers(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        if (currentUser.getShop() == null) {
            redirectAttributes.addFlashAttribute("error", "Manager is not assigned to a shop");
            return "redirect:/manager/dashboard";
        }

        List<User> users = userService.getUsersByShopId(currentUser.getShop().getShopId()).stream()
                .filter(user -> !"MANAGER".equals(normalizedRole(user.getRole())))
                .filter(user -> !"SUPER_ADMIN".equals(normalizedRole(user.getRole())))
                .collect(Collectors.toList());
        model.addAttribute("users", users);
        return "manager-users";
    }

    @GetMapping("/manager/users/add")
    public String managerAddUser(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        if (currentUser.getShop() == null) {
            redirectAttributes.addFlashAttribute("error", "Manager is not assigned to a shop");
            return "redirect:/manager/dashboard";
        }

        model.addAttribute("roles", List.of("ACCOUNTANT"));
        return "manager-user-form";
    }

    @PostMapping("/manager/users/save")
    public String saveManagerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        if (currentUser.getShop() == null) {
            redirectAttributes.addFlashAttribute("error", "Manager is not assigned to a shop");
            return "redirect:/manager/dashboard";
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/manager/users/add";
        }
        if (!"ACCOUNTANT".equals(normalizedRole(role))) {
            redirectAttributes.addFlashAttribute("error", "Managers can only create accountant users");
            return "redirect:/manager/users/add";
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ACCOUNTANT");
        user.setShop(currentUser.getShop());
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Staff user created successfully");
        return "redirect:/manager/users";
    }

    @PostMapping("/manager/users/{id}/delete")
    public String deleteManagerUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !belongsToCurrentShop(userOpt.get().getShop(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/manager/users";
        }

        User user = userOpt.get();
        if (currentUser.getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete the current logged in user");
            return "redirect:/manager/users";
        }
        if (!"ACCOUNTANT".equals(normalizedRole(user.getRole()))) {
            redirectAttributes.addFlashAttribute("error", "Managers can only delete accountant users");
            return "redirect:/manager/users";
        }

        userRepository.delete(user);
        redirectAttributes.addFlashAttribute("success", "Staff user deleted");
        return "redirect:/manager/users";
    }

    @GetMapping("/manager/reports")
    public String managerReports(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }

        addReportAttributes(model, currentUser, "MANAGER");
        return "manager-reports";
    }

    @GetMapping("/manager/reports/print")
    public String managerReportsPrint(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }

        addReportAttributes(model, currentUser, "MANAGER");
        return "report-print";
    }

    @GetMapping("/accountant/dashboard")
    public String accountantDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isAccountantManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Accountant access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);

        Long shopId = currentUser.getShop() != null ? currentUser.getShop().getShopId() : null;
        List<Product> products = productService.getProductsByShopId(shopId);
        List<Sale> sales = shopId == null ? salesService.getAllSales() : salesService.getSalesByShop(shopId);

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("products", products);
        model.addAttribute("stats", Map.of(
                "activeProducts", products.size(),
                "lowStock", lowStockProducts(products).size(),
                "salesMonth", sales.stream().mapToDouble(Sale::getTotal).sum()
        ));
        model.addAttribute("user", currentUser);
        model.addAttribute("recentSales", sales.stream()
                .sorted(Comparator.comparing(Sale::getDateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList()));
        addBillingNotice(model, currentUser, sales);
        return "accountant-dashboard";
    }

    @GetMapping("/accountant/products")
    public String accountantProducts(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isAccountantManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Accountant access required");
            return "redirect:/login";
        }
        return "redirect:/accountant/dashboard";
    }

    @GetMapping("/accountant/reports")
    public String accountantReports(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isAccountantManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Accountant access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/accountant/dashboard";
        }

        addReportAttributes(model, currentUser, "ACCOUNTANT");
        return "accountant-reports";
    }

    @GetMapping("/accountant/reports/print")
    public String accountantReportsPrint(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isAccountantManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Accountant access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/accountant/dashboard";
        }

        addReportAttributes(model, currentUser, "ACCOUNTANT");
        return "report-print";
    }

    @GetMapping("/shop/register")
    public String shopRegistrationEntry(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, "SUPER_ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Please login as super admin to register a shop");
            return "redirect:/login";
        }
        return "redirect:/admin";
    }

    @PostMapping("/accountant/sell")
    public String createSale(
            @RequestParam Long productId,
            @RequestParam int quantity,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isAccountantManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Accountant access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/accountant/dashboard";
        }

        Optional<Product> productOpt = productService.getProductById(productId);
        if (productOpt.isEmpty() || !belongsToCurrentShop(productOpt.get().getShop(), currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/accountant/dashboard";
        }

        try {
            Sale sale = salesService.createSale(productId, quantity);
            model.addAttribute("sale", sale);
            return "receipt";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accountant/dashboard";
        }
    }

    @PostMapping("/change-language")
    public String changeLanguage(@RequestHeader(value = "Referer", required = false) String referer) {
        return referer == null || referer.isBlank() ? "redirect:/login" : "redirect:" + referer;
    }

    @PostMapping("/manager/shop/payment-number")
    public String updateManagerPaymentNumber(
            @RequestParam String paymentNumber,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = requireAuthenticatedUser(session);
        if (currentUser == null || !isManagerOrAdmin(currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Manager access required");
            return "redirect:/login";
        }
        currentUser = refreshUserShop(currentUser);
        if (isRestrictedShopUser(currentUser)) {
            redirectAttributes.addFlashAttribute("error", inactiveShopMessage(currentUser, shopCurrentBillingAmount(currentUser.getShop())));
            return "redirect:/manager/dashboard";
        }
        if (currentUser.getShop() == null) {
            redirectAttributes.addFlashAttribute("error", "Manager is not assigned to a shop");
            return "redirect:/manager/dashboard";
        }

        shopService.updatePaymentNumber(currentUser.getShop().getShopId(), paymentNumber);
        redirectAttributes.addFlashAttribute("success", "Lipa number updated successfully");
        return "redirect:/manager/dashboard";
    }

    private User requireAuthenticatedUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }

    private User refreshUserShop(User user) {
        if (user == null || user.getShop() == null) {
            return user;
        }
        Shop refreshedShop = shopService.refreshBillingStatus(user.getShop());
        user.setShop(refreshedShop);
        return user;
    }

    private boolean hasRole(HttpSession session, String role) {
        Object currentRole = session.getAttribute("role");
        return currentRole != null && role.equals(currentRole.toString());
    }

    private boolean isManagerOrAdmin(User user) {
        String role = normalizedRole(user.getRole());
        return "MANAGER".equals(role) || "SUPER_ADMIN".equals(role);
    }

    private boolean isAccountantManagerOrAdmin(User user) {
        String role = normalizedRole(user.getRole());
        return "ACCOUNTANT".equals(role) || "MANAGER".equals(role) || "SUPER_ADMIN".equals(role);
    }

    private boolean belongsToCurrentShop(Shop shop, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if ("SUPER_ADMIN".equals(normalizedRole(currentUser.getRole()))) {
            return true;
        }
        return shop != null && currentUser.getShop() != null && Objects.equals(shop.getShopId(), currentUser.getShop().getShopId());
    }

    private boolean isRestrictedShopUser(User currentUser) {
        if (currentUser == null || "SUPER_ADMIN".equals(normalizedRole(currentUser.getRole()))) {
            return false;
        }
        return currentUser.getShop() != null && !currentUser.getShop().isActive();
    }

    private String dashboardForRole(String role) {
        return switch (normalizedRole(role)) {
            case "SUPER_ADMIN" -> "/admin";
            case "ACCOUNTANT" -> "/accountant/dashboard";
            default -> "/manager/dashboard";
        };
    }

    private String normalizedRole(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (Objects.equals(rawPassword, storedPassword)) {
            return true;
        }
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    private List<Product> lowStockProducts(List<Product> products) {
        return products.stream()
                .filter(product -> product.getStock() <= 5)
                .collect(Collectors.toList());
    }

    private void addReportAttributes(Model model, User currentUser, String audience) {
        Long shopId = currentUser.getShop() != null ? currentUser.getShop().getShopId() : null;
        List<Sale> sales = shopId == null ? salesService.getAllSales() : salesService.getSalesByShop(shopId);
        List<Sale> orderedSales = sales.stream()
                .sorted(Comparator.comparing(Sale::getDateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        model.addAttribute("sales", orderedSales);
        model.addAttribute("totalSales", orderedSales.stream().mapToDouble(Sale::getTotal).sum());
        model.addAttribute("totalProfit", orderedSales.stream().mapToDouble(Sale::getProfit).sum());
        model.addAttribute("totalQuantity", orderedSales.stream().mapToInt(Sale::getQuantity).sum());
        model.addAttribute("reportAudience", audience);
        model.addAttribute("reportTitle", "ACCOUNTANT".equals(audience) ? "Accountant Sales Reports" : "Manager Sales Reports");
        model.addAttribute("reportBackLink", "ACCOUNTANT".equals(audience) ? "/accountant/dashboard" : "/manager/dashboard");
        model.addAttribute("reportPrintLink", "ACCOUNTANT".equals(audience) ? "/accountant/reports/print" : "/manager/reports/print");
        model.addAttribute("reportGeneratedAt", java.time.LocalDateTime.now());
        model.addAttribute("reportShopName", currentUser.getShop() != null ? currentUser.getShop().getName() : "All Shops");
        model.addAttribute("loggedInUsername", currentUser.getUsername());
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("user", currentUser);
        addBillingNotice(model, currentUser, sales);
    }

    private void addBillingNotice(Model model, User currentUser, List<Sale> sales) {
        if (currentUser == null || currentUser.getShop() == null || "SUPER_ADMIN".equals(normalizedRole(currentUser.getRole()))) {
            model.addAttribute("billingMessage", null);
            model.addAttribute("billingRestricted", false);
            model.addAttribute("shopPaymentNumber", null);
            return;
        }

        Shop shop = shopService.refreshBillingStatus(currentUser.getShop());
        currentUser.setShop(shop);
        LocalDate today = LocalDate.now();
        double billingAmount = shopBillingAmount(shop, sales);
        boolean restricted = !shop.isActive();
        String message = null;
        String level = "info";

        if (shopService.isWithinFreePeriod(shop, today)) {
            long daysRemaining = shopService.freeDaysRemaining(shop, today);
            message = "Ndugu mteja bado una ofa ya siku " + daysRemaining + " za bure hivyo baada ya siku hizo utatakiwa kulipa asilimia 1 ya faida yako kwa mwezi ahsante kwa kuendelea kutumia mfumo wetu";
            level = "success";
        } else if (restricted) {
            message = inactiveShopMessage(currentUser, billingAmount);
            level = "danger";
        } else if (shop.isPaymentComplete() && shop.getPaymentExpiryDate() != null) {
            long daysRemaining = shopService.paymentDaysRemaining(shop, today);
            if (daysRemaining <= 3) {
                message = "Ndugu " + currentUser.getUsername() + ", muda wa malipo ya duka lako unaisha baada ya siku " + daysRemaining + ". Tafadhali jiandae kulipa " + formatCurrency(billingAmount) + " ili huduma isiingie kwenye hali ya kusimama.";
                level = "warning";
            }
        }

        model.addAttribute("billingMessage", message);
        model.addAttribute("billingLevel", level);
        model.addAttribute("billingRestricted", restricted);
        model.addAttribute("shopPaymentNumber", shop.getPaymentNumber());
        model.addAttribute("billingAmount", formatCurrency(billingAmount));
    }

    private double todayProfit(List<Sale> sales) {
        return salesService.getTodaySales().stream()
                .filter(sale -> sale.getShop() == null || sales.stream().anyMatch(current -> Objects.equals(current.getSaleId(), sale.getSaleId())))
                .mapToDouble(Sale::getProfit)
                .sum();
    }

    private List<Object[]> topProductTotals(List<Sale> sales, boolean quantityMode) {
        Map<String, Double> totals = new LinkedHashMap<>();
        sales.stream()
                .filter(sale -> sale.getProduct() != null && sale.getProduct().getName() != null)
                .forEach(sale -> totals.merge(
                        sale.getProduct().getName(),
                        quantityMode ? (double) sale.getQuantity() : sale.getProfit(),
                        Double::sum
                ));

        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .collect(Collectors.toList());
    }

    private Map<String, Double> recentMonthSales(List<Sale> sales) {
        Map<String, Double> monthly = new LinkedHashMap<>();
        LocalDate today = LocalDate.now().withDayOfMonth(1);
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            monthly.put(month.getMonth().name().substring(0, 1) + month.getMonth().name().substring(1, 3).toLowerCase(), 0d);
        }
        sales.stream()
                .filter(sale -> sale.getDateTime() != null)
                .forEach(sale -> {
                    LocalDate month = sale.getDateTime().toLocalDate().withDayOfMonth(1);
                    String label = month.getMonth().name().substring(0, 1) + month.getMonth().name().substring(1, 3).toLowerCase();
                    if (monthly.containsKey(label)) {
                        monthly.merge(label, sale.getTotal(), Double::sum);
                    }
                });
        return monthly;
    }

    private boolean isActiveUser(User user) {
        if (user == null) {
            return false;
        }
        if ("SUPER_ADMIN".equals(normalizedRole(user.getRole()))) {
            return true;
        }
        return user.getShop() != null && user.getShop().isActive();
    }

    private Shop mostProfitableShop(List<Shop> shops, List<Sale> sales) {
        return shops.stream()
                .max(Comparator.comparingDouble(shop -> shopProfit(shop, sales)))
                .orElse(null);
    }

    private double shopProfit(Shop shop, List<Sale> sales) {
        if (shop == null) {
            return 0;
        }
        return sales.stream()
                .filter(sale -> sale.getShop() != null && Objects.equals(sale.getShop().getShopId(), shop.getShopId()))
                .mapToDouble(Sale::getProfit)
                .sum();
    }

    private double shopSales(Shop shop, List<Sale> sales) {
        if (shop == null) {
            return 0;
        }
        return sales.stream()
                .filter(sale -> sale.getShop() != null && Objects.equals(sale.getShop().getShopId(), shop.getShopId()))
                .mapToDouble(Sale::getTotal)
                .sum();
    }

    private double currentMonthProfit(Shop shop, List<Sale> sales) {
        if (shop == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return sales.stream()
                .filter(sale -> sale.getShop() != null && Objects.equals(sale.getShop().getShopId(), shop.getShopId()))
                .filter(sale -> sale.getDateTime() != null
                        && sale.getDateTime().getYear() == today.getYear()
                        && sale.getDateTime().getMonth() == today.getMonth())
                .mapToDouble(Sale::getProfit)
                .sum();
    }

    private boolean isFirstBillingMonth(Shop shop) {
        if (shop == null || shop.getCreatedDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return shop.getCreatedDate().getYear() == today.getYear()
                && shop.getCreatedDate().getMonth() == today.getMonth();
    }

    private double shopBillingAmount(Shop shop, List<Sale> sales) {
        return Math.max(0, currentMonthProfit(shop, sales) * 0.01d);
    }

    private double shopCurrentBillingAmount(Shop shop) {
        if (shop == null) {
            return 0;
        }
        List<Sale> sales = salesService.getSalesByShop(shop.getShopId());
        return shopBillingAmount(shop, sales);
    }

    private String inactiveShopMessage(User currentUser, double billingAmount) {
        return "Ndugu " + currentUser.getUsername() + " wa Mfumo Unatakiwa kulipa " + formatCurrency(billingAmount)
                + " kwenda kwa namba Tigopesa 0679299258 jina Daniphord Nkurushi Mahanga au piga simu 0738686917 kwa msaada";
    }

    private String formatCurrency(double amount) {
        return String.format("%.0f TZS", amount);
    }

    private List<String> adminNotifications(List<Shop> shops, List<User> users, List<Sale> sales) {
        List<String> notifications = new ArrayList<>();
        long unpaidShops = shops.stream().filter(shop -> !shop.isPaymentComplete()).count();
        long inactiveShops = shops.stream().filter(shop -> !shop.isActive()).count();
        double unpaidFees = shops.stream()
                .filter(shop -> !shop.isPaymentComplete())
                .mapToDouble(Shop::getCurrentMonthFeeDue)
                .sum();
        long unassignedUsers = users.stream()
                .filter(user -> !"SUPER_ADMIN".equals(normalizedRole(user.getRole())))
                .filter(user -> user.getShop() == null)
                .count();

        if (unpaidShops > 0) {
            notifications.add(unpaidShops + " shop(s) have unpaid 1% monthly profit fees worth " + Math.round(unpaidFees) + " TZS.");
        }
        if (inactiveShops > 0) {
            notifications.add(inactiveShops + " shop(s) are currently inactive.");
        }
        if (unassignedUsers > 0) {
            notifications.add(unassignedUsers + " user(s) are not assigned to a shop.");
        }
        if (sales.isEmpty()) {
            notifications.add("No sales recorded yet.");
        } else {
            Shop topShop = mostProfitableShop(shops, sales);
            if (topShop != null) {
                notifications.add(topShop.getName() + " is currently the most profitable shop.");
            }
        }
        if (notifications.isEmpty()) {
            notifications.add("All shops and users look healthy.");
        }
        return notifications;
    }
}
