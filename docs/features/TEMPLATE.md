# Feature Specification: [Feature Name]

## 🎯 Objective
*Briefly describe what this feature does and why it exists.*
> Example: "Allow users to recover their password via email link."

## 📜 User Stories / Requirements
- [ ] User enters email address.
- [ ] System sends a unique token (valid for 15 mins).
- [ ] User clicks link, enters new password.
- [ ] Old password sessions are invalidated.

## 🛠 Technical Implementation
**Endpoints:**
- `POST /api/auth/recover` (Input: email)
- `POST /api/auth/reset` (Input: token, newPassword)

**Data Models:**
- Update `User` table? No.
- New `PasswordResetToken` entity (id, userId, token, expiresAt).

**Security Constraints:**
- Rate limit: 3 attempts per hour per IP.
- Token must be hashed in DB? Yes.

## ✅ Verification
- Test case: Expired token should fail.
- Test case: Used token cannot be used twice.
