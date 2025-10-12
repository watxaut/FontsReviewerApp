# Supabase Edge Function Setup Guide

## 📍 Where to Create Edge Functions

Edge Functions are created **in your Supabase project**, not in your Android app code.

There are **two ways** to create and deploy Edge Functions:

---

## 🎯 Method 1: Supabase CLI (Recommended)

This is the standard way to create and deploy Edge Functions.

### **Prerequisites:**

1. **Install Supabase CLI:**

```bash
# macOS (using Homebrew)
brew install supabase/tap/supabase

# macOS/Linux (using npm)
npm install -g supabase

# Windows (using Scoop)
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase
```

2. **Verify Installation:**

```bash
supabase --version
# Should output: supabase 1.x.x
```

3. **Login to Supabase:**

```bash
supabase login
# Opens browser for authentication
# Follow the prompts to login
```

---

### **Step-by-Step: Create & Deploy Edge Function**

#### **1. Initialize Supabase in Your Project**

```bash
# Navigate to your project directory
cd /Users/joan.heredia/AndroidStudioProjects/FontsReviewer

# Initialize Supabase (creates supabase/ directory)
supabase init
```

**Output:**
```
✓ Finished supabase init.
✓ Created supabase directory.
✓ Created supabase/config.toml
```

#### **2. Link to Your Supabase Project**

```bash
# Get your project reference from Supabase Dashboard
# URL format: https://app.supabase.com/project/YOUR_PROJECT_ID

supabase link --project-ref YOUR_PROJECT_ID
```

**Find your Project ID:**
1. Go to: https://app.supabase.com/
2. Select your project (FontsReviewer)
3. Settings → General
4. Copy "Reference ID" (looks like: `zibnlshkbketdkegddno`)

**Example:**
```bash
supabase link --project-ref zibnlshkbketdkegddno
```

#### **3. Create the Edge Function**

```bash
supabase functions new delete-user-account
```

**Output:**
```
✓ Created new Function at supabase/functions/delete-user-account/index.ts
```

**File Structure Created:**
```
FontsReviewer/
├── supabase/
│   ├── functions/
│   │   └── delete-user-account/
│   │       └── index.ts          ← Your function code goes here
│   └── config.toml
```

#### **4. Write the Function Code**

Edit `supabase/functions/delete-user-account/index.ts`:

```typescript
// supabase/functions/delete-user-account/index.ts

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Get the authorization header from the request
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      throw new Error('No authorization header')
    }

    const token = authHeader.replace('Bearer ', '')

    // Create a Supabase client with the user's JWT
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: authHeader },
        },
      }
    )

    // Get the user from the JWT
    const {
      data: { user },
      error: userError,
    } = await supabaseClient.auth.getUser(token)

    if (userError || !user) {
      throw new Error('Unauthorized')
    }

    console.log(`Deleting account for user: ${user.id}`)

    // Create admin client with service role key
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Step 1: Delete profile (this will cascade delete reviews)
    const { error: profileError } = await supabaseAdmin
      .from('profiles')
      .delete()
      .eq('id', user.id)

    if (profileError) {
      console.error('Profile deletion error:', profileError)
      throw new Error(`Failed to delete profile: ${profileError.message}`)
    }

    console.log('Profile deleted successfully')

    // Step 2: Delete auth user
    const { error: authError } = await supabaseAdmin.auth.admin.deleteUser(
      user.id
    )

    if (authError) {
      console.error('Auth deletion error:', authError)
      throw new Error(`Failed to delete auth user: ${authError.message}`)
    }

    console.log('Auth user deleted successfully')

    return new Response(
      JSON.stringify({
        success: true,
        message: 'Account deleted successfully',
      }),
      {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    )
  } catch (error) {
    console.error('Error:', error.message)
    return new Response(
      JSON.stringify({
        success: false,
        error: error.message,
      }),
      {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    )
  }
})
```

#### **5. Deploy the Edge Function**

```bash
supabase functions deploy delete-user-account
```

**Output:**
```
✓ Deploying function delete-user-account...
✓ Function delete-user-account deployed successfully.
✓ URL: https://YOUR_PROJECT_ID.supabase.co/functions/v1/delete-user-account
```

#### **6. Test the Edge Function**

**Via cURL:**
```bash
curl -i --location --request POST \
  'https://YOUR_PROJECT_ID.supabase.co/functions/v1/delete-user-account' \
  --header 'Authorization: Bearer YOUR_USER_JWT' \
  --header 'Content-Type: application/json'
```

**Get JWT Token:**
You can get the user's JWT from the Supabase client in your app after login.

---

### **7. Update Your Android App to Use Edge Function**

Replace the `deleteAccount()` function in `SupabaseService.kt`:

```kotlin
suspend fun deleteAccount(): Result<Unit> {
    return try {
        val userId = getCurrentUserId()
        if (userId == null) {
            return Result.failure(Exception("No user logged in"))
        }
        
        // Get current session token
        val session = client.auth.currentSessionOrNull()
        val token = session?.accessToken
        if (token == null) {
            return Result.failure(Exception("No session token"))
        }
        
        // Call Edge Function
        val response = client.functions.invoke(
            function = "delete-user-account",
            body = emptyMap<String, String>() // No body needed, user identified by JWT
        )
        
        // Check if successful
        if (response.error != null) {
            return Result.failure(Exception(response.error.toString()))
        }
        
        // Sign out locally
        client.auth.signOut()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 🌐 Method 2: Supabase Dashboard (Visual)

If you prefer not to use CLI, you can create Edge Functions via the dashboard.

### **Steps:**

1. **Go to Supabase Dashboard:**
   - https://app.supabase.com/
   - Select your project (FontsReviewer)

2. **Navigate to Edge Functions:**
   - Click **"Functions"** in left sidebar
   - Click **"Create a new function"**

3. **Create Function:**
   - **Name:** `delete-user-account`
   - **Import:** Paste the TypeScript code from above
   - Click **"Deploy"**

4. **Function URL:**
   - After deployment, you'll see the URL:
   - `https://YOUR_PROJECT_ID.supabase.co/functions/v1/delete-user-account`

