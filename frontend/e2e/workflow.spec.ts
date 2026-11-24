import { test, expect } from '@playwright/test';

test.describe('Emblemix End-to-End User Journeys', () => {
  test('User can authenticate and navigate dashboard', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByText('Sign in to Emblemix')).toBeVisible();

    await page.getByLabel(/email address/i).fill('admin@emblemix.com');
    await page.getByPlaceholder('••••••••').fill('Password123!');
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.getByText('Platform Overview')).toBeVisible();
    await expect(page.getByText('Connected Systems')).toBeVisible();
  });
});
