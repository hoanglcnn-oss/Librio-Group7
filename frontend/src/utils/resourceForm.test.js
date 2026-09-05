import assert from 'node:assert/strict'
import test from 'node:test'
import { emptyResourceForm, resourceFormToPayload, validateResourceForm } from './resourceForm.js'

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
