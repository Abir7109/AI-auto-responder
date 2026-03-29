# Google Play Protect Appeal - Additional Information

## App Name
AI AutoResponder

## Package Name
com.zai.autoresponder

## Developer
RM Abir

## App Description
AI AutoResponder is a personal assistant app that helps users automatically respond to messages on messaging platforms using AI. It is designed for personal use only and helps users stay responsive even when they are busy.

## Core Features
1. **Auto-reply to messages** - Automatically generates and sends AI-powered responses to incoming messages
2. **Multi-platform support** - Works with WhatsApp, Messenger, Telegram, Instagram, and Facebook
3. **Custom rules** - Users can create custom auto-reply rules for specific contacts or keywords
4. **Knowledge base (Brain)** - Users can add personal information about themselves so AI can give personalized responses
5. **Analytics dashboard** - Track sent messages and response statistics

## Why This App Requires Special Permissions

### 1. Accessibility Service
**Purpose:** To read chat messages and send automated replies
**Why needed:** This is the only way to access message content from other apps on Android without root access. This is the same method used by similar apps like AutoResponder.ai.
**How it's used:** The app only reads message content and sends replies when enabled by the user. It does NOT collect, store, or transmit any personal data to external servers (except the message content sent to the AI API for generating responses).

### 2. Notification Listener Service
**Purpose:** To detect incoming messages
**Why needed:** To know when a new message arrives so the app can generate and send an appropriate response
**How it's used:** The service only monitors notification events and does not store or share any notification content.

### 3. Internet Permission
**Purpose:** To communicate with AI API (Groq Llama 3.1)
**Why needed:** To generate AI-powered responses using the Groq API
**How it's used:** Only the message text is sent to the AI API to generate a response. No other data is collected or transmitted.

## Privacy & Security
- All data is stored locally on the device
- No personal data is collected or shared with third parties
- Users have full control over the app and can disable it at any time
- The app requires explicit user permission to access messages

## Similar Apps on Google Play
This app functions similarly to other auto-reply apps already available on Google Play, such as:
- AutoResponder.ai
- Auto Reply - Text Auto Reply
- WhatsAuto Reply

## VirusTotal Analysis
- SHA256: 1f9104ea1cdd2b26229f167e04a820cf8c09ca83cc3baa930bd7f6c6202270bf
- MD5: 76f0bd6645eb487835acde949eae877e
- File Size: 7,780,722 bytes

## Request
This app is a legitimate personal productivity tool designed to help users manage their messages. It does not contain any malicious code, viruses, or malware. The permissions are necessary for the core functionality and are only used when explicitly enabled by the user.

Please reconsider the blocking of this application.
