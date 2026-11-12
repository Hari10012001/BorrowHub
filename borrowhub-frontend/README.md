# BorrowHub — Frontend Web Application

## Overview
The **borrowhub-frontend** module is a responsive, modern single-page application built with React 19/18, Vite 8.3, and Tailwind CSS 3.4. It delivers role-based dashboards and operational workflows for Students, Faculty, and Administrators.

## Architecture Highlights
- **Build Tool:** Vite 8.3
- **Styling:** Tailwind CSS 3.4
- **Routing:** React Router DOM 7
- **HTTP Client:** Axios with JWT bearer interceptors and global error handling
- **State Management:** React Context API (`AuthContext`)
- **Icons:** Lucide React

## Views & Routes
- `/login` — Secure user authentication
- `/register` — Self-registration for Students and Faculty
- `/catalog` — Resource catalog search, filtering, and live stock indicators
- `/resources/:id` — Resource details, unit tracking, and borrowing/reservation actions
- `/my-borrowings` — Active and historical borrowings, return dates, and outstanding quantities
- `/my-reservations` — Time-windowed reservations and cancellation actions
- `/my-waitlist` — Real-time waitlist queue position
- `/circulation` — Circulation Desk checkout and return scanner (Admin / Faculty)
- `/pending-approvals` — Administrative borrowing request approvals/rejections

## Running & Building
```bash
# Install dependencies
npm install

# Start development server with proxy to :8080
npm run dev

# Build production bundle
npm run build

# Preview production build
npm run preview
```
Port: `5173` (proxies `/api` to `http://localhost:8080`)
