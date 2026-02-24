# CraftCV Backend Deployment Guide (Render)

## Overview
Deploy your FastAPI backend to Render for production. This guide assumes you already have GitHub set up with your code.

---

## Step 1: Create Render Account & Connect GitHub

1. Go to [render.com](https://render.com)
2. Sign up with GitHub (recommended) or email
3. Click **Dashboard**
4. Click **New +** → **Web Service**
5. Click **Connect repository** and authorize GitHub
6. Select your repository (the one with `resume-tailor-backend/`)
7. Click **Connect**

---

## Step 2: Configure Web Service

Fill in these fields:

| Field | Value |
|-------|-------|
| **Name** | `craftcv-api` |
| **Environment** | `Python 3` |
| **Region** | Oregon (or closest to you) |
| **Branch** | `main` (or your default) |
| **Build Command** | `pip install -r requirements.txt` |
| **Start Command** | `uvicorn main:app --host 0.0.0.0 --port $PORT` |
| **Plan** | Starter ($7/month, recommended) or Free (limited) |

**Important:** 
- Set **Root Directory** to: `resume-tailor-backend`
- Do NOT check "Auto-deploy"

Then click **Create Web Service**

---

## Step 3: Create PostgreSQL Database

1. In Render Dashboard, click **New +** → **PostgreSQL**
2. **Database Name**: `craftcv_db`
3. **Region**: Same as your web service
4. **Plan**: Starter ($15/month, recommended) or Free (limited)
5. Click **Create Database**

The database will take a minute to start. Once ready, copy the **Internal Database URL** (not the external one).

---

## Step 4: Set Environment Variables

In your Render web service dashboard:

1. Go to **Settings** → **Environment**
2. Add each variable:

```
DATABASE_URL = [Paste PostgreSQL URL from Step 3]
GEMINI_API_KEY = [Your Gemini API key from makersuite.google.com]
GROQ_API_KEY = [Your Groq API key from console.groq.com]
MISTRAL_API_KEY = [Your Mistral API key from console.mistral.ai]
OPENAI_API_KEY = [Your OpenAI key, if using OpenAI]
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON = [See Step 5]
GOOGLE_PLAY_PACKAGE_NAME = com.craftcv.app
UPGRADE_SECRET = [Keep the one in local .env or generate new]
CORS_ORIGINS = *
PYTHON_VERSION = 3.12.0
```

3. Click **Save**

---

## Step 5: Get Google Play Service Account (Optional but Recommended)

This is needed to verify Google Play purchases. If you skip this, purchases won't verify (but billing still works via Play Store's own validation).

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing one
3. Enable **Google Play API** (APIs & Services → Library → search "Google Play")
4. Go to **Service Accounts** (IAM & Admin → Service Accounts)
5. Click **Create Service Account**
   - Name: `craftcv-play-billing`
   - Grant role: **Editor** (or specifically "Android Publisher")
6. Click on the created account → **Keys** tab
7. **Create key** → **JSON** → Download the file
8. Base64 encode the file:
   - **Windows PowerShell**: 
     ```powershell
     [Convert]::ToBase64String([System.IO.File]::ReadAllBytes("C:\path\to\key.json"))
     ```
   - **Mac/Linux**: 
     ```bash
     base64 key.json
     ```
9. Copy the entire output and paste into `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` in Render

---

## Step 6: Connect Custom Domain

1. In Render service → **Settings** → **Custom Domain**
2. Enter: `api.craftcv.com`
3. Render will give you a CNAME value (e.g., `craftcv-api.onrender.com`)
4. In your domain registrar (GoDaddy, Namecheap, etc.):
   - Add a CNAME record:
     - **Name/Host**: `api`
     - **Value**: `craftcv-api.onrender.com` (from Render)
   - Save
5. Wait 5-10 minutes for DNS to propagate
6. Test: `curl https://api.craftcv.com/` → should return `{"status":"ok"}`

---

## Step 7: Run Database Migrations

Once deployed and database is connected:

1. Click your Render service → **Shell** tab
2. Run:
   ```bash
   cd resume-tailor-backend
   alembic upgrade head
   ```
3. Should see output like: `INFO  [alembic.migration] Context impl PostgresqlImpl()`

---

## Step 8: Verify Deployment

Test these endpoints:

```bash
# Check API is alive
curl https://api.craftcv.com/

# Check status endpoint
curl https://api.craftcv.com/status/test-device

# Should return: {"usesRemaining": 5, "isPro": false, "message": "..."}
```

---

## Troubleshooting

**🔴 Error: "cannot find module"**
- Render logs show missing dependencies
- Fix: Push a change to GitHub to trigger redeploy (or manually trigger in Render UI)

**🔴 Error: "DATABASE connection failed"**
- PostgreSQL URL is wrong
- Fix: Copy **Internal URLs** from PostgreSQL dashboard, not External

**🔴 Error: "Port already in use"**
- Usually means old process still running
- Fix: Click **Restart Service** in Render UI

**🔴 Domain not resolving**
- CNAME record not propagated yet
- Wait 10-15 minutes and try again at [dnschecker.org](https://dnschecker.org)

---

## Android App Configuration

Update your app's release build to use the production backend. In `build.gradle.kts`:

```kotlin
buildTypes {
    release {
        buildConfigField("String", "API_URL_RELEASE", "\"https://api.craftcv.com\"")
    }
}
```

Then in your app code: `RetrofitClient.baseUrl = BuildConfig.API_URL_RELEASE` for release builds.

---

## Cost Summary

- **Render Web Service**: $7/month (Starter)
- **Render PostgreSQL**: $15/month (Starter)
- **Custom Domain**: Free (register via GoDaddy/Namecheap ~$12/year)
- **Google Play Fees**: $25 one-time registration

**Total monthly**: ~$22/month

---

## Next Steps

Once backend is live at `https://api.craftcv.com`:
1. Update app's release build config to use production URL
2. Test a full flow: upload resume → verify response
3. Test Play Store billing verification
4. Upload to Google Play Console
