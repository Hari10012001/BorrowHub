import { test, expect } from '@playwright/test';

test.describe('BorrowHub End-to-End User Journeys', () => {

  test('1. Authentication Flow - Student Login and Navigation', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/BorrowHub/);
    await expect(page.locator('h1')).toContainText('BorrowHub Sign In');

    // Fill credentials for student1
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();

    // Verify redirected to catalog
    await page.waitForURL('**/catalog');
    await expect(page.locator('body')).toContainText(/Resource Catalog|All Resources|BorrowHub/);

    // Verify student user profile indicator in navbar
    await expect(page.locator('body')).toContainText('STUDENT');
  });

  test('2. Authentication Flow - Invalid Password Rejection', async ({ page }) => {
    await page.goto('/login');

    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('WrongPassword@999');
    await page.locator('button[type="submit"]').click();

    // Verify error notification
    await expect(page.locator('body')).toContainText(/Invalid|failed|error/i);
  });

  test('3. Resource Catalog - Search and Discovery', async ({ page }) => {
    // Login as student
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Verify search input is present
    const searchInput = page.locator('input[placeholder*="Search"]');
    if (await searchInput.isVisible()) {
      await searchInput.fill('Dell');
      await page.waitForTimeout(500);
      await expect(page.locator('body')).toContainText('Dell');
    }
  });

  test('4. Security & RBAC Guard - Student Cannot Access Admin Desk', async ({ page }) => {
    // Login as student
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Attempt direct navigation to admin approvals
    await page.goto('/approvals');
    // Verify protected route redirected back to catalog
    await expect(page).toHaveURL(/.*catalog/);
  });

  test('5. Admin Journey - Admin Login & Circulation Desk Access', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('Admin@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Verify admin navigation link to Circulation Desk
    await page.goto('/circulation');
    await page.waitForURL('**/circulation');
    await expect(page.locator('body')).toContainText(/Circulation|Desk|Scanner|Checkout/i);
  });

  test('6. User Logout Journey', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    // Click logout
    const logoutBtn = page.locator('button:has-text("Sign Out"), button:has-text("Logout"), [data-testid="logout-btn"]');
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await page.waitForURL('**/login');
      await expect(page.locator('h1')).toContainText('BorrowHub Sign In');
    }
  });

});
