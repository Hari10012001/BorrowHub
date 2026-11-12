import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import * as fs from 'fs';
import * as path from 'path';

test.describe('Automated WCAG Accessibility Audit (Axe-Core)', () => {
  const auditResults = {};

  test.afterAll(async () => {
    const reportPath = path.resolve('../qa/accessibility-audit-results.json');
    fs.writeFileSync(reportPath, JSON.stringify(auditResults, null, 2));
    console.log(`[A11y] Complete accessibility audit written to ${reportPath}`);
  });

  test('1. Axe-Core Accessibility Audit: Sign-In / Login Page', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    const violationsBySeverity = {
      critical: accessibilityScanResults.violations.filter(v => v.impact === 'critical'),
      serious: accessibilityScanResults.violations.filter(v => v.impact === 'serious'),
      moderate: accessibilityScanResults.violations.filter(v => v.impact === 'moderate'),
      minor: accessibilityScanResults.violations.filter(v => v.impact === 'minor'),
    };

    auditResults['LoginPage'] = {
      url: '/login',
      violationsCount: accessibilityScanResults.violations.length,
      violations: accessibilityScanResults.violations.map(v => ({
        id: v.id,
        impact: v.impact,
        description: v.description,
        help: v.help,
        nodes: v.nodes.length
      })),
      severitySummary: {
        critical: violationsBySeverity.critical.length,
        serious: violationsBySeverity.serious.length,
        moderate: violationsBySeverity.moderate.length,
        minor: violationsBySeverity.minor.length
      }
    };

    console.log(`[A11y Login] Violations: ${accessibilityScanResults.violations.length} (Critical: ${violationsBySeverity.critical.length}, Serious: ${violationsBySeverity.serious.length}, Moderate: ${violationsBySeverity.moderate.length}, Minor: ${violationsBySeverity.minor.length})`);
    
    // Verify zero critical and zero serious violations
    expect(violationsBySeverity.critical.length).toBe(0);
    expect(violationsBySeverity.serious.length).toBe(0);
  });

  test('2. Axe-Core Accessibility Audit: Resource Catalog Page (Authenticated)', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');
    await page.waitForLoadState('networkidle');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    const violationsBySeverity = {
      critical: accessibilityScanResults.violations.filter(v => v.impact === 'critical'),
      serious: accessibilityScanResults.violations.filter(v => v.impact === 'serious'),
      moderate: accessibilityScanResults.violations.filter(v => v.impact === 'moderate'),
      minor: accessibilityScanResults.violations.filter(v => v.impact === 'minor'),
    };

    auditResults['ResourceCatalogPage'] = {
      url: '/catalog',
      violationsCount: accessibilityScanResults.violations.length,
      violations: accessibilityScanResults.violations.map(v => ({
        id: v.id,
        impact: v.impact,
        description: v.description,
        help: v.help,
        nodes: v.nodes.length
      })),
      severitySummary: {
        critical: violationsBySeverity.critical.length,
        serious: violationsBySeverity.serious.length,
        moderate: violationsBySeverity.moderate.length,
        minor: violationsBySeverity.minor.length
      }
    };

    console.log(`[A11y Catalog] Violations: ${accessibilityScanResults.violations.length} (Critical: ${violationsBySeverity.critical.length}, Serious: ${violationsBySeverity.serious.length}, Moderate: ${violationsBySeverity.moderate.length}, Minor: ${violationsBySeverity.minor.length})`);

    accessibilityScanResults.violations.forEach(v => {
      console.log(`  Violation [${v.impact}]: ${v.id} - ${v.help}`);
      v.nodes.forEach(n => console.log(`    Target: ${n.target.join(' ')} | HTML: ${n.html}`));
    });

    expect(violationsBySeverity.critical.length).toBe(0);
    expect(violationsBySeverity.serious.length).toBe(0);
  });

  test('3. Axe-Core Accessibility Audit: Student Borrowings Page', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('student1');
    await page.locator('input[type="password"]').fill('Student@123');
    await page.locator('button[type="submit"]').click();
    await page.waitForURL('**/catalog');

    await page.goto('/borrowings');
    await page.waitForURL('**/borrowings');
    await page.waitForLoadState('networkidle');

    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();

    const violationsBySeverity = {
      critical: accessibilityScanResults.violations.filter(v => v.impact === 'critical'),
      serious: accessibilityScanResults.violations.filter(v => v.impact === 'serious'),
      moderate: accessibilityScanResults.violations.filter(v => v.impact === 'moderate'),
      minor: accessibilityScanResults.violations.filter(v => v.impact === 'minor'),
    };

    auditResults['StudentBorrowingsPage'] = {
      url: '/borrowings',
      violationsCount: accessibilityScanResults.violations.length,
      violations: accessibilityScanResults.violations.map(v => ({
        id: v.id,
        impact: v.impact,
        description: v.description,
        help: v.help,
        nodes: v.nodes.length
      })),
      severitySummary: {
        critical: violationsBySeverity.critical.length,
        serious: violationsBySeverity.serious.length,
        moderate: violationsBySeverity.moderate.length,
        minor: violationsBySeverity.minor.length
      }
    };

    console.log(`[A11y Borrowings] Violations: ${accessibilityScanResults.violations.length} (Critical: ${violationsBySeverity.critical.length}, Serious: ${violationsBySeverity.serious.length}, Moderate: ${violationsBySeverity.moderate.length}, Minor: ${violationsBySeverity.minor.length})`);

    accessibilityScanResults.violations.forEach(v => {
      console.log(`  Violation [${v.impact}]: ${v.id} - ${v.help}`);
      v.nodes.slice(0, 10).forEach(n => console.log(`    Target: ${n.target.join(' ')} | HTML: ${n.html}`));
    });

    expect(violationsBySeverity.critical.length).toBe(0);
    expect(violationsBySeverity.serious.length).toBe(0);
  });
});