**Note:** Dashboard method is easier but CLI is recommended for version control and team collaboration.

---

## 🔐 Environment Variables

Edge Functions automatically have access to these environment variables:

- `SUPABASE_URL` - Your project URL
- `SUPABASE_ANON_KEY` - Your anon/public key
- `SUPABASE_SERVICE_ROLE_KEY` - Your service role key (admin privileges)

**You don't need to configure these manually!** They're automatically injected by Supabase.

---

## 📁 Project Structure After Setup

```
FontsReviewer/
├── app/                          ← Your Android app
│   ├── src/
│   └── build.gradle.kts
├── supabase/                     ← Supabase configuration (NEW)
│   ├── functions/                ← Edge Functions
│   │   └── delete-user-account/
│   │       └── index.ts
│   └── config.toml               ← Supabase config
├── local.properties
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🧪 Testing Edge Functions

### **1. Test Locally (Optional):**

```bash
# Start local Supabase (includes Edge Functions)
supabase start

# Deploy function locally
supabase functions serve delete-user-account

# Function available at:
# http://localhost:54321/functions/v1/delete-user-account
```

### **2. Test in Production:**

```bash
# Deploy to production
supabase functions deploy delete-user-account

# Test with curl
curl -i --location --request POST \
  'https://zibnlshkbketdkegddno.supabase.co/functions/v1/delete-user-account' \
  --header 'Authorization: Bearer YOUR_JWT_TOKEN' \
  --header 'Content-Type: application/json'
```

### **3. View Logs:**

```bash
# View function logs
supabase functions logs delete-user-account

# Or in dashboard:
# Functions → delete-user-account → Logs
```

---

## 🚀 Complete Deployment Workflow

### **First Time Setup:**

```bash
# 1. Install Supabase CLI
brew install supabase/tap/supabase

# 2. Login
supabase login

# 3. Initialize project
cd /Users/joan.heredia/AndroidStudioProjects/FontsReviewer
supabase init

# 4. Link to your project
supabase link --project-ref YOUR_PROJECT_ID

# 5. Create function
supabase functions new delete-user-account

# 6. Edit supabase/functions/delete-user-account/index.ts
# (Paste the code from above)

# 7. Deploy
supabase functions deploy delete-user-account
```

### **Subsequent Deployments:**

```bash
# After making changes to index.ts
supabase functions deploy delete-user-account
```

---

## 🛠️ Troubleshooting

### **Issue: Command not found: supabase**

**Solution:**
```bash
# Reinstall Supabase CLI
brew uninstall supabase
brew install supabase/tap/supabase

# Or use npm
npm install -g supabase
```

### **Issue: Project not linked**

**Solution:**
```bash
supabase link --project-ref YOUR_PROJECT_ID
```

### **Issue: Unauthorized when calling function**

**Solution:**
- Make sure you're passing the correct JWT token in the `Authorization` header
- Token format: `Bearer YOUR_JWT_TOKEN`
- Get token from: `client.auth.currentSessionOrNull()?.accessToken`

### **Issue: Service role key not working**

**Solution:**
- The service role key is automatically available as `SUPABASE_SERVICE_ROLE_KEY`
- You don't need to configure it manually
- Supabase injects it automatically into Edge Functions

### **Issue: Function returns 404**

**Solution:**
- Verify deployment: `supabase functions list`
- Check function name matches exactly
- URL format: `https://PROJECT_ID.supabase.co/functions/v1/FUNCTION_NAME`

---

## 📊 Cost Considerations

**Edge Functions Pricing (Supabase):**
- **Free Tier:** 500,000 function invocations per month
- **Pro Tier:** $25/month includes 2,000,000 invocations
- **Additional:** $2 per 1,000,000 invocations

**For Your App:**
- Account deletion is rare (< 1% of users per month)
- If you have 10,000 users, expect ~100 deletions/month
- **Well within free tier** ✅

---

## ✅ Quick Start Checklist

For your FontsReviewer app:

- [ ] Install Supabase CLI: `brew install supabase/tap/supabase`
- [ ] Login: `supabase login`
- [ ] Navigate to project: `cd /Users/joan.heredia/AndroidStudioProjects/FontsReviewer`
- [ ] Initialize: `supabase init`
- [ ] Link project: `supabase link --project-ref YOUR_PROJECT_ID`
- [ ] Create function: `supabase functions new delete-user-account`
- [ ] Copy code into: `supabase/functions/delete-user-account/index.ts`
- [ ] Deploy: `supabase functions deploy delete-user-account`
- [ ] Update Android app to call Edge Function
- [ ] Test with a test user
- [ ] Verify in Supabase Dashboard that auth user is deleted

---

## 📝 Summary

**Where to create:** In your Supabase project using CLI or Dashboard

**Recommended:** Use Supabase CLI (Method 1)

**Location on disk:** `FontsReviewer/supabase/functions/delete-user-account/index.ts`

**Deployment:** `supabase functions deploy delete-user-account`

**URL:** `https://YOUR_PROJECT_ID.supabase.co/functions/v1/delete-user-account`

**Next Steps:**
1. Install CLI
2. Create function
3. Deploy
4. Update Android app to call it
5. Test!

---

**Need help with any step?** Let me know which part you'd like me to clarify! 🚀
