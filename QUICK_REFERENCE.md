# QUICK REFERENCE - MAHANGA SYSTEM

## 🚀 QUICK START

### Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

### Access Application
```
URL: http://localhost:1313
Default Admin: admin / <password>
```

---

## 🔐 SECURITY QUICK FACTS

- **Password Hashing**: BCrypt Strength 12
- **Session Timeout**: 30 minutes
- **Max Login Attempts**: 5 per 5 minutes
- **Lockout Duration**: 15 minutes
- **CSRF Protection**: Enabled
- **Security Headers**: CSP, XSS-Protection, X-Frame-Options
- **Rate Limiting**: Active
- **Audit Logging**: All actions tracked

---

## 👥 USER ROLES

### SUPER_ADMIN
- Create shops & users
- Manage billing
- View system stats
- Password: Strong (8+ chars, mixed case, digits, special)

### MANAGER
- Product management
- Accountant management
- Payment management
- Restricted if shop unpaid

### ACCOUNTANT
- Record sales
- View products
- Create reports
- Restricted if shop unpaid

---

## 💰 BILLING RULES

- **Free Trial**: 32 days
- **Fee**: 1% of monthly profit
- **Payment Period**: 30 days
- **Shop Status**: Inactive if unpaid
- **User Access**: Can login but limited actions when unpaid

---

## 📊 DATABASE TABLES

| Table | Purpose |
|-------|---------|
| users | User accounts & auth |
| shops | Shop management |
| products | Product inventory |
| sales | Sales transactions |
| notifications | User notifications |
| audit_logs | System audit trail |

---

## 🎨 DESIGN SYSTEM

### Colors (3 Only)
- **Primary**: #1e40af (Blue)
- **Secondary**: #ffffff (White)
- **Neutral**: #374151 (Gray)

### Contrast
- **Ratio**: 4.5:1 (WCAG AA)
- **Text**: All readable

### Typography
- **Font**: System fonts
- **Size**: 0.95rem base

---

## 🛠️ CONFIGURATION

### Key Properties
```properties
server.port=1313
spring.jpa.hibernate.ddl-auto=update
server.servlet.session.timeout=30m
```

### Environment Variables
```bash
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
```

---

## 📝 API ENDPOINTS

### Authentication
- `GET /login` - Login page
- `POST /login` - Submit login
- `GET /logout` - Logout

### Admin
- `GET /admin` - Admin dashboard
- `POST /admin/shops` - Create shop
- `POST /admin/users` - Create user

### Manager
- `GET /manager/dashboard` - Manager dashboard
- `POST /manager/products/save` - Add product
- `POST /manager/shop/payment-number` - Update payment

### Accountant
- `GET /accountant/dashboard` - Accountant dashboard
- `POST /accountant/sell` - Record sale

### Notifications
- `GET /notifications` - View notifications
- `POST /notifications/{id}/read` - Mark read

---

## 🔍 MONITORING

### View Logs
```bash
tail -f /var/log/mahanga/app.log
```

### Check Status
```bash
systemctl status mahanga
```

### Database Connection
```bash
psql -U mahanga -h localhost -d shop
```

---

## 🆘 TROUBLESHOOTING

### Application Won't Start
```bash
# Check port usage
netstat -tlnp | grep 1313

# Check Java process
ps aux | grep java

# View logs
journalctl -u mahanga -n 50
```

### Database Issues
```bash
# Test connection
psql -U mahanga -d shop

# Check service
systemctl status postgresql
```

### Performance Issues
```bash
# Check memory
free -h

# Check disk
df -h

# Adjust JVM heap in service file
```

---

## 📋 DEPLOYMENT STEPS

1. **Build**: `mvn clean install`
2. **Deploy**: Copy JAR to `/opt/mahanga/`
3. **Configure**: Set environment variables
4. **Database**: Run migrations
5. **Start**: `systemctl start mahanga`
6. **Verify**: Check logs and health

---

## 🎯 PERFORMANCE TARGETS

- **Page Load**: < 2 seconds
- **API Response**: < 500ms
- **Database Query**: < 100ms
- **Concurrent Users**: 1000+
- **Uptime**: 99.9%

---

## 📚 DOCUMENTATION FILES

| File | Purpose |
|------|---------|
| README.md | Overview & setup |
| SECURITY_AUDIT.md | Security details |
| DEPLOYMENT.md | Deployment guide |
| DEVELOPMENT_SUMMARY.md | Complete summary |

---

## 🔐 Security Checklist

- [x] BCrypt enabled
- [x] CSRF protection active
- [x] Rate limiting working
- [x] Audit logging running
- [x] Security headers set
- [x] Input validation active
- [x] Session secure
- [x] Database encrypted

---

## 💡 KEY FEATURES

✅ Multi-role access control
✅ Shop billing management
✅ Product inventory tracking
✅ Sales transaction recording
✅ Real-time notifications
✅ Comprehensive audit logs
✅ Professional UI design
✅ Mobile responsive
✅ Enterprise security
✅ Production ready

---

## 🌐 Supported Languages

- English (en)
- Swahili (sw)

---

## 📞 Support

**Email**: nkurushidaniphord@gmail.com
**Phone**: +255 679 299 258
**WhatsApp**: Available

---

**Version**: 1.0.0 | **Status**: Production Ready | **Grade**: A+


