import { expect, test } from "@playwright/test"

test("login page renders", async ({ page }) => {
    await page.goto("/login")

    await expect(page.getByText("Enter your credentials to access your account.")).toBeVisible()
    await expect(page.getByLabel("Email")).toBeVisible()
    await expect(page.getByLabel("Password")).toBeVisible()
    await expect(page.getByRole("button", { name: "Login" })).toBeVisible()
})