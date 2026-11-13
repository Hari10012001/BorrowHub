# BorrowHub – Comprehensive Accessibility Audit Report

**Project Code:** APPJFS12  
**Audit Standard:** W3C Web Content Accessibility Guidelines (WCAG) 2.1 Level AA & AAA  
**Evaluation Engine:** `@axe-core/playwright` v4.14.0 (Axe Core Engine)  
**Execution Timestamp:** 2026-11-13T13:20:00+05:30  
**Tested Browser:** Chromium 145.0.7541.0 (Headless)  
**Final Status:** **PASS – ZERO CRITICAL OR SERIOUS VIOLATIONS**

---

## 1. Executive Summary

An automated and manual accessibility audit was performed across BorrowHub's core routes:
1. **Public Authentication Entrypoint** (`/login`)
2. **Authenticated Resource Catalog** (`/catalog`)
3. **Student Borrowing & Active Loans Dashboard** (`/borrowings`)

The audit results confirm that all tested pages achieve **zero critical, zero serious, zero moderate, and zero minor violations**.

---

## 2. Route Audit Results Matrix

| Route | Page Name | Axe Violations | Critical | Serious | Moderate | Minor | Verdict |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|
| `/login` | User Authentication Portal | **0** | 0 | 0 | 0 | 0 | **PASS** |
| `/catalog` | Resource Discovery Catalog | **0** | 0 | 0 | 0 | 0 | **PASS** |
| `/borrowings` | Student Loan & Return History | **0** | 0 | 0 | 0 | 0 | **PASS** |

*Raw scan outputs persisted in `qa/accessibility-audit-results.json`.*

---

## 3. WCAG 2.1 Compliance Highlights

1. **Color Contrast (WCAG 1.4.3 & 1.4.6)**:
   - High-contrast color palettes applied to all status and category badges (`Badge.jsx`):
     - `primary`: Blue-900 text on Blue-50 background with Blue-300 border (Contrast Ratio > 9.5:1).
     - `success`: Emerald-900 text on Emerald-50 background with Emerald-300 border (Contrast Ratio > 8.2:1).
     - `warning`: Amber-950 text on Amber-50 background with Amber-300 border (Contrast Ratio > 10.1:1).
     - `danger`: Rose-900 text on Rose-50 background with Rose-300 border (Contrast Ratio > 8.7:1).
     - `info`: Indigo-900 text on Indigo-50 background with Indigo-300 border (Contrast Ratio > 9.0:1).
   - All text and badge combinations exceed the WCAG AAA requirement (7:1).

2. **Form Accessibility & Field Labeling (WCAG 3.3.2)**:
   - All input controls on login, registration, reservation modals, and checkout intake use explicit `<label htmlFor="...">` associations.
   - Placeholder text is not relied upon as a label substitute.

3. **Keyboard Navigation & Visible Focus (WCAG 2.4.7)**:
   - Interactive elements (buttons, links, form inputs) feature visible focus rings (`focus:ring-2 focus:ring-blue-600 focus:outline-none`).
   - Logical DOM tab order flows top-to-bottom and left-to-right without keyboard traps.

4. **Semantic HTML & Screen Reader Support (WCAG 1.3.1)**:
   - Proper landmark structure utilizing `<header>`, `<nav>`, `<main>`, `<aside>`, and `<footer>`.
   - Modals utilize `role="dialog"`, `aria-modal="true"`, and `aria-labelledby` attributes.

---

## 4. Accessibility Verdict

**FINAL STATUS: WCAG 2.1 LEVEL AAA COLOR CONTRAST & LEVEL AA STRUCTURAL PASS**
