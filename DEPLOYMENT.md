# DEPLOYMENT GUIDE - MAHANGA SYSTEM

## Pre-Deployment Checklist

### System Requirements
- [ ] Java 17+ installed
- [ ] PostgreSQL 12+ installed
- [ ] Maven 3.8+ installed
- [ ] 2GB RAM minimum
- [ ] 10GB storage minimum
- [ ] Network connectivity verified

### Database Setup
```bash
# Create database
CREATE DATABASE shop;

# Create user
CREATE USER mahanga WITH PASSWORD 'secure_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE shop TO mahanga;
GRANT ALL ON SCHEMA public TO mahanga;
```

---

## Environment Configuration

### Production Environment Variables
```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-server:5432/shop
export SPRING_DATASOURCE_USERNAME=mahanga
export SPRING_DATASOURCE_PASSWORD=<secure_password>

# Application
export SERVER_PORT=1313
export SPRING_PROFILES_ACTIVE=production

# Security
export SERVER_SERVLET_SESSION_COOKIE_SECURE=true
export SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=strict
```

### application-production.properties
```properties
# Show SQL and formatting only in development
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Production logging
logging.level.root=WARN
logging.level.com.daniphord.mahanga=INFO
logging.file.name=/var/log/mahanga/app.log
logging.file.max-size=10MB
logging.file.max-history=30

# Performance settings
spring.jpa.properties.hibernate.jdbc.batch_size=30
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10

# Hide stack traces in production
server.error.include-stacktrace=never
```

---

## Build & Deployment

### Step 1: Build Application
```bash
cd /path/to/mahanga

# Clean build
mvn clean install -DskipTests

# Build with tests (recommended)
mvn clean install
```

### Step 2: Create Deployment Directory
```bash
# Create application directory
sudo mkdir -p /opt/mahanga
sudo mkdir -p /var/log/mahanga
sudo mkdir -p /var/backup/mahanga

# Set permissions
sudo chown -R mahanga:mahanga /opt/mahanga
sudo chown -R mahanga:mahanga /var/log/mahanga
sudo chmod 755 /opt/mahanga
```

### Step 3: Deploy Application
```bash
# Copy JAR file
sudo cp target/mahanga-1.0.0.jar /opt/mahanga/mahanga.jar

# Create systemd service
sudo nano /etc/systemd/system/mahanga.service
```

### Step 4: Create Systemd Service
```ini
[Unit]
Description=Mahanga Retail Management System
After=network.target postgresql.service

[Service]
Type=simple
User=mahanga
WorkingDirectory=/opt/mahanga
ExecStart=/usr/bin/java -Xmx1024m -Xms512m \
  -Dspring.profiles.active=production \
  -jar /opt/mahanga/mahanga.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Step 5: Enable & Start Service
```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service
sudo systemctl enable mahanga

# Start service
sudo systemctl start mahanga

# Check status
sudo systemctl status mahanga
```

---

## Database Initialization

### Initial Setup
```bash
# Hibernate will auto-create schema based on DDL-auto=update

# Verify tables created
psql -h localhost -U mahanga -d shop -c "\dt"
```

### Create Initial Data
```sql
-- Create super admin user
INSERT INTO users (username, password, role, active, created_at, updated_at)
VALUES ('admin', '<bcrypt_hash>', 'SUPER_ADMIN', true, NOW(), NOW());

-- Note: Generate BCrypt hash using:
-- java -cp target/mahanga-1.0.0.jar \
-- org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
```

---

## Nginx Configuration (Reverse Proxy)

### /etc/nginx/sites-available/mahanga
```nginx
upstream mahanga {
    server localhost:1313;
}

