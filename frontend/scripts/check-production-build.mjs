import { access, readFile, readdir } from 'node:fs/promises'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const distDir = fileURLToPath(new URL('../dist/', import.meta.url))
await access(join(distDir, 'index.html'))

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const nested = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? collectFiles(path) : [path]
  }))
  return nested.flat()
}

const files = (await collectFiles(distDir)).filter((file) => /\.(?:html|js|css)$/.test(file))
// React Router itself contains an internal `http://localhost` URL-construction fallback.
// Reject application/server endpoints with local ports instead of dependency internals.
const forbidden = /localhost:8080|127\.0\.0\.1:\d+|VITE_USE_MOCK_[A-Z_]+/i
for (const file of files) {
  const content = await readFile(file, 'utf8')
  if (forbidden.test(content)) throw new Error(`Production bundle contains local/mock configuration: ${file}`)
}

console.log(`Production bundle verified: ${files.length} deployable files scanned.`)
