# Content Moderation Strategy

**Last Updated:** 2025-10-13  
**Purpose:** Protect community from inappropriate content, spam, and off-topic reviews  
**Scope:** Review comments on fountain ratings

---

## 🎯 Overview

FontsReviewer allows users to add optional text comments to their fountain reviews. While most users will provide helpful feedback, we need a strategy to handle:

- **Off-topic content** - Comments not related to fountains (spam, ads, unrelated topics)
- **Hate speech** - Racist, sexist, homophobic, or discriminatory language
- **Harassment** - Personal attacks, threats, doxxing
- **Explicit content** - Profanity, sexual content, violence
- **Misinformation** - False claims about water safety
- **Spam** - Promotional content, repetitive messages

**Key Principle:** Balance safety with simplicity for a small app with limited resources.

---

## 🔄 Three-Tier Moderation Approach

### Tier 1: Prevention (Automated - Pre-Launch)
**Goal:** Stop obvious bad content before it's posted  
**Effort:** Medium upfront, minimal ongoing  
**Cost:** Low to free

### Tier 2: Detection (Semi-Automated - Post-Launch)
**Goal:** Flag suspicious content for review  
**Effort:** Low setup, medium ongoing  
**Cost:** Low

### Tier 3: Response (Manual - As Needed)
**Goal:** Human review and action on reported content  
**Effort:** Low to medium ongoing  
**Cost:** Free (admin time)

---

## 🛡️ Tier 1: Prevention (Automated Filters)

### A. Client-Side Input Validation

**Already implemented:**
```kotlin
// In ReviewScreen.kt
OutlinedTextField(
    value = uiState.comment,
    onValueChange = onCommentChange,
    label = { Text(stringResource(R.string.comment_optional)) },
    modifier = Modifier.fillMaxWidth(),
    minLines = 3,
    maxLines = 5
)
```

**Recommended additions:**

```kotlin
// Add character limit
OutlinedTextField(
    value = uiState.comment,
    onValueChange = { if (it.length <= 500) onCommentChange(it) },
    label = { Text(stringResource(R.string.comment_optional)) },
    supportingText = { Text("${uiState.comment.length}/500") },
    modifier = Modifier.fillMaxWidth(),
    minLines = 3,
    maxLines = 5
)
```

**Benefits:**
- ✅ Prevents extremely long spam messages
- ✅ Improves UX (users know the limit)
- ✅ No server processing needed

### B. Basic Profanity Filter (Optional)

**Implementation:** Add to `ReviewViewModel.onCommentChange()`

```kotlin
private val bannedWords = setOf(
    // Basic profanity list
    "badword1", "badword2", // etc.
)

fun onCommentChange(value: String) {
    val lowerValue = value.lowercase()
    val containsBannedWord = bannedWords.any { lowerValue.contains(it) }
    
    if (containsBannedWord) {
        _uiState.update { 
            it.copy(
                comment = value,
                commentWarning = "Please keep comments respectful"
            ) 
        }
    } else {
        _uiState.update { it.copy(comment = value, commentWarning = null) }
    }
}
```

**Pros:**
- Fast, free, offline
- Deters casual bad actors
- No external dependencies

**Cons:**
- Easy to bypass (sp4c3s, special ch@rs)
- Maintenance overhead (word list)
- False positives (Scunthorpe problem)

**Recommendation:** ⚠️ Skip for MVP. Use Tier 2 instead.

---

## 🔍 Tier 2: Detection (AI Moderation)

### A. OpenAI Moderation API (Recommended)

