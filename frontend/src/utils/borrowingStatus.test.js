import assert from 'node:assert/strict'
import test from 'node:test'
import { getBorrowingPresentation } from './borrowingStatus.js'

test('renders overdue only when the server flag is true', () => {
  assert.deepEqual(getBorrowingPresentation({ overdue: true }), {
    overdue: true,
    requestLabel: 'Quá hạn',
    itemLabel: 'QUÁ HẠN',
  })
})

test('does not infer overdue from a past due date', () => {
  const presentation = getBorrowingPresentation({
    dueDate: '2000-01-01T00:00:00Z',
    overdue: false,
  })

  assert.equal(presentation.overdue, false)
  assert.equal(presentation.requestLabel, 'Đang mượn')
})

test('fails safe when an older backend omits the overdue flag', () => {
  assert.equal(getBorrowingPresentation({}).overdue, false)
})
