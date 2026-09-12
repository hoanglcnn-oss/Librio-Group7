import assert from 'node:assert/strict'
import test from 'node:test'
import { emptyResourceForm, resourceFormToPayload, validateResourceForm, resourceToForm, applyMetadataPrefill } from './resourceForm.js'

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


test('applyMetadataPrefill applies provider fields and preserves others', () => {
  const current = {
    ...emptyResourceForm,
    hasPhysical: true,
    category: 'Science',
    title: 'Old Title',
    authors: 'Old Author',
    description: 'Old Desc'
  }
  const meta = {
    title: 'New Title',
    authors: ['A', 'B'],
    description: 'New Desc',
    isbn: '123',
    coverImageUrl: 'img',
    externalSourceId: 'ext123'
  }
  const result = applyMetadataPrefill(current, meta)
  assert.equal(result.title, 'New Title')
  assert.equal(result.authors, 'A, B')
  assert.equal(result.description, 'New Desc')
  assert.equal(result.isbn, '123')
  assert.equal(result.coverImageUrl, 'img')
  assert.equal(result.externalSourceId, 'ext123')
  assert.equal(result.metadataSource, 'GOOGLE_BOOKS')
  
  // preserves
  assert.equal(result.hasPhysical, true)
  assert.equal(result.category, 'Science')
})

test('applyMetadataPrefill ignores missing optional fields', () => {
  const current = {
    ...emptyResourceForm,
    title: 'Old Title',
    authors: 'Old Author',
    description: 'Old Desc'
  }
  const meta = {
    title: 'New Title',
    isbn: '123'
  }
  const result = applyMetadataPrefill(current, meta)
  assert.equal(result.title, 'New Title')
  assert.equal(result.authors, 'Old Author')
  assert.equal(result.description, 'Old Desc')
  assert.equal(result.isbn, '123')
})