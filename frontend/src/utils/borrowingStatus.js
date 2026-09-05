export function getBorrowingPresentation(borrowing) {
  const overdue = borrowing?.overdue === true

  return {
    overdue,
    requestLabel: overdue ? 'Quá hạn' : 'Đang mượn',
    itemLabel: overdue ? 'QUÁ HẠN' : 'ĐANG MƯỢN',
  }
}
