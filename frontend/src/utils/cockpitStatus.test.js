import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatAttentionReason,
  formatInventoryStatus,
  formatCirculationStatus,
  formatOperationStatus,
  formatDate,
} from './cockpitStatus.js'

test('formatAttentionReason formats backend reason codes to Vietnamese labels', () => {
  assert.equal(formatAttentionReason('INVENTORY_LOST'), 'Mất sách')
  assert.equal(formatAttentionReason('INVENTORY_DAMAGED'), 'Sách hỏng')
  assert.equal(formatAttentionReason('LOCATION_UNASSIGNED'), 'Chưa gán vị trí')
  assert.equal(formatAttentionReason('BORROWING_OVERDUE'), 'Mượn quá hạn')
  assert.equal(formatAttentionReason('CIRCULATION_DATA_MISMATCH'), 'Bất thường dữ liệu lưu thông')
  assert.equal(formatAttentionReason('UNKNOWN_REASON'), 'UNKNOWN_REASON')
})

test('formatInventoryStatus formats status codes correctly', () => {
  assert.equal(formatInventoryStatus('ACTIVE'), 'Đang hoạt động')
  assert.equal(formatInventoryStatus('LOST'), 'Đã báo mất')
  assert.equal(formatInventoryStatus('DAMAGED'), 'Hỏng hóc')
  assert.equal(formatInventoryStatus('WITHDRAWN'), 'Đã rút khỏi lưu thông')
})

test('formatCirculationStatus formats circulation codes correctly', () => {
  assert.equal(formatCirculationStatus('AVAILABLE'), 'Có sẵn')
  assert.equal(formatCirculationStatus('RESERVED'), 'Đã giữ chỗ')
  assert.equal(formatCirculationStatus('BORROWED'), 'Đang mượn')
})

test('formatOperationStatus formats request status codes', () => {
  assert.equal(formatOperationStatus('REQUESTED'), 'Chờ xử lý')
  assert.equal(formatOperationStatus('READY_FOR_PICKUP'), 'Sẵn sàng nhận')
})

test('formatDate safely formats valid dates and handles empty values', () => {
  assert.equal(formatDate(null), '-')
  assert.equal(formatDate(''), '-')
  assert.notEqual(formatDate('2026-09-11T12:00:00Z'), '-')
})
