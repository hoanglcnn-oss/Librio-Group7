import test from 'node:test'
import assert from 'node:assert'
import { parseCockpitPage, parseCockpitSize } from './cockpitFilters.js'

test('parseCockpitPage normalizes invalid or negative pages to 0', () => {
  assert.strictEqual(parseCockpitPage('-1'), 0)
  assert.strictEqual(parseCockpitPage('abc'), 0)
  assert.strictEqual(parseCockpitPage(''), 0)
  assert.strictEqual(parseCockpitPage(null), 0)
  assert.strictEqual(parseCockpitPage('-99'), 0)
})

test('parseCockpitPage keeps valid pages', () => {
  assert.strictEqual(parseCockpitPage('0'), 0)
  assert.strictEqual(parseCockpitPage('1'), 1)
  assert.strictEqual(parseCockpitPage('42'), 42)
})

test('parseCockpitSize normalizes out-of-bounds or invalid sizes to 20', () => {
  assert.strictEqual(parseCockpitSize('0'), 20)
  assert.strictEqual(parseCockpitSize('-1'), 20)
  assert.strictEqual(parseCockpitSize('500'), 20)
  assert.strictEqual(parseCockpitSize('101'), 20)
  assert.strictEqual(parseCockpitSize('abc'), 20)
  assert.strictEqual(parseCockpitSize(''), 20)
  assert.strictEqual(parseCockpitSize(null), 20)
})

test('parseCockpitSize normalizes non-standard allowed options to 20', () => {
  assert.strictEqual(parseCockpitSize('30'), 20)
  assert.strictEqual(parseCockpitSize('99'), 20)
})

test('parseCockpitSize keeps valid allowed sizes', () => {
  assert.strictEqual(parseCockpitSize('20'), 20)
  assert.strictEqual(parseCockpitSize('50'), 50)
  assert.strictEqual(parseCockpitSize('100'), 100)
})
