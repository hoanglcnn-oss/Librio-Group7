import assert from 'node:assert/strict'
import test from 'node:test'
import { createMockPdfBlob } from './mockDigitalAccess.js'

test('creates a PDF payload for the T-125 preview', async () => {
  const blob = createMockPdfBlob()
  const content = await blob.text()
  assert.equal(blob.type, 'application/pdf')
  assert.match(content, /^%PDF-1\.4/)
  assert.match(content, /%%EOF$/)
})
