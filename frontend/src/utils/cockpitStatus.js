export const ATTENTION_REASON_LABELS = {
  INVENTORY_LOST: 'Mất sách',
  INVENTORY_DAMAGED: 'Sách hỏng',
  LOCATION_UNASSIGNED: 'Chưa gán vị trí',
  BORROWING_OVERDUE: 'Mượn quá hạn',
  CIRCULATION_DATA_MISMATCH: 'Bất thường dữ liệu lưu thông',
}

export const INVENTORY_STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  LOST: 'Đã báo mất',
  DAMAGED: 'Hỏng hóc',
  WITHDRAWN: 'Đã rút khỏi lưu thông',
}

export const CIRCULATION_STATUS_LABELS = {
  AVAILABLE: 'Có sẵn',
  RESERVED: 'Đã giữ chỗ',
  BORROWED: 'Đang mượn',
}

export function formatAttentionReason(reason) {
  return ATTENTION_REASON_LABELS[reason] || reason
}

export function formatInventoryStatus(status) {
  return INVENTORY_STATUS_LABELS[status] || status
}

export function formatCirculationStatus(status) {
  return CIRCULATION_STATUS_LABELS[status] || status
}

export function formatOperationStatus(status) {
  const map = {
    REQUESTED: 'Chờ xử lý',
    READY_FOR_PICKUP: 'Sẵn sàng nhận',
  }
  return map[status] || status
}

export function formatDate(value) {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString('vi-VN')
  } catch {
    return String(value)
  }
}
