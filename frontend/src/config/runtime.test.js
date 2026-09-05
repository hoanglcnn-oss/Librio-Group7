import assert from 'node:assert/strict'
import test from 'node:test'

test('production environment file uses a same-origin API and disables mocks', async () => {
  const env = await import('node:fs/promises').then((fs) => fs.readFile(new URL('../../.env.production', import.meta.url), 'utf8'))
  assert.match(env, /^VITE_API_BASE_URL=\/api$/m)
  assert.doesNotMatch(env, /=true$/m)
  assert.doesNotMatch(env, /localhost|password|secret/i)
})
