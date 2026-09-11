import assert from 'node:assert/strict'
import test from 'node:test'
import { emptyResourceForm, resourceFormToPayload, validateResourceForm, resourceToForm } from './resourceForm.js'

test('requires title, author and at least one access type', () => {
  const errors = validateResourceForm(emptyResourceForm)
  assert.ok(errors.title)
  assert.ok(errors.authors)
  assert.ok(errors.accessTypes)
})

test('rejects invalid physical copy counts', () => {
  const errors = validateResourceForm({ ...emptyResourceForm, title: 'Sách', authors: 'Tác giả', hasPhysical: true, physicalCopies: '-1' })
  assert.ok(errors.physicalCopies)
})

test('normalizes a valid resource payload', () => {
  const payload = resourceFormToPayload({ ...emptyResourceForm, title: '  Sách mới  ', authors: 'An, Bình', hasDigital: true })
  assert.equal(payload.title, 'Sách mới')
  assert.deepEqual(payload.authors, ['An', 'Bình'])
  assert.deepEqual(payload.accessTypes, ['DIGITAL'])
})

test('resourceToForm includes persisted metadata', () => {
  const form = resourceToForm({
    title: 'Hello',
    isbn: '1234567890123',
    coverImageUrl: 'http://example.com/cover.jpg',
    metadataSource: 'GOOGLE_BOOKS',
    externalSourceId: 'abc123xyz'
  })
  assert.equal(form.isbn, '1234567890123')
  assert.equal(form.coverImageUrl, 'http://example.com/cover.jpg')
  assert.equal(form.metadataSource, 'GOOGLE_BOOKS')
  assert.equal(form.externalSourceId, 'abc123xyz')
})

test('resourceFormToPayload includes metadata and trims', () => {
  const payload = resourceFormToPayload({
    ...emptyResourceForm,
    title: 'A',
    authors: 'B',
    hasPhysical: true,
    isbn: ' 978-0-1234-5  ',
    coverImageUrl: ' url ',
    metadataSource: 'GOOGLE_BOOKS',
    externalSourceId: 'id'
  })
  assert.equal(payload.isbn, '978-0-1234-5')
  assert.equal(payload.coverImageUrl, 'url')
  assert.equal(payload.metadataSource, 'GOOGLE_BOOKS')
  assert.equal(payload.externalSourceId, 'id')
})

test('manual path remains MANUAL', () => {
  const payload = resourceFormToPayload({
    ...emptyResourceForm,
    title: 'A',
    authors: 'B',
    hasDigital: true,
  })
  assert.equal(payload.metadataSource, 'MANUAL')
  assert.equal(payload.isbn, null)
  assert.equal(payload.coverImageUrl, null)
})
