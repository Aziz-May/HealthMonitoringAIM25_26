# Frontend Validation & Error Handling Improvements

## What Was Added

### 1. **Login Page (`login.html`)**
- ✅ **Username Validation:**
  - Must be at least 3 characters
  - Maximum 50 characters
  - Only allows letters, numbers, underscore (_), and hyphen (-)
  - Shows error message immediately when user leaves the field

- ✅ **Password Validation:**
  - Must be at least 6 characters
  - Shows error if empty

- ✅ **Error Display:**
  - Red error banner at the top for general errors
  - Field-specific error messages below each input
  - Visual feedback (red border) on invalid fields
  - Smooth animations for error display

- ✅ **Backend Error Handling:**
  - If backend returns "Invalid credentials", user sees friendly message
  - No more confusing backend error pages

### 2. **Registration Page (`register.html`)**
- ✅ **Username Validation:**
  - Same rules as login (3-50 chars, alphanumeric with _ and -)
  - Real-time validation

- ✅ **Password Strength Indicator:**
  - Visual bar showing password strength (Weak/Medium/Strong)
  - Minimum 8 characters required (more secure than login)
  - Checks for:
    - Length
    - Uppercase + lowercase letters
    - Numbers
    - Special characters (!@#$%^&*)
  - Color-coded: Red (Weak) → Yellow (Medium) → Green (Strong)

- ✅ **Confirm Password:**
  - Checks if passwords match
  - Shows error if they don't match

- ✅ **Duplicate Username:**
  - Backend checks if username exists
  - User sees "Username already exists" message instead of error page

### 3. **Backend Changes (`AuthenticationEndpoint.java`)**
- ✅ **Redirect with Errors Instead of Error Pages:**
  - Login failures redirect back to login page with `?error=...&error_description=...`
  - Registration failures redirect back to register page with error details
  - OAuth parameters preserved in URL so user doesn't lose their session

## How It Works

### User Experience Flow:

#### **Login Scenario:**
1. User clicks "Login" button on frontend
2. Redirected to backend `/authorize` → `/login`
3. Enters username and password
4. **If valid:** Proceeds to consent or dashboard
5. **If invalid:** 
   - Backend redirects to `/authorize?error=authentication_failed&error_description=Invalid+username+or+password`
   - Login page shows red error banner: "Invalid username or password"
   - User can try again immediately

#### **Registration Scenario:**
1. User clicks "Sign Up" button on frontend
2. Redirected to `/register` page
3. Types username → Real-time check (3+ chars, valid format)
4. Types password → Strength indicator appears (Weak → Medium → Strong)
5. Types confirm password → Checks if it matches
6. **If username exists:**
   - Backend redirects: `/register?error=registration_failed&error_description=Username+already+exists&client_id=...&state=...`
   - Register page shows: "Username already exists"
   - OAuth params preserved in URL
7. **If passwords don't match:**
   - Frontend shows error before submission
   - Form won't submit until fixed

## Technical Details

### Frontend Validation Rules:
```javascript
Username:
- Length: 3-50 characters
- Pattern: /^[a-zA-Z0-9_-]+$/
- Examples: ✅ "john_doe", "user123", "health-monitor" ❌ "ab", "user@123", "name with spaces"

Password (Login):
- Minimum: 6 characters

Password (Registration):
- Minimum: 8 characters
- Strength calculation:
  * +1 point: Length >= 8
  * +1 point: Has both uppercase AND lowercase
  * +1 point: Has numbers
  * +1 point: Has special characters
  * Weak: 0-1 points (rejected)
  * Medium: 2-3 points (accepted)
  * Strong: 4 points (accepted)
```

### Error Display System:
1. **General Errors** → Red banner at top
2. **Field Errors** → Message below the field + red border
3. **URL Parameters** → `?error=type&error_description=message`
4. **Smooth UX** → Animations, auto-scroll to errors, clear on input

## Testing

### To Test Login Validation:
1. Go to `http://127.0.0.1:5500/app/`
2. Click "Login"
3. Try these:
   - Empty username → "Username is required"
   - "ab" → "Username must be at least 3 characters"
   - "user@name" → "Username can only contain letters, numbers, _ and -"
   - Correct username but wrong password → Backend returns "Invalid username or password"

### To Test Registration Validation:
1. Click "Sign Up"
2. Try these:
   - Username "ab" → Error immediately
   - Password "123" → Shows as "Weak", won't submit
   - Password "Password123!" → Shows as "Strong", allowed
   - Different passwords in confirm → "Passwords do not match"
   - Existing username → Backend shows "Username already exists"

## Benefits
✅ **Better UX:** Users know exactly what's wrong  
✅ **Security:** Strong password requirements  
✅ **No Confusion:** Friendly error messages instead of backend stack traces  
✅ **Professional:** Smooth animations and visual feedback  
✅ **Preserved State:** OAuth flow continues even after errors  
