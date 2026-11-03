import { expect, test } from '@playwright/test'

const user = {
  id: 1,
  name: 'Administrador de Teste',
  email: 'admin@redekrill.com',
  role: 'ADMIN',
  active: true,
  mustChangePassword: false,
  createdAt: '2026-01-01T00:00:00Z'
}

async function mockCsrf(page) {
  await page.route('**/api/auth/csrf', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'e2e-csrf-token',
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf'
      })
    })
  )
}

test('redirects to the safe internal destination after login', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 401, contentType: 'application/json', body: '{}' })
  )
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: null, tokenType: null, expiresIn: 900, user })
    })
  )
  await page.route('**/api/appointments/capacity*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        date: '2026-11-02',
        dailyPallets: 0,
        dailyLimit: 480,
        dailyOccupancyPercentage: 0,
        slots: []
      })
    })
  )

  await page.goto('/login?redirect=//evil.example/phishing')
  await page.locator('#email').fill(user.email)
  await page.locator('#password').fill('SenhaSegura@2026')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  expect(new URL(page.url()).host).toBe('localhost:5173')
})

test('blocks navigation until the temporary password is changed', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 401, contentType: 'application/json', body: '{}' })
  )
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: null,
        tokenType: null,
        expiresIn: 900,
        user: { ...user, mustChangePassword: true }
      })
    })
  )

  await page.goto('/login')
  await page.locator('#email').fill(user.email)
  await page.locator('#password').fill('SenhaTemporaria@2026')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page).toHaveURL(/\/change-password$/)
  await expect(page.getByRole('heading', { name: 'Troque sua senha' })).toBeVisible()
})

test('shows the API message for invalid credentials', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 401, contentType: 'application/json', body: '{}' })
  )
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'E-mail ou senha inválidos.' })
    })
  )

  await page.goto('/login')
  await page.locator('#email').fill(user.email)
  await page.locator('#password').fill('SenhaErrada')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page.getByRole('alert')).toContainText('E-mail ou senha inválidos.')
  await expect(page).toHaveURL(/\/login$/)
})
