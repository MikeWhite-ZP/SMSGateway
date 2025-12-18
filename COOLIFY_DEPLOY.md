# Deploying SMS Gateway with Coolify

This guide covers deploying the SMS Gateway to your Ubuntu server using Coolify.

## Prerequisites

- Coolify installed on your Ubuntu server
- Git repository with this code (GitHub, GitLab, etc.)

## Deployment Steps

### Step 1: Push Code to Git Repository

Push this project to your Git repository (GitHub, GitLab, Gitea, etc.):

```bash
git init
git add .
git commit -m "SMS Gateway for Coolify deployment"
git remote add origin YOUR_REPO_URL
git push -u origin main
```

### Step 2: Create New Service in Coolify

1. Log into your Coolify dashboard
2. Go to your project or create a new one
3. Click **"+ New"** and select **"Service"**
4. Choose **"Docker Compose"** as the deployment type
5. Connect your Git repository
6. Select the branch (usually `main`)

### Step 3: Configure Environment Variables

In Coolify's environment settings, add these variables:

| Variable | Description | Required |
|----------|-------------|----------|
| `SMS_API_TOKEN` | Token for booking system API calls | Yes (or auto-generated) |
| `DEVICE_TOKEN` | Token for Android app authentication | Yes (or auto-generated) |

**Generating secure tokens:**
```bash
openssl rand -base64 32
```

### Step 4: Configure Domain

1. In Coolify, go to your service settings
2. Add your domain (e.g., `sms.yourdomain.com`)
3. Enable HTTPS (Let's Encrypt)
4. Set the exposed port to `5000`

### Step 5: Deploy

Click **"Deploy"** in Coolify. The build process will:
1. Clone your repository
2. Build the Docker image
3. Start the container
4. Set up SSL certificate

## Verify Deployment

Once deployed, test the API:

```bash
# Health check
curl https://sms.yourdomain.com/health

# Should return:
# {"connected_devices": 0, "status": "ok"}
```

## Configure Android App

After deployment:

1. Open the Android app
2. Enter your Coolify domain: `https://sms.yourdomain.com`
3. Enter the `DEVICE_TOKEN` you set in Coolify
4. Press Connect

## Configure Booking System

Update your booking system to use the Coolify URL:

```python
from sms_gateway_client import SMSGateway

sms = SMSGateway(
    server_url="https://sms.yourdomain.com",
    api_token="YOUR_SMS_API_TOKEN"
)

result = sms.send_sms(
    to_number="+1234567890",
    message="Your booking is confirmed!"
)
```

## Coolify Configuration Options

### Resources (Optional)

In Coolify service settings, you can limit resources:

```yaml
resources:
  limits:
    cpus: '0.5'
    memory: 256M
```

### Persistent Tokens

If you want tokens to persist across container restarts, set them as environment variables in Coolify rather than letting them auto-generate.

### Reverse Proxy Settings

Coolify handles the reverse proxy automatically. The app runs on port 5000 inside the container, and Coolify exposes it via your configured domain with HTTPS.

## Updating

To update your SMS Gateway:

1. Push changes to your Git repository
2. In Coolify, click **"Redeploy"**

Or enable auto-deploy in Coolify to automatically redeploy on git push.

## Monitoring

### Health Check

The docker-compose.yml includes a health check that Coolify will use:

- Endpoint: `/health`
- Interval: 30 seconds
- Timeout: 10 seconds

### Logs

View logs in Coolify's dashboard or via CLI:

```bash
docker logs sms-gateway
```

## Troubleshooting

### Container Won't Start

1. Check Coolify logs for build errors
2. Verify environment variables are set
3. Ensure port 5000 isn't blocked

### Android App Can't Connect

1. Verify the domain is accessible (check SSL certificate)
2. Confirm `DEVICE_TOKEN` matches in app and Coolify
3. Check firewall allows HTTPS traffic

### API Returns 401

- Verify `SMS_API_TOKEN` matches between booking system and Coolify
- Check Authorization header format: `Bearer YOUR_TOKEN`