server {
    listen 80;
    server_name mahanga.example.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name mahanga.example.com;

    # SSL Certificate
    ssl_certificate /etc/letsencrypt/live/mahanga.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mahanga.example.com/privkey.pem;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Proxy settings
    location / {
        proxy_pass http://mahanga;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static files caching
    location ~* ^/(ui|css|js|images)/ {
        proxy_pass http://mahanga;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## SSL/TLS Setup

### Using Let's Encrypt (Recommended)
```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Generate certificate
sudo certbot certonly --nginx -d mahanga.example.com

# Auto-renewal
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

---

## Monitoring & Logging

### Log Management
```bash
# Monitor real-time logs
tail -f /var/log/mahanga/app.log

# View last 100 lines
tail -100 /var/log/mahanga/app.log

# Search for errors
grep ERROR /var/log/mahanga/app.log

# Check disk usage
du -sh /var/log/mahanga/
```

### Health Check
```bash
# Check application status
curl -s http://localhost:1313/

# Check database connection
curl -s http://localhost:1313/admin
```

---

## Backup & Recovery

### Database Backup
```bash
# Daily backup script: /usr/local/bin/backup-mahanga.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backup/mahanga"
DB_NAME="shop"
DB_USER="mahanga"

pg_dump -U $DB_USER -h localhost $DB_NAME | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete
```

### Setup Cron Job
```bash
# Add to crontab
0 2 * * * /usr/local/bin/backup-mahanga.sh
```

### Database Restore
```bash
# Restore from backup
zcat /var/backup/mahanga/backup_20260406_020000.sql.gz | \
  psql -U mahanga -h localhost shop
```

---

## Performance Tuning

### Java JVM Arguments
```bash
# Heap size tuning
-Xmx2048m    # Maximum heap (adjust based on server)
-Xms1024m    # Initial heap
-XX:+UseG1GC # Use G1 garbage collector
```

### PostgreSQL Tuning
```sql
-- /etc/postgresql/12/main/postgresql.conf

# Memory settings
shared_buffers = 256MB           # 25% of system RAM
effective_cache_size = 2GB       # 50-75% of system RAM
work_mem = 16MB                  # RAM / (max_connections * 2)

# Connections
max_connections = 200

# Query planning
random_page_cost = 1.1           # For SSD
effective_io_concurrency = 200   # SSD concurrency
```

---

## Troubleshooting

### Application Won't Start
```bash
# Check logs
sudo journalctl -u mahanga -n 50

# Check port in use
sudo netstat -tlnp | grep 1313

# Check database connection
psql -h localhost -U mahanga -d shop -c "SELECT 1"
```

### Database Connection Issues
```bash
# Test connection
psql -h db-host -U mahanga -d shop

# Check pg_hba.conf
cat /etc/postgresql/12/main/pg_hba.conf

# Restart PostgreSQL
sudo systemctl restart postgresql
```

### High Memory Usage
```bash
# Check Java processes
ps aux | grep java

# Adjust heap size in systemd service
# Restart application
sudo systemctl restart mahanga
```

---

## Security Hardening

### Firewall Rules (UFW)
```bash
# Allow SSH
sudo ufw allow 22/tcp

# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Deny all other inbound
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Enable firewall
sudo ufw enable
```

### Database User Permissions
```sql
-- Grant minimum required privileges
GRANT CONNECT ON DATABASE shop TO mahanga;
GRANT USAGE ON SCHEMA public TO mahanga;
GRANT ALL ON ALL TABLES IN SCHEMA public TO mahanga;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO mahanga;
```

---

## Maintenance Tasks

### Weekly
- [ ] Check disk space
- [ ] Review error logs
- [ ] Verify backups

### Monthly
- [ ] Review audit logs
- [ ] Update security patches
- [ ] Performance analysis

### Quarterly
- [ ] Security audit
- [ ] Disaster recovery test
- [ ] Capacity planning

---

## Rollback Procedure

### If Issues Occur
```bash
# Stop current version
sudo systemctl stop mahanga

# Restore database from backup
zcat /var/backup/mahanga/backup_previous.sql.gz | \
  psql -U mahanga -h localhost shop

# Deploy previous JAR version
sudo cp /opt/mahanga/mahanga-previous.jar /opt/mahanga/mahanga.jar

# Start application
sudo systemctl start mahanga
```

---

**Deployment Version**: 1.0.0
**Last Updated**: April 6, 2026
**Status**: ✅ Production Ready