**Service:** [OpenAI Moderation API](https://platform.openai.com/docs/guides/moderation)  
**Cost:** **FREE** (unlimited requests)  
**Latency:** ~200-500ms per request  
**Accuracy:** Very high (trained on billions of examples)

#### How It Works

```json
POST https://api.openai.com/v1/moderations
{
  "input": "User's comment text here"
}

Response:
{
  "results": [{
    "flagged": true,
    "categories": {
      "hate": false,
      "hate/threatening": false,
      "harassment": false,
      "harassment/threatening": false,
      "self-harm": false,
      "sexual": false,
      "sexual/minors": false,
      "violence": false,
      "violence/graphic": false
    },
    "category_scores": {
      "hate": 0.001,
      "harassment": 0.89,  // High score = likely violation
      "violence": 0.002
    }
  }]
}
```

#### Implementation Strategy

**Option 1: Block on Submit (Strictest)**
```kotlin
// In ReviewViewModel.onSubmit()
suspend fun moderateComment(text: String): Boolean {
    if (text.isBlank()) return true // Allow empty comments
    
    val result = openAIModeration.check(text)
    return !result.flagged // Block if flagged
}

// Usage
if (state.comment.isNotBlank()) {
    val isAllowed = moderateComment(state.comment)
    if (!isAllowed) {
        _uiState.update { 
            it.copy(
                errorMessage = "Comment violates community guidelines",
                isSubmitting = false
            )
        }
        return
    }
}
```

**Pros:**
- ✅ No bad content ever reaches database
- ✅ Immediate feedback to user
- ✅ Simplest architecture

**Cons:**
- ❌ Adds 200-500ms to submission time
- ❌ Submission fails if API is down
- ❌ Requires API key in app (security risk)

**Option 2: Async Moderation (Recommended)**
```sql
-- Add moderation columns to reviews table
ALTER TABLE reviews ADD COLUMN moderation_status TEXT DEFAULT 'pending';
ALTER TABLE reviews ADD COLUMN moderation_checked_at TIMESTAMP;
ALTER TABLE reviews ADD COLUMN is_hidden BOOLEAN DEFAULT false;
```

```kotlin
// Client submits without moderation
// Backend Edge Function runs async moderation
```

**Pros:**
- ✅ No submission delay for users
- ✅ API key stays on server (secure)
- ✅ Can retry if moderation fails
- ✅ Can audit all checks

**Cons:**
- ❌ Bad content briefly visible
- ❌ Requires Supabase Edge Function setup
- ❌ More complex architecture

#### Setup Steps (Option 2)

**1. Add OpenAI API Key to Supabase Edge Function**
```bash
supabase secrets set OPENAI_API_KEY=sk-...
```

**2. Create Edge Function: `moderate-review`**
```typescript
// supabase/functions/moderate-review/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const openAiKey = Deno.env.get('OPENAI_API_KEY');
const supabaseUrl = Deno.env.get('SUPABASE_URL');
const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');

serve(async (req) => {
  const supabase = createClient(supabaseUrl!, supabaseKey!);
  
  // Get unmoderated reviews
  const { data: reviews } = await supabase
    .from('reviews')
    .select('id, comment')
    .eq('moderation_status', 'pending')
    .not('comment', 'is', null)
    .limit(100);
  
  for (const review of reviews || []) {
    // Call OpenAI Moderation API
    const modResponse = await fetch('https://api.openai.com/v1/moderations', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${openAiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ input: review.comment }),
    });
    
    const result = await modResponse.json();
    const flagged = result.results[0].flagged;
    
    // Update review
    await supabase
      .from('reviews')
      .update({
        moderation_status: flagged ? 'rejected' : 'approved',
        moderation_checked_at: new Date().toISOString(),
        is_hidden: flagged,
      })
      .eq('id', review.id);
  }
  
  return new Response(JSON.stringify({ success: true }), {
    headers: { 'Content-Type': 'application/json' },
  });
});
```

**3. Schedule via Supabase Cron (pg_cron)**
```sql
-- Run every 5 minutes
SELECT cron.schedule(
  'moderate-reviews',
  '*/5 * * * *',  -- Every 5 minutes
  $$
  SELECT net.http_post(
    url := 'https://[PROJECT-ID].supabase.co/functions/v1/moderate-review',
    headers := '{"Authorization": "Bearer [SERVICE_ROLE_KEY]"}'::jsonb
  );
  $$
);
```

**4. Update RLS Policy to Hide Flagged Content**
```sql
-- Update existing SELECT policy
CREATE POLICY "Reviews are viewable by everyone except hidden ones"
ON reviews FOR SELECT
USING (is_hidden = false OR is_hidden IS NULL);
```

### B. Alternative: Perspective API (Google Jigsaw)

**Service:** [Perspective API](https://perspectiveapi.com/)  
**Cost:** Free (up to 1 QPS, ~86k requests/day)  
**Attributes:** Toxicity, severe toxicity, insults, profanity, threats, identity attacks

**Pros:**
- ✅ Free
- ✅ Multi-language support
- ✅ Widely used (Wikipedia, NYTimes)

**Cons:**
- ❌ Requires API approval (takes days)
- ❌ Lower rate limits vs OpenAI
- ❌ More false positives

**Recommendation:** Use OpenAI Moderation (free + better accuracy + no approval needed)

---

## 👥 Tier 3: Manual Response (Community Moderation)

### A. User Reporting System

**Add "Report" Button to Reviews**

```kotlin
// In FountainDetailsScreen.kt - Review item
Row {
    Text(review.comment)
    
    IconButton(onClick = { onReportReview(review.id) }) {
        Icon(Icons.Default.Flag, "Report")
    }
}
```

**Database Schema:**
```sql
CREATE TABLE review_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id UUID REFERENCES reviews(id) ON DELETE CASCADE,
    reporter_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    reason TEXT NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'reviewed', 'action_taken', 'dismissed')),
    created_at TIMESTAMP DEFAULT NOW(),
    reviewed_by UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMP
);

CREATE INDEX idx_review_reports_status ON review_reports(status);
CREATE INDEX idx_review_reports_review ON review_reports(review_id);
```

**Report Reasons:**
```kotlin
enum class ReportReason(val displayName: String) {
    OFF_TOPIC("Not about the fountain"),
    SPAM("Spam or advertising"),
    HARASSMENT("Harassment or hate speech"),
    INAPPROPRIATE("Inappropriate content"),
    MISINFORMATION("False safety claims"),
    OTHER("Other")
}
```

### B. Admin Dashboard

**Option 1: Supabase Dashboard (Quick)**
- Admins can directly query `review_reports` table
- Use SQL to hide reviews: `UPDATE reviews SET is_hidden = true WHERE id = 'xxx'`
- Good for MVP, no code needed

**Option 2: In-App Admin Panel (Better UX)**

Add new screen for admin users:

```kotlin
// AdminModerationScreen.kt
@Composable
fun AdminModerationScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val reports by viewModel.pendingReports.collectAsState()
    
    LazyColumn {
        items(reports) { report ->
            Card {
                Column {
                    Text("Review ID: ${report.reviewId}")
                    Text("Comment: ${report.reviewComment}")
                    Text("Reported for: ${report.reason}")
                    Text("Reporter: ${report.reporterNickname}")
                    
                    Row {
                        Button(onClick = { viewModel.dismissReport(report.id) }) {
                            Text("Dismiss")
                        }
                        Button(onClick = { viewModel.hideReview(report.reviewId) }) {
                            Text("Hide Review")
                        }
                        Button(onClick = { viewModel.banUser(report.reviewUserId) }) {
                            Text("Ban User")
                        }
                    }
                }
            }
        }
    }
}
```

**Actions Available:**
1. **Dismiss Report** - False alarm, no action
2. **Hide Review** - Remove from public view
3. **Delete Review** - Permanently remove
4. **Warn User** - Send warning notification
5. **Temp Ban** - Block for 7/30 days
6. **Permanent Ban** - Revoke posting privileges

### C. Ban System (Optional)

```sql
ALTER TABLE profiles ADD COLUMN is_banned BOOLEAN DEFAULT false;
ALTER TABLE profiles ADD COLUMN ban_reason TEXT;
ALTER TABLE profiles ADD COLUMN ban_expires_at TIMESTAMP;

-- RLS policy to block banned users from posting
CREATE POLICY "Banned users cannot create reviews"
ON reviews FOR INSERT
WITH CHECK (
    auth.uid() IN (
        SELECT id FROM profiles 
        WHERE is_banned = false 
        OR ban_expires_at < NOW()
    )
);
```

---

## 📊 Monitoring & Metrics

### Key Metrics to Track

```sql
-- Dashboard queries
-- 1. Moderation funnel
SELECT 
    COUNT(*) FILTER (WHERE comment IS NOT NULL) as total_comments,
    COUNT(*) FILTER (WHERE moderation_status = 'approved') as approved,
    COUNT(*) FILTER (WHERE moderation_status = 'rejected') as rejected,
    COUNT(*) FILTER (WHERE is_hidden = true) as hidden
FROM reviews;

-- 2. Report volume
SELECT 
    DATE(created_at) as date,
    COUNT(*) as reports,
    COUNT(*) FILTER (WHERE status = 'action_taken') as actions_taken
FROM review_reports
GROUP BY DATE(created_at)
ORDER BY date DESC
LIMIT 30;

-- 3. Top reported users
SELECT 
    p.nickname,
    COUNT(rr.id) as report_count
FROM review_reports rr
JOIN reviews r ON rr.review_id = r.id
JOIN profiles p ON r.user_id = p.id
WHERE rr.status = 'pending'
GROUP BY p.id, p.nickname
ORDER BY report_count DESC
LIMIT 10;

-- 4. False positive rate (if tracking)
SELECT 
    COUNT(*) FILTER (WHERE moderation_status = 'rejected' AND is_hidden = false) as false_positives,
    COUNT(*) FILTER (WHERE moderation_status = 'rejected') as total_flagged
FROM reviews;
```

### Alerts to Set Up

**Supabase SQL Hook Example:**
```sql
-- Alert if >10 reports in an hour
CREATE OR REPLACE FUNCTION check_report_spike()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM review_reports WHERE created_at > NOW() - INTERVAL '1 hour') > 10 THEN
        -- Send notification (via webhook, email, etc.)
        PERFORM net.http_post(
            url := 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL',
            body := '{"text": "⚠️ High report volume detected"}'::jsonb
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER report_spike_check
AFTER INSERT ON review_reports
FOR EACH ROW EXECUTE FUNCTION check_report_spike();
```

---

## 🚀 Recommended Implementation Roadmap

### Phase 1: MVP Launch (Week 1-2)
**Goal:** Basic protection with minimal effort

✅ **Implement:**
1. Character limit (500 chars) on comment field
2. User report button with simple reasons
3. Manual admin review via Supabase dashboard
4. Hide flagged reviews via SQL

⏱️ **Effort:** 4-6 hours  
💰 **Cost:** $0  
✅ **Launch Ready:** Yes

### Phase 2: Automated Detection (Month 1-2)
**Goal:** Scale moderation as user base grows

✅ **Implement:**
1. OpenAI Moderation API (async via Edge Function)
2. Auto-hide high-confidence violations
3. Admin dashboard in app
4. Scheduled checks every 5 minutes

⏱️ **Effort:** 8-12 hours  
💰 **Cost:** $0 (free API)  
📈 **Trigger:** 100+ reviews with comments, or first spam incident

### Phase 3: Advanced Tools (Month 3-6)
**Goal:** Comprehensive moderation system

✅ **Implement:**
1. User ban system (temp + permanent)
2. Moderation metrics dashboard
3. Appeal system
4. Rate limiting (max 5 reviews/day)
5. Email notifications to admins

⏱️ **Effort:** 16-24 hours  
💰 **Cost:** $0  
📈 **Trigger:** 1,000+ active users

---

## 📖 Community Guidelines (Draft)

**Create file:** `docs/COMMUNITY_GUIDELINES.md`  
**Link from:** App settings, report dialog, Play Store

```markdown
# FontsReviewer Community Guidelines

Our mission is to help people find the best fountains in Barcelona. 
To keep the community helpful and respectful, please follow these guidelines:

## ✅ DO:
- Share your honest experience with the fountain
- Comment on water quality, taste, location, accessibility
- Be constructive and helpful to other users
- Report fountains with safety issues

## ❌ DON'T:
- Post content unrelated to fountains
- Use hate speech, discrimination, or harassment
- Share personal information of others
- Post spam, ads, or promotional content
- Make false claims about water safety
- Use excessive profanity

## 🚫 Violations
Users who violate these guidelines may have content removed, 
receive warnings, or be banned from posting reviews.

## 📧 Contact
Questions? Email: support@fontsreviewer.com
```

**Add to string resources:**
```xml
<!-- values/strings.xml -->
<string name="community_guidelines">Community Guidelines</string>
<string name="report_review">Report Review</string>
<string name="report_reason">Why are you reporting this?</string>
<string name="report_submitted">Report submitted. Thank you!</string>
```

---

## 🔒 Legal Considerations

### GDPR Compliance (EU Users)

**User Rights:**
- Right to report content
- Right to appeal moderation decisions
- Right to delete their own reviews

**Admin Obligations:**
- Document moderation policies
- Store moderation logs (who, when, why)
- Respond to GDPR requests

**Already Compliant:** Account deletion function (see `EDGE_FUNCTION_SETUP.md`)

### Digital Services Act (DSA) - EU

**Thresholds:**
- **< 45M EU users:** Basic transparency requirements
- **You're fine** - Small local app, no DSA obligations

**Good Practices Anyway:**
- Clear reporting mechanism ✅
- Transparent guidelines ✅
- Reasonable response time ✅

### Section 230 (US) / Article 14 (EU)

**Platform vs Publisher:**
- You're a platform (users create content)
- Not liable for user content IF you:
  - Don't editorially control content
  - Remove illegal content when notified
  - Act in good faith

**Your moderation does NOT make you a publisher** - you're protected.

---

## 💰 Cost Analysis

| Solution | Setup Time | Ongoing Time | Monthly Cost | Accuracy |
|----------|------------|--------------|--------------|----------|
| **Manual only** | 4 hours | 2-10 hrs/week | $0 | Medium |
| **OpenAI Moderation** | 12 hours | 1 hr/week | $0 | High |
| **Perspective API** | 12 hours | 1 hr/week | $0 | Medium-High |
| **Client filter** | 2 hours | 1 hr/month | $0 | Low |
| **Paid service (e.g., Sift)** | 8 hours | 0 hrs/week | $200-500 | Very High |

**Recommended:** OpenAI Moderation (async) + Manual review for reports

---

## 🎯 Success Metrics

### Targets (First 3 Months)

| Metric | Target | Notes |
|--------|--------|-------|
| **Report rate** | < 1% of comments | Industry standard: 0.5-2% |
| **False positive rate** | < 5% | Balance safety vs. over-blocking |
| **Admin response time** | < 24 hours | For reported content |
| **Appeal success rate** | Track only | Indicates filter accuracy |
| **Repeat offenders** | < 0.1% of users | Most users are good |

### Red Flags

⚠️ **Investigate if:**
- Report rate > 5% (system issue or attack)
- Same user reported 3+ times
- Spike in reports (coordinated abuse)
- False positive rate > 20% (filter too strict)

---

## 🛠️ Tools & Resources

### Free Moderation APIs
- **OpenAI Moderation:** https://platform.openai.com/docs/guides/moderation
- **Perspective API:** https://perspectiveapi.com/
- **Azure Content Safety:** https://azure.microsoft.com/en-us/products/ai-services/ai-content-safety

### Profanity Lists (if needed)
- **LDNOOBW:** https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words
- **Better Profanity:** https://github.com/snguyenthanh/better_profanity

### Best Practices Guides
- **Trust & Safety Professional Association:** https://www.tspa.org/
- **Mozilla Community Guidelines:** https://www.mozilla.org/en-US/about/governance/policies/participation/

---

## 📝 Next Steps

### For MVP Launch (This Week)
1. ✅ Add character limit to comment field (already done in code)
2. ⬜ Add report button to review items
3. ⬜ Create `review_reports` table in Supabase
4. ⬜ Document community guidelines
5. ⬜ Test manual moderation workflow

### For Post-Launch (Month 1)
1. ⬜ Set up OpenAI Moderation Edge Function
2. ⬜ Add `is_hidden` column to reviews
3. ⬜ Update RLS policies
4. ⬜ Schedule cron job for automated checks
5. ⬜ Monitor metrics

### For Scale (Month 3+)
1. ⬜ Build in-app admin panel
2. ⬜ Implement ban system
3. ⬜ Add appeal workflow
4. ⬜ Set up alert system

---

## 🤔 FAQ

**Q: Do I need moderation for an MVP?**  
A: Yes, basic reporting + manual review. Users expect safety features, even day 1.

**Q: Will moderation slow down reviews?**  
A: Not with async approach. Review posts instantly, gets checked within 5 minutes.

**Q: What if someone posts something really bad?**  
A: OpenAI flags it within 5 minutes, auto-hides it. You review & delete permanently.

**Q: Can I be sued for user content?**  
A: Very unlikely if you moderate in good faith and remove illegal content when notified. Platforms have legal protection.

**Q: How much time does moderation take?**  
A: MVP (manual): 1-2 hours/week. Automated: 15 min/week to check reports.

**Q: Should I hire a content moderator?**  
A: Not until 10,000+ active users. You can handle it yourself until then.

---

## 📞 Support

**Questions about this strategy?**
- Email: joan.heredia@example.com
- GitHub Issues: FontsReviewer repo

**Incident Response:**
1. Hide review immediately (SQL: `UPDATE reviews SET is_hidden = true WHERE id = 'xxx'`)
2. Document incident
3. Notify affected users (if needed)
4. Review moderation rules

---

**Last Updated:** 2025-10-13  
**Version:** 1.0  
**Status:** Ready for Implementation  
**Next Review:** After 100 reviews with comments OR first incident
