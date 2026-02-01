# ⚡ Q Welcome Config Generator

<div align="center">

**Create and manage message templates for the Q Welcome mobile app**

🎨 **Easy to Use** • 💾 **Works Offline** • 🔒 **Private & Secure** • 📱 **Mobile Friendly**

[🚀 Get Started](#-getting-started) • [📖 User Guide](#-user-guide) • [❓ FAQ](#-troubleshooting)

</div>

---

## 📋 What is This?

Q Welcome Config Generator is a web-based tool for technicians and support staff to create, organize, and share message templates for the Q Welcome mobile app.

**Perfect for:**

- 🏢 ISP technicians sending WiFi credentials
- 📞 Support teams with standard responses
- 👥 Teams sharing template collections
- 💼 Technicians backing up personal templates

---

## ✨ Key Features

### 📝 Smart Template Editor

- Real-time character counter
- SMS segment calculator (know if your message splits)
- Live preview with sample data
- Click-to-insert placeholders

### 💾 Two Export Modes

- **Template Pack** - Share with your team (no personal info)
- **Full Backup** - Save everything including your tech profile

### 🔄 Import & Merge

- Import templates from teammates
- Automatically skip duplicates
- Validate before importing

### 🎨 Comfortable Interface

- Dark theme (easy on the eyes)
- Density toggle (compact or spacious)
- Works on phone, tablet, or desktop
- Keyboard shortcuts for power users

### 🔒 Your Data Stays Private

- Everything saved in your browser
- Works 100% offline
- No account required
- No tracking or analytics

---

## 🚀 Getting Started

### First-Time Setup

1. **Choose Your Export Type**

   When you first open the app, select:
   - **📦 Template Pack** if you're creating templates to share
   - **💼 Full Backup** if you want to include your personal info

2. **Fill in Tech Profile** (Full Backup only)

   Add your details so they auto-fill in templates:
   - Name (e.g., "John Smith")
   - Job Title (e.g., "Field Technician")
   - Department (e.g., "Network Services")

3. **You're Ready!**

   Your settings are automatically saved. You can change them anytime.

---

## 📖 User Guide

### Creating a Template

**Step 1: Add Template Information**

```text
Template Name: WiFi Welcome
```

The slug (URL-friendly name) is auto-generated: `wifi_welcome`

**Step 2: Write Your Message**

Type your message in the editor. Click any placeholder to insert it:

- `{{ customer_name }}` - Customer's name
- `{{ ssid }}` - WiFi network name
- `{{ password }}` - WiFi password
- `{{ account_number }}` - Account/ticket number
- `{{ tech_signature }}` - Your signature (if enabled)

**Example Template:**

```text
Hi {{ customer_name }}!

Your WiFi is now active:
📶 Network: {{ ssid }}
🔑 Password: {{ password }}
👤 Account: {{ account_number }}

{{ tech_signature }}
```

**Step 3: Monitor Your Message**

Watch the character counter:

- ✅ **Green** - Good to go
- ⚠️ **Orange** - Getting long (1000+ chars)
- 🔴 **Red** - Very long (1500+ chars)

The SMS counter shows approximately how many text messages this will send as.

**Step 4: Save**

Click **➕ Add Template** - done!

### Managing Templates

**Edit a Template**

- Click the ✏️ edit icon next to any template
- Make your changes
- Click **➕ Add Template** to update

**Delete a Template**

- Click the 🗑️ delete icon
- Confirm when prompted

**View Templates**

- All templates appear in the right panel
- Active template is highlighted
- Click any template to edit it

### Exporting Your Work

You have two options:

**📋 Copy to Clipboard**

1. Click **Copy JSON** button
2. Paste anywhere (Slack, email, notes)

**💾 Download File**

1. Click **Download JSON** button
2. File saves as `qwelcome-config-YYYY-MM-DD.json`
3. Share this file or keep as backup

### Importing Templates

**From a File or Text**

1. Switch to **📥 Import JSON** tab
2. Paste JSON or drag file content
3. Click **Import & Merge** to add templates
4. Or click **Validate Only** to check first

**What Happens:**

- New templates are added
- Duplicates are skipped (based on slug)
- You'll see a summary of what was imported

### Keyboard Shortcuts

Save time with these shortcuts:

| Windows/Linux | Mac | Action |
| --------------- | ----- | -------- |
| `Ctrl + S` | `⌘ + S` | Download JSON |
| `Ctrl + K` | `⌘ + K` | Copy to clipboard |
| `Esc` | `Esc` | Close expanded editor |
| `→` `←` | `→` `←` | Switch export type |
| `Tab` | `Tab` | Navigate tabs |

### Expanded Editor

For longer templates, click **⛶ Expand Editor**:

- Larger editing area
- Side-by-side preview
- Same placeholders available
- Press `Esc` to close

---

## 🧩 Understanding Placeholders

Placeholders are replaced with real data when you use templates in the Q Welcome app.

| Placeholder | Gets Replaced With | Example |
| ------------- | ------------------- | --------- |
| `{{ customer_name }}` | Customer's full name | John Smith |
| `{{ ssid }}` | WiFi network name | HomeNetwork_5G |
| `{{ password }}` | WiFi password | SecurePass123! |
| `{{ account_number }}` | Account or ticket number | ACC-12345 |
| `{{ tech_signature }}` | Your signature block | Mike Johnson<br>Field Technician<br>Network Services |

**💡 Tip:** The signature only appears if you:

1. Selected **Full Backup** export type
2. Filled in your tech profile
3. Enabled "Include signature in templates"

---

## 🔄 Template Pack vs Full Backup

### 📦 Template Pack

**Best for:** Sharing with teammates

**What's included:**

- ✅ All your templates
- ✅ Export date/time
- ❌ No personal information

**When to use:**

- Sharing in Slack or email
- Posting to team wiki
- Onboarding new team members
- Company-wide distribution

### 💼 Full Backup

**Best for:** Personal backup

**What's included:**

- ✅ All your templates
- ✅ Export date/time
- ✅ Your tech profile (name, title, dept)
- ✅ Signature settings

**When to use:**

- Switching devices
- Personal archive
- Backup before system wipe
- Testing with signature enabled

---

## 💡 Tips & Tricks

### Working Faster

**Use the Density Toggle**

- Click the **📐 Comfy** / **📏 Compact** button in header
- **Comfy**: Larger buttons, more spacing (better for touch)
- **Compact**: Information-dense, more on screen (better for desktop)

**Edit Multiple Templates**

- Create base template
- Save it
- Click edit, modify, save as new
- Faster than starting from scratch

**Template Naming**

- Use clear names: "WiFi Setup", "Follow-up Call", "Account Created"
- Avoid: "Template 1", "test", "asdf"
- Makes finding templates easier

### Avoiding Common Mistakes

**❌ Don't:**

- Include real customer data in templates (use placeholders)
- Create duplicate slugs (app will reject them)
- Skip validation when importing
- Delete all templates without exporting first

**✅ Do:**

- Use descriptive template names
- Test templates with the preview
- Export backups regularly
- Validate before importing shared templates

---

## 🐛 Troubleshooting

### "My templates aren't saving!"

**Check these:**

1. Is JavaScript enabled in your browser?
2. Is localStorage enabled? (Check Settings → Privacy)
3. Are you in Private/Incognito mode? (Data won't persist)
4. Is your browser storage full? (Clear old site data)

**Quick fix:** Try in a normal (non-private) browser tab.

---

### "Import failed!"

**Common causes:**

- JSON syntax error (missing comma, quote, bracket)
- Wrong export type (not from Q Welcome Config Generator)
- File corrupted during copy/paste

**How to fix:**

1. Click **Validate Only** first
2. Read the error message (it tells you what's wrong)
3. Fix the JSON or get a fresh copy
4. Try again

---

### "Where did my templates go?"

Your templates are saved in **browser storage** for that specific site.

**Lost templates if:**

- ❌ Cleared browser data/cookies
- ❌ Used different browser or device
- ❌ Used private/incognito mode

**Recovery:**

- If you exported: Just re-import your JSON file
- If not: Templates are gone (can't recover)

**Prevention:** Export your templates regularly!

---

### "Template looks wrong in app"

**The preview is approximate** - actual appearance depends on:

- Mobile app version
- Device screen size
- Font settings
- Real data length

**Always test** templates with real data in the mobile app.

---

### "Can't copy or download"

**Browser permissions:**

- Some browsers block clipboard access
- Some browsers block downloads from data URLs

**Solutions:**

1. **Copy**: Click button, allow permission when prompted
2. **Download**: Check browser download settings
3. **Alternative**: Manually select JSON preview, copy with Ctrl+C

---

## 🔐 Privacy & Security

**Your data is safe:**

- ✅ Everything runs in your browser
- ✅ Nothing sent to any server
- ✅ Works completely offline
- ✅ No account or login needed
- ✅ No tracking or analytics
- ✅ No cookies (except localStorage)

**What this means:**

- Your templates never leave your device
- No one can see what you create
- No internet connection needed after first load
- Safe to use on company networks

**Security tip:** Use HTTPS when accessing the site (check for 🔒 in browser address bar).

---

## 📱 Device Compatibility

**Works on:**

- ✅ Desktop: Windows, Mac, Linux
- ✅ Phones: iPhone, Android
- ✅ Tablets: iPad, Android tablets
- ✅ Browsers: Chrome, Firefox, Safari, Edge

**Best experience:**

- Chrome 90+ or Firefox 88+ (latest features)
- Screen width 320px minimum
- JavaScript enabled

**Accessibility:**

- ✅ Screen reader compatible
- ✅ Keyboard navigation
- ✅ High contrast support
- ✅ Respects reduced motion settings

---

## ❓ FAQ

**Q: Do I need to create an account?**  
A: No! Everything is stored locally in your browser.

**Q: Can I use this on multiple devices?**  
A: Yes, but templates don't sync automatically. Export from device A, import to device B.

**Q: How many templates can I create?**  
A: Browser storage allows thousands, but we recommend keeping under 100 for best performance.

**Q: Can I share templates with teammates?**  
A: Yes! Export as **Template Pack** and share the JSON file via email, Slack, etc.

**Q: Will my templates sync to the mobile app?**  
A: Not automatically. You'll import the JSON file into the Q Welcome mobile app separately.

**Q: What happens if I clear my browser data?**  
A: Your templates will be deleted. Always keep an exported backup!

**Q: Can I edit the JSON file directly?**  
A: Yes, but use the validation tool before importing to catch errors.

**Q: Is there a template limit?**  
A: No hard limit, but browser storage is typically 5-10MB per site.

---

## 🆘 Need Help?

**Found a bug?**  
Report it on [GitHub Issues](https://github.com/H2OKing89/QWelcome/issues)

**Have a question?**  
Ask in [GitHub Discussions](https://github.com/H2OKing89/QWelcome/discussions)

**Want a feature?**  
Suggest it on [GitHub Issues](https://github.com/H2OKing89/QWelcome/issues) with the "enhancement" label

---

<div align="center">

**Made with 💜 for technicians everywhere**

[⬆ Back to Top](#-q-welcome-config-generator)

</div>
