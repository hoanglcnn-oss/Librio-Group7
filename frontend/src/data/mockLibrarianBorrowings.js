let activeBorrowings = [
  {
    id: 2001,
    borrowRequestId: 1001,
    reader: { id: 1, displayName: 'Nguyễn Minh Anh', email: 'reader@librio.local' },
    resource: { id: 1, title: 'Clean Code', authors: ['Robert C. Martin'] },
    physicalItemId: 101,
    borrowedAt: '2026-08-20T09:00:00+07:00',
    dueDate: '2026-09-03T09:00:00+07:00',
    overdue: false,
  },
  {
    id: 2002,
    borrowRequestId: 1002,
    reader: { id: 2, displayName: 'Trần Gia Huy', email: 'reader2@librio.local' },
    resource: { id: 2, title: 'Design Patterns', authors: ['Erich Gamma'] },
    physicalItemId: 201,
    borrowedAt: '2026-08-05T14:30:00+07:00',
    dueDate: '2026-08-19T14:30:00+07:00',
    overdue: true,
  },
]

export async function getMockLibrarianBorrowings() {
  return { activeBorrowings: [...activeBorrowings] }
}

export async function returnMockLibrarianBorrowing(borrowingId) {
  const borrowing = activeBorrowings.find((item) => item.id === borrowingId)
  if (!borrowing) {
    const error = new Error('Lượt mượn không còn hoạt động.')
    error.status = 409
    throw error
  }

  activeBorrowings = activeBorrowings.filter((item) => item.id !== borrowingId)
  return { ...borrowing, returnedAt: new Date().toISOString() }
}
