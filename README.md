# 🔁 SwapIt  
### A Swipe-Based Marketplace for Exchanging Second-Hand Items  
**Android • Kotlin • Firebase • MVVM • Secure by Design**

---

## 📱 Overview

**SwapIt** is a fully functional Android application that enables users to discover, match, and chat to exchange second-hand items nearby.

Inspired by Tinder’s intuitive swipe interaction model, SwapIt transforms traditional marketplace browsing into a fast, engaging, and structured matching experience.

This project was developed as a Kotlin final project and engineered with production-level architecture, real-time synchronization, and secure Firebase integration.

---

## 🎯 Product Vision

SwapIt replaces static marketplace scrolling with:

- Swipe-driven discovery  
- Real-time matching  
- Structured user-item ownership  
- Location-based filtering  
- Integrated chat between matched users  

The result is a clean, responsive, and secure item-exchange platform.

---

# 🚀 Full Feature Set

## 🔐 Authentication & User Management

- Firebase Email & Password Authentication  
- Persistent session handling  
- Auth state lifecycle listener  
- Secure route protection  
- Automatic redirection for unauthenticated users  
- Structured user profile storage:
  - UID  
  - Username  
  - Email  
  - CreatedAt timestamp  
  - User-owned items  

---

## 🧾 Item Publishing System

Users can publish items with:

- Title  
- Description  
- Category selection  
- Multiple image uploads  
- Automatic timestamp  
- Owner association  

Images are uploaded securely to **Firebase Storage**.  
Metadata is stored in **Firebase Realtime Database**.

Each user’s items are organized under:

```plaintext
users/{uid}/userItems/{itemId}
```

And globally indexed under:

```plaintext
items/{itemId}
```

---

## ❤️ Swipe-Based Discovery Engine

- Smooth horizontal swipe gestures  
- Like / Dislike interaction  
- Real-time feed updates  
- Server-side swipe tracking  
- Local filtering of already-seen items  
- Automatic feed refresh when new items are available  

The swipe engine ensures users never see items they have already interacted with.

---

## 🎯 Location & Category Filtering

- Location-aware discovery  
- Radius-based filtering (nearby users within defined range)  
- Category-based filtering  
- Dynamic filtering applied to swipe feed  
- Optimized Firebase queries  

---

## 💬 Match & Chat System

- Mutual interest detection  
- Real-time match creation  
- Dedicated chat screen per match  
- Firebase-backed message storage  
- Live message updates  
- Organized chat structure per conversation  
- Secure access control for participants only  

---

## 👤 Profile & My Items Screen

- Display user information  
- View all published items  
- Edit or remove items  
- Image editing support  
- Clean scrollable UI  
- Real-time synchronization  

---

# 🏗 Architecture

## 🧠 Design Pattern

- MVVM (Model-View-ViewModel)  
- Repository Pattern  
- LiveData for reactive UI updates  

---

## 🧩 Layer Structure

```
UI (Fragments & Activities)
        ↓
ViewModels
        ↓
ItemRepository / ChatRepository
        ↓
Firebase Realtime Database & Storage
```

---

# 🔒 Security Implementation

Security was implemented at both database and storage levels.

## 🔐 Firebase Realtime Database Rules

- `auth != null` required for all reads and writes  
- Users can only modify their own:
  - Profile data  
  - Items  
  - Swipes  
  - Messages  
- Ownership validation enforced  
- Structured path isolation to prevent privilege escalation  

---

## 🗄 Storage Security

- User-scoped storage paths  
- Controlled upload locations  
- Size restrictions enforced  
- Authentication validation via storage rules  

---

# 🛠 Tech Stack

- Kotlin  
- Android SDK  
- Firebase Authentication  
- Firebase Realtime Database  
- Firebase Storage  
- Android Navigation Component  
- ViewBinding  
- Coil (image loading)  
- ConstraintLayout  
- RecyclerView  

---



# 🎨 UX & Interaction Design

- Gesture-first interface  
- Minimal friction onboarding  
- Scrollable fragments for full content visibility  
- Clean empty states  
- Smooth UI animations  
- Consistent visual hierarchy  
- Modern Material Design components  

---

# 🧪 Engineering Challenges Solved

- Real-time listener lifecycle management  
- Firebase upload session handling  
- Swipe filtering synchronization  
- Structured user-item relational mapping  
- Scrollable layout corrections  
- Image rendering consistency  
- Auth state transition safety  

---



# 📸 Application Screens

<table>
<tr>
<td align="center" width="50%">

### 🔑 Authentication  
Secure email/password login with session handling.

<img src="https://github.com/user-attachments/assets/dfd30037-b9bc-47b6-8b17-1e0b96f13e60" width="250"/>

</td>
<td align="center" width="50%">

### 🏠 Swipe Discovery  
Tinder-style swipe engine with real-time filtering.

<img src="https://github.com/user-attachments/assets/62feb05c-d8be-4766-b8b1-0629a120b49e" width="250"/>

</td>
</tr>

<tr>
<td align="center">

### ➕ Add Item  
Publish items with images, category and description.

<img src="https://github.com/user-attachments/assets/d6fa3a39-cd7e-4c12-8c62-f6eeb0e35283" width="250"/>

</td>
<td align="center">

### 📦 Item Details  
Detailed item view with swipe and match logic.

<img src="https://github.com/user-attachments/assets/aa9a8d61-6d2d-4924-bc2b-55659288e5d6" width="250"/>

</td>
</tr>

<tr>
<td align="center">

### 🤝 Match Confirmation  
Mutual interest detection and match creation.

<img src="https://github.com/user-attachments/assets/828d829d-9cf5-47e6-8bf2-0fabba9bfa73" width="250"/>

</td>
<td align="center">

### 💬 Chat  
Real-time messaging between matched users.

<img src="https://github.com/user-attachments/assets/a3211458-9344-4fa7-af3e-264acdc32b82" width="250"/>

</td>
</tr>

<tr>
<td align="center">

### 📦 My Items  
Manage and edit published items.

<img src="https://github.com/user-attachments/assets/3ae8373c-6c62-4221-a57a-22fabab7f036" width="250"/>

</td>
<td align="center">

### 👤 Profile  
User profile with account information.

<img src="https://github.com/user-attachments/assets/e1b4680a-8a6d-490b-8404-f652e90a4823" width="250"/>

</td>
</tr>
</table>


