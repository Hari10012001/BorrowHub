import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const SEED_STUDENT_PW = process.env.BORROWHUB_SEED_STUDENT_PASSWORD || 'Student@123';
const SEED_FACULTY_PW = process.env.BORROWHUB_SEED_FACULTY_PASSWORD || 'Faculty@123';
const SEED_ADMIN_PW = process.env.BORROWHUB_SEED_ADMIN_PASSWORD || 'Admin@123';

test.describe('BorrowHub Comprehensive Live Browser Demo Verification', () => {
  const browserErrors = [];

  test.beforeEach(async ({ page }) => {
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        browserErrors.push(`[Console Error] ${msg.text()}`);
      }
    });
    page.on('pageerror', (err) => {
      browserErrors.push(`[Page Error] ${err.message}`);
    });
  });

  test('1. Role Verification - Student Login & Navigation Restrictions', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/BorrowHub/);

    // Login as Student
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill(SEED_STUDENT_PW);
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Verify Student Navbar & Sidebar Navigation
    await expect(page.locator('body')).toContainText('STUDENT');
    await expect(page.locator('aside')).toContainText('Browse Catalog');
    await expect(page.locator('aside')).toContainText('My Borrowings');
    await expect(page.locator('aside')).toContainText('My Reservations');
    await expect(page.locator('aside')).toContainText('My Waitlist');

    // Verify Forbidden Links are NOT in Sidebar
    await expect(page.locator('aside')).not.toContainText('Circulation Desk');
    await expect(page.locator('aside')).not.toContainText('Pending Approvals');
    await expect(page.locator('aside')).not.toContainText('Administration');
    await expect(page.locator('aside')).not.toContainText('Inventory & Assets');

    // Attempt Direct URL Navigation to Admin Routes
    await page.goto('/circulation');
    await page.waitForURL('**/catalog'); // Guard redirect
    await page.goto('/approvals');
    await page.waitForURL('**/catalog'); // Guard redirect
    await page.goto('/admin/resources');
    await page.waitForURL('**/catalog'); // Guard redirect
  });

  test('2. Role Verification - Faculty Login & Navigation Permissions', async ({ page }) => {
    await page.goto('/login');

    // Login as Faculty
    await page.locator('input[type="text"]').fill('faculty1');
    await page.locator('input[type="password"]').fill(SEED_FACULTY_PW);
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Verify Faculty Role Indicator
    await expect(page.locator('body')).toContainText('FACULTY');

    // Verify Permitted Faculty Links
    await expect(page.locator('aside')).toContainText('Browse Catalog');
    await expect(page.locator('aside')).toContainText('My Borrowings');
    await expect(page.locator('aside')).toContainText('Circulation Desk');
    await expect(page.locator('aside')).toContainText('Issue & Returns');
    await expect(page.locator('aside')).toContainText('Pending Approvals');

    // Verify Admin-Only Link is NOT present
    await expect(page.locator('aside')).not.toContainText('Administration');
    await expect(page.locator('aside')).not.toContainText('Inventory & Assets');

    // Verify Access to Pending Approvals Page
    await page.goto('/approvals');
    await page.waitForURL('**/approvals');
    await expect(page.locator('h1')).toContainText(/Approvals|Requests/i);

    // Verify Guard Redirect on Admin Resources Page
    await page.goto('/admin/resources');
    await page.waitForURL('**/catalog');
  });

  test('3. Role Verification - Admin Login & Full Platform Access', async ({ page }) => {
    await page.goto('/login');

    // Login as Admin
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill(SEED_ADMIN_PW);
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Verify Admin Role Indicator
    await expect(page.locator('body')).toContainText('ADMIN');

    // Verify Full Sidebar Navigation
    await expect(page.locator('aside')).toContainText('Browse Catalog');
    await expect(page.locator('aside')).toContainText('Circulation Desk');
    await expect(page.locator('aside')).toContainText('Issue & Returns');
    await expect(page.locator('aside')).toContainText('Pending Approvals');
    await expect(page.locator('aside')).toContainText('Administration');
    await expect(page.locator('aside')).toContainText('Inventory & Assets');

    // Verify Access to Circulation Desk
    await page.goto('/circulation');
    await page.waitForURL('**/circulation');
    await expect(page.locator('h1')).toContainText(/Circulation Desk/i);

    // Verify Access to Inventory & Asset Management
    await page.goto('/admin/resources');
    await page.waitForURL('**/admin/resources');
    await expect(page.locator('h1')).toContainText(/Inventory|Resource Management/i);
  });

  test('4. Browser Console & Runtime Stability Check', async ({ page }) => {
    // Assert no fatal uncaught JavaScript exceptions or console errors occurred
    expect(browserErrors.length).toBe(0);
  });
});
